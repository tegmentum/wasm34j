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

import java.util.Objects;

/**
 * Binds a {@link HostFunction} to the import slot (module + field name) and signature it satisfies.
 */
public final class HostFunctionDefinition {

    private final String moduleName;
    private final String functionName;
    private final FunctionType type;
    private final HostFunction function;

    public HostFunctionDefinition(
            final String moduleName,
            final String functionName,
            final FunctionType type,
            final HostFunction function) {
        this.moduleName = Objects.requireNonNull(moduleName, "moduleName");
        this.functionName = Objects.requireNonNull(functionName, "functionName");
        this.type = Objects.requireNonNull(type, "type");
        this.function = Objects.requireNonNull(function, "function");
    }

    public String moduleName() {
        return moduleName;
    }

    public String functionName() {
        return functionName;
    }

    public FunctionType type() {
        return type;
    }

    public HostFunction function() {
        return function;
    }
}
