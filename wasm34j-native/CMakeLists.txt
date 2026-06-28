cmake_minimum_required(VERSION 3.16)
project(wasm34j_native C)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
endif()

# Locate the JNI headers (jni.h / jni_md.h).
# On Android the NDK provides jni.h in its sysroot (and there is no JDK), so find_package(JNI)
# would fail; the desktop/server build honors JAVA_HOME.
if(ANDROID)
    set(WASM34J_JNI_INCLUDE_DIRS "")
else()
    find_package(JNI REQUIRED)
    set(WASM34J_JNI_INCLUDE_DIRS ${JNI_INCLUDE_DIRS})
endif()

# wasm3 interpreter sources (vendored as a git submodule, pinned to a release tag).
set(WASM3_SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/wasm3/source)
file(GLOB WASM3_SOURCES ${WASM3_SOURCE_DIR}/*.c)

add_library(wasm34j_native SHARED
    ${WASM3_SOURCES}
    ${CMAKE_CURRENT_SOURCE_DIR}/src/wasm34j_jni.c
)

target_include_directories(wasm34j_native PRIVATE
    ${WASM3_SOURCE_DIR}
    ${WASM34J_JNI_INCLUDE_DIRS}
)

# Emit the shared library directly into the build root (no per-config subdir),
# so the Maven resources copy step finds it at a predictable path.
set_target_properties(wasm34j_native PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}
    RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}
)
