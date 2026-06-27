package ai.tegmentum.wasm34j;

import java.util.Arrays;

/**
 * The signature of a function: its parameter and result value types.
 */
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
        return "FunctionType" + Arrays.toString(parameterTypes) + "->" + Arrays.toString(resultTypes);
    }
}
