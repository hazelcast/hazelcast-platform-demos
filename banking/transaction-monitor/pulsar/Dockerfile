FROM --platform=linux/amd64 apachepulsar/pulsar:2.8.2

CMD ["bash", "-c", "set -euo pipefail \
      && echo @@@@@@@@@@@@@@@@@@@@ \
      && echo mkdir -p /pulsar/data /pulsar/conf \
      && mkdir -p /pulsar/data /pulsar/conf \
      && echo @@@@@@@@@@@@@@@@@@@@ \
      && echo bin/pulsar standalone \
      && bin/pulsar standalone \
     "]
