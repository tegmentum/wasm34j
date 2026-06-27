package ai.tegmentum.wasm34j.internal;

import ai.tegmentum.wasm34j.exception.WasmException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Locates and extracts the bundled native library so a backend can load it.
 *
 * <p>The library is shipped as a classpath resource at
 * {@code META-INF/native/<platform>/<libfile>} (see {@code wasm34j-native}). Backends
 * either hand the extracted path to {@code System.load} (JNI) or to
 * {@code SymbolLookup.libraryLookup} (Panama).
 *
 * <p>Shared by all backends.
 */
public final class NativeLibrary {

    /** Base name of the bundled native library (same one for every backend). */
    public static final String BASE_NAME = "wasm34j_native";

    private NativeLibrary() {
    }

    /**
     * @param anchor a class whose class loader can see the bundled resource
     * @return whether the native library resource exists for the current platform
     */
    public static boolean isResourceAvailable(final Class<?> anchor) {
        return anchor.getResource(NativePlatform.resourcePath(BASE_NAME)) != null;
    }

    /**
     * Extracts the bundled native library to a temporary file scheduled for deletion on
     * JVM exit.
     *
     * @param anchor a class whose class loader can see the bundled resource
     * @return the path to the extracted library
     * @throws WasmException if the resource is missing or extraction fails
     */
    public static Path extractToTempFile(final Class<?> anchor) {
        final String resourcePath = NativePlatform.resourcePath(BASE_NAME);
        try (InputStream in = anchor.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new WasmException(
                        "Native library not found on classpath for platform "
                                + NativePlatform.platform() + " (expected " + resourcePath + ")");
            }
            final String fileName = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
            final Path tempDir = Files.createTempDirectory("wasm34j-native");
            final Path tempFile = tempDir.resolve(fileName);
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            tempFile.toFile().deleteOnExit();
            tempDir.toFile().deleteOnExit();
            return tempFile;
        } catch (final IOException e) {
            throw new WasmException("Failed to extract the wasm34j native library", e);
        }
    }
}
