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
package ai.tegmentum.wasm34j.jni.impl;

import ai.tegmentum.wasm34j.ValueType;
import ai.tegmentum.wasm34j.WasmGlobal;
import ai.tegmentum.wasm34j.WasmValue;
import ai.tegmentum.wasm34j.jni.internal.Wasm3Native;

/** JNI-backed {@link WasmGlobal} wrapping a wasm3 {@code IM3Global} handle. */
final class JniWasmGlobal implements WasmGlobal {

    private final long handle;

    JniWasmGlobal(final long handle) {
        this.handle = handle;
    }

    @Override
    public ValueType type() {
        return ValueType.fromNative(Wasm3Native.globalType(handle));
    }

    @Override
    public WasmValue get() {
        return WasmValue.ofRaw(type(), Wasm3Native.globalGet(handle));
    }

    @Override
    public void set(final WasmValue value) {
        Wasm3Native.globalSet(handle, value.type().toNative(), value.rawBits());
    }
}
