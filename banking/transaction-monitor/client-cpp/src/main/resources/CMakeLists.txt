cmake_minimum_required(VERSION 3.30)

project(transaction-monitor-@my.transaction-monitor.flavor@
        VERSION 1.0.0
        LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_MAKE_PROGRAM /usr/bin/make)
set(CMAKE_TOOLCHAIN_FILE /build/vcpkg/scripts/buildsystems/vcpkg.cmake)

find_package(hazelcast-cpp-client CONFIG REQUIRED)
#find_package(Boost 1.85.0 REQUIRED)
#find_package(OpenSSL 3.3.1 REQUIRED)

add_executable(cpp_client Client.cpp)

target_link_libraries(cpp_client PRIVATE hazelcast-cpp-client::hazelcast-cpp-client)
