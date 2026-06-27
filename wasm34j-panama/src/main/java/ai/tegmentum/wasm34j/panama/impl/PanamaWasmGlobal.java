package ai.tegmentum.wasm34j.panama.impl;

import ai.tegmentum.wasm34j.ValueType;
import ai.tegmentum.wasm34j.WasmGlobal;
import ai.tegmentum.wasm34j.WasmValue;
import ai.tegmentum.wasm34j.panama.internal.Wasm3Library;

import java.lang.foreign.MemorySegment;

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
