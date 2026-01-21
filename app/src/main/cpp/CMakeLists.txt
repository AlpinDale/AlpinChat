cmake_minimum_required(VERSION 3.22.1)
project("tokenizer" LANGUAGES C ASM)

# Compiler flags for performance
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O3 -ffast-math")

# ARM64-specific flags
if(CMAKE_ANDROID_ARCH_ABI STREQUAL "arm64-v8a")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -march=armv8-a+crc")
    set(CMAKE_ASM_FLAGS "${CMAKE_ASM_FLAGS} -march=armv8-a+crc")
endif()

# Source files
set(TOKENIZER_SOURCES
    tokenizer/gpt2bpe.c
    tokenizer/simd.c
    tokenizer/unicode_tables.c
    tokenizer_jni.c
)

# ARM64 assembly for SIMD optimizations
if(CMAKE_ANDROID_ARCH_ABI STREQUAL "arm64-v8a")
    list(APPEND TOKENIZER_SOURCES tokenizer/simd_arm64.S)
endif()

# Create shared library
add_library(tokenizer SHARED ${TOKENIZER_SOURCES})

# Include directories
target_include_directories(tokenizer PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/tokenizer
)

# Link libraries
target_link_libraries(tokenizer
    android
    log
)
