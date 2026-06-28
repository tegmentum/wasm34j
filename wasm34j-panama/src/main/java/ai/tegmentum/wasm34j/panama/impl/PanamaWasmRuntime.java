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
package ai.tegmentum.wasm34j.panama.impl;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import ai.tegmentum.wasm34j.WebAssemblyModule;
import ai.tegmentum.wasm34j.WebAssemblyRuntime;
import ai.tegmentum.wasm34j.exception.WasmException;
import ai.tegmentum.wasm34j.internal.BuildInfo;
import ai.tegmentum.wasm34j.panama.internal.Wasm3Library;

/**
 * Panama-backed {@link WebAssemblyRuntime}, wrapping a wasm3 {@code IM3Environment} pointer.
 * Closing frees the native environment.
 */
public final class PanamaWasmRuntime implements WebAssemblyRuntime {

    // wasm3 exposes its version only as a compile-time macro, so it is read from the
    // Maven-filtered build resource (shared with the JNI backend) rather than hardcoded.
    private static final String ENGINE_VERSION = BuildInfo.wasm3Version();

    private final MemorySegment environment;
    private boolean closed;

    public PanamaWasmRuntime() {
        this.environment = Wasm3Library.newEnvironment();
        if (environment.address() == 0) {
            throw new WasmException("Failed to create wasm3 environment");
        }
    }

    @Override
    public WebAssemblyModule compile(final byte[] wasmBytes) {
        ensureOpen();
        // The module's wasm bytes must outlive the module; they live in this arena, whose
        // ownership passes to the module (and then the instance).
        final Arena moduleArena = Arena.ofShared();
        try {
            final MemorySegment module =
                    Wasm3Library.parseModule(environment, wasmBytes, moduleArena);
            return new PanamaWasmModule(environment, module, moduleArena);
        } catch (final RuntimeException e) {
            moduleArena.close();
            throw e;
        }
    }

    @Override
    public String engineVersion() {
        return ENGINE_VERSION;
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            Wasm3Library.freeEnvironment(environment);
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new WasmException("Runtime has been closed");
        }
    }
}
