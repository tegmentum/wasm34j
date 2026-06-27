package ai.tegmentum.wasm34j.jni.impl;

import ai.tegmentum.wasm34j.WebAssemblyModule;
import ai.tegmentum.wasm34j.WebAssemblyRuntime;
import ai.tegmentum.wasm34j.exception.WasmException;
import ai.tegmentum.wasm34j.jni.internal.Wasm3Native;

/**
 * JNI-backed {@link WebAssemblyRuntime}, wrapping a wasm3 {@code IM3Environment} handle.
 * Closing frees the native environment.
 */
public final class JniWasmRuntime implements WebAssemblyRuntime {

    private final long environment;
    private boolean closed;

    public JniWasmRuntime() {
        this.environment = Wasm3Native.newEnvironment();
        if (environment == 0) {
            throw new WasmException("Failed to create wasm3 environment");
        }
    }

    @Override
    public WebAssemblyModule compile(final byte[] wasmBytes) {
        ensureOpen();
        final long module = Wasm3Native.parseModule(environment, wasmBytes);
        return new JniWasmModule(environment, module);
    }

    @Override
    public String engineVersion() {
        return Wasm3Native.version();
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            Wasm3Native.freeEnvironment(environment);
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new WasmException("Runtime has been closed");
        }
    }
}
