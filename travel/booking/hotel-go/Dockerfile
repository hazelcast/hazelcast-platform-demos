FROM library/golang:1.22.0-bullseye

WORKDIR /root

COPY target/classes/go.mod   /root/
COPY target/classes/go.sum   /root/
COPY target/classes/hotel/   /root/hotel/

RUN CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo ./hotel/app

CMD ["./app"]
