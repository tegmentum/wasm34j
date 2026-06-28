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

/**
 * Entry point to a wasm3-backed WebAssembly runtime. Parses modules and reports engine metadata.
 * Closing the runtime releases the underlying native environment.
 *
 * <p>Obtain an instance via {@link RuntimeFactory}.
 */
public interface WebAssemblyRuntime extends AutoCloseable {

    /**
     * Parses a WebAssembly binary into a module.
     *
     * @param wasmBytes the {@code .wasm} binary
     * @return the parsed module
     */
    WebAssemblyModule compile(byte[] wasmBytes);

    /**
     * @return the version string of the underlying wasm3 engine.
     */
    String engineVersion();

    @Override
    void close();
}
