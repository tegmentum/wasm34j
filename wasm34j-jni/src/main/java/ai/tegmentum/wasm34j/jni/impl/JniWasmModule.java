package ai.tegmentum.wasm34j.jni.impl;

import ai.tegmentum.wasm34j.HostFunctionDefinition;
import ai.tegmentum.wasm34j.WasmImports;
import ai.tegmentum.wasm34j.WebAssemblyInstance;
import ai.tegmentum.wasm34j.WebAssemblyModule;
import ai.tegmentum.wasm34j.exception.WasmException;
import ai.tegmentum.wasm34j.jni.internal.HostFunctionRegistry;
import ai.tegmentum.wasm34j.jni.internal.Wasm3Native;

import java.util.List;

/**
 * JNI-backed {@link WebAssemblyModule}, wrapping a parsed wasm3 {@code IM3Module} handle
 * plus the native byte buffer backing its wasm bytes.
 *
 * <p>wasm3 ties a module to a single runtime and requires host imports to be linked on the
 * module before it is loaded; both happen in {@link #instantiate(WasmImports)}, which
 * transfers ownership of the module, byte buffer, and host-function registrations to the
 * resulting instance. A module may therefore only be instantiated once.
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
    public WebAssemblyInstance instantiate(final WasmImports imports) {
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

        final List<HostFunctionDefinition> defs = imports.hostFunctions();
        final int[] hostIds = new int[defs.size()];
        int linked = 0;
        try {
            // wasm3 requires the module to be loaded (so its runtime/code pages exist) before
            // host functions can be linked.
            Wasm3Native.loadModule(runtime, module);
            for (final HostFunctionDefinition def : defs) {
                final int id = HostFunctionRegistry.register(def.function(), def.type());
                hostIds[linked++] = id;
                Wasm3Native.linkRawFunction(
                        module,
                        def.moduleName(),
                        def.functionName(),
                        HostFunctionRegistry.signature(def.type()),
                        id);
            }
            instantiated = true;
            return new JniWasmInstance(runtime, module, buffer, hostIds);
        } catch (final RuntimeException e) {
            for (int i = 0; i < linked; i++) {
                HostFunctionRegistry.unregister(hostIds[i]);
            }
            Wasm3Native.freeRuntime(runtime);
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
            Wasm3Native.freeModule(module);
            Wasm3Native.freeBuffer(buffer);
        }
    }
}
