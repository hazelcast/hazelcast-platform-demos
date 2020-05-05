cmake_minimum_required(VERSION 3.15)
project(HazelcastJet)

include_directories(include)

file(GLOB SERVERSOURCES
        "src/*.cc"
        "src/JsonHandler.cpp"
        "src/Pricer.cpp"
        "src/ProtoHandler.cpp"
        "src/QLUtils.cpp"
        "src/cvamain.cpp")

file(GLOB CLIENTSOURCES
        "src/*.cc"
        "src/JsonHandler.cpp"
        "src/QLUtils.cpp"
        "src/cvaclient.cpp")


find_library(ql_location NAMES libQuantLib.a)
add_library(ql IMPORTED STATIC)
set_target_properties(ql PROPERTIES IMPORTED_LOCATION ${ql_location})
set(QuantLibLib ${ql})


set(protobuf_MODULE_COMPATIBLE TRUE)
find_package(Protobuf CONFIG REQUIRED)
find_package(Boost COMPONENTS system filesystem REQUIRED)

set(THREADS_PREFER_PTHREAD_FLAG ON)
find_package(Threads REQUIRED)

set(_PROTOBUF_LIBPROTOBUF protobuf::libprotobuf)
set(_REFLECTION gRPC::grpc++_reflection)

find_package(gRPC CONFIG REQUIRED)
set(_GRPC_GRPCPP gRPC::grpc++)

add_executable(cvarisk_server ${SERVERSOURCES})

target_link_libraries(cvarisk_server
        ${ql_location}
        ${_REFLECTION}
        ${_GRPC_GRPCPP}
        ${_PROTOBUF_LIBPROTOBUF}
        ${Boost_FILESYSTEM_LIBRARY}
        ${Boost_SYSTEM_LIBRARY}
        Threads::Threads)

add_executable(cvarisk_client ${CLIENTSOURCES})
target_link_libraries(cvarisk_client
        ${ql_location}
        ${_REFLECTION}
        ${_GRPC_GRPCPP}
        ${_PROTOBUF_LIBPROTOBUF}
        ${Boost_FILESYSTEM_LIBRARY}
        ${Boost_SYSTEM_LIBRARY}
        Threads::Threads)
