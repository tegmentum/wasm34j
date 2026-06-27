package ai.tegmentum.wasm34j.panama;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.tegmentum.wasm34j.FunctionType;
import ai.tegmentum.wasm34j.RuntimeFactory;
import ai.tegmentum.wasm34j.ValueType;
import ai.tegmentum.wasm34j.WasmGlobal;
import ai.tegmentum.wasm34j.WasmImports;
import ai.tegmentum.wasm34j.WasmValue;
import ai.tegmentum.wasm34j.WebAssemblyInstance;
import ai.tegmentum.wasm34j.WebAssemblyMemory;
import ai.tegmentum.wasm34j.WebAssemblyModule;
import ai.tegmentum.wasm34j.WebAssemblyRuntime;
import ai.tegmentum.wasm34j.exception.WasmException;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

/** Exercises the broader API (memory, globals, host functions) on the Panama backend. */
class PanamaFeaturesTest {

    private static byte[] load(final String resource) throws IOException {
        try (InputStream in = PanamaFeaturesTest.class.getResourceAsStream(resource)) {
            assertThat(in).as(resource).isNotNull();
            return in.readAllBytes();
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

            final WebAssemblyMemory memory = instance.findMemory("memory").orElseThrow();
            assertThat(memory.byteSize()).isEqualTo(65536);

            memory.write(8, leInt(0x04030201));
            assertThat(instance.getFunction("load").invoke(8)).isEqualTo(0x04030201);

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

            final WasmGlobal counter = instance.findGlobal("counter").orElseThrow();
            assertThat(counter.get().asInt()).isEqualTo(7);
            counter.set(WasmValue.i32(42));
            assertThat(counter.get().asInt()).isEqualTo(42);
            assertThat(instance.getFunction("getCounter").invoke()).isEqualTo(42);

            final WasmGlobal constant = instance.findGlobal("constant").orElseThrow();
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
        final int[] logged = {-1};

        final WasmImports imports = WasmImports.builder()
                .function(
                        "env",
                        "add",
                        FunctionType.of(
                                new ValueType[] {ValueType.I32, ValueType.I32},
                                new ValueType[] {ValueType.I32}),
                        args -> new WasmValue[] {WasmValue.i32(args[0].asInt() + args[1].asInt())})
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
