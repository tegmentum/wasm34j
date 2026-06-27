package ai.tegmentum.wasm34j;

/**
 * A parsed WebAssembly module, ready to be instantiated.
 */
public interface WebAssemblyModule extends AutoCloseable {

    /**
     * Instantiates the module with no host imports.
     *
     * @return the instance
     */
    default WebAssemblyInstance instantiate() {
        return instantiate(WasmImports.empty());
    }

    /**
     * Instantiates the module, linking the supplied host imports.
     *
     * @param imports the host functions to satisfy the module's imports
     * @return the instance
     */
    WebAssemblyInstance instantiate(WasmImports imports);

    @Override
    void close();
}
