FROM --platform=linux/amd64 library/python:3.7-buster

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
RUN pip install hazelcast-python-client

COPY target/classes/custom-entrypoint.sh  /
ENTRYPOINT ["/bin/bash", "/custom-entrypoint.sh"]
