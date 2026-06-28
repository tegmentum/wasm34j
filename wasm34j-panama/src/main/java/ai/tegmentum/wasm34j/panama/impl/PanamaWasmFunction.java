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
package ai.tegmentum.wasm34j.panama.impl;

import java.lang.foreign.MemorySegment;

import ai.tegmentum.wasm34j.ValueType;
import ai.tegmentum.wasm34j.WasmValue;
import ai.tegmentum.wasm34j.WebAssemblyFunction;
import ai.tegmentum.wasm34j.exception.WasmException;
import ai.tegmentum.wasm34j.panama.internal.Wasm3Library;

/** Panama-backed {@link WebAssemblyFunction}, wrapping a wasm3 {@code IM3Function} pointer. */
final class PanamaWasmFunction implements WebAssemblyFunction {

    private final MemorySegment handle;

    PanamaWasmFunction(final MemorySegment handle) {
        this.handle = handle;
    }

    @Override
    public WasmValue[] call(final WasmValue... args) {
        final long[] rawArgs = new long[args.length];
        for (int i = 0; i < args.length; i++) {
            rawArgs[i] = args[i].rawBits();
        }
        final long[] rawResults = Wasm3Library.call(handle, rawArgs);
        final WasmValue[] results = new WasmValue[rawResults.length];
        for (int i = 0; i < rawResults.length; i++) {
            results[i] = WasmValue.ofRaw(resultType(i), rawResults[i]);
        }
        return results;
    }

    @Override
    public Object invoke(final Object... args) {
        final int expected = parameterCount();
        if (args.length != expected) {
            throw new WasmException("Expected " + expected + " argument(s) but got " + args.length);
        }
        final WasmValue[] typedArgs = new WasmValue[expected];
        for (int i = 0; i < expected; i++) {
            typedArgs[i] = coerce(args[i], parameterType(i));
        }
        final WasmValue[] results = call(typedArgs);
        return results.length == 0 ? null : results[0].boxed();
    }

    private static WasmValue coerce(final Object arg, final ValueType type) {
        if (!(arg instanceof Number)) {
            throw new WasmException(
                    "Argument of type "
                            + (arg == null ? "null" : arg.getClass().getName())
                            + " is not a supported numeric value");
        }
        final Number n = (Number) arg;
        switch (type) {
            case I32:
                return WasmValue.i32(n.intValue());
            case I64:
                return WasmValue.i64(n.longValue());
            case F32:
                return WasmValue.f32(n.floatValue());
            case F64:
                return WasmValue.f64(n.doubleValue());
            default:
                throw new WasmException("Unsupported parameter type: " + type);
        }
    }

    @Override
    public int parameterCount() {
        return Wasm3Library.getArgCount(handle);
    }

    @Override
    public int resultCount() {
        return Wasm3Library.getRetCount(handle);
    }

    @Override
    public ValueType parameterType(final int index) {
        return ValueType.fromNative(Wasm3Library.getArgType(handle, index));
    }

    @Override
    public ValueType resultType(final int index) {
        return ValueType.fromNative(Wasm3Library.getRetType(handle, index));
    }
}
