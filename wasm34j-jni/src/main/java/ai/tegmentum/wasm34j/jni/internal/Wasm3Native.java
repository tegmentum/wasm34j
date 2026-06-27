package ai.tegmentum.wasm34j.jni.internal;

/**
 * Thin static binding to the wasm3 C API via JNI. Native handles (environment, runtime,
 * module, and function pointers) are represented as {@code long} values.
 *
 * <p>The native library is loaded when this class is initialized. All methods are
 * implemented in {@code wasm34j_native} (see {@code wasm34j-native/src/wasm34j_jni.c}).
 */
public final class Wasm3Native {

    static {
        NativeLibraryLoader.ensureLoaded();
    }

    private Wasm3Native() {
    }

    /** Forces class initialization (and therefore native library loading). */
    public static void ensureInitialized() {
        // No-op; triggering <clinit> is the point.
    }

    public static native long newEnvironment();

    public static native void freeEnvironment(long environment);

    public static native long newRuntime(long environment, int stackBytes);

    public static native void freeRuntime(long runtime);

    /**
     * Parses a module from the given bytes; throws on error.
     *
     * @return a two-element array {@code {modulePtr, bufferPtr}}; the buffer backs the
     *     module's wasm bytes and must be released with {@link #freeBuffer(long)} once the
     *     owning module/runtime is freed
     */
    public static native long[] parseModule(long environment, byte[] wasm);

    /** Frees an unloaded module (never successfully loaded into a runtime). */
    public static native void freeModule(long module);

    /** Frees the native byte buffer backing a module's wasm bytes. */
    public static native void freeBuffer(long buffer);

    /** Loads (instantiates) a parsed module into a runtime; throws on error. */
    public static native void loadModule(long runtime, long module);

    /** @return the function handle, or 0 if no such export exists. */
    public static native long findFunction(long runtime, String name);

    public static native int getArgCount(long function);

    public static native int getRetCount(long function);

    public static native int getArgType(long function, int index);

    public static native int getRetType(long function, int index);

    /**
     * Calls a function with raw 64-bit argument patterns and returns raw 64-bit result
     * patterns; throws on error.
     */
    public static native long[] call(long function, long[] rawArgs);

    // --- Memory ---

    public static native int memorySize(long runtime);

    /** @return a direct {@link java.nio.ByteBuffer} over the runtime memory, or null if none. */
    public static native java.nio.ByteBuffer memoryBuffer(long runtime);

    public static native byte[] memoryRead(long runtime, long offset, int length);

    public static native void memoryWrite(long runtime, long offset, byte[] data);

    // --- Globals ---

    /** @return the global handle, or 0 if no such export exists. */
    public static native long findGlobal(long module, String name);

    public static native int globalType(long global);

    public static native long globalGet(long global);

    public static native void globalSet(long global, int type, long bits);

    // --- Host functions ---

    /** Caches the Java dispatch entry point for host-function callbacks. */
    public static native void nativeInitDispatch(Class<?> dispatchClass);

    /** Links a host function (by registry id) into a module import before it is loaded. */
    public static native void linkRawFunction(
            long module, String moduleName, String functionName, String signature, int id);

    /** @return the wasm3 engine version string. */
    public static native String version();
}
