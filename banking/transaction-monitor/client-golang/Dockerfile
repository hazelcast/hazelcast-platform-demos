FROM --platform=linux/amd64 library/golang:1.20.1-bullseye

WORKDIR /build

COPY target/classes/go.mod /build/
COPY target/classes/go.sum /build/
COPY target/classes/hz/   /build/hz/

RUN go mod download
RUN env CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -o /tmp/client ./hz/client

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
      && echo /tmp/client \
      && /tmp/client \
     "]
