/*
 * JNI glue between ai.tegmentum.wasm34j.jni.internal.Wasm3Native and the wasm3 C API.
 *
 * Native handles (environment / runtime / module / function pointers) are passed
 * across the boundary as jlong values. Function names follow the JNI mangling of
 * the fully-qualified Java method names, so no generated header is required.
 */
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

#include "wasm3.h"

/* Token-paste helper so the JNIEXPORT names stay readable below. */
#define JNI_FN(name) JNICALL Java_ai_tegmentum_wasm34j_jni_internal_Wasm3Native_##name

/* Cached at JNI_OnLoad / nativeInitDispatch for host-function callbacks into the JVM. */
static JavaVM *g_vm = NULL;
static jclass g_dispatchClass = NULL;
static jmethodID g_dispatchMethod = NULL;

static const char *const HOST_FUNCTION_ERROR = "host function threw a Java exception";

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    (void) reserved;
    g_vm = vm;
    return JNI_VERSION_1_6;
}

static void throwWasm(JNIEnv *env, const char *message) {
    jclass cls = (*env)->FindClass(env, "ai/tegmentum/wasm34j/exception/WasmException");
    if (cls != NULL) {
        (*env)->ThrowNew(env, cls, message != NULL ? message : "wasm3 error");
    }
}

JNIEXPORT jlong JNI_FN(newEnvironment)(JNIEnv *env, jclass cls) {
    (void) env;
    (void) cls;
    return (jlong) (intptr_t) m3_NewEnvironment();
}

JNIEXPORT void JNI_FN(freeEnvironment)(JNIEnv *env, jclass cls, jlong handle) {
    (void) env;
    (void) cls;
    if (handle != 0) {
        m3_FreeEnvironment((IM3Environment) (intptr_t) handle);
    }
}

JNIEXPORT jlong JNI_FN(newRuntime)(JNIEnv *env, jclass cls, jlong envHandle, jint stackBytes) {
    (void) env;
    (void) cls;
    return (jlong) (intptr_t) m3_NewRuntime(
        (IM3Environment) (intptr_t) envHandle, (uint32_t) stackBytes, NULL);
}

JNIEXPORT void JNI_FN(freeRuntime)(JNIEnv *env, jclass cls, jlong handle) {
    (void) env;
    (void) cls;
    if (handle != 0) {
        m3_FreeRuntime((IM3Runtime) (intptr_t) handle);
    }
}

/*
 * Parses a module and returns a two-element array { modulePtr, bufferPtr }.
 *
 * wasm3 retains a pointer to the wasm bytes for the lifetime of the module, so the buffer
 * must outlive the call. Ownership of the buffer is tracked on the Java side and released
 * via freeBuffer() once the owning module/runtime is freed (see JniWasmModule /
 * JniWasmInstance). On error a Java exception is thrown and NULL is returned.
 */
JNIEXPORT jlongArray JNI_FN(parseModule)(JNIEnv *env, jclass cls, jlong envHandle, jbyteArray wasm) {
    (void) cls;
    jsize length = (*env)->GetArrayLength(env, wasm);

    uint8_t *buffer = (uint8_t *) malloc((size_t) length);
    if (buffer == NULL) {
        throwWasm(env, "out of memory allocating wasm module buffer");
        return NULL;
    }
    (*env)->GetByteArrayRegion(env, wasm, 0, length, (jbyte *) buffer);

    IM3Module module = NULL;
    M3Result result = m3_ParseModule(
        (IM3Environment) (intptr_t) envHandle, &module, buffer, (uint32_t) length);
    if (result != m3Err_none) {
        free(buffer);
        throwWasm(env, result);
        return NULL;
    }

    jlong handles[2];
    handles[0] = (jlong) (intptr_t) module;
    handles[1] = (jlong) (intptr_t) buffer;
    jlongArray out = (*env)->NewLongArray(env, 2);
    if (out != NULL) {
        (*env)->SetLongArrayRegion(env, out, 0, 2, handles);
    }
    return out;
}

/* Frees an unloaded module (one never successfully loaded into a runtime). */
JNIEXPORT void JNI_FN(freeModule)(JNIEnv *env, jclass cls, jlong module) {
    (void) env;
    (void) cls;
    if (module != 0) {
        m3_FreeModule((IM3Module) (intptr_t) module);
    }
}

/* Frees the malloc'd wasm byte buffer backing a module. */
JNIEXPORT void JNI_FN(freeBuffer)(JNIEnv *env, jclass cls, jlong buffer) {
    (void) env;
    (void) cls;
    if (buffer != 0) {
        free((void *) (intptr_t) buffer);
    }
}

