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
package ai.tegmentum.wasm34j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import ai.tegmentum.wasm34j.exception.WasmException;

/** Exercises the broader API (memory, globals, host functions) on the JNI backend. */
class FeaturesTest {

    private static byte[] load(final String resource) throws IOException {
        try (InputStream in = FeaturesTest.class.getResourceAsStream(resource)) {
            assertThat(in).as(resource).isNotNull();
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }
    }

    private static int readLeInt(final byte[] b) {
        return (b[0] & 0xff) | ((b[1] & 0xff) << 8) | ((b[2] & 0xff) << 16) | ((b[3] & 0xff) << 24);
    }

    private static byte[] leInt(final int v) {
        return new byte[] {(byte) v, (byte) (v >> 8), (byte) (v >> 16), (byte) (v >> 24)};
    }

    @Test
    void memoryReadWriteRoundTrip() throws IOException {
        final byte[] wasm = load("/mem.wasm");
        try (WebAssemblyRuntime runtime = RuntimeFactory.create();
                WebAssemblyModule module = runtime.compile(wasm);
                WebAssemblyInstance instance = module.instantiate()) {

            final WebAssemblyMemory memory =
                    instance.findMemory("memory")
                            .orElseThrow(() -> new AssertionError("no memory"));
            assertThat(memory.byteSize()).isEqualTo(65536);

            // Java writes to memory; wasm reads it back via load(offset).
            memory.write(8, leInt(0x04030201));
            assertThat(instance.getFunction("load").invoke(8)).isEqualTo(0x04030201);

            // wasm writes via store(offset, value); Java reads it back.
            instance.getFunction("store").invoke(16, 999);
            assertThat(readLeInt(memory.read(16, 4))).isEqualTo(999);
        }
    }

    @Test
    void globalGetSet() throws IOException {
        final byte[] wasm = load("/global.wasm");
        try (WebAssemblyRuntime runtime = RuntimeFactory.create();
                WebAssemblyModule module = runtime.compile(wasm);
                WebAssemblyInstance instance = module.instantiate()) {

            final WasmGlobal counter =
                    instance.findGlobal("counter")
                            .orElseThrow(() -> new AssertionError("no counter global"));
            assertThat(counter.type()).isEqualTo(ValueType.I32);
            assertThat(counter.get().asInt()).isEqualTo(7);

            counter.set(WasmValue.i32(42));
            assertThat(counter.get().asInt()).isEqualTo(42);
            assertThat(instance.getFunction("getCounter").invoke()).isEqualTo(42);

            final WasmGlobal constant =
                    instance.findGlobal("constant")
                            .orElseThrow(() -> new AssertionError("no constant global"));
            assertThat(constant.get().asInt()).isEqualTo(99);

            // wasm3 does not currently enforce global immutability (the check is a TODO
            // upstream), but a type mismatch is rejected.
            assertThatThrownBy(() -> counter.set(WasmValue.i64(1L)))
                    .isInstanceOf(WasmException.class);
        }
    }

    @Test
    void hostFunctionImports() throws IOException {
        final byte[] wasm = load("/host.wasm");
        final int[] logged = new int[] {-1};

        final WasmImports imports =
                WasmImports.builder()
                        .function(
                                "env",
                                "add",
                                FunctionType.of(
                                        new ValueType[] {ValueType.I32, ValueType.I32},
                                        new ValueType[] {ValueType.I32}),
                                args ->
                                        new WasmValue[] {
                                            WasmValue.i32(args[0].asInt() + args[1].asInt())
                                        })
                        .function(
                                "env",
                                "log",
                                FunctionType.of(new ValueType[] {ValueType.I32}, new ValueType[0]),
                                args -> {
                                    logged[0] = args[0].asInt();
                                    return new WasmValue[0];
                                })
                        .build();

        try (WebAssemblyRuntime runtime = RuntimeFactory.create();
                WebAssemblyModule module = runtime.compile(wasm);
                WebAssemblyInstance instance = module.instantiate(imports)) {

            assertThat(instance.getFunction("callAdd").invoke(20, 22)).isEqualTo(42);

            instance.getFunction("doLog").invoke(7);
            assertThat(logged[0]).isEqualTo(7);
        }
    }
}
