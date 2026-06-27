package ai.tegmentum.wasm34j.panama.impl;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

import ai.tegmentum.wasm34j.WebAssemblyMemory;
import ai.tegmentum.wasm34j.exception.WasmException;
import ai.tegmentum.wasm34j.panama.internal.Wasm3Library;

import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Panama-backed {@link WebAssemblyMemory} over a runtime's linear memory. */
final class PanamaWasmMemory implements WebAssemblyMemory {

    private final MemorySegment runtime;

    PanamaWasmMemory(final MemorySegment runtime) {
        this.runtime = runtime;
    }

    @Override
    public long byteSize() {
        return Integer.toUnsignedLong(Wasm3Library.memorySize(runtime));
    }

    @Override
    public byte[] read(final long offset, final int length) {
        final MemorySegment memory = liveMemory();
        if (offset < 0 || length < 0 || offset + length > memory.byteSize()) {
            throw new WasmException("memory read out of bounds");
        }
        final byte[] out = new byte[length];
        MemorySegment.copy(memory, JAVA_BYTE, offset, out, 0, length);
        return out;
    }

    @Override
    public void write(final long offset, final byte[] data) {
        final MemorySegment memory = liveMemory();
        if (offset < 0 || offset + data.length > memory.byteSize()) {
            throw new WasmException("memory write out of bounds");
        }
        MemorySegment.copy(data, 0, memory, JAVA_BYTE, offset, data.length);
    }

    @Override
    public ByteBuffer asByteBuffer() {
        final MemorySegment memory = Wasm3Library.memorySegment(runtime);
        if (memory.address() == 0) {
            return ByteBuffer.allocate(0);
        }
        return memory.asByteBuffer().order(ByteOrder.LITTLE_ENDIAN);
    }

    private MemorySegment liveMemory() {
        final MemorySegment memory = Wasm3Library.memorySegment(runtime);
        if (memory.address() == 0) {
            throw new WasmException("module has no memory");
        }
        return memory;
    }
}