JNIEXPORT void JNI_FN(loadModule)(JNIEnv *env, jclass cls, jlong runtime, jlong module) {
    (void) cls;
    M3Result result = m3_LoadModule(
        (IM3Runtime) (intptr_t) runtime, (IM3Module) (intptr_t) module);
    if (result != m3Err_none) {
        throwWasm(env, result);
    }
}

JNIEXPORT jlong JNI_FN(findFunction)(JNIEnv *env, jclass cls, jlong runtime, jstring name) {
    (void) cls;
    const char *cname = (*env)->GetStringUTFChars(env, name, NULL);
    IM3Function function = NULL;
    M3Result result = m3_FindFunction(&function, (IM3Runtime) (intptr_t) runtime, cname);
    (*env)->ReleaseStringUTFChars(env, name, cname);
    if (result != m3Err_none) {
        return 0; /* Treated as "not found" on the Java side. */
    }
    return (jlong) (intptr_t) function;
}

JNIEXPORT jint JNI_FN(getArgCount)(JNIEnv *env, jclass cls, jlong function) {
    (void) env;
    (void) cls;
    return (jint) m3_GetArgCount((IM3Function) (intptr_t) function);
}

JNIEXPORT jint JNI_FN(getRetCount)(JNIEnv *env, jclass cls, jlong function) {
    (void) env;
    (void) cls;
    return (jint) m3_GetRetCount((IM3Function) (intptr_t) function);
}

JNIEXPORT jint JNI_FN(getArgType)(JNIEnv *env, jclass cls, jlong function, jint index) {
    (void) env;
    (void) cls;
    return (jint) m3_GetArgType((IM3Function) (intptr_t) function, (uint32_t) index);
}

JNIEXPORT jint JNI_FN(getRetType)(JNIEnv *env, jclass cls, jlong function, jint index) {
    (void) env;
    (void) cls;
    return (jint) m3_GetRetType((IM3Function) (intptr_t) function, (uint32_t) index);
}

JNIEXPORT jlongArray JNI_FN(call)(JNIEnv *env, jclass cls, jlong function, jlongArray rawArgs) {
    (void) cls;
    IM3Function fn = (IM3Function) (intptr_t) function;

    jsize argc = (*env)->GetArrayLength(env, rawArgs);
    uint64_t *slots = NULL;
    const void **argptrs = NULL;
    if (argc > 0) {
        slots = (uint64_t *) malloc(sizeof(uint64_t) * (size_t) argc);
        argptrs = (const void **) malloc(sizeof(void *) * (size_t) argc);
        if (slots == NULL || argptrs == NULL) {
            free(slots);
            free(argptrs);
            throwWasm(env, "out of memory marshalling call arguments");
            return NULL;
        }
        jlong *elems = (*env)->GetLongArrayElements(env, rawArgs, NULL);
        for (jsize i = 0; i < argc; i++) {
            slots[i] = (uint64_t) elems[i];
            argptrs[i] = &slots[i];
        }
        (*env)->ReleaseLongArrayElements(env, rawArgs, elems, JNI_ABORT);
    }

    M3Result result = m3_Call(fn, (uint32_t) argc, argptrs);
    free(argptrs);
    if (result != m3Err_none) {
        free(slots);
        throwWasm(env, result);
        return NULL;
    }

    uint32_t retc = m3_GetRetCount(fn);
    uint64_t *rets = NULL;
    const void **retptrs = NULL;
    if (retc > 0) {
        rets = (uint64_t *) calloc(retc, sizeof(uint64_t));
        retptrs = (const void **) malloc(sizeof(void *) * retc);
        if (rets == NULL || retptrs == NULL) {
            free(slots);
            free(rets);
            free(retptrs);
            throwWasm(env, "out of memory marshalling call results");
            return NULL;
        }
        for (uint32_t i = 0; i < retc; i++) {
            retptrs[i] = &rets[i];
        }
    }

    result = m3_GetResults(fn, retc, retptrs);
    free(slots);
    if (result != m3Err_none) {
        free(rets);
        free(retptrs);
        throwWasm(env, result);
        return NULL;
    }

    jlongArray out = (*env)->NewLongArray(env, (jsize) retc);
    if (out != NULL && retc > 0) {
        jlong stack[16];
        jlong *tmp = (retc <= 16) ? stack : (jlong *) malloc(sizeof(jlong) * retc);
        if (tmp != NULL) {
            for (uint32_t i = 0; i < retc; i++) {
                tmp[i] = (jlong) rets[i];
            }
            (*env)->SetLongArrayRegion(env, out, 0, (jsize) retc, tmp);
            if (tmp != stack) {
                free(tmp);
            }
        }
    }
    free(rets);
    free(retptrs);
    return out;
}

