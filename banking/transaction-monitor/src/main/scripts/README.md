# Kubernetes specific instructions

```
kubernetes-1-zookeeper-kafka-firsthalf.yaml
kubernetes-2-create-configmap.sh
kubernetes-3-kafka-secondhalf.yaml
kubernetes-4-kafdrop-topic-postgres.yaml
kubernetes-5-optional-hazelcast-enterprise.yaml
kubernetes-5-optional-hazelcast.yaml
kubernetes-6-webapp-and-monitoring.yaml
kubernetes-7-transaction-producer.yaml
```

## Steps

### 1. `kubernetes-1-zookeeper-kafka-firsthalf.yaml`

YAML to create Zookeeper and services for Kafka.

### 2. `kubernetes-2-create-configmap.sh`

Shell script to create a YAML ConfigMap with external IPs of Kafka brokers.

### 3. `kubernetes-3-kafka-secondhalf.yaml`

YAML to create Kafka brokers using above ConfigMap to configure.

### 4. `kubernetes-4-kafdrop-topic-postgres.yaml`

YAML to create Kakfa topic with non-default partition count for optimal reading, Kafdrop for inspection and a Postgres database.

### 5

Do one of 5.A, 5.B or 5.3.

### 5.A `kubernetes-5-optional-hazelcast-enterprise.yaml`

Create Enterprise Hazelcast clusters for transaction storage.

### 5.B `kubernetes-5-optional-hazelcast.yaml`

Create open source Hazelcast cluster for transaction storage.

### 5.C Viridian

Use an existing Viridian cluster, so don't run any YAML for step 5.

### 6=. `kubernetes-6-webapp-and-monitoring.yaml`

YAML to create WebApp connecting to Hazelcast Cloud, and Grafana/Prometheus for charting.

Can be run before or after `kubernetes-7-transaction-producer.yaml`. Before would be usual, but there is no dependency
between these two.

### 6=. `kubernetes-6-transaction-producer.yaml`

YAML to create a stream of transactions.

Can be run before or after `kubernetes-6-webapp-and-monitoring.yaml`. After would be usual, but there is no dependency
between these two.

