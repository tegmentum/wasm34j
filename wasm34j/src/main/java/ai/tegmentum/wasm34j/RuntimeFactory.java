/*
 * Copyright (c) 2026 Tegmentum AI, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.tegmentum.wasm34j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import ai.tegmentum.wasm34j.exception.WasmException;
import ai.tegmentum.wasm34j.spi.RuntimeProvider;

/**
 * Discovers and creates {@link WebAssemblyRuntime} instances from the {@link RuntimeProvider}
 * implementations on the classpath.
 *
 * <p>By default the highest-priority available provider is chosen. The selection can be forced with
 * the system property {@code wasm34j.runtime} (e.g. {@code -Dwasm34j.runtime=jni}).
 */
public final class RuntimeFactory {

    /** System property used to force a specific provider by {@link RuntimeProvider#name()}. */
    public static final String RUNTIME_PROPERTY = "wasm34j.runtime";

    private RuntimeFactory() {}

    /**
     * Creates a runtime using the highest-priority available provider, unless overridden by the
     * {@value #RUNTIME_PROPERTY} system property.
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
        throw new WasmException(
                "No available wasm34j runtime provider named '" + providerName + "'");
    }

    /**
     * @return all available providers, sorted by descending priority
     */
    public static List<RuntimeProvider> availableProviders() {
        final List<RuntimeProvider> providers = new ArrayList<>();
        // Iterate defensively: a backend compiled for a newer Java release (e.g. the Panama
        // provider on a Java 17-21 JVM) cannot be loaded and surfaces as a
        // ServiceConfigurationError. Skip such providers rather than letting one broken entry
        // hide all the others (the ServiceLoader iterator advances past the failed element).
        final Iterator<RuntimeProvider> iterator =
                ServiceLoader.load(RuntimeProvider.class).iterator();
        while (true) {
            final RuntimeProvider provider;
            try {
                if (!iterator.hasNext()) {
                    break;
                }
                provider = iterator.next();
            } catch (final ServiceConfigurationError e) {
                continue;
            }
            try {
                if (provider.isAvailable()) {
                    providers.add(provider);
                }
            } catch (final Throwable t) {
                // A provider that reports availability badly should not break discovery.
            }
        }
        providers.sort(Comparator.comparingInt(RuntimeProvider::priority).reversed());
        return providers;
    }
}
