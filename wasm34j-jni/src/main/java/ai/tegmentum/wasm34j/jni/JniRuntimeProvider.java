package ai.tegmentum.wasm34j.jni;

import ai.tegmentum.wasm34j.WebAssemblyRuntime;
import ai.tegmentum.wasm34j.exception.WasmException;
import ai.tegmentum.wasm34j.jni.impl.JniWasmRuntime;
import ai.tegmentum.wasm34j.jni.internal.NativeLibraryLoader;
import ai.tegmentum.wasm34j.spi.RuntimeProvider;

/**
 * {@link RuntimeProvider} that creates JNI-backed wasm3 runtimes. Discovered via
 * {@link java.util.ServiceLoader}.
 */
public final class JniRuntimeProvider implements RuntimeProvider {

    private static final int PRIORITY = 100;

    @Override
    public String name() {
        return "jni";
    }

    @Override
    public int priority() {
        return PRIORITY;
    }

    @Override
    public boolean isAvailable() {
        return NativeLibraryLoader.isResourceAvailable();
    }

    @Override
    public WebAssemblyRuntime createRuntime() {
        try {
            return new JniWasmRuntime();
        } catch (final WasmException e) {
            throw e;
        } catch (final Throwable t) {
            throw new WasmException("Failed to create JNI wasm3 runtime", t);
        }
    }
}
