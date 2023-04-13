FROM --platform=linux/amd64 library/python:3.8-buster

ARG MC_CLUSTER1_NAME
ARG MC_CLUSTER1_ADDRESSLIST

# Preconfigure cluster connections
ENV MC_CLUSTER1_NAME=$MC_CLUSTER1_NAME
ENV MC_CLUSTER1_ADDRESSLIST=$MC_CLUSTER1_ADDRESSLIST
ENV MY_KUBERNETES_ENABLED "true"

RUN mkdir finos

COPY target/classes/public/* /finos/
COPY target/classes/requirements.txt /finos/

RUN pip install --upgrade pip
RUN pip install -r /finos/requirements.txt

COPY target/classes/custom-entrypoint.sh  /
CMD ["bash", "-c", "set -euo pipefail \
      && echo @@@@@@@@@@ \
      && echo arch \
      && arch \
      && echo @@@@@@@@@@ \
      && chmod 755 /custom-entrypoint.sh \
      && echo /custom-entrypoint.sh \
      && /custom-entrypoint.sh \
     "]
