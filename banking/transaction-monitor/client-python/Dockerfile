FROM --platform=linux/amd64 library/python:3.10

ARG CONTROL_FILE

COPY target/classes/*.py /
COPY target/classes/requirements.txt /

RUN pip install --upgrade pip
RUN pip install hazelcast-python-client
RUN pip install psutil

# Control file stops Docker copy failing if no PEM files found. Put in target by maven-antrun-plugin
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
      && echo python client.py \
      && python client.py \
     "]
