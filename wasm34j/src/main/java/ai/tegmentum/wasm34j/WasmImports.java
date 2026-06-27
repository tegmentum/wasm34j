package ai.tegmentum.wasm34j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The set of host imports supplied to a module at instantiation time — the standalone
 * analogue of the webassembly4j {@code LinkingContext}.
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
            hostFunctions.add(
                    new HostFunctionDefinition(moduleName, functionName, type, function));
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
