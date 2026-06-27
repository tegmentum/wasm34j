package ai.tegmentum.wasm34j.exception;

/**
 * Unchecked exception for all wasm34j failures: module parsing, instantiation,
 * function lookup, and invocation errors surfaced from the underlying wasm3 runtime.
 *
 * <p>This type is referenced by name from the native JNI glue, which throws it when a
 * wasm3 {@code M3Result} indicates an error. Keep its fully-qualified name in sync with
 * the native code if it ever moves.
 */
public class WasmException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public WasmException(final String message) {
        super(message);
    }

    public WasmException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
