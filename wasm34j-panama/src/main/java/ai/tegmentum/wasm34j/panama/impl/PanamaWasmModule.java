package ai.tegmentum.wasm34j.panama.impl;

import ai.tegmentum.wasm34j.HostFunctionDefinition;
import ai.tegmentum.wasm34j.WasmImports;
import ai.tegmentum.wasm34j.WebAssemblyInstance;
import ai.tegmentum.wasm34j.WebAssemblyModule;
import ai.tegmentum.wasm34j.exception.WasmException;
import ai.tegmentum.wasm34j.internal.Signatures;
import ai.tegmentum.wasm34j.panama.internal.Wasm3Library;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

/**
 * Panama-backed {@link WebAssemblyModule}, wrapping a parsed wasm3 {@code IM3Module}
 * pointer plus the {@link Arena} holding its wasm bytes (and any host-function upcall stubs).
 *
 * <p>Host imports are linked into the module before it is loaded; the upcall stubs are
 * allocated in the module arena so they outlive the instance. A module may only be
 * instantiated once.
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
    public WebAssemblyInstance instantiate(final WasmImports imports) {
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
        try {
            // wasm3 requires the module to be loaded (so its runtime/code pages exist) before
            // host functions can be linked.
            Wasm3Library.loadModule(runtime, module);
            for (final HostFunctionDefinition def : imports.hostFunctions()) {
                final MemorySegment stub =
                        Wasm3Library.createHostUpcall(def.function(), def.type(), moduleArena);
                Wasm3Library.linkRawFunction(
                        module, def.moduleName(), def.functionName(),
                        Signatures.wasm3(def.type()), stub);
            }
            instantiated = true;
            return new PanamaWasmInstance(runtime, module, moduleArena);
        } catch (final RuntimeException e) {
            Wasm3Library.freeRuntime(runtime);
            throw e;
        }
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
