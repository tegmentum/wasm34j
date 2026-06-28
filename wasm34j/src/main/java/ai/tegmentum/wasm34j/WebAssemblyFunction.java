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

/** A callable WebAssembly export. */
public interface WebAssemblyFunction {

    /**
     * Calls the function with typed arguments and returns its typed results.
     *
     * @param args the arguments, whose count and types must match the function signature
     * @return the result values (empty array for a function with no results)
     */
    WasmValue[] call(WasmValue... args);

    /**
     * Convenience call that boxes/unboxes values automatically. Numeric arguments are coerced to
     * the function's declared parameter types.
     *
     * @param args numeric arguments ({@link Number} instances)
     * @return the single result boxed as its natural Java type, or {@code null} when the function
     *     returns no value
     */
    Object invoke(Object... args);

    /**
     * @return the number of parameters.
     */
    int parameterCount();

    /**
     * @return the number of results.
     */
    int resultCount();

    /**
     * @param index the parameter index
     * @return the type of the parameter at {@code index}
     */
    ValueType parameterType(int index);

    /**
     * @param index the result index
     * @return the type of the result at {@code index}
     */
    ValueType resultType(int index);
}
