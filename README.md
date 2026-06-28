# wasm34j

Java bindings for the [wasm3](https://github.com/wasm3/wasm3) WebAssembly interpreter.

`wasm34j` is part of the `webassembly4j` runtime family (alongside `wasm4j`, `wamr4j`,
and `wasmtime4j`) and is designed to be integrated as another backend runtime in
[`webassembly4j`](https://github.com/tegmentum/webassembly4j) via the `EngineProvider` SPI.

It exposes a small, runtime-agnostic API and pluggable backends:

- **JNI** backend — thin C glue over the wasm3 C API, Java 8+. *(implemented)*
- **Panama** backend — `java.lang.foreign` (FFM) downcalls straight into wasm3, Java 22+. *(implemented)*

Both backends load the same bundled native library: the Panama backend links the raw
`m3_*` symbols exported by it and downcalls them directly, so no JNI glue is involved on
that path.

## Status

A working vertical slice on both backends: a `.wasm` module can be parsed, instantiated,
and have an exported function called. The highest-priority available backend is selected
automatically (Panama outranks JNI on Java 22+). See
`wasm34j-tests/.../AddIntegrationTest.java` (JNI) and
`wasm34j-panama/.../PanamaAddTest.java` (Panama).

## Modules

| Module           | Description                                                             |
|------------------|-------------------------------------------------------------------------|
| `wasm34j`        | Public, runtime-agnostic API (`WebAssemblyRuntime`, `RuntimeFactory`, SPI) |
| `wasm34j-native` | wasm3 (git submodule) + JNI glue, compiled via CMake and bundled per platform |
| `wasm34j-jni`    | JNI-backed implementation of the API (Java 8+)                          |
| `wasm34j-panama` | Panama (FFM) implementation of the API (Java 22+)                       |
| `wasm34j-tests`  | End-to-end integration tests                                            |

## Requirements

- JDK 8+ to consume; the build runs on a recent JDK (developed against JDK 25).
- CMake 3.16+ and a C toolchain (for building `wasm34j-native`).
- `JAVA_HOME` set so CMake can locate the JNI headers.

## Building

```bash
git clone --recurse-submodules <repo-url>
# or, if already cloned:
git submodule update --init --recursive

./mvnw clean install
```

To skip the native CMake build and use a pre-built library on `java.library.path`:

```bash
./mvnw -Pskip-native install
```

### Quality gates

Static analysis (Spotless / google-java-format AOSP, Checkstyle, SpotBugs + findsecbugs) is
off by default so the normal build stays fast. Run it explicitly:

```bash
./mvnw -Pquality verify     # check formatting + lint + bug patterns
./mvnw spotless:apply       # auto-format the source tree
```

### Cross-platform builds and releases

The native library is built per-platform on each OS's own runner in CI (see
`.github/workflows/ci.yml`); a local build only produces the host platform's library. The
release workflow downloads every platform's library and bundles them all into a single set
of jars (under `META-INF/native/<platform>/`), so one published artifact works everywhere.

### Publishing to Maven Central

Pushing a `v*` tag runs `.github/workflows/release.yml`, which builds all native libraries,
then signs and deploys `wasm34j`, `wasm34j-native`, `wasm34j-jni`, and `wasm34j-panama` to
Maven Central via the `release` profile (GPG signing + the `central-publishing-maven-plugin`,
with `flattenMode=ossrh` so each POM is self-contained), and attaches the jars + checksums
to the GitHub release.

Required repository secrets (Settings → Secrets and variables → Actions):

| Secret | Purpose |
|--------|---------|
| `MAVEN_CENTRAL_USERNAME` | Central Portal user token name |
| `MAVEN_CENTRAL_PASSWORD` | Central Portal user token password |
| `GPG_PRIVATE_KEY` | ASCII-armored GPG private key used to sign artifacts |
| `GPG_PASSPHRASE` | passphrase for that key |

To cut a release: ensure the secrets are set, then push a tag (the version is taken from the
tag, e.g. `v0.5.0-1.0.0` → `0.5.0-1.0.0`):

```bash
git tag -a v0.5.0-1.0.0 -m "wasm34j 0.5.0-1.0.0" && git push origin v0.5.0-1.0.0
```

## Android

Android's ART runtime has no `java.lang.foreign`, so only the **JNI** backend is used there.
The `wasm34j-android` module is a Gradle Android library that builds the wasm3 native library
per ABI (`arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86`) with the NDK — reusing the same
`wasm34j-native/CMakeLists.txt` via `externalNativeBuild` — and packages it into an AAR that
re-exports `wasm34j` + `wasm34j-jni`.

Requirements: Android SDK + NDK (r27+ recommended for 16 KB page alignment), `minSdk 26`,
and core library desugaring (enabled in the module). Build it after installing the Java
artifacts to the local Maven repo:

```bash
./mvnw -Pskip-native -DskipTests install -pl wasm34j-jni -am   # publish jars to ~/.m2
cd wasm34j-android && ./gradlew assembleRelease                 # -> build/outputs/aar/
./gradlew connectedDebugAndroidTest                            # on-device smoke test
```

Consuming app: depend on the AAR; `System.loadLibrary` resolves the bundled `.so` from the
APK, so no runtime extraction is involved. See `.github/workflows/android.yml`.

## Use from webassembly4j

A `wasm3-provider` module in [`webassembly4j`](https://github.com/tegmentum/webassembly4j)
adapts this library to the `EngineProvider` SPI, so wasm3 can be selected as a backend
there. It bundles both wasm34j backends and prefers Panama on Java 22+, falling back to JNI.

## Usage

```java
import ai.tegmentum.wasm34j.*;

byte[] wasm = Files.readAllBytes(Path.of("add.wasm"));

try (WebAssemblyRuntime runtime = RuntimeFactory.create();
     WebAssemblyModule module = runtime.compile(wasm);
     WebAssemblyInstance instance = module.instantiate()) {

    WebAssemblyFunction add = instance.getFunction("add");

    // Typed call:
    WasmValue[] result = add.call(WasmValue.i32(20), WasmValue.i32(22));
    int sum = result[0].asInt(); // 42

    // Or the convenience form:
    Object boxed = add.invoke(20, 22); // Integer 42
}
```

### Selecting a backend

The highest-priority available backend is chosen automatically. Force one with:

```bash
-Dwasm34j.runtime=jni
```

## Native build details

`wasm34j-native` vendors wasm3 as a git submodule (pinned to a release tag) under
`wasm34j-native/wasm3`. `CMakeLists.txt` compiles the wasm3 sources together with the JNI
glue in `src/wasm34j_jni.c` into `libwasm34j_native`, which the Maven build copies to
`META-INF/native/<platform>/` inside the jar. At runtime `NativeLibraryLoader` extracts and
loads the library for the host platform.

## License

Apache License 2.0.
