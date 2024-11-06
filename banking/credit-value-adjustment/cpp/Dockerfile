FROM --platform=linux/amd64 ubuntu:22.04

ARG DEBIAN_FRONTEND=noninteractive

RUN apt-get update && \
        apt-get install -y build-essential \
        git cmake autoconf libtool pkg-config \
        automake libtool curl make g++ unzip \
        wget libgflags-dev clang-15 libc++-dev \
        curl
        
# https://github.blog/2021-09-01-improving-git-protocol-security-github/
RUN git config --global url.https://github.insteadOf git://github

RUN git clone https://github.com/grpc/grpc.git -b v1.66.0  \
  && mkdir -p grpc/cmake/build \
  && cd grpc \
  && git submodule update --init

RUN cd grpc/cmake/build && \
  cmake ../.. && \
  make -j 12 && \
  make install

RUN rm -r /grpc

RUN apt-get install -y libboost-all-dev && \
  wget https://github.com/lballabio/QuantLib/releases/download/QuantLib-v1.18/QuantLib-1.18.tar.gz && \
  tar xvf QuantLib-1.18.tar.gz && \
  rm QuantLib-1.18.tar.gz && \
  cd QuantLib-1.18 && \
  ./configure && \
  make -j 12 && \
  make install && \
  ldconfig

RUN rm -r /QuantLib-1.18

RUN find . -name generated_message_table_driven.h 

# Copy local C++ files and compile
COPY src/main/cpp      /cvarisk
RUN mkdir -p /cvarisk/build && \
  cd /cvarisk/build && \
  cmake .. && \
  make

ENTRYPOINT exec /cvarisk/build/cvarisk_server 0.0.0.0:$0
