FROM --platform=linux/amd64 ubuntu:24.10

ARG CONTROL_FILE
ARG DEBIAN_FRONTEND=noninteractive
ENV VCPKG_FORCE_SYSTEM_BINARIES="1"

WORKDIR /build

RUN apt upgrade
RUN apt update
RUN apt-get install -y binutils curl g++ git-all libssl-dev linux-libc-dev make ninja-build pkg-config tar unzip wget zip

RUN wget --quiet https://github.com/Kitware/CMake/releases/download/v3.27.8/cmake-3.27.8-Linux-x86_64.sh && \
    chmod +x ./cmake-3.27.8-Linux-x86_64.sh && \
    ./cmake-3.27.8-Linux-x86_64.sh --prefix=/usr/local/ --exclude-subdir && \
    rm cmake-3.27.8-Linux-x86_64.sh

RUN git clone https://github.com/microsoft/vcpkg && vcpkg/bootstrap-vcpkg.sh 
# Use revision for builtin-baseline in vcpkg.json
RUN cd vcpkg && git rev-parse HEAD

# build client application
COPY target/classes/Client.cpp /build
COPY target/classes/CMakeLists.txt /build
COPY target/classes/vcpkg.json /build

RUN cmake -S . -B build -DCMAKE_BUILD_TYPE=Release \
 -DCMAKE_CXX_COMPILER=/usr/bin/g++ \
 -DCMAKE_TOOLCHAIN_FILE=/build/vcpkg/scripts/buildsystems/vcpkg.cmake
RUN cmake --build build 
RUN cp build/cpp_client /tmp/client

# Control file stops Docker copy failing if no PFX/PEM files found. Put in target by maven-antrun-plugin
COPY target/classes/${CONTROL_FILE} target/*.pfx /tmp/
COPY target/classes/${CONTROL_FILE} target/*.pem /tmp/

# Default assume in Kubernetes, else Docker
ENV HOST_IP=""
ENV MY_KUBERNETES_ENABLED="true"

CMD ["bash", "-c", "set -euo pipefail \
      && echo @@@@@@@@@@ \
      && echo arch \
      && arch \
      && echo @@@@@@@@@@ \
      && echo date \
      && date \
      && echo @@@@@@@@@@ \
      && echo /tmp/client \
      && /tmp/client \
     "]
