FROM hazelcast-platform-demos/cva-cpp-tmp1:latest

# https://github.blog/2021-09-01-improving-git-protocol-security-github/
RUN git config --global url.https://github.insteadOf git://github

RUN git clone https://github.com/grpc/grpc.git -b v1.31.1  \
  && mkdir -p grpc/cmake/build \
  && cd grpc \
  && git submodule update --init

RUN cd grpc/cmake/build && \
  cmake ../.. && \
  make -j 12 && \
  make install

# Saves 1.4GB, but needs "--squash"
# RUN rm -r /grpc
