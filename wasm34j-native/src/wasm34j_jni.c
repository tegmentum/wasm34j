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

#include "wasm3.h"

/* Token-paste helper so the JNIEXPORT names stay readable below. */
#define JNI_FN(name) JNICALL Java_ai_tegmentum_wasm34j_jni_internal_Wasm3Native_##name

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

JNIEXPORT jstring JNI_FN(version)(JNIEnv *env, jclass cls) {
    (void) cls;
    return (*env)->NewStringUTF(env, M3_VERSION);
}
