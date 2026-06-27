package ai.tegmentum.wasm34j.jni.internal;

import ai.tegmentum.wasm34j.FunctionType;
import ai.tegmentum.wasm34j.HostFunction;
import ai.tegmentum.wasm34j.ValueType;
import ai.tegmentum.wasm34j.WasmValue;
import ai.tegmentum.wasm34j.exception.WasmException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Process-wide registry of host functions linked through the JNI backend.
 *
 * <p>Each registered host function gets an integer id, which is passed to wasm3 as the
 * import {@code userdata}. The native trampoline calls {@link #dispatch(int, long[])} with
 * that id and the raw argument bits; this class reconstructs typed values, invokes the
 * {@link HostFunction}, and returns the raw result bits.
 */
public final class HostFunctionRegistry {

    private static final ConcurrentHashMap<Integer, Entry> REGISTRY = new ConcurrentHashMap<>();
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    static {
        // Ensure the native library is loaded, then hand the trampoline our dispatch method.
        Wasm3Native.ensureInitialized();
        Wasm3Native.nativeInitDispatch(HostFunctionRegistry.class);
    }

    private HostFunctionRegistry() {
    }

    /** Forces class initialization (native dispatch wiring). */
    public static void ensureInitialized() {
        // No-op; triggering <clinit> is the point.
    }

    public static int register(final HostFunction function, final FunctionType type) {
        final int id = NEXT_ID.getAndIncrement();
        REGISTRY.put(id, new Entry(function, type));
        return id;
    }

    public static void unregister(final int id) {
        REGISTRY.remove(id);
    }

    /**
     * Builds a wasm3 link signature ({@code ret(args)}) for a host function type.
     *
     * @param type the function type (at most one result is supported by wasm3 signatures)
     * @return the signature string
     */
    public static String signature(final FunctionType type) {
        return ai.tegmentum.wasm34j.internal.Signatures.wasm3(type);
    }

    /** Invoked from native code. Reconstructs args, calls the host function, returns raw results. */
    public static long[] dispatch(final int id, final long[] rawArgs) {
        final Entry entry = REGISTRY.get(id);
        if (entry == null) {
            throw new WasmException("No host function registered for id " + id);
        }
        final ValueType[] paramTypes = entry.type.parameterTypes();
        final WasmValue[] args = new WasmValue[rawArgs.length];
        for (int i = 0; i < rawArgs.length; i++) {
            args[i] = WasmValue.ofRaw(paramTypes[i], rawArgs[i]);
        }
        final WasmValue[] results = entry.function.call(args);
        final long[] raw = new long[results == null ? 0 : results.length];
        for (int i = 0; i < raw.length; i++) {
            raw[i] = results[i].rawBits();
        }
        return raw;
    }

    private static final class Entry {
        private final HostFunction function;
        private final FunctionType type;

        Entry(final HostFunction function, final FunctionType type) {
            this.function = function;
            this.type = type;
        }
    }
}
