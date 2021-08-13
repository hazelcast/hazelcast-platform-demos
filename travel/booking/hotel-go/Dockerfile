FROM library/golang:1.16.6-buster

WORKDIR /root

COPY target/classes/go.mod   /root/
COPY target/classes/go.sum   /root/
COPY target/classes/hotel/   /root/hotel/

RUN CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo ./hotel/app

CMD ["./app"]
