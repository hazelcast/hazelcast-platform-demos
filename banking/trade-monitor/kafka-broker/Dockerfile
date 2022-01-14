FROM bitnami/kafka:3.0.0

COPY target/classes/custom-entrypoint.sh  /
ENTRYPOINT ["/bin/bash", "/custom-entrypoint.sh"]
