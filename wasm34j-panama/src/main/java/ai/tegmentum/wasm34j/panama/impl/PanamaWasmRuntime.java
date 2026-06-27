package ai.tegmentum.wasm34j.panama.impl;

import ai.tegmentum.wasm34j.WebAssemblyModule;
import ai.tegmentum.wasm34j.WebAssemblyRuntime;
import ai.tegmentum.wasm34j.exception.WasmException;
import ai.tegmentum.wasm34j.internal.BuildInfo;
import ai.tegmentum.wasm34j.panama.internal.Wasm3Library;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

/**
 * Panama-backed {@link WebAssemblyRuntime}, wrapping a wasm3 {@code IM3Environment}
 * pointer. Closing frees the native environment.
 */
public final class PanamaWasmRuntime implements WebAssemblyRuntime {

    // wasm3 exposes its version only as a compile-time macro, so it is read from the
    // Maven-filtered build resource (shared with the JNI backend) rather than hardcoded.
    private static final String ENGINE_VERSION = BuildInfo.wasm3Version();

    private final MemorySegment environment;
    private boolean closed;

    public PanamaWasmRuntime() {
        this.environment = Wasm3Library.newEnvironment();
        if (environment.address() == 0) {
            throw new WasmException("Failed to create wasm3 environment");
        }
    }

    @Override
    public WebAssemblyModule compile(final byte[] wasmBytes) {
        ensureOpen();
        // The module's wasm bytes must outlive the module; they live in this arena, whose
        // ownership passes to the module (and then the instance).
        final Arena moduleArena = Arena.ofShared();
        try {
            final MemorySegment module =
                    Wasm3Library.parseModule(environment, wasmBytes, moduleArena);
            return new PanamaWasmModule(environment, module, moduleArena);
        } catch (final RuntimeException e) {
            moduleArena.close();
            throw e;
        }
    }

    @Override
    public String engineVersion() {
        return ENGINE_VERSION;
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            Wasm3Library.freeEnvironment(environment);
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new WasmException("Runtime has been closed");
        }
    }
}
