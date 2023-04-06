#!/bin/bash

#######################################################################################
# NOTES:
#  protoc version to be consistent with or ideally matching Java
# and for Docker compiles the version used by Ubuntu.
#  You only need run this script if the version has to change.
#######################################################################################
# Localhost needs for this script:
# Install https://github.com/protocolbuffers/protobuf.git including submodules
# Install https://github.com/grpc/grpc.git including submodules
#######################################################################################

BASEDIR=`dirname $0`
cd $BASEDIR
PROJECT_BASE=`cd ../../.. ; pwd`

TARGET=${PROJECT_BASE}/cpp/src/main/cpp

TMPDIR=/tmp/`basename $0`.$$
mkdir $TMPDIR

cp ${PROJECT_BASE}/protobuf/src/main/proto/*.proto $TMPDIR
cd $TMPDIR
ls $TMPDIR

PROTOS="curve.proto fixing.proto swap.proto exchange.proto mtms.proto"
for PROTO in $PROTOS
do
   FILE=`echo $PROTO | cut -d. -f1`
   protoc --cpp_out=. $PROTO
   #perl -pi -e 's/#include "/#include "..\/include\//g' $filename.pb.cc
   cp ${FILE}.pb.h ${TARGET}/include
   cp ${FILE}.pb.cc ${TARGET}/src
done

echo protoc --grpc_out=. --plugin=protoc-gen-grpc=/usr/local/bin/grpc_cpp_plugin JetToCpp.proto
echo cp JetToCpp.grpc.pb.cc ${TARGET}/src
echo cp JetToCpp.grpc.pb.h ${TARGET}/include

/bin/rm -rf $TMPDIR
