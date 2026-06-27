package ai.tegmentum.wasm34j.jni.internal;

import ai.tegmentum.wasm34j.exception.WasmException;
import ai.tegmentum.wasm34j.internal.NativeLibrary;

import java.nio.file.Path;

/**
 * Loads the wasm34j native library exactly once per JVM for the JNI backend.
 *
 * <p>It first tries {@link System#loadLibrary(String)} (honoring {@code java.library.path}),
 * then falls back to extracting the platform-specific library bundled on the classpath and
 * loading it via {@link System#load(String)}.
 */
public final class NativeLibraryLoader {

    private static volatile boolean loaded;
    private static volatile Throwable failure;

    private NativeLibraryLoader() {
    }

    /**
     * Ensures the native library is loaded. Loading is attempted once; a prior failure is
     * rethrown rather than retried.
     *
     * @throws WasmException if the library cannot be loaded
     */
    public static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        if (failure != null) {
            throw new WasmException("Native library previously failed to load", failure);
        }
        try {
            load();
            loaded = true;
        } catch (final Throwable t) {
            failure = t;
            throw new WasmException("Failed to load the wasm34j native library", t);
        }
    }

    private static void load() {
        try {
            System.loadLibrary(NativeLibrary.BASE_NAME);
            return;
        } catch (final UnsatisfiedLinkError ignored) {
            // Fall back to extracting the bundled library from the classpath.
        }
        final Path extracted = NativeLibrary.extractToTempFile(NativeLibraryLoader.class);
        System.load(extracted.toAbsolutePath().toString());
    }

    /**
     * @return whether the bundled native library resource exists for the current platform
     *     (without attempting to load it)
     */
    public static boolean isResourceAvailable() {
        return NativeLibrary.isResourceAvailable(NativeLibraryLoader.class);
    }
}
