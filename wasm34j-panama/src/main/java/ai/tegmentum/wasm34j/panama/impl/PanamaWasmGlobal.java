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
import ai.tegmentum.wasm34j.WasmGlobal;
import ai.tegmentum.wasm34j.WasmValue;
import ai.tegmentum.wasm34j.panama.internal.Wasm3Library;

/** Panama-backed {@link WasmGlobal} wrapping a wasm3 {@code IM3Global} pointer. */
final class PanamaWasmGlobal implements WasmGlobal {

    private final MemorySegment handle;

    PanamaWasmGlobal(final MemorySegment handle) {
        this.handle = handle;
    }

    @Override
    public ValueType type() {
        return ValueType.fromNative(Wasm3Library.globalType(handle));
    }

    @Override
    public WasmValue get() {
        return WasmValue.ofRaw(type(), Wasm3Library.globalGet(handle));
    }

    @Override
    public void set(final WasmValue value) {
        Wasm3Library.globalSet(handle, value.type().toNative(), value.rawBits());
    }
}
