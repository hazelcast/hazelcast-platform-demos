#!/bin/bash

#######################################################################################
# NOTES:
#  protoc version to be consistent with or ideally matching Java
# and for Docker compiles the version used by Ubuntu.
#  You only need run this script if the version has to change.
#######################################################################################

BASEDIR=`dirname $0`
cd $BASEDIR
PROJECT_BASE=`cd ../../.. ; pwd`

TARGET=${PROJECT_BASE}/cpp/src/main/cpp

TMPDIR=/tmp/`basename $0`.$$
mkdir $TMPDIR

cp ${PROJECT_BASE}/protobuf/src/main/proto/*.proto $TMPDIR
cd $TMPDIR
#ls $TMPDIR

# Protobuf
PROTOS="curve.proto fixing.proto swap.proto exchange.proto mtms.proto"
for PROTO in $PROTOS
do
   echo Processing: $PROTO
   FILE=`echo $PROTO | cut -d. -f1`
   protoc --cpp_out=. $PROTO
   mv ${FILE}.pb.cc tmp
   sed 's/#include "/#include "..\/include\//g' < tmp > ${FILE}.pb.cc
   cp ${FILE}.pb.h ${TARGET}/include
   cp ${FILE}.pb.cc ${TARGET}/src
   rm ${FILE}.* tmp
done

# GRPC
PROTOS="JetToCpp.proto"
for PROTO in $PROTOS
do
   echo Processing: $PROTO
   FILE=`echo $PROTO | cut -d. -f1`
   protoc --grpc_out=. --plugin=protoc-gen-grpc=${HOME}/git/grpc/cmake/build/grpc_cpp_plugin $PROTO
   mv ${FILE}.grpc.pb.cc tmp
   sed 's/#include "/#include "..\/include\//g' < tmp > ${FILE}.grpc.pb.cc
   cp ${FILE}.grpc.pb.h ${TARGET}/include
   cp ${FILE}.grpc.pb.cc ${TARGET}/src
   rm ${FILE}.* tmp
done

/bin/rm -rf $TMPDIR
