FROM --platform=linux/amd64 bitnami/kafka:3.7.0

COPY target/classes/custom-entrypoint.sh  /
ENTRYPOINT ["/bin/bash", "/custom-entrypoint.sh"]
