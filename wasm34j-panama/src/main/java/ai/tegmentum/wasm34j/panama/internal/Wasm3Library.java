package ai.tegmentum.wasm34j.panama.internal;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

import ai.tegmentum.wasm34j.FunctionType;
import ai.tegmentum.wasm34j.HostFunction;
import ai.tegmentum.wasm34j.exception.WasmException;
import ai.tegmentum.wasm34j.internal.NativeLibrary;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Path;

/**
 * Foreign Function and Memory (Panama) binding to the wasm3 C API.
 *
 * <p>The bundled native library exports the raw {@code m3_*} symbols (the JNI glue lives
 * alongside them but is unused here), so this backend links the same library and makes
 * downcalls straight into wasm3 — no extra native code required.
 *
 * <p>A wasm3 {@code M3Result} is {@code const char*}: {@code NULL} on success, otherwise a
 * pointer to an error message.
 */
public final class Wasm3Library {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LOOKUP;

    private static final MethodHandle NEW_ENVIRONMENT;
    private static final MethodHandle FREE_ENVIRONMENT;
    private static final MethodHandle NEW_RUNTIME;
    private static final MethodHandle FREE_RUNTIME;
    private static final MethodHandle PARSE_MODULE;
    private static final MethodHandle FREE_MODULE;
    private static final MethodHandle LOAD_MODULE;
    private static final MethodHandle FIND_FUNCTION;
    private static final MethodHandle GET_ARG_COUNT;
    private static final MethodHandle GET_RET_COUNT;
    private static final MethodHandle GET_ARG_TYPE;
    private static final MethodHandle GET_RET_TYPE;
    private static final MethodHandle CALL;
    private static final MethodHandle GET_RESULTS;
    private static final MethodHandle GET_MEMORY;
    private static final MethodHandle GET_MEMORY_SIZE;
    private static final MethodHandle FIND_GLOBAL;
    private static final MethodHandle GET_GLOBAL;
    private static final MethodHandle SET_GLOBAL;
    private static final MethodHandle GET_GLOBAL_TYPE;
    private static final MethodHandle LINK_RAW_FUNCTION;

    /** Layout of {@code M3TaggedValue { int type; union{...} value; }} (4 + 4 pad + 8). */
    private static final MemoryLayout TAGGED_VALUE = MemoryLayout.structLayout(
            JAVA_INT.withName("type"),
            MemoryLayout.paddingLayout(4),
            JAVA_LONG.withName("value"));
    private static final long TAGGED_VALUE_OFFSET = 8L;

