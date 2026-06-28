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
package ai.tegmentum.wasm34j.jni;

import ai.tegmentum.wasm34j.WebAssemblyRuntime;
import ai.tegmentum.wasm34j.exception.WasmException;
import ai.tegmentum.wasm34j.jni.impl.JniWasmRuntime;
import ai.tegmentum.wasm34j.jni.internal.NativeLibraryLoader;
import ai.tegmentum.wasm34j.spi.RuntimeProvider;

/**
 * {@link RuntimeProvider} that creates JNI-backed wasm3 runtimes. Discovered via {@link
 * java.util.ServiceLoader}.
 */
public final class JniRuntimeProvider implements RuntimeProvider {

    private static final int PRIORITY = 100;

    @Override
    public String name() {
        return "jni";
    }

    @Override
    public int priority() {
        return PRIORITY;
    }

    @Override
    public boolean isAvailable() {
        return NativeLibraryLoader.isResourceAvailable();
    }

    @Override
    public WebAssemblyRuntime createRuntime() {
        try {
            return new JniWasmRuntime();
        } catch (final WasmException e) {
            throw e;
        } catch (final Throwable t) {
            throw new WasmException("Failed to create JNI wasm3 runtime", t);
        }
    }
}
