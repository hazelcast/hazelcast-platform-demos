FROM --platform=linux/amd64 library/golang:1.21.1-bullseye

ARG CONTROL_FILE

# https://github.blog/2021-09-01-improving-git-protocol-security-github/
RUN git config --global url.https://github.insteadOf git://github

RUN cd /tmp \
  && git clone https://github.com/hazelcast/hazelcast-commandline-client.git \
  && cd hazelcast-commandline-client \
  && make \
  && ./build/clc version
  
COPY target/classes/client.sh /tmp/
COPY target/classes/hazelcast-client-clc.yaml /tmp/
COPY target/classes/hazelcast-client-clc-viridian.yaml /tmp/

RUN chmod 755 /tmp/*.sh

# Control file stops Docker copy failing if no PFX/PEM files found. Put in target by maven-antrun-plugin
COPY target/classes/${CONTROL_FILE} target/*.pfx /tmp/
COPY target/classes/${CONTROL_FILE} target/*.pem /tmp/

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
      && echo /tmp/client.sh /tmp/hazelcast-commandline-client /tmp/hazelcast-client-clc.yaml /tmp/hazelcast-client-clc-viridian.yaml \
      && /tmp/client.sh /tmp/hazelcast-commandline-client /tmp/hazelcast-client-clc.yaml /tmp/hazelcast-client-clc-viridian.yaml \
     "]
