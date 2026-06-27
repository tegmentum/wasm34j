package ai.tegmentum.wasm34j;

/**
 * A parsed WebAssembly module, ready to be instantiated.
 */
public interface WebAssemblyModule extends AutoCloseable {

    /**
     * Instantiates the module, producing a live runtime instance.
     *
     * @return the instance
     */
    WebAssemblyInstance instantiate();

    @Override
    void close();
}
