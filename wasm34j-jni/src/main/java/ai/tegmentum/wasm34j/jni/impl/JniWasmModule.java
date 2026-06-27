package ai.tegmentum.wasm34j.jni.impl;

import ai.tegmentum.wasm34j.WebAssemblyInstance;
import ai.tegmentum.wasm34j.WebAssemblyModule;
import ai.tegmentum.wasm34j.exception.WasmException;
import ai.tegmentum.wasm34j.jni.internal.Wasm3Native;

/**
 * JNI-backed {@link WebAssemblyModule}, wrapping a parsed wasm3 {@code IM3Module} handle.
 *
 * <p>wasm3 ties a module to a single runtime: {@link #instantiate()} creates a fresh
 * runtime and loads this module into it, transferring ownership of the module to that
 * runtime. As a result a module may only be instantiated once.
 */
final class JniWasmModule implements WebAssemblyModule {

    private static final int DEFAULT_STACK_BYTES = 64 * 1024;

    private final long environment;
    private final long module;
    private boolean instantiated;

    JniWasmModule(final long environment, final long module) {
        this.environment = environment;
        this.module = module;
    }

    @Override
    public WebAssemblyInstance instantiate() {
        if (instantiated) {
            throw new WasmException("Module has already been instantiated");
        }
        final long runtime = Wasm3Native.newRuntime(environment, DEFAULT_STACK_BYTES);
        if (runtime == 0) {
            throw new WasmException("Failed to create wasm3 runtime");
        }
        Wasm3Native.loadModule(runtime, module);
        instantiated = true;
        return new JniWasmInstance(runtime);
    }

    @Override
    public void close() {
        // Once instantiated, the module is owned by its runtime and is freed with it.
        // A never-instantiated module's native memory is reclaimed when the owning
        // environment is closed.
    }
}
