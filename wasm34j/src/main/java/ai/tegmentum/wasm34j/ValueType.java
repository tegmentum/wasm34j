package ai.tegmentum.wasm34j;

/**
 * WebAssembly value types as exposed by wasm34j.
 */
public enum ValueType {
    I32,
    I64,
    F32,
    F64,
    FUNCREF,
    EXTERNREF,
    V128,
    UNKNOWN;

    /**
     * Maps a wasm3 {@code M3ValueType} code to a {@link ValueType}.
     *
     * <p>wasm3 codes: 1=i32, 2=i64, 3=f32, 4=f64; anything else is reported as
     * {@link #UNKNOWN}.
     *
     * @param code the native type code
     * @return the corresponding value type
     */
    public static ValueType fromNative(final int code) {
        switch (code) {
            case 1:
                return I32;
            case 2:
                return I64;
            case 3:
                return F32;
            case 4:
                return F64;
            default:
                return UNKNOWN;
        }
    }
}
