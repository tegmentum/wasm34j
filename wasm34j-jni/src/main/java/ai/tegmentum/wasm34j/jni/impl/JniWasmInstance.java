package ai.tegmentum.wasm34j.jni.impl;

import ai.tegmentum.wasm34j.WebAssemblyFunction;
import ai.tegmentum.wasm34j.WebAssemblyInstance;
import ai.tegmentum.wasm34j.jni.internal.Wasm3Native;

import java.util.Optional;

/**
 * JNI-backed {@link WebAssemblyInstance}, wrapping a wasm3 {@code IM3Runtime} handle into
 * which a module has been loaded, plus the byte buffer backing that module.
 *
 * <p>Closing frees the native runtime (which frees the loaded module) and then the byte
 * buffer, in that order.
 */
final class JniWasmInstance implements WebAssemblyInstance {

    private final long runtime;
    private final long buffer;
    private boolean closed;

    JniWasmInstance(final long runtime, final long buffer) {
        this.runtime = runtime;
        this.buffer = buffer;
    }

    @Override
    public Optional<WebAssemblyFunction> findFunction(final String name) {
        final long fn = Wasm3Native.findFunction(runtime, name);
        return fn == 0 ? Optional.empty() : Optional.of(new JniWasmFunction(fn));
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        Wasm3Native.freeRuntime(runtime);
        Wasm3Native.freeBuffer(buffer);
    }
}
