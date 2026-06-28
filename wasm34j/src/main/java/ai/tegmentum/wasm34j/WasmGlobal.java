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

/** An exported WebAssembly global. */
public interface WasmGlobal {

    /**
     * @return the value type of this global.
     */
    ValueType type();

    /**
     * @return the current value.
     */
    WasmValue get();

    /**
     * Sets the value of a mutable global.
     *
     * @param value the new value
     */
    void set(WasmValue value);

    /**
     * @return whether the global is mutable. wasm3 does not expose mutability, so this is reported
     *     best-effort as {@code true}; a {@link #set(WasmValue)} on an immutable global will fail
     *     at call time.
     */
    default boolean mutable() {
        return true;
    }
}