    /** M3RawCall: (IM3Runtime, IM3ImportContext, uint64_t* sp, void* mem) -> M3Result. */
    private static final FunctionDescriptor M3_RAW_CALL =
            FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS);
    private static final MethodHandle HOST_INVOKE;

    /** Returned (as a non-null M3Result) when a host function throws. */
    private static final MemorySegment HOST_ERROR =
            Arena.global().allocateFrom("host function threw a Java exception");

    static {
        // The library lives for the lifetime of the JVM, so it is looked up in the global arena.
        final Path library = NativeLibrary.extractToTempFile(Wasm3Library.class);
        LOOKUP = SymbolLookup.libraryLookup(library, Arena.global());

        NEW_ENVIRONMENT = downcall("m3_NewEnvironment", FunctionDescriptor.of(ADDRESS));
        FREE_ENVIRONMENT = downcall("m3_FreeEnvironment", FunctionDescriptor.ofVoid(ADDRESS));
        NEW_RUNTIME = downcall(
                "m3_NewRuntime", FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT, ADDRESS));
        FREE_RUNTIME = downcall("m3_FreeRuntime", FunctionDescriptor.ofVoid(ADDRESS));
        PARSE_MODULE = downcall(
                "m3_ParseModule", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, JAVA_INT));
        FREE_MODULE = downcall("m3_FreeModule", FunctionDescriptor.ofVoid(ADDRESS));
        LOAD_MODULE = downcall("m3_LoadModule", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));
        FIND_FUNCTION = downcall(
                "m3_FindFunction", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS));
        GET_ARG_COUNT = downcall("m3_GetArgCount", FunctionDescriptor.of(JAVA_INT, ADDRESS));
        GET_RET_COUNT = downcall("m3_GetRetCount", FunctionDescriptor.of(JAVA_INT, ADDRESS));
        GET_ARG_TYPE = downcall("m3_GetArgType", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));
        GET_RET_TYPE = downcall("m3_GetRetType", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));
        CALL = downcall("m3_Call", FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT, ADDRESS));
        GET_RESULTS = downcall(
                "m3_GetResults", FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT, ADDRESS));
        GET_MEMORY = downcall(
                "m3_GetMemory", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, JAVA_INT));
        GET_MEMORY_SIZE = downcall("m3_GetMemorySize", FunctionDescriptor.of(JAVA_INT, ADDRESS));
        FIND_GLOBAL = downcall("m3_FindGlobal", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));
        GET_GLOBAL = downcall("m3_GetGlobal", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));
        SET_GLOBAL = downcall("m3_SetGlobal", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));
        GET_GLOBAL_TYPE = downcall("m3_GetGlobalType", FunctionDescriptor.of(JAVA_INT, ADDRESS));
        LINK_RAW_FUNCTION = downcall(
                "m3_LinkRawFunction",
                FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS));

        try {
            HOST_INVOKE = MethodHandles.lookup().findVirtual(
                    HostFunctionBridge.class,
                    "invoke",
                    MethodType.methodType(
                            MemorySegment.class, MemorySegment.class, MemorySegment.class,
                            MemorySegment.class, MemorySegment.class));
        } catch (final ReflectiveOperationException e) {
            throw new WasmException("Failed to bind host-function upcall handle", e);
        }
    }

    private Wasm3Library() {
    }

    /** Forces class initialization (and therefore native library loading). */
    public static void ensureInitialized() {
        // No-op; triggering <clinit> is the point.
    }

    private static MethodHandle downcall(final String symbol, final FunctionDescriptor descriptor) {
        final MemorySegment address = LOOKUP.find(symbol)
                .orElseThrow(() -> new WasmException("wasm3 symbol not found: " + symbol));
        return LINKER.downcallHandle(address, descriptor);
    }

    public static MemorySegment newEnvironment() {
        try {
            return (MemorySegment) NEW_ENVIRONMENT.invokeExact();
        } catch (final Throwable t) {
            throw rethrow("m3_NewEnvironment", t);
        }
    }

    public static void freeEnvironment(final MemorySegment environment) {
        try {
            FREE_ENVIRONMENT.invokeExact(environment);
        } catch (final Throwable t) {
            throw rethrow("m3_FreeEnvironment", t);
        }
    }

    public static MemorySegment newRuntime(final MemorySegment environment, final int stackBytes) {
        try {
            return (MemorySegment) NEW_RUNTIME.invokeExact(environment, stackBytes, MemorySegment.NULL);
        } catch (final Throwable t) {
            throw rethrow("m3_NewRuntime", t);
        }
    }

    public static void freeRuntime(final MemorySegment runtime) {
        try {
            FREE_RUNTIME.invokeExact(runtime);
        } catch (final Throwable t) {
            throw rethrow("m3_FreeRuntime", t);
        }
    }

    /**
     * Parses a module, allocating its wasm bytes in {@code moduleArena}.
     *
     * <p>wasm3 retains a pointer to the bytes for the module's lifetime, so they live in the
     * caller-supplied arena, which the owning module/instance closes when done.
     */
    public static MemorySegment parseModule(
            final MemorySegment environment, final byte[] wasm, final Arena moduleArena) {
        final MemorySegment wasmSegment = moduleArena.allocate(wasm.length);
        MemorySegment.copy(wasm, 0, wasmSegment, JAVA_BYTE, 0, wasm.length);
        try (Arena temp = Arena.ofConfined()) {
            final MemorySegment outModule = temp.allocate(ADDRESS);
            final MemorySegment result = (MemorySegment) PARSE_MODULE.invokeExact(
                    environment, outModule, wasmSegment, wasm.length);
            checkError(result, "m3_ParseModule");
            return outModule.get(ADDRESS, 0);
        } catch (final WasmException e) {
            throw e;
        } catch (final Throwable t) {
            throw rethrow("m3_ParseModule", t);
        }
    }

    /** Frees an unloaded module (never successfully loaded into a runtime). */
    public static void freeModule(final MemorySegment module) {
        try {
            FREE_MODULE.invokeExact(module);
        } catch (final Throwable t) {
            throw rethrow("m3_FreeModule", t);
        }
    }

    public static void loadModule(final MemorySegment runtime, final MemorySegment module) {
        try {
            final MemorySegment result = (MemorySegment) LOAD_MODULE.invokeExact(runtime, module);
            checkError(result, "m3_LoadModule");
        } catch (final WasmException e) {
            throw e;
        } catch (final Throwable t) {
            throw rethrow("m3_LoadModule", t);
        }
    }

    /** @return the function pointer, or {@link MemorySegment#NULL} if no such export exists. */
    public static MemorySegment findFunction(final MemorySegment runtime, final String name) {
        try (Arena temp = Arena.ofConfined()) {
            final MemorySegment outFunction = temp.allocate(ADDRESS);
            final MemorySegment cName = temp.allocateFrom(name);
            final MemorySegment result =
                    (MemorySegment) FIND_FUNCTION.invokeExact(outFunction, runtime, cName);
            if (result.address() != 0) {
                return MemorySegment.NULL;
            }
            return outFunction.get(ADDRESS, 0);
        } catch (final Throwable t) {
            throw rethrow("m3_FindFunction", t);
        }
    }

    public static int getArgCount(final MemorySegment function) {
        try {
            return (int) GET_ARG_COUNT.invokeExact(function);
        } catch (final Throwable t) {
            throw rethrow("m3_GetArgCount", t);
        }
    }

    public static int getRetCount(final MemorySegment function) {
        try {
            return (int) GET_RET_COUNT.invokeExact(function);
        } catch (final Throwable t) {
            throw rethrow("m3_GetRetCount", t);
        }
    }

    public static int getArgType(final MemorySegment function, final int index) {
        try {
            return (int) GET_ARG_TYPE.invokeExact(function, index);
        } catch (final Throwable t) {
            throw rethrow("m3_GetArgType", t);
        }
    }

    public static int getRetType(final MemorySegment function, final int index) {
        try {
            return (int) GET_RET_TYPE.invokeExact(function, index);
        } catch (final Throwable t) {
            throw rethrow("m3_GetRetType", t);
        }
    }

    /**
     * Calls a function with raw 64-bit argument patterns and returns raw 64-bit result
     * patterns. i32/f32 values occupy the low 32 bits of each slot.
     */
    public static long[] call(final MemorySegment function, final long[] rawArgs) {
        try (Arena temp = Arena.ofConfined()) {
            final int argc = rawArgs.length;
            MemorySegment argPointers = MemorySegment.NULL;
            if (argc > 0) {
                argPointers = temp.allocate(ADDRESS, argc);
                for (int i = 0; i < argc; i++) {
                    final MemorySegment slot = temp.allocate(JAVA_LONG);
                    slot.set(JAVA_LONG, 0, rawArgs[i]);
                    argPointers.setAtIndex(ADDRESS, i, slot);
                }
            }

            final MemorySegment callResult =
                    (MemorySegment) CALL.invokeExact(function, argc, argPointers);
            checkError(callResult, "m3_Call");

            final int retc = (int) GET_RET_COUNT.invokeExact(function);
            if (retc == 0) {
                return new long[0];
            }

            final MemorySegment retSlots = temp.allocate(JAVA_LONG, retc);
            retSlots.fill((byte) 0); // ensure clean upper bits for i32/f32 results
            final MemorySegment retPointers = temp.allocate(ADDRESS, retc);
            for (int i = 0; i < retc; i++) {
                retPointers.setAtIndex(ADDRESS, i, retSlots.asSlice((long) i * JAVA_LONG.byteSize(), JAVA_LONG.byteSize()));
            }

            final MemorySegment resultsResult =
                    (MemorySegment) GET_RESULTS.invokeExact(function, retc, retPointers);
            checkError(resultsResult, "m3_GetResults");

            final long[] results = new long[retc];
            for (int i = 0; i < retc; i++) {
                results[i] = retSlots.getAtIndex(JAVA_LONG, i);
            }
            return results;
        } catch (final WasmException e) {
            throw e;
        } catch (final Throwable t) {
            throw rethrow("m3_Call", t);
        }
    }

    // --- Memory ---

    /** @return the live linear-memory segment (reinterpreted to its size), or NULL if none. */
    public static MemorySegment memorySegment(final MemorySegment runtime) {
        try (Arena temp = Arena.ofConfined()) {
            final MemorySegment sizeOut = temp.allocate(JAVA_INT);
            final MemorySegment base = (MemorySegment) GET_MEMORY.invokeExact(runtime, sizeOut, 0);
            if (base.address() == 0) {
                return MemorySegment.NULL;
            }
            final int size = sizeOut.get(JAVA_INT, 0);
            return base.reinterpret(Integer.toUnsignedLong(size));
        } catch (final Throwable t) {
            throw rethrow("m3_GetMemory", t);
        }
    }

    public static int memorySize(final MemorySegment runtime) {
        try {
            return (int) GET_MEMORY_SIZE.invokeExact(runtime);
        } catch (final Throwable t) {
            throw rethrow("m3_GetMemorySize", t);
        }
    }

    // --- Globals ---

    /** @return the global pointer, or {@link MemorySegment#NULL} if absent. */
    public static MemorySegment findGlobal(final MemorySegment module, final String name) {
        try (Arena temp = Arena.ofConfined()) {
            final MemorySegment cName = temp.allocateFrom(name);
            return (MemorySegment) FIND_GLOBAL.invokeExact(module, cName);
        } catch (final Throwable t) {
            throw rethrow("m3_FindGlobal", t);
        }
    }

    public static int globalType(final MemorySegment global) {
        try {
            return (int) GET_GLOBAL_TYPE.invokeExact(global);
        } catch (final Throwable t) {
            throw rethrow("m3_GetGlobalType", t);
        }
    }

    /** @return the global's value as raw 64-bit bits (i32/f32 in the low 32 bits). */
    public static long globalGet(final MemorySegment global) {
        try (Arena temp = Arena.ofConfined()) {
            final MemorySegment tagged = temp.allocate(TAGGED_VALUE);
            final MemorySegment result = (MemorySegment) GET_GLOBAL.invokeExact(global, tagged);
            checkError(result, "m3_GetGlobal");
            return tagged.get(JAVA_LONG, TAGGED_VALUE_OFFSET);
        } catch (final WasmException e) {
            throw e;
        } catch (final Throwable t) {
            throw rethrow("m3_GetGlobal", t);
        }
    }

    public static void globalSet(final MemorySegment global, final int type, final long bits) {
        try (Arena temp = Arena.ofConfined()) {
            final MemorySegment tagged = temp.allocate(TAGGED_VALUE);
            tagged.set(JAVA_INT, 0, type);
            tagged.set(JAVA_LONG, TAGGED_VALUE_OFFSET, bits);
            final MemorySegment result = (MemorySegment) SET_GLOBAL.invokeExact(global, tagged);
            checkError(result, "m3_SetGlobal");
        } catch (final WasmException e) {
            throw e;
        } catch (final Throwable t) {
            throw rethrow("m3_SetGlobal", t);
        }
    }

    // --- Host functions ---

    /**
     * Creates a native {@code M3RawCall} stub (in {@code arena}) that invokes the given host
     * function.
     */
    public static MemorySegment createHostUpcall(
            final HostFunction function, final FunctionType type, final Arena arena) {
        final HostFunctionBridge bridge = new HostFunctionBridge(function, type);
        final MethodHandle bound = HOST_INVOKE.bindTo(bridge);
        return LINKER.upcallStub(bound, M3_RAW_CALL, arena);
    }

    /** Links a host {@code M3RawCall} stub into a module import before it is loaded. */
    public static void linkRawFunction(
            final MemorySegment module,
            final String moduleName,
            final String functionName,
            final String signature,
            final MemorySegment rawCall) {
        try (Arena temp = Arena.ofConfined()) {
            final MemorySegment m = temp.allocateFrom(moduleName);
            final MemorySegment n = temp.allocateFrom(functionName);
            final MemorySegment s = temp.allocateFrom(signature);
            final MemorySegment result =
                    (MemorySegment) LINK_RAW_FUNCTION.invokeExact(module, m, n, s, rawCall);
            if (result.address() != 0) {
                final String message = result.reinterpret(Long.MAX_VALUE).getString(0);
                // A module that does not import this function is not an error.
                if (!message.contains("lookup failed")) {
                    throw new WasmException("m3_LinkRawFunction failed: " + message);
                }
            }
        } catch (final WasmException e) {
            throw e;
        } catch (final Throwable t) {
            throw rethrow("m3_LinkRawFunction", t);
        }
    }

    /** Adapts a {@link HostFunction} to the wasm3 {@code M3RawCall} calling convention. */
    private static final class HostFunctionBridge {

        private final HostFunction function;
        private final FunctionType type;

        HostFunctionBridge(final HostFunction function, final FunctionType type) {
            this.function = function;
            this.type = type;
        }

        @SuppressWarnings("unused") // invoked via upcall MethodHandle
        MemorySegment invoke(
                final MemorySegment runtime,
                final MemorySegment ctx,
                final MemorySegment sp,
                final MemorySegment mem) {
            try {
                final int retc = type.resultCount();
                final int argc = type.parameterCount();
                final var paramTypes = type.parameterTypes();
                final MemorySegment stack =
                        sp.reinterpret((long) (retc + argc) * JAVA_LONG.byteSize());
                final ai.tegmentum.wasm34j.WasmValue[] args =
                        new ai.tegmentum.wasm34j.WasmValue[argc];
                for (int i = 0; i < argc; i++) {
                    args[i] = ai.tegmentum.wasm34j.WasmValue.ofRaw(
                            paramTypes[i], stack.getAtIndex(JAVA_LONG, retc + i));
                }
                final ai.tegmentum.wasm34j.WasmValue[] results = function.call(args);
                for (int i = 0; i < retc; i++) {
                    stack.setAtIndex(JAVA_LONG, i, results[i].rawBits());
                }
                return MemorySegment.NULL;
            } catch (final Throwable t) {
                return HOST_ERROR;
            }
        }
    }

    private static void checkError(final MemorySegment result, final String operation) {
        if (result.address() != 0) {
            final String message = result.reinterpret(Long.MAX_VALUE).getString(0);
            throw new WasmException(operation + " failed: " + message);
        }
    }

    private static WasmException rethrow(final String operation, final Throwable cause) {
        return new WasmException("Panama call to " + operation + " failed", cause);
    }
}
