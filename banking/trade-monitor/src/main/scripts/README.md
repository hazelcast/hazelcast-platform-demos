# Kubernetes specific instructions

```
kubernetes-1-zookeeper-kafka-firsthalf.yaml
kubernetes-2-create-configmap.sh
kubernetes-3-kafka-secondhalf.yaml
kubernetes-4-kafdrop-topic-create.yaml
kubernetes-5-webapp-and-monitoring.yaml
kubernetes-6-trade-producer.yaml
```

## Steps

### 1. `kubernetes-1-zookeeper-kafka-firsthalf.yaml`

YAML to create Zookeeper and services for Kafka.

### 2. `kubernetes-2-create-configmap.sh`

Shell script to create a YAML ConfigMap with external IPs of Kafka brokers.

### 3. `kubernetes-3-kafka-secondhalf.yaml`

YAML to create Kafka brokers using above ConfigMap to configure.

### 4. `kubernetes-4-kafdrop-topic-create.yaml`

YAML to create Kakfa topic with non-default partition count for optimal reading, and Kafdrop for inspection.

### 5=. `kubernetes-5-webapp-and-monitoring.yaml`

YAML to create WebApp connecting to Hazelcast Cloud, and Grafana/Prometheus for charting.

Can be run before or after `kubernetes-6-trade-producer.yaml`. Before would be usual, but there is no dependency
between these two.

### 5=. `kubernetes-6-trade-producer.yaml`

YAML to create a stream of trades.

Can be run before or after `kubernetes-5-webapp-and-monitoring.yaml`. After would be usual, but there is no dependency
between these two.


