package ai.tegmentum.wasm34j.jni.internal;

import ai.tegmentum.wasm34j.exception.WasmException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Loads the wasm34j native library exactly once per JVM.
 *
 * <p>It first tries {@link System#loadLibrary(String)} (honoring {@code java.library.path}),
 * then falls back to extracting the platform-specific library bundled at
 * {@code META-INF/native/<platform>/} on the classpath into a temporary file and loading
 * it via {@link System#load(String)}.
 */
public final class NativeLibraryLoader {

    private static final String LIBRARY_BASE_NAME = "wasm34j_native";

    private static volatile boolean loaded;
    private static volatile Throwable failure;

    private NativeLibraryLoader() {
    }

    /**
     * Ensures the native library is loaded, retrying is not attempted after a failure.
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

    private static void load() throws IOException {
        try {
            System.loadLibrary(LIBRARY_BASE_NAME);
            return;
        } catch (final UnsatisfiedLinkError ignored) {
            // Fall back to extracting the bundled library from the classpath.
        }

        final String resourcePath = NativePlatform.resourcePath(LIBRARY_BASE_NAME);
        try (InputStream in = NativeLibraryLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException(
                        "Native library not found on classpath for platform "
                                + NativePlatform.platform() + " (expected " + resourcePath + ")");
            }
            final String fileName =
                    resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
            final Path tempDir = Files.createTempDirectory("wasm34j-native");
            final Path tempFile = tempDir.resolve(fileName);
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            tempFile.toFile().deleteOnExit();
            tempDir.toFile().deleteOnExit();
            System.load(tempFile.toAbsolutePath().toString());
        }
    }

    /**
     * @return whether the bundled native library resource exists for the current platform
     *     (without attempting to load it)
     */
    public static boolean isResourceAvailable() {
        return NativeLibraryLoader.class.getResource(
                NativePlatform.resourcePath(LIBRARY_BASE_NAME)) != null;
    }
}
