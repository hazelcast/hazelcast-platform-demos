# Copy from first image, before Grpc and QuantLib build
FROM hazelcast-platform-demos/cva-cpp-tmp1:latest AS base_build

#############################################################################
# As "--squash" is still experimental, so may not be available, copy what we
# need from intermediate images
#############################################################################

# Copy grpc build output from stage 2 and QuantLib from stage 3. Stage 3 includes 2.
COPY --from=hazelcast-platform-demos/cva-cpp-tmp3:latest /usr/bin          /usr/bin
COPY --from=hazelcast-platform-demos/cva-cpp-tmp3:latest /usr/include      /usr/include
COPY --from=hazelcast-platform-demos/cva-cpp-tmp3:latest /usr/lib          /usr/lib
COPY --from=hazelcast-platform-demos/cva-cpp-tmp3:latest /usr/local        /usr/local
COPY --from=hazelcast-platform-demos/cva-cpp-tmp3:latest /usr/share        /usr/share

# Copy local C++ files and compile
COPY src/main/cpp      /cvarisk
RUN mkdir -p /cvarisk/build && \
  cd /cvarisk/build && \
  cmake .. && \
  make

ENTRYPOINT exec /cvarisk/build/cvarisk_server 0.0.0.0:$0
