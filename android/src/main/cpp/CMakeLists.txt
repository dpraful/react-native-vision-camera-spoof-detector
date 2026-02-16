cmake_minimum_required(VERSION 3.4.1)

project(fastyuv)

set(SOURCE_FILES
    FastYuvJni.cpp
    fast_yuv.cpp
)

add_library(fastyuv SHARED ${SOURCE_FILES})

# Enable NEON SIMD optimizations for ARM architectures
if(CMAKE_SYSTEM_PROCESSOR MATCHES "armv7" OR CMAKE_SYSTEM_PROCESSOR MATCHES "aarch64")
    # ARM NEON optimization flags
    target_compile_options(fastyuv PRIVATE
        -fPIC
        -O3
    )
    
    # ARM-specific NEON enablement (clang doesn't support -march=native for cross-compilation)
    if(CMAKE_SYSTEM_PROCESSOR MATCHES "armv7")
        # ARMv7 with NEON (32-bit ARM)
        target_compile_options(fastyuv PRIVATE -march=armv7-a -mfpu=neon -mfloat-abi=softfp)
    elseif(CMAKE_SYSTEM_PROCESSOR MATCHES "aarch64")
        # ARMv8 with full SIMD support (64-bit ARM)
        target_compile_options(fastyuv PRIVATE -march=armv8-a)
    endif()
endif()

# Always add libyuv include dir so headers like libyuv/convert.h are found.
target_include_directories(fastyuv PRIVATE ${CMAKE_SOURCE_DIR}/libyuv/include)

# Prefer using libyuv subproject if available.
if (EXISTS ${CMAKE_SOURCE_DIR}/libyuv/CMakeLists.txt)
    add_subdirectory(libyuv)
    # Link against the libyuv target produced by that CMake. The lib creates
    # targets named 'yuv' (static) and 'yuv_shared' (shared) whose output
    # library name is 'libyuv'. Prefer the shared target when available.
    if (TARGET yuv_shared)
        target_link_libraries(fastyuv PRIVATE yuv_shared)
    elseif (TARGET yuv)
        target_link_libraries(fastyuv PRIVATE yuv)
    endif()
endif()

# Android log
find_library(log-lib log)
target_link_libraries(fastyuv PRIVATE ${log-lib})
cmake_minimum_required(VERSION 3.18)
project(faceantispoof)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

get_filename_component(CMAKE_SOURCE_ABS "${CMAKE_SOURCE_DIR}" ABSOLUTE)
# .../react-native-vision-camera-spoof-detector/android/src/main/cpp -> ../../../.. = .../node_modules
get_filename_component(PARENT_1 "${CMAKE_SOURCE_ABS}/.." ABSOLUTE)  # main
get_filename_component(PARENT_2 "${PARENT_1}/.." ABSOLUTE)  # src
get_filename_component(PARENT_3 "${PARENT_2}/.." ABSOLUTE)  # android
get_filename_component(PARENT_4 "${PARENT_3}/.." ABSOLUTE)  # react-native-vision-camera-spoof-detector
get_filename_component(NODE_MODULES_ABS "${PARENT_4}/.." ABSOLUTE)  # node_modules

set(REACT_NATIVE_DIR "${NODE_MODULES_ABS}/react-native")
set(REACT_COMMON_DIR "${REACT_NATIVE_DIR}/ReactCommon")

message(STATUS "NODE_MODULES_ABS: ${NODE_MODULES_ABS}")
message(STATUS "REACT_NATIVE_DIR: ${REACT_NATIVE_DIR}")
message(STATUS "REACT_COMMON_DIR: ${REACT_COMMON_DIR}")

# Compile JSI implementation
file(GLOB JSI_CPP "${REACT_COMMON_DIR}/jsi/jsi/jsi.cpp")

add_library(faceantispoof SHARED FaceAntiSpoofJSI.cpp ${JSI_CPP})

target_include_directories(faceantispoof PRIVATE
    "${REACT_COMMON_DIR}/jsi"
    "${REACT_COMMON_DIR}/cxxreact"
    "${REACT_COMMON_DIR}/callinvoker/ReactCommon"
    "${REACT_COMMON_DIR}/runtimeexecutor/ReactCommon"
    "${REACT_COMMON_DIR}"
)

find_library(log-lib log)

target_link_libraries(
    faceantispoof
    "${log-lib}"
    c++_shared
    atomic
    m
)


