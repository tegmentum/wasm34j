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

import java.util.Optional;

import ai.tegmentum.wasm34j.WasmGlobal;
import ai.tegmentum.wasm34j.WebAssemblyFunction;
import ai.tegmentum.wasm34j.WebAssemblyInstance;
import ai.tegmentum.wasm34j.WebAssemblyMemory;
import ai.tegmentum.wasm34j.jni.internal.HostFunctionRegistry;
import ai.tegmentum.wasm34j.jni.internal.Wasm3Native;

/**
 * JNI-backed {@link WebAssemblyInstance}, wrapping a wasm3 {@code IM3Runtime} handle into which a
 * module has been loaded, along with the module handle (for global lookups), the byte buffer
 * backing the module, and any host-function registry ids.
 *
 * <p>Closing frees the native runtime (which frees the loaded module), then the byte buffer, and
 * unregisters host functions.
 */
final class JniWasmInstance implements WebAssemblyInstance {

    private final long runtime;
    private final long module;
    private final long buffer;
    private final int[] hostIds;
    private boolean closed;

    JniWasmInstance(final long runtime, final long module, final long buffer, final int[] hostIds) {
        this.runtime = runtime;
        this.module = module;
        this.buffer = buffer;
        this.hostIds = hostIds;
    }

    @Override
    public Optional<WebAssemblyFunction> findFunction(final String name) {
        final long fn = Wasm3Native.findFunction(runtime, name);
        return fn == 0 ? Optional.empty() : Optional.of(new JniWasmFunction(fn));
    }

    @Override
    public Optional<WebAssemblyMemory> findMemory(final String name) {
        // wasm3 exposes a single memory; presence is detected via the direct buffer.
        return Wasm3Native.memoryBuffer(runtime) == null
                ? Optional.empty()
                : Optional.of(new JniWasmMemory(runtime));
    }

    @Override
    public Optional<WasmGlobal> findGlobal(final String name) {
        final long global = Wasm3Native.findGlobal(module, name);
        return global == 0 ? Optional.empty() : Optional.of(new JniWasmGlobal(global));
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        Wasm3Native.freeRuntime(runtime);
        Wasm3Native.freeBuffer(buffer);
        for (final int id : hostIds) {
            HostFunctionRegistry.unregister(id);
        }
    }
}
