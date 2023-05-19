cmake_minimum_required(VERSION 3.26)

project(transaction-monitor-@my.transaction-monitor.flavor@
        VERSION 1.0.0
        LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(hazelcast-cpp-client CONFIG REQUIRED)

add_executable(cpp_client Client.cpp)

target_link_libraries(cpp_client PRIVATE hazelcast-cpp-client::hazelcast-cpp-client)
