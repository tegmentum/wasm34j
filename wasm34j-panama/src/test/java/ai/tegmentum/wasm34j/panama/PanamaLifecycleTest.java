package ai.tegmentum.wasm34j.panama;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import ai.tegmentum.wasm34j.RuntimeFactory;
import ai.tegmentum.wasm34j.WebAssemblyInstance;
import ai.tegmentum.wasm34j.WebAssemblyModule;
import ai.tegmentum.wasm34j.WebAssemblyRuntime;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

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
            assertThatCode(() -> {
                instance.close();
                instance.close();
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
                    runtime.compile(wasm).close();
                }
            }).doesNotThrowAnyException();
        }
    }
}
