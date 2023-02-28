FROM --platform=linux/amd64 library/python:3.10

COPY target/classes/*.py /
COPY target/classes/requirements.txt /

RUN pip install --upgrade pip
RUN pip install hazelcast-python-client
RUN pip install psutil

# Default assume in Kubernetes, else Docker
ENV HOST_IP ""
ENV MY_KUBERNETES_ENABLED "true"

CMD ["bash", "-c", "set -euo pipefail \
      && echo @@@@@@@@@@ \
      && echo arch \
      && arch \
      && echo @@@@@@@@@@ \
      && echo python client.py \
      && python client.py \
     "]
