FROM --platform=linux/amd64 library/python:3.8-buster

ARG CONTROL_FILE

RUN mkdir finos

COPY target/classes/public/* /finos/
COPY target/classes/requirements.txt /finos/

RUN pip install --upgrade pip
RUN pip install hazelcast-python-client
RUN pip install -r /finos/requirements.txt

# Control file stops Docker copy failing if no PFX/PEM files found. Put in target by maven-antrun-plugin
COPY target/classes/${CONTROL_FILE} target/*.pfx /tmp/
COPY target/classes/${CONTROL_FILE} target/*.pem /tmp/

# Default assume in Kubernetes, else Docker
ENV HOST_IP ""
ENV MY_KUBERNETES_ENABLED "true"

# Need customized to make environment available in browser
COPY target/classes/custom-entrypoint.sh  /
CMD ["bash", "-c", "set -euo pipefail \
      && echo @@@@@@@@@@ \
      && echo arch \
      && arch \
      && echo @@@@@@@@@@ \
      && echo date \
      && date \
      && echo @@@@@@@@@@ \
      && chmod 755 /custom-entrypoint.sh \
      && echo /custom-entrypoint.sh \
      && /custom-entrypoint.sh \
     "]
