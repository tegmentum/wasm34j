package ai.tegmentum.wasm34j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

/**
 * Exercises the module/instance native-memory lifecycle: many compile/instantiate/close
 * cycles must not leak or crash, and close must be idempotent and safe in any order.
 */
class LifecycleTest {

    private static byte[] addWasm() throws IOException {
        try (InputStream in = LifecycleTest.class.getResourceAsStream("/add.wasm")) {
            assertThat(in).isNotNull();
            final java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            final byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }

    @Test
    void manyCompileInstantiateCloseCycles() throws IOException {
        final byte[] wasm = addWasm();
        try (WebAssemblyRuntime runtime = RuntimeFactory.create()) {
            for (int i = 0; i < 2000; i++) {
                try (WebAssemblyModule module = runtime.compile(wasm);
                        WebAssemblyInstance instance = module.instantiate()) {
                    final Object result = instance.getFunction("add").invoke(20, 22);
                    assertThat(result).isEqualTo(42);
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
            assertThatCode(() -> {
                instance.close();
                instance.close();
                module.close();
                module.close();
            }).doesNotThrowAnyException();
        }
    }

    @Test
    void closeModuleWithoutInstantiating() throws IOException {
        final byte[] wasm = addWasm();
        try (WebAssemblyRuntime runtime = RuntimeFactory.create()) {
            assertThatCode(() -> {
                for (int i = 0; i < 1000; i++) {
                    final WebAssemblyModule module = runtime.compile(wasm);
                    module.close();
                }
            }).doesNotThrowAnyException();
        }
    }
}
