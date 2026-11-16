cmake_minimum_required(VERSION 3.22.1)
project(fluidsynth_player LANGUAGES C CXX)

# FluidSynth's Android CMake branch expects its own repository root to be
# CMAKE_SOURCE_DIR. Since it is included as a subdirectory, preserve the
# Android app build directory while correcting the source-root lookup.
set(FLUIDSYNTH_ROOT "${CMAKE_CURRENT_LIST_DIR}/../../../../..")
get_filename_component(FLUIDSYNTH_ROOT "${FLUIDSYNTH_ROOT}" ABSOLUTE)
set(CMAKE_SOURCE_DIR "${FLUIDSYNTH_ROOT}")

set(BUILD_SHARED_LIBS ON CACHE BOOL "" FORCE)
add_subdirectory("${FLUIDSYNTH_ROOT}" "${CMAKE_CURRENT_BINARY_DIR}/fluidsynth")

add_library(player SHARED native_player.cpp synth_engine.cpp)
target_include_directories(player PRIVATE
    "${FLUIDSYNTH_ROOT}/include"
    "${CMAKE_CURRENT_BINARY_DIR}/fluidsynth/include"
)
target_link_libraries(player PRIVATE libfluidsynth log)
