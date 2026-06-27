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
