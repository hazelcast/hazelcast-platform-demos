FROM --platform=linux/amd64 library/python:3.10

COPY target/classes/*.py /

RUN pip install hazelcast-python-client

CMD ["bash", "-c", "set -euo pipefail \
      && echo @@@@@@@@@@ \
      && echo arch \
      && arch \
      && echo @@@@@@@@@@ \
      && echo python client.py \
      && python client.py \
     "]
