package ai.tegmentum.wasm34j.panama.impl;

import ai.tegmentum.wasm34j.WebAssemblyInstance;
import ai.tegmentum.wasm34j.WebAssemblyModule;
import ai.tegmentum.wasm34j.exception.WasmException;
import ai.tegmentum.wasm34j.panama.internal.Wasm3Library;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

/**
 * Panama-backed {@link WebAssemblyModule}, wrapping a parsed wasm3 {@code IM3Module}
 * pointer plus the {@link Arena} holding its wasm bytes.
 *
 * <p>As with the JNI backend, wasm3 ties a module to a single runtime, so a module may only
 * be instantiated once. {@link #instantiate()} transfers ownership of the module and its
 * arena to the resulting instance; a never-instantiated module frees both on {@link #close()}.
 */
final class PanamaWasmModule implements WebAssemblyModule {

    private static final int DEFAULT_STACK_BYTES = 64 * 1024;

    private final MemorySegment environment;
    private final MemorySegment module;
    private final Arena moduleArena;
    private boolean instantiated;
    private boolean closed;

    PanamaWasmModule(
            final MemorySegment environment, final MemorySegment module, final Arena moduleArena) {
        this.environment = environment;
        this.module = module;
        this.moduleArena = moduleArena;
    }

    @Override
    public WebAssemblyInstance instantiate() {
        if (instantiated) {
            throw new WasmException("Module has already been instantiated");
        }
        if (closed) {
            throw new WasmException("Module has been closed");
        }
        final MemorySegment runtime = Wasm3Library.newRuntime(environment, DEFAULT_STACK_BYTES);
        if (runtime.address() == 0) {
            throw new WasmException("Failed to create wasm3 runtime");
        }
        Wasm3Library.loadModule(runtime, module);
        instantiated = true;
        // Ownership of the module (now owned by the runtime) and the arena moves to the
        // instance, which closes the arena on close.
        return new PanamaWasmInstance(runtime, moduleArena);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (!instantiated) {
            Wasm3Library.freeModule(module);
            moduleArena.close();
        }
    }
}
