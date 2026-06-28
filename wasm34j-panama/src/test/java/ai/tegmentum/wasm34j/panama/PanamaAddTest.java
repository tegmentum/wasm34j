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

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import ai.tegmentum.wasm34j.RuntimeFactory;
import ai.tegmentum.wasm34j.ValueType;
import ai.tegmentum.wasm34j.WasmValue;
import ai.tegmentum.wasm34j.WebAssemblyFunction;
import ai.tegmentum.wasm34j.WebAssemblyInstance;
import ai.tegmentum.wasm34j.WebAssemblyModule;
import ai.tegmentum.wasm34j.WebAssemblyRuntime;

/**
 * Vertical slice for the Panama (FFM) backend: load a real {@code .wasm} module and call an
 * exported function via foreign downcalls into wasm3.
 */
class PanamaAddTest {

    private static byte[] loadWasm(final String resource) throws IOException {
        try (InputStream in = PanamaAddTest.class.getResourceAsStream(resource)) {
            assertThat(in).as("test resource " + resource).isNotNull();
            return in.readAllBytes();
        }
    }

    @Test
    void selectsPanamaProvider() {
        // The surefire config forces wasm34j.runtime=panama; confirm that is what loads.
        try (WebAssemblyRuntime runtime = RuntimeFactory.create()) {
            assertThat(runtime)
                    .isInstanceOf(ai.tegmentum.wasm34j.panama.impl.PanamaWasmRuntime.class);
        }
    }

    @Test
    void callsExportedAddFunction() throws IOException {
        final byte[] wasm = loadWasm("/add.wasm");

        try (WebAssemblyRuntime runtime = RuntimeFactory.create();
                WebAssemblyModule module = runtime.compile(wasm);
                WebAssemblyInstance instance = module.instantiate()) {

            final WebAssemblyFunction add = instance.getFunction("add");
            assertThat(add.parameterCount()).isEqualTo(2);
            assertThat(add.resultCount()).isEqualTo(1);
            assertThat(add.parameterType(0)).isEqualTo(ValueType.I32);
            assertThat(add.resultType(0)).isEqualTo(ValueType.I32);

            final WasmValue[] typed = add.call(WasmValue.i32(20), WasmValue.i32(22));
            assertThat(typed).hasSize(1);
            assertThat(typed[0].asInt()).isEqualTo(42);

            assertThat(add.invoke(20, 22)).isEqualTo(42);
        }
    }

    @Test
    void missingFunctionIsReportedAbsent() throws IOException {
        final byte[] wasm = loadWasm("/add.wasm");
        try (WebAssemblyRuntime runtime = RuntimeFactory.create();
                WebAssemblyModule module = runtime.compile(wasm);
                WebAssemblyInstance instance = module.instantiate()) {
            assertThat(instance.findFunction("does_not_exist")).isEmpty();
        }
    }
}
