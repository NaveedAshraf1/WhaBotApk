cmake_minimum_required(VERSION 3.4.1)

set(NODEJS_ROOT ${CMAKE_SOURCE_DIR}/libnode)

add_library(node SHARED IMPORTED)
set_target_properties(node PROPERTIES IMPORTED_LOCATION ${NODEJS_ROOT}/bin/${ANDROID_ABI}/libnode.so)

include_directories(${NODEJS_ROOT}/include/node)

add_library(
        nodejs-bridge
        SHARED
        src/main/cpp/nodejs-bridge.cpp)

find_library(log-lib log)

target_link_libraries(
        nodejs-bridge
        node
        ${log-lib})
