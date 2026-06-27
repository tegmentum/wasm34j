package ai.tegmentum.wasm34j.jni.impl;

import ai.tegmentum.wasm34j.WebAssemblyMemory;
import ai.tegmentum.wasm34j.jni.internal.Wasm3Native;

import java.nio.ByteBuffer;

/** JNI-backed {@link WebAssemblyMemory} over a runtime's linear memory. */
final class JniWasmMemory implements WebAssemblyMemory {

    private final long runtime;

    JniWasmMemory(final long runtime) {
        this.runtime = runtime;
    }

    @Override
    public long byteSize() {
        return Integer.toUnsignedLong(Wasm3Native.memorySize(runtime));
    }

    @Override
    public byte[] read(final long offset, final int length) {
        return Wasm3Native.memoryRead(runtime, offset, length);
    }

    @Override
    public void write(final long offset, final byte[] data) {
        Wasm3Native.memoryWrite(runtime, offset, data);
    }

    @Override
    public ByteBuffer asByteBuffer() {
        final ByteBuffer buffer = Wasm3Native.memoryBuffer(runtime);
        return buffer == null ? ByteBuffer.allocate(0) : buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
    }
}
