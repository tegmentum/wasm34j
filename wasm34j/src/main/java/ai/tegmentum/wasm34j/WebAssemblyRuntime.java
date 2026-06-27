package ai.tegmentum.wasm34j;

/**
 * Entry point to a wasm3-backed WebAssembly runtime. Parses modules and reports engine
 * metadata. Closing the runtime releases the underlying native environment.
 *
 * <p>Obtain an instance via {@link RuntimeFactory}.
 */
public interface WebAssemblyRuntime extends AutoCloseable {

    /**
     * Parses a WebAssembly binary into a module.
     *
     * @param wasmBytes the {@code .wasm} binary
     * @return the parsed module
     */
    WebAssemblyModule compile(byte[] wasmBytes);

    /** @return the version string of the underlying wasm3 engine. */
    String engineVersion();

    @Override
    void close();
}
