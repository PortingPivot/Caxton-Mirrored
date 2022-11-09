cmake_minimum_required(VERSION 3.17)
project(CaxtonJni C)

add_compile_definitions(HB_NO_SINGLE_HEADER_ERROR=1)

add_library(caxtonbindings SHARED
    ${PROJECT_SOURCE_DIR}/../build/bindings/cpp/hb_wrapper.c)

set(CMAKE_C_STANDARD 11)
set(CMAKE_CXX_STANDARD 17)

find_package(JNI REQUIRED)

target_include_directories(caxtonbindings PUBLIC ${JNI_INCLUDE_DIRS})

target_link_libraries(caxtonbindings ${JNI_LIBRARIES} harfbuzz)
