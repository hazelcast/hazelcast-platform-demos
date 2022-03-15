FROM apachepulsar/pulsar:2.8.1

CMD ["bash", "-c", "set -euo pipefail \
      && echo @@@@@@@@@@@@@@@@@@@@ \
      && echo mkdir -p /pulsar/data /pulsar/conf \
      && mkdir -p /pulsar/data /pulsar/conf \
      && echo @@@@@@@@@@@@@@@@@@@@ \
      && echo bin/pulsar standalone \
      && bin/pulsar standalone \
     "]
