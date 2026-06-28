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
package ai.tegmentum.wasm34j.panama;

import ai.tegmentum.wasm34j.WebAssemblyRuntime;
import ai.tegmentum.wasm34j.exception.WasmException;
import ai.tegmentum.wasm34j.internal.NativeLibrary;
import ai.tegmentum.wasm34j.panama.impl.PanamaWasmRuntime;
import ai.tegmentum.wasm34j.spi.RuntimeProvider;

/**
 * {@link RuntimeProvider} that creates Panama (FFM) backed wasm3 runtimes. Discovered via {@link
 * java.util.ServiceLoader}.
 *
 * <p>Ranked above the JNI provider so it is preferred when running on a JVM with the Foreign
 * Function and Memory API (Java 22+).
 */
public final class PanamaRuntimeProvider implements RuntimeProvider {

    private static final int PRIORITY = 200;
    private static final int MINIMUM_JAVA_VERSION = 22;

    @Override
    public String name() {
        return "panama";
    }

    @Override
    public int priority() {
        return PRIORITY;
    }

    @Override
    public boolean isAvailable() {
        return Runtime.version().feature() >= MINIMUM_JAVA_VERSION
                && NativeLibrary.isResourceAvailable(PanamaRuntimeProvider.class);
    }

    @Override
    public WebAssemblyRuntime createRuntime() {
        try {
            return new PanamaWasmRuntime();
        } catch (final WasmException e) {
            throw e;
        } catch (final Throwable t) {
            throw new WasmException("Failed to create Panama wasm3 runtime", t);
        }
    }
}
