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

import java.nio.ByteBuffer;

import ai.tegmentum.wasm34j.WebAssemblyMemory;
import ai.tegmentum.wasm34j.jni.internal.Wasm3Native;

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
        return buffer == null
                ? ByteBuffer.allocate(0)
                : buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
    }
}
