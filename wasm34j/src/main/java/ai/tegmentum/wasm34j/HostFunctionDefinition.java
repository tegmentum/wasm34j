package ai.tegmentum.wasm34j;

import java.util.Objects;

/**
 * Binds a {@link HostFunction} to the import slot (module + field name) and signature it
 * satisfies.
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
