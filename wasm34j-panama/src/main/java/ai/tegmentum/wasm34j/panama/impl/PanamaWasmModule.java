package ai.tegmentum.wasm34j.panama.impl;

import ai.tegmentum.wasm34j.WebAssemblyInstance;
import ai.tegmentum.wasm34j.WebAssemblyModule;
import ai.tegmentum.wasm34j.exception.WasmException;
import ai.tegmentum.wasm34j.panama.internal.Wasm3Library;

import java.lang.foreign.MemorySegment;

/**
 * Panama-backed {@link WebAssemblyModule}, wrapping a parsed wasm3 {@code IM3Module}
 * pointer.
 *
 * <p>As with the JNI backend, wasm3 ties a module to a single runtime, so a module may
 * only be instantiated once.
 */
final class PanamaWasmModule implements WebAssemblyModule {

    private static final int DEFAULT_STACK_BYTES = 64 * 1024;

    private final MemorySegment environment;
    private final MemorySegment module;
    private boolean instantiated;

    PanamaWasmModule(final MemorySegment environment, final MemorySegment module) {
        this.environment = environment;
        this.module = module;
    }

    @Override
    public WebAssemblyInstance instantiate() {
        if (instantiated) {
            throw new WasmException("Module has already been instantiated");
        }
        final MemorySegment runtime = Wasm3Library.newRuntime(environment, DEFAULT_STACK_BYTES);
        if (runtime.address() == 0) {
            throw new WasmException("Failed to create wasm3 runtime");
        }
        Wasm3Library.loadModule(runtime, module);
        instantiated = true;
        return new PanamaWasmInstance(runtime);
    }

    @Override
    public void close() {
        // Once instantiated, the module is owned by its runtime and freed with it.
    }
}
