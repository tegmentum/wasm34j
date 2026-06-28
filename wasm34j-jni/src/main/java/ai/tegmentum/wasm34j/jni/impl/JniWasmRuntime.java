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
package ai.tegmentum.wasm34j.jni.impl;

import ai.tegmentum.wasm34j.WebAssemblyModule;
import ai.tegmentum.wasm34j.WebAssemblyRuntime;
import ai.tegmentum.wasm34j.exception.WasmException;
import ai.tegmentum.wasm34j.jni.internal.Wasm3Native;

/**
 * JNI-backed {@link WebAssemblyRuntime}, wrapping a wasm3 {@code IM3Environment} handle. Closing
 * frees the native environment.
 */
public final class JniWasmRuntime implements WebAssemblyRuntime {

    private final long environment;
    private boolean closed;

    public JniWasmRuntime() {
        this.environment = Wasm3Native.newEnvironment();
        if (environment == 0) {
            throw new WasmException("Failed to create wasm3 environment");
        }
    }

    @Override
    public WebAssemblyModule compile(final byte[] wasmBytes) {
        ensureOpen();
        final long[] handles = Wasm3Native.parseModule(environment, wasmBytes);
        return new JniWasmModule(environment, handles[0], handles[1]);
    }

    @Override
    public String engineVersion() {
        return Wasm3Native.version();
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            Wasm3Native.freeEnvironment(environment);
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new WasmException("Runtime has been closed");
        }
    }
}
