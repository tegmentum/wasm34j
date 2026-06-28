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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import ai.tegmentum.wasm34j.RuntimeFactory;
import ai.tegmentum.wasm34j.WebAssemblyInstance;
import ai.tegmentum.wasm34j.WebAssemblyModule;
import ai.tegmentum.wasm34j.WebAssemblyRuntime;

/**
 * Panama-backend lifecycle test: many compile/instantiate/close cycles must not leak off-heap
 * arenas or crash, and close must be idempotent.
 */
class PanamaLifecycleTest {

    private static byte[] addWasm() throws IOException {
        try (InputStream in = PanamaLifecycleTest.class.getResourceAsStream("/add.wasm")) {
            assertThat(in).isNotNull();
            return in.readAllBytes();
        }
    }

    @Test
    void manyCompileInstantiateCloseCycles() throws IOException {
        final byte[] wasm = addWasm();
        try (WebAssemblyRuntime runtime = RuntimeFactory.create()) {
            for (int i = 0; i < 2000; i++) {
                try (WebAssemblyModule module = runtime.compile(wasm);
                        WebAssemblyInstance instance = module.instantiate()) {
                    assertThat(instance.getFunction("add").invoke(20, 22)).isEqualTo(42);
                }
            }
        }
    }

    @Test
    void doubleCloseIsSafe() throws IOException {
        final byte[] wasm = addWasm();
        try (WebAssemblyRuntime runtime = RuntimeFactory.create()) {
            final WebAssemblyModule module = runtime.compile(wasm);
            final WebAssemblyInstance instance = module.instantiate();
            assertThatCode(
                            () -> {
                                instance.close();
                                instance.close();
                                module.close();
                            })
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void closeModuleWithoutInstantiating() throws IOException {
        final byte[] wasm = addWasm();
        try (WebAssemblyRuntime runtime = RuntimeFactory.create()) {
            assertThatCode(
                            () -> {
                                for (int i = 0; i < 1000; i++) {
                                    runtime.compile(wasm).close();
                                }
                            })
                    .doesNotThrowAnyException();
        }
    }
}
