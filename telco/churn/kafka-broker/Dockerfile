FROM bitnami/kafka:2.4.0

# OpenJDK 11.0.6
RUN java --version

# To reach bootstrap servers, only 9092 if separate hosts (Kubernetes)
EXPOSE 9092
EXPOSE 9093
EXPOSE 9094

# Extra config goes here
