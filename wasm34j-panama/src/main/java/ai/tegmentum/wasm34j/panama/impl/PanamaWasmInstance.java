package ai.tegmentum.wasm34j.panama.impl;

import ai.tegmentum.wasm34j.WebAssemblyFunction;
import ai.tegmentum.wasm34j.WebAssemblyInstance;
import ai.tegmentum.wasm34j.panama.internal.Wasm3Library;

import java.lang.foreign.MemorySegment;
import java.util.Optional;

/**
 * Panama-backed {@link WebAssemblyInstance}, wrapping a wasm3 {@code IM3Runtime} pointer
 * into which a module has been loaded. Closing frees the native runtime.
 */
final class PanamaWasmInstance implements WebAssemblyInstance {

    private final MemorySegment runtime;
    private boolean closed;

    PanamaWasmInstance(final MemorySegment runtime) {
        this.runtime = runtime;
    }

    @Override
    public Optional<WebAssemblyFunction> findFunction(final String name) {
        final MemorySegment fn = Wasm3Library.findFunction(runtime, name);
        return fn.address() == 0 ? Optional.empty() : Optional.of(new PanamaWasmFunction(fn));
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            Wasm3Library.freeRuntime(runtime);
        }
    }
}
