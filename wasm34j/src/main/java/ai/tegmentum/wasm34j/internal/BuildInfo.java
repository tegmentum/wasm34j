package ai.tegmentum.wasm34j.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Exposes build-time versions baked into {@code wasm34j.properties} (Maven-filtered), so all
 * backends report the same engine version without hardcoding it.
 */
public final class BuildInfo {

    private static final String WASM3_VERSION;
    private static final String WASM34J_VERSION;

    static {
        final Properties props = new Properties();
        try (InputStream in = BuildInfo.class.getResourceAsStream("/wasm34j.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (final IOException ignored) {
            // Fall through to defaults.
        }
        WASM3_VERSION = props.getProperty("wasm3.version", "unknown");
        WASM34J_VERSION = props.getProperty("wasm34j.version", "unknown");
    }

    private BuildInfo() {
    }

    /** @return the bundled wasm3 engine version. */
    public static String wasm3Version() {
        return WASM3_VERSION;
    }

    /** @return the wasm34j library version. */
    public static String wasm34jVersion() {
        return WASM34J_VERSION;
    }
}
