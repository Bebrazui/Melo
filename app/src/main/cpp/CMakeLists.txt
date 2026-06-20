cmake_minimum_required(VERSION 3.22.1)
project(byedpi_native)

set(CMAKE_C_STANDARD 99)

file(GLOB BYE_DPI_SRC byedpi/*.c)

# Exclude Windows-only files
list(FILTER BYE_DPI_SRC EXCLUDE REGEX "win_service\\.c$")

add_library(byedpi SHARED ${BYE_DPI_SRC} native-lib.c)

target_include_directories(byedpi PRIVATE byedpi)

target_compile_options(byedpi PRIVATE
    -std=c99
    -O2
    -Wall
    -Wextra
    -Wno-unused-parameter
    -Wno-sign-compare
    -DANDROID_APP
    -DFAKE_SUPPORT
    -DTIMEOUT_SUPPORT
)

target_link_libraries(byedpi PRIVATE android log)
