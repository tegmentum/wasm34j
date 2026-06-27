# wasm34j

Java bindings for the [wasm3](https://github.com/wasm3/wasm3) WebAssembly interpreter.

`wasm34j` is part of the `webassembly4j` runtime family (alongside `wasm4j`, `wamr4j`,
and `wasmtime4j`) and is designed to be integrated as another backend runtime in
[`webassembly4j`](https://github.com/tegmentum/webassembly4j) via the `EngineProvider` SPI.

It exposes a small, runtime-agnostic API and pluggable backends:

- **JNI** backend — thin C glue over the wasm3 C API, Java 8+. *(implemented)*
- **Panama** backend — `java.lang.foreign` (FFM) downcalls straight into wasm3, Java 22+. *(planned)*

## Status

A working vertical slice: a `.wasm` module can be parsed, instantiated, and have an
exported function called through the JNI backend. See
`wasm34j-tests/src/test/java/.../AddIntegrationTest.java`.

## Modules

| Module           | Description                                                             |
|------------------|-------------------------------------------------------------------------|
| `wasm34j`        | Public, runtime-agnostic API (`WebAssemblyRuntime`, `RuntimeFactory`, SPI) |
| `wasm34j-native` | wasm3 (git submodule) + JNI glue, compiled via CMake and bundled per platform |
| `wasm34j-jni`    | JNI-backed implementation of the API                                    |
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

mvn clean install
```

To skip the native CMake build and use a pre-built library on `java.library.path`:

```bash
mvn -Pskip-native install
```

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
