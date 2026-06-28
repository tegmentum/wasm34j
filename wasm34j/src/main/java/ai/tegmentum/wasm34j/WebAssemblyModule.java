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

/** A parsed WebAssembly module, ready to be instantiated. */
public interface WebAssemblyModule extends AutoCloseable {

    /**
     * Instantiates the module with no host imports.
     *
     * @return the instance
     */
    default WebAssemblyInstance instantiate() {
        return instantiate(WasmImports.empty());
    }

    /**
     * Instantiates the module, linking the supplied host imports.
     *
     * @param imports the host functions to satisfy the module's imports
     * @return the instance
     */
    WebAssemblyInstance instantiate(WasmImports imports);

    @Override
    void close();
}
