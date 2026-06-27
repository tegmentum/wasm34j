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

    /** Parses a module from the given bytes; throws on error. Returns the module handle. */
    public static native long parseModule(long environment, byte[] wasm);

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

    /** @return the wasm3 engine version string. */
    public static native String version();
}
