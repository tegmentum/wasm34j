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

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import ai.tegmentum.wasm34j.WebAssemblyMemory;
import ai.tegmentum.wasm34j.exception.WasmException;
import ai.tegmentum.wasm34j.panama.internal.Wasm3Library;

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
