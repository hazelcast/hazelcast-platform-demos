FROM hazelcast-platform-demos/cva-cpp-tmp2:latest

RUN apt-get install -y libboost-all-dev && \
  wget https://github.com/lballabio/QuantLib/releases/download/QuantLib-v1.18/QuantLib-1.18.tar.gz && \
  tar xvf QuantLib-1.18.tar.gz && \
  rm QuantLib-1.18.tar.gz && \
  cd QuantLib-1.18 && \
  ./configure && \
  make -j 12 && \
  make install && \
  ldconfig

# Saves 8GB, but needs "--squash"
#RUN rm -r /QuantLib-1.18
