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
package ai.tegmentum.wasm34j.jni.internal;

import java.nio.file.Path;

import ai.tegmentum.wasm34j.exception.WasmException;
import ai.tegmentum.wasm34j.internal.NativeLibrary;

/**
 * Loads the wasm34j native library exactly once per JVM for the JNI backend.
 *
 * <p>It first tries {@link System#loadLibrary(String)} (honoring {@code java.library.path}), then
 * falls back to extracting the platform-specific library bundled on the classpath and loading it
 * via {@link System#load(String)}.
 */
public final class NativeLibraryLoader {

    private static final Object LOCK = new Object();
    private static volatile boolean loaded;
    private static volatile Throwable failure;

    private NativeLibraryLoader() {}

    /**
     * Ensures the native library is loaded. Loading is attempted once; a prior failure is rethrown
     * rather than retried.
     *
     * @throws WasmException if the library cannot be loaded
     */
    public static void ensureLoaded() {
        synchronized (LOCK) {
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
     * @return whether the bundled native library resource exists for the current platform (without
     *     attempting to load it)
     */
    public static boolean isResourceAvailable() {
        return NativeLibrary.isResourceAvailable(NativeLibraryLoader.class);
    }
}
