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

import java.util.Arrays;

/** The signature of a function: its parameter and result value types. */
public final class FunctionType {

    private final ValueType[] parameterTypes;
    private final ValueType[] resultTypes;

    public FunctionType(final ValueType[] parameterTypes, final ValueType[] resultTypes) {
        this.parameterTypes = parameterTypes.clone();
        this.resultTypes = resultTypes.clone();
    }

    public static FunctionType of(final ValueType[] parameterTypes, final ValueType[] resultTypes) {
        return new FunctionType(parameterTypes, resultTypes);
    }

    public ValueType[] parameterTypes() {
        return parameterTypes.clone();
    }

    public ValueType[] resultTypes() {
        return resultTypes.clone();
    }

    public int parameterCount() {
        return parameterTypes.length;
    }

    public int resultCount() {
        return resultTypes.length;
    }

    @Override
    public String toString() {
        return "FunctionType"
                + Arrays.toString(parameterTypes)
                + "->"
                + Arrays.toString(resultTypes);
    }
}
