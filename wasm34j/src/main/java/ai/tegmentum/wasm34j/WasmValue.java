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
 * An immutable, typed WebAssembly value.
 *
 * <p>The underlying value is stored as the raw 64-bit pattern that crosses the native boundary:
 * i32/f32 occupy the low 32 bits, i64/f64 occupy all 64 bits.
 */
public final class WasmValue {

    private final ValueType type;
    private final long bits;

    private WasmValue(final ValueType type, final long bits) {
        this.type = type;
        this.bits = bits;
    }

    public static WasmValue i32(final int value) {
        return new WasmValue(ValueType.I32, value & 0xFFFFFFFFL);
    }

    public static WasmValue i64(final long value) {
        return new WasmValue(ValueType.I64, value);
    }

    public static WasmValue f32(final float value) {
        return new WasmValue(ValueType.F32, Float.floatToRawIntBits(value) & 0xFFFFFFFFL);
    }

    public static WasmValue f64(final double value) {
        return new WasmValue(ValueType.F64, Double.doubleToRawLongBits(value));
    }

    /**
     * Reconstructs a value from a type and its raw 64-bit pattern (as returned by the native call
     * boundary).
     *
     * @param type the value type
     * @param bits the raw bit pattern
     * @return the value
     */
    public static WasmValue ofRaw(final ValueType type, final long bits) {
        return new WasmValue(type, bits);
    }

    public ValueType type() {
        return type;
    }

    /**
     * @return the raw 64-bit pattern that crosses the native boundary.
     */
    public long rawBits() {
        return bits;
    }

    public int asInt() {
        return (int) bits;
    }

    public long asLong() {
        return bits;
    }

    public float asFloat() {
        return Float.intBitsToFloat((int) bits);
    }

    public double asDouble() {
        return Double.longBitsToDouble(bits);
    }

    /**
     * @return the value boxed as the natural Java type for {@link #type()} ({@link Integer}, {@link
     *     Long}, {@link Float}, {@link Double}), or the raw {@code long} bits for non-numeric
     *     types.
     */
    public Object boxed() {
        switch (type) {
            case I32:
                return asInt();
            case I64:
                return asLong();
            case F32:
                return asFloat();
            case F64:
                return asDouble();
            default:
                return bits;
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WasmValue)) {
            return false;
        }
        final WasmValue other = (WasmValue) o;
        return bits == other.bits && type == other.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, bits);
    }

    @Override
    public String toString() {
        return "WasmValue{" + type + "=" + boxed() + "}";
    }
}
