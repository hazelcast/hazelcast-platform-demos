cmake_minimum_required(VERSION 3.10)

project(transaction-monitor-@my.transaction-monitor.flavor@
        VERSION 1.0.0
        LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_FLAGS -DHZ_BUILD_WITH_SSL)

find_package(hazelcast-cpp-client REQUIRED)

add_executable(cpp_client Client.cpp)

target_link_libraries(cpp_client PRIVATE hazelcast-cpp-client::hazelcast-cpp-client)
