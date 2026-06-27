package ai.tegmentum.wasm34j;

import ai.tegmentum.wasm34j.exception.WasmException;
import ai.tegmentum.wasm34j.spi.RuntimeProvider;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Discovers and creates {@link WebAssemblyRuntime} instances from the
 * {@link RuntimeProvider} implementations on the classpath.
 *
 * <p>By default the highest-priority available provider is chosen. The selection can be
 * forced with the system property {@code wasm34j.runtime} (e.g. {@code -Dwasm34j.runtime=jni}).
 */
public final class RuntimeFactory {

    /** System property used to force a specific provider by {@link RuntimeProvider#name()}. */
    public static final String RUNTIME_PROPERTY = "wasm34j.runtime";

    private RuntimeFactory() {
    }

    /**
     * Creates a runtime using the highest-priority available provider, unless overridden
     * by the {@value #RUNTIME_PROPERTY} system property.
     *
     * @return a new runtime
     * @throws WasmException if no suitable provider is available
     */
    public static WebAssemblyRuntime create() {
        final String requested = System.getProperty(RUNTIME_PROPERTY);
        if (requested != null && !requested.isEmpty()) {
            return create(requested);
        }
        final List<RuntimeProvider> providers = availableProviders();
        if (providers.isEmpty()) {
            throw new WasmException(
                    "No wasm34j runtime providers available on the classpath. "
                            + "Add wasm34j-jni (or another backend) as a dependency.");
        }
        return providers.get(0).createRuntime();
    }

    /**
     * Creates a runtime using the named provider.
     *
     * @param providerName the provider name (case-insensitive), see {@link RuntimeProvider#name()}
     * @return a new runtime
     * @throws WasmException if no available provider matches the name
     */
    public static WebAssemblyRuntime create(final String providerName) {
        for (final RuntimeProvider provider : availableProviders()) {
            if (provider.name().equalsIgnoreCase(providerName)) {
                return provider.createRuntime();
            }
        }
        throw new WasmException("No available wasm34j runtime provider named '" + providerName + "'");
    }

    /**
     * @return all available providers, sorted by descending priority
     */
    public static List<RuntimeProvider> availableProviders() {
        final List<RuntimeProvider> providers = new ArrayList<>();
        for (final RuntimeProvider provider : ServiceLoader.load(RuntimeProvider.class)) {
            if (provider.isAvailable()) {
                providers.add(provider);
            }
        }
        providers.sort(Comparator.comparingInt(RuntimeProvider::priority).reversed());
        return providers;
    }
}
