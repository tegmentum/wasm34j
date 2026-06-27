package ai.tegmentum.wasm34j.jni.impl;

import ai.tegmentum.wasm34j.WebAssemblyInstance;
import ai.tegmentum.wasm34j.WebAssemblyModule;
import ai.tegmentum.wasm34j.exception.WasmException;
import ai.tegmentum.wasm34j.jni.internal.Wasm3Native;

/**
 * JNI-backed {@link WebAssemblyModule}, wrapping a parsed wasm3 {@code IM3Module} handle
 * plus the native byte buffer backing its wasm bytes.
 *
 * <p>wasm3 ties a module to a single runtime: {@link #instantiate()} creates a fresh
 * runtime and loads this module into it, transferring ownership of both the module and the
 * byte buffer to the resulting {@link JniWasmInstance}. A module may therefore only be
 * instantiated once. If never instantiated, {@link #close()} frees the module and buffer.
 */
final class JniWasmModule implements WebAssemblyModule {

    private static final int DEFAULT_STACK_BYTES = 64 * 1024;

    private final long environment;
    private final long module;
    private final long buffer;
    private boolean instantiated;
    private boolean closed;

    JniWasmModule(final long environment, final long module, final long buffer) {
        this.environment = environment;
        this.module = module;
        this.buffer = buffer;
    }

    @Override
    public WebAssemblyInstance instantiate() {
        if (instantiated) {
            throw new WasmException("Module has already been instantiated");
        }
        if (closed) {
            throw new WasmException("Module has been closed");
        }
        final long runtime = Wasm3Native.newRuntime(environment, DEFAULT_STACK_BYTES);
        if (runtime == 0) {
            throw new WasmException("Failed to create wasm3 runtime");
        }
        Wasm3Native.loadModule(runtime, module);
        instantiated = true;
        // Ownership of the module (now owned by the runtime) and the byte buffer moves to
        // the instance, which frees them on close.
        return new JniWasmInstance(runtime, buffer);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (!instantiated) {
            Wasm3Native.freeModule(module);
            Wasm3Native.freeBuffer(buffer);
        }
    }
}
