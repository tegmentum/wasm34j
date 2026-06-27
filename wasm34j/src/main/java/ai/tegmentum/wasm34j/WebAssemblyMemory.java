package ai.tegmentum.wasm34j;

import java.nio.ByteBuffer;

/**
 * A module instance's linear memory. wasm3 supports a single memory per module.
 */
public interface WebAssemblyMemory {

    /** Bytes per WebAssembly page. */
    long PAGE_SIZE = 65536;

    /** @return the current size in bytes. */
    long byteSize();

    /** @return the current size in pages. */
    default long pageCount() {
        return byteSize() / PAGE_SIZE;
    }

    /**
     * Reads {@code length} bytes starting at {@code offset}.
     *
     * @param offset the byte offset into linear memory
     * @param length the number of bytes to read
     * @return the bytes read
     */
    byte[] read(long offset, int length);

    /**
     * Writes {@code data} at {@code offset}.
     *
     * @param offset the byte offset into linear memory
     * @param data the bytes to write
     */
    void write(long offset, byte[] data);

    /**
     * @return a {@link ByteBuffer} view over the live linear memory; the buffer is only
     *     valid while the owning instance is open and the memory has not been resized
     */
    ByteBuffer asByteBuffer();

    /**
     * Grows the memory by {@code pages} pages.
     *
     * @param pages the number of pages to add
     * @return the previous page count
     * @throws UnsupportedOperationException wasm3 does not expose host-driven memory growth
     */
    default long grow(final long pages) {
        throw new UnsupportedOperationException("wasm3 does not support host-driven memory growth");
    }
}
