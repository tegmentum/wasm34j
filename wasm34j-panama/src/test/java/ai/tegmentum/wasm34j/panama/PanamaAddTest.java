package ai.tegmentum.wasm34j.panama;

import static org.assertj.core.api.Assertions.assertThat;

import ai.tegmentum.wasm34j.RuntimeFactory;
import ai.tegmentum.wasm34j.ValueType;
import ai.tegmentum.wasm34j.WasmValue;
import ai.tegmentum.wasm34j.WebAssemblyFunction;
import ai.tegmentum.wasm34j.WebAssemblyInstance;
import ai.tegmentum.wasm34j.WebAssemblyModule;
import ai.tegmentum.wasm34j.WebAssemblyRuntime;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

/**
 * Vertical slice for the Panama (FFM) backend: load a real {@code .wasm} module and call
 * an exported function via foreign downcalls into wasm3.
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
            assertThat(runtime).isInstanceOf(
                    ai.tegmentum.wasm34j.panama.impl.PanamaWasmRuntime.class);
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
