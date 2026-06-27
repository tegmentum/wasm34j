package ai.tegmentum.wasm34j.spi;

import ai.tegmentum.wasm34j.WebAssemblyRuntime;

/**
 * Service-provider interface for a concrete wasm34j backend (JNI, Panama, ...).
 *
 * <p>Implementations are discovered via {@link java.util.ServiceLoader} and selected by
 * {@link ai.tegmentum.wasm34j.RuntimeFactory} according to {@link #priority()}.
 */
public interface RuntimeProvider {

    /** @return a short, stable provider name, e.g. {@code "jni"} or {@code "panama"}. */
    String name();

    /** @return the selection priority; higher wins when multiple providers are available. */
    int priority();

    /** @return whether this provider can be used in the current environment. */
    boolean isAvailable();

    /**
     * Creates a new runtime backed by this provider.
     *
     * @return the runtime
     */
    WebAssemblyRuntime createRuntime();
}
