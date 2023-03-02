FROM --platform=linux/amd64 library/fedora:22

ARG VERSION=5.1.0

RUN dnf groups install -y "Development Tools"
RUN dnf install -y gcc-c++ tar wget bzip2 unzip cmake which
#RUN g++ --version

RUN wget --quiet https://github.com/Kitware/CMake/releases/download/v3.19.0/cmake-3.19.0-Linux-x86_64.sh && \
    chmod +x ./cmake-3.19.0-Linux-x86_64.sh && \
    ./cmake-3.19.0-Linux-x86_64.sh --prefix=/usr/local/ --exclude-subdir && \
    rm cmake-3.19.0-Linux-x86_64.sh

# install boost "--quiet"
#    https://sourceforge.net/projects/boost/files/boost/1.75.0/boost_1_75_0.tar.gz \
RUN cd tmp \
    && wget --quiet https://boostorg.jfrog.io/artifactory/main/release/1.71.0/source/boost_1_71_0.tar.gz \
    && tar xvf boost_1_71_0.tar.gz \
    && cd boost_1_71_0 \
    && ./bootstrap.sh \
    && ./b2 address-model=64 --with-thread --with-chrono install \
    && cd .. \
    && rm -rf boost_1_71_0 boost_1_71_0.tar.gz

# install hazelcast cpp client
RUN wget https://github.com/hazelcast/hazelcast-cpp-client/archive/v${VERSION}.zip \
    && unzip v${VERSION}.zip \
    && rm v${VERSION}.zip \
    && cd hazelcast-cpp-client-${VERSION} \
    && cmake -S . -B build \
    && cmake --build build --verbose \
    && cmake --install build \
    && cd .. && rm -rf hazelcast-cpp-client-${VERSION}

# build client application
COPY target/classes/Client.cpp .
COPY target/classes/CMakeLists.txt .

RUN cmake -S . -B build -DCMAKE_BUILD_TYPE=Release \
 && cmake --build build --verbose \
 && cp build/cpp_client /tmp/client

ENV LD_LIBRARY_PATH /usr/local/lib

# Default assume in Kubernetes, else Docker
ENV HOST_IP ""
ENV MY_KUBERNETES_ENABLED "true"

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
