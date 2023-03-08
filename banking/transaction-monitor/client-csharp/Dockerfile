# 7 may not work with M1 chip
#FROM --platform=linux/amd64 mcr.microsoft.com/dotnet/sdk:7.0
FROM --platform=linux/amd64 mcr.microsoft.com/dotnet/sdk:6.0

ARG CONTROL_FILE

WORKDIR /build

COPY target/classes/Client.* /build

RUN dotnet build

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
      && echo dotnet run \
      && dotnet run \
     "]
