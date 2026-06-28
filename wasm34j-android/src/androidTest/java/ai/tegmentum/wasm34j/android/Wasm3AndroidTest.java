package ai.tegmentum.wasm34j.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import ai.tegmentum.wasm34j.RuntimeFactory;
import ai.tegmentum.wasm34j.WebAssemblyFunction;
import ai.tegmentum.wasm34j.WebAssemblyInstance;
import ai.tegmentum.wasm34j.WebAssemblyModule;
import ai.tegmentum.wasm34j.WebAssemblyRuntime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * On-device smoke test: loads a real {@code .wasm} module and calls an exported function
 * through the JNI backend, proving the NDK-built native library loads and runs on Android.
 */
@RunWith(AndroidJUnit4.class)
public class Wasm3AndroidTest {

    private static byte[] readAsset(final String name) throws IOException {
        final Context context = InstrumentationRegistry.getInstrumentation().getContext();
        try (InputStream in = context.getAssets().open(name)) {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }

    @Test
    public void callsExportedAddFunction() throws IOException {
        final byte[] wasm = readAsset("add.wasm");

        try (WebAssemblyRuntime runtime = RuntimeFactory.create();
                WebAssemblyModule module = runtime.compile(wasm);
                WebAssemblyInstance instance = module.instantiate()) {

            assertNotNull(runtime.engineVersion());

            final WebAssemblyFunction add = instance.getFunction("add");
            assertEquals(42, add.invoke(20, 22));
        }
    }
}
