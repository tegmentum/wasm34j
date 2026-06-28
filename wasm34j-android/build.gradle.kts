plugins {
    id("com.android.library") version "8.5.2"
}

// Keep in sync with the Maven build's revision (wasm3.version-wasm34j.version).
val wasm34jVersion = "0.5.0-1.0.0-SNAPSHOT"

android {
    namespace = "ai.tegmentum.wasm34j.android"
    compileSdk = 34

    defaultConfig {
        // Android has no java.lang.foreign, so only the JNI backend is used here. minSdk 26
        // gives native java.util.Optional (24) and java.nio.file (26); core library desugaring
        // is also enabled as a safety net.
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
        }
        externalNativeBuild {
            cmake {
                // wasm3 + JNI glue are pure C; no C++ STL is needed.
                arguments += listOf("-DANDROID_STL=none")
                cppFlags += "-DNDEBUG"
            }
        }
    }

    // Reuse the existing CMake build that compiles wasm3 + the JNI glue. The CMakeLists
    // guards find_package(JNI) under ANDROID (jni.h comes from the NDK sysroot).
    externalNativeBuild {
        cmake {
            path = file("../wasm34j-native/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    // Re-export the public API and the JNI backend so consumers get them transitively.
    api("ai.tegmentum.wasm34j:wasm34j:$wasm34jVersion")
    // Exclude wasm34j-native: it bundles desktop .so/.dylib/.dll resources, but on Android the
    // library is built per-ABI by externalNativeBuild and packaged into the AAR's jniLibs.
    api("ai.tegmentum.wasm34j:wasm34j-jni:$wasm34jVersion") {
        exclude(group = "ai.tegmentum.wasm34j", module = "wasm34j-native")
    }

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
}
