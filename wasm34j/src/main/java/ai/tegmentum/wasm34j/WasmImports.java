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
import java.util.Collections;
import java.util.List;

/**
 * The set of host imports supplied to a module at instantiation time — the standalone analogue of
 * the webassembly4j {@code LinkingContext}.
 */
public final class WasmImports {

    private static final WasmImports EMPTY = new WasmImports(Collections.emptyList());

    private final List<HostFunctionDefinition> hostFunctions;

    private WasmImports(final List<HostFunctionDefinition> hostFunctions) {
        this.hostFunctions = Collections.unmodifiableList(new ArrayList<>(hostFunctions));
    }

    public static WasmImports empty() {
        return EMPTY;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<HostFunctionDefinition> hostFunctions() {
        return hostFunctions;
    }

    public boolean isEmpty() {
        return hostFunctions.isEmpty();
    }

    /** Fluent builder for {@link WasmImports}. */
    public static final class Builder {

        private final List<HostFunctionDefinition> hostFunctions = new ArrayList<>();

        public Builder function(
                final String moduleName,
                final String functionName,
                final FunctionType type,
                final HostFunction function) {
            hostFunctions.add(new HostFunctionDefinition(moduleName, functionName, type, function));
            return this;
        }

        public Builder function(final HostFunctionDefinition definition) {
            hostFunctions.add(definition);
            return this;
        }

        public WasmImports build() {
            return new WasmImports(hostFunctions);
        }
    }
}
