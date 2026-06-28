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
package ai.tegmentum.wasm34j.spi;

import ai.tegmentum.wasm34j.WebAssemblyRuntime;

/**
 * Service-provider interface for a concrete wasm34j backend (JNI, Panama, ...).
 *
 * <p>Implementations are discovered via {@link java.util.ServiceLoader} and selected by {@link
 * ai.tegmentum.wasm34j.RuntimeFactory} according to {@link #priority()}.
 */
public interface RuntimeProvider {

    /**
     * @return a short, stable provider name, e.g. {@code "jni"} or {@code "panama"}.
     */
    String name();

    /**
     * @return the selection priority; higher wins when multiple providers are available.
     */
    int priority();

    /**
     * @return whether this provider can be used in the current environment.
     */
    boolean isAvailable();

    /**
     * Creates a new runtime backed by this provider.
     *
     * @return the runtime
     */
    WebAssemblyRuntime createRuntime();
}
