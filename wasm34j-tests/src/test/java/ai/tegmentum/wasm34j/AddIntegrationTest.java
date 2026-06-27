package ai.tegmentum.wasm34j;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

/**
 * End-to-end vertical slice: load a real {@code .wasm} module and call an exported
 * function through the JNI backend.
 */
class AddIntegrationTest {

    private static byte[] loadWasm(final String resource) throws IOException {
        try (InputStream in = AddIntegrationTest.class.getResourceAsStream(resource)) {
            assertThat(in).as("test resource " + resource).isNotNull();
            return readAll(in);
        }
    }

    private static byte[] readAll(final InputStream in) throws IOException {
        final java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        final byte[] buffer = new byte[4096];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    @Test
    void callsExportedAddFunction() throws IOException {
        final byte[] wasm = loadWasm("/add.wasm");

        try (WebAssemblyRuntime runtime = RuntimeFactory.create();
                WebAssemblyModule module = runtime.compile(wasm);
                WebAssemblyInstance instance = module.instantiate()) {

            assertThat(runtime.engineVersion()).isNotBlank();

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
