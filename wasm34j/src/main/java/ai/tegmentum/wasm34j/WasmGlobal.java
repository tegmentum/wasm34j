package ai.tegmentum.wasm34j;

/**
 * An exported WebAssembly global.
 */
public interface WasmGlobal {

    /** @return the value type of this global. */
    ValueType type();

    /** @return the current value. */
    WasmValue get();

    /**
     * Sets the value of a mutable global.
     *
     * @param value the new value
     */
    void set(WasmValue value);

    /**
     * @return whether the global is mutable. wasm3 does not expose mutability, so this is
     *     reported best-effort as {@code true}; a {@link #set(WasmValue)} on an immutable
     *     global will fail at call time.
     */
    default boolean mutable() {
        return true;
    }
}
