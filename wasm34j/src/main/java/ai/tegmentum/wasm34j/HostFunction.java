package ai.tegmentum.wasm34j;

/**
 * A host (Java) function importable by a WebAssembly module.
 *
 * <p>Arguments arrive as typed {@link WasmValue}s matching the declared
 * {@link FunctionType}; the returned array must match the declared result types (use an
 * empty array for a function with no results).
 */
@FunctionalInterface
public interface HostFunction {

    WasmValue[] call(WasmValue[] args);
}
