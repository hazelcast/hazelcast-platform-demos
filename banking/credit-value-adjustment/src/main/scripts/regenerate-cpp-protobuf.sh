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

PROTOC=/opt/homebrew/Cellar/protobuf/21.12/bin/protoc
echo '========================================='
CMD="$PROTOC --version"
echo $CMD
$CMD
RC=$?
echo RC=$RC
COUNT=5
while [ $COUNT -gt 0 ]
do
 echo "."
 COUNT=$(($COUNT - 1))
 sleep 1
done
echo '========================================='

TARGET=${PROJECT_BASE}/cpp/src/main/cpp

TMPDIR=/tmp/`basename $0`.$$
mkdir $TMPDIR

cp ${PROJECT_BASE}/protobuf/src/main/proto/*.proto $TMPDIR
cd $TMPDIR
#ls $TMPDIR

# Protobuf
PROTOS="JetToCpp.proto curve.proto fixing.proto swap.proto exchange.proto mtms.proto"
for PROTO in $PROTOS
do
   echo Processing: PROTOBUF: $PROTO
   FILE=`echo $PROTO | cut -d. -f1`
   $PROTOC --cpp_out=. $PROTO
   mv ${FILE}.pb.cc tmp
   sed 's/#include "/#include "..\/include\//g' < tmp > ${FILE}.pb.cc
   cp ${FILE}.pb.h ${TARGET}/include
   cp ${FILE}.pb.cc ${TARGET}/src
done

# GRPC
PROTOS="JetToCpp.proto"
for PROTO in $PROTOS
do
   echo Processing: GRPC: $PROTO
   FILE=`echo $PROTO | cut -d. -f1`
   $PROTOC --grpc_out=. --plugin=protoc-gen-grpc=${HOME}/git/grpc/cmake/build/grpc_cpp_plugin $PROTO
   mv ${FILE}.grpc.pb.cc tmp
   sed 's/#include "/#include "..\/include\//g' < tmp > ${FILE}.grpc.pb.cc
   cp ${FILE}.grpc.pb.h ${TARGET}/include
   cp ${FILE}.grpc.pb.cc ${TARGET}/src
done

echo '========================================='
/bin/rm -rf $TMPDIR
