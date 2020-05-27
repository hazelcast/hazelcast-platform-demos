FROM amd64/ubuntu:20.04

ARG DEBIAN_FRONTEND=noninteractive

RUN apt-get update && \
        apt-get install -y build-essential \
        git cmake autoconf libtool pkg-config \
        automake libtool curl make g++ unzip \
        wget libgflags-dev clang-6.0 libc++-dev \
        curl
        