/* ---------------------------------------------------------------------------------------
 *  Memory access
 * ------------------------------------------------------------------------------------- */

JNIEXPORT jint JNI_FN(memorySize)(JNIEnv *env, jclass cls, jlong runtime) {
    (void) env;
    (void) cls;
    return (jint) m3_GetMemorySize((IM3Runtime) (intptr_t) runtime);
}

JNIEXPORT jobject JNI_FN(memoryBuffer)(JNIEnv *env, jclass cls, jlong runtime) {
    (void) cls;
    uint32_t size = 0;
    uint8_t *base = m3_GetMemory((IM3Runtime) (intptr_t) runtime, &size, 0);
    if (base == NULL) {
        return NULL;
    }
    return (*env)->NewDirectByteBuffer(env, base, (jlong) size);
}

JNIEXPORT jbyteArray JNI_FN(memoryRead)(
    JNIEnv *env, jclass cls, jlong runtime, jlong offset, jint length) {
    (void) cls;
    uint32_t size = 0;
    uint8_t *base = m3_GetMemory((IM3Runtime) (intptr_t) runtime, &size, 0);
    if (base == NULL) {
        throwWasm(env, "module has no memory");
        return NULL;
    }
    if (offset < 0 || length < 0 || (uint64_t) offset + (uint64_t) length > (uint64_t) size) {
        throwWasm(env, "memory read out of bounds");
        return NULL;
    }
    jbyteArray out = (*env)->NewByteArray(env, length);
    if (out != NULL) {
        (*env)->SetByteArrayRegion(env, out, 0, length, (const jbyte *) (base + offset));
    }
    return out;
}

JNIEXPORT void JNI_FN(memoryWrite)(
    JNIEnv *env, jclass cls, jlong runtime, jlong offset, jbyteArray data) {
    (void) cls;
    jsize length = (*env)->GetArrayLength(env, data);
    uint32_t size = 0;
    uint8_t *base = m3_GetMemory((IM3Runtime) (intptr_t) runtime, &size, 0);
    if (base == NULL) {
        throwWasm(env, "module has no memory");
        return;
    }
    if (offset < 0 || (uint64_t) offset + (uint64_t) length > (uint64_t) size) {
        throwWasm(env, "memory write out of bounds");
        return;
    }
    (*env)->GetByteArrayRegion(env, data, 0, length, (jbyte *) (base + offset));
}

/* ---------------------------------------------------------------------------------------
 *  Globals
 * ------------------------------------------------------------------------------------- */

JNIEXPORT jlong JNI_FN(findGlobal)(JNIEnv *env, jclass cls, jlong module, jstring name) {
    (void) cls;
    const char *cname = (*env)->GetStringUTFChars(env, name, NULL);
    IM3Global global = m3_FindGlobal((IM3Module) (intptr_t) module, cname);
    (*env)->ReleaseStringUTFChars(env, name, cname);
    return (jlong) (intptr_t) global;
}

JNIEXPORT jint JNI_FN(globalType)(JNIEnv *env, jclass cls, jlong global) {
    (void) env;
    (void) cls;
    return (jint) m3_GetGlobalType((IM3Global) (intptr_t) global);
}

JNIEXPORT jlong JNI_FN(globalGet)(JNIEnv *env, jclass cls, jlong global) {
    (void) cls;
    M3TaggedValue value;
    M3Result result = m3_GetGlobal((IM3Global) (intptr_t) global, &value);
    if (result != m3Err_none) {
        throwWasm(env, result);
        return 0;
    }
    uint64_t bits = 0;
    switch (value.type) {
        case c_m3Type_i32:
            bits = value.value.i32;
            break;
        case c_m3Type_i64:
            bits = value.value.i64;
            break;
        case c_m3Type_f32: {
            uint32_t tmp;
            memcpy(&tmp, &value.value.f32, sizeof(tmp));
            bits = tmp;
            break;
        }
        case c_m3Type_f64:
            memcpy(&bits, &value.value.f64, sizeof(bits));
            break;
        default:
            bits = 0;
            break;
    }
    return (jlong) bits;
}

JNIEXPORT void JNI_FN(globalSet)(JNIEnv *env, jclass cls, jlong global, jint type, jlong bits) {
    (void) cls;
    M3TaggedValue value;
    value.type = (M3ValueType) type;
    uint64_t raw = (uint64_t) bits;
    switch (type) {
        case c_m3Type_i32:
            value.value.i32 = (uint32_t) raw;
            break;
        case c_m3Type_i64:
            value.value.i64 = raw;
            break;
        case c_m3Type_f32: {
            uint32_t tmp = (uint32_t) raw;
            memcpy(&value.value.f32, &tmp, sizeof(tmp));
            break;
        }
        case c_m3Type_f64:
            memcpy(&value.value.f64, &raw, sizeof(raw));
            break;
        default:
            break;
    }
    M3Result result = m3_SetGlobal((IM3Global) (intptr_t) global, &value);
    if (result != m3Err_none) {
        throwWasm(env, result);
    }
}

