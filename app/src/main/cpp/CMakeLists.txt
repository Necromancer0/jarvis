cmake_minimum_required(VERSION 3.22.1)
project(jarvis_llama CXX)

include(FetchContent)

set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_COMMON OFF CACHE BOOL "" FORCE)
set(LLAMA_CURL OFF CACHE BOOL "" FORCE)

# llama.cpp pinned to tag b5401 (verified API era used by llama_jni.cpp).
# Integrity: official GitHub source tarball, hash pinned below.
FetchContent_Declare(
    llamacpp
    URL https://github.com/ggml-org/llama.cpp/archive/refs/tags/b5401.tar.gz
    URL_HASH SHA256=140f105308371c79e5a7f2f0e111481c24ff1342e44cae2bc0cedd2808d8e24c
)
FetchContent_MakeAvailable(llamacpp)

add_library(jarvis_llama SHARED llama_jni.cpp)

find_library(android_log log)
target_link_libraries(jarvis_llama PRIVATE llama ${android_log})
target_compile_options(jarvis_llama PRIVATE -O3 -fexceptions -frtti -Wall)
target_include_directories(jarvis_llama PRIVATE ${llamacpp_SOURCE_DIR}/include)
