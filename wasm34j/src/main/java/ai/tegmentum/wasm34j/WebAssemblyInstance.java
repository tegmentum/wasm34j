package ai.tegmentum.wasm34j;

import ai.tegmentum.wasm34j.exception.WasmException;

import java.util.Optional;

/**
 * An instantiated WebAssembly module: a live runtime from which exports can be looked
 * up and called. Closing an instance releases the underlying native runtime.
 */
public interface WebAssemblyInstance extends AutoCloseable {

    /**
     * Looks up an exported function by name.
     *
     * @param name the export name
     * @return the function, or empty if no such export exists
     */
    Optional<WebAssemblyFunction> findFunction(String name);

    /**
     * Looks up an exported function by name, failing if it is absent.
     *
     * @param name the export name
     * @return the function
     * @throws WasmException if no such export exists
     */
    default WebAssemblyFunction getFunction(final String name) {
        return findFunction(name)
                .orElseThrow(() -> new WasmException("No exported function named '" + name + "'"));
    }

    /**
     * Looks up this instance's linear memory. wasm3 supports a single memory; the
     * {@code name} is accepted for API symmetry but the runtime memory is returned
     * regardless.
     *
     * @param name the export name (advisory)
     * @return the memory, or empty if the module has no memory
     */
    Optional<WebAssemblyMemory> findMemory(String name);

    /**
     * Looks up an exported global by name.
     *
     * @param name the export name
     * @return the global, or empty if no such export exists
     */
    Optional<WasmGlobal> findGlobal(String name);

    @Override
    void close();
}