/* ---------------------------------------------------------------------------------------
 *  Host functions
 * ------------------------------------------------------------------------------------- */

/* Caches the Java dispatch entry point used by the host-function trampoline. */
JNIEXPORT void JNI_FN(nativeInitDispatch)(JNIEnv *env, jclass cls, jclass dispatchClass) {
    (void) cls;
    g_dispatchClass = (jclass) (*env)->NewGlobalRef(env, dispatchClass);
    g_dispatchMethod = (*env)->GetStaticMethodID(env, dispatchClass, "dispatch", "(I[J)[J");
}

/*
 * Single M3RawCall trampoline shared by all linked host functions. The import's userdata
 * carries the registry id; arguments live at _sp[retc + i] and results are written to
 * _sp[0 .. retc-1].
 */
static const void *hostTrampoline(
    IM3Runtime runtime, IM3ImportContext ctx, uint64_t *sp, void *mem) {
    (void) runtime;
    (void) mem;
    const int id = (int) (intptr_t) ctx->userdata;
    IM3Function function = ctx->function;
    const uint32_t argc = m3_GetArgCount(function);
    const uint32_t retc = m3_GetRetCount(function);

    JNIEnv *env = NULL;
    int attached = 0;
    jint status = (*g_vm)->GetEnv(g_vm, (void **) &env, JNI_VERSION_1_6);
    if (status == JNI_EDETACHED) {
        if ((*g_vm)->AttachCurrentThread(g_vm, (void **) &env, NULL) != JNI_OK) {
            return HOST_FUNCTION_ERROR;
        }
        attached = 1;
    }

    jlongArray jargs = (*env)->NewLongArray(env, (jsize) argc);
    if (argc > 0) {
        jlong stackbuf[16];
        jlong *tmp = (argc <= 16) ? stackbuf : (jlong *) malloc(sizeof(jlong) * argc);
        for (uint32_t i = 0; i < argc; i++) {
            tmp[i] = (jlong) sp[retc + i];
        }
        (*env)->SetLongArrayRegion(env, jargs, 0, (jsize) argc, tmp);
        if (tmp != stackbuf) {
            free(tmp);
        }
    }

    jlongArray jresults = (jlongArray) (*env)->CallStaticObjectMethod(
        env, g_dispatchClass, g_dispatchMethod, (jint) id, jargs);

    const void *outcome = m3Err_none;
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionClear(env);
        outcome = HOST_FUNCTION_ERROR;
    } else if (retc > 0 && jresults != NULL) {
        jlong *r = (*env)->GetLongArrayElements(env, jresults, NULL);
        for (uint32_t i = 0; i < retc; i++) {
            sp[i] = (uint64_t) r[i];
        }
        (*env)->ReleaseLongArrayElements(env, jresults, r, JNI_ABORT);
    }

    if (jargs != NULL) {
        (*env)->DeleteLocalRef(env, jargs);
    }
    if (jresults != NULL) {
        (*env)->DeleteLocalRef(env, jresults);
    }
    if (attached) {
        (*g_vm)->DetachCurrentThread(g_vm);
    }
    return outcome;
}

/* Links a host function (by registry id) into a module's import slot before it is loaded. */
JNIEXPORT void JNI_FN(linkRawFunction)(
    JNIEnv *env, jclass cls, jlong module, jstring moduleName, jstring functionName,
    jstring signature, jint id) {
    (void) cls;
    const char *cmod = (*env)->GetStringUTFChars(env, moduleName, NULL);
    const char *cname = (*env)->GetStringUTFChars(env, functionName, NULL);
    const char *csig = (*env)->GetStringUTFChars(env, signature, NULL);

    M3Result result = m3_LinkRawFunctionEx(
        (IM3Module) (intptr_t) module, cmod, cname, csig, &hostTrampoline,
        (const void *) (intptr_t) id);

    (*env)->ReleaseStringUTFChars(env, moduleName, cmod);
    (*env)->ReleaseStringUTFChars(env, functionName, cname);
    (*env)->ReleaseStringUTFChars(env, signature, csig);

    /* A module that does not import this function is not an error; other failures are. */
    if (result != m3Err_none && result != m3Err_functionLookupFailed) {
        throwWasm(env, result);
    }
}

JNIEXPORT jstring JNI_FN(version)(JNIEnv *env, jclass cls) {
    (void) cls;
    return (*env)->NewStringUTF(env, M3_VERSION);
}
