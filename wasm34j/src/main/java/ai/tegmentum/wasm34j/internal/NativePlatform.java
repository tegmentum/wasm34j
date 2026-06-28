/*
 * Copyright (c) 2026 Tegmentum AI, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.tegmentum.wasm34j.internal;

import java.util.Locale;

/**
 * Normalizes the host OS/architecture into the platform identifiers used to name the bundled native
 * library and its classpath resource. These identifiers must match the {@code
 * native.platform.classifier} values produced by the Maven build.
 *
 * <p>Shared by all backends (JNI, Panama).
 */
public final class NativePlatform {

    private NativePlatform() {}

    /**
     * @return {@code "windows"}, {@code "darwin"}, or {@code "linux"}.
     */
    public static String osName() {
        final String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return "windows";
        }
        if (os.contains("mac") || os.contains("darwin")) {
            return "darwin";
        }
        return "linux";
    }

    /**
     * @return {@code "x86_64"} or {@code "aarch64"} (best-effort for other arches).
     */
    public static String arch() {
        final String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        if ("amd64".equals(arch) || "x86_64".equals(arch) || "x64".equals(arch)) {
            return "x86_64";
        }
        if ("aarch64".equals(arch) || "arm64".equals(arch)) {
            return "aarch64";
        }
        return arch;
    }

    /**
     * @return the platform classifier, e.g. {@code "darwin-aarch64"}.
     */
    public static String platform() {
        return osName() + "-" + arch();
    }

    /**
     * @param baseName the library base name, e.g. {@code "wasm34j_native"}
     * @return the platform-specific shared-library file name
     */
    public static String libraryFileName(final String baseName) {
        switch (osName()) {
            case "windows":
                return baseName + ".dll";
            case "darwin":
                return "lib" + baseName + ".dylib";
            default:
                return "lib" + baseName + ".so";
        }
    }

    /**
     * @param baseName the library base name, e.g. {@code "wasm34j_native"}
     * @return the absolute classpath resource path of the bundled native library
     */
    public static String resourcePath(final String baseName) {
        return "/META-INF/native/" + platform() + "/" + libraryFileName(baseName);
    }
}
