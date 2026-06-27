package ai.tegmentum.wasm34j.panama.impl;

import ai.tegmentum.wasm34j.WasmGlobal;
import ai.tegmentum.wasm34j.WebAssemblyFunction;
import ai.tegmentum.wasm34j.WebAssemblyInstance;
import ai.tegmentum.wasm34j.WebAssemblyMemory;
import ai.tegmentum.wasm34j.panama.internal.Wasm3Library;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Optional;

/**
 * Panama-backed {@link WebAssemblyInstance}, wrapping a wasm3 {@code IM3Runtime} pointer
 * into which a module has been loaded, the module pointer (for global lookups), and the
 * {@link Arena} holding that module's bytes and host-function stubs.
 *
 * <p>Closing frees the native runtime (which frees the loaded module) and then closes the
 * arena, in that order.
 */
final class PanamaWasmInstance implements WebAssemblyInstance {

    private final MemorySegment runtime;
    private final MemorySegment module;
    private final Arena moduleArena;
    private boolean closed;

    PanamaWasmInstance(
            final MemorySegment runtime, final MemorySegment module, final Arena moduleArena) {
        this.runtime = runtime;
        this.module = module;
        this.moduleArena = moduleArena;
    }

    @Override
    public Optional<WebAssemblyFunction> findFunction(final String name) {
        final MemorySegment fn = Wasm3Library.findFunction(runtime, name);
        return fn.address() == 0 ? Optional.empty() : Optional.of(new PanamaWasmFunction(fn));
    }

    @Override
    public Optional<WebAssemblyMemory> findMemory(final String name) {
        return Wasm3Library.memorySegment(runtime).address() == 0
                ? Optional.empty()
                : Optional.of(new PanamaWasmMemory(runtime));
    }

    @Override
    public Optional<WasmGlobal> findGlobal(final String name) {
        final MemorySegment global = Wasm3Library.findGlobal(module, name);
        return global.address() == 0 ? Optional.empty() : Optional.of(new PanamaWasmGlobal(global));
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        Wasm3Library.freeRuntime(runtime);
        moduleArena.close();
    }
}
