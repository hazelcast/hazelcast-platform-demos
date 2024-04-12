# Kubernetes specific instructions

Use these scripts, edit to replace `FLAVOR` with what you're using, and you may need to adjust the image
name to match your image repository.

```
kubernetes-1-zookeeper-kafka-firsthalf.yaml
kubernetes-2-create-configmap.sh
kubernetes-3-kafka-secondhalf.yaml
kubernetes-4-kafdrop-topic-rdbms.yaml
kubernetes-5-optional-hazelcast-enterprise.yaml
kubernetes-5-optional-hazelcast.yaml
kubernetes-6-webapp.yaml
kubernetes-7-transaction-producer.yaml
kubernetes-8-polyglot-clients.yaml
```

## Steps

### 1. `kubernetes-1-zookeeper-kafka-firsthalf.yaml`

YAML to create Zookeeper and services for Kafka.

### 2. `kubernetes-2-create-configmap.sh`

Shell script to create a YAML ConfigMap with external IPs of Kafka brokers.

### 3. `kubernetes-3-kafka-secondhalf.yaml`

YAML to create Kafka brokers using above ConfigMap to configure.

### 4. `kubernetes-4-kafdrop-topic-rdbms.yaml`

YAML to create Kakfa topic with non-default partition count for optimal reading, Kafdrop for inspection. Also MySql, Postgres, etc databases.

### 5

Do one of 5.A, 5.B or 5.C.

### 5.A `kubernetes-5-optional-hazelcast-enterprise.yaml`

Create Enterprise Hazelcast clusters for transaction storage, and Grafana/Prometheus for charting.

Use `kubectl exec --stdin --tty transaction-monitor-ecommerce-live-hazelcast-0 -- /bin/bash` to connect to see tiered store
directory "/data/transaction-monitor".

### 5.B `kubernetes-5-optional-hazelcast.yaml`

Create open source Hazelcast cluster for transaction storage.

### 5.C Hazelcast Cloud

Use an existing Hazelcast Cloud cluster, so don't run any YAML for step 5.

### 6=. `kubernetes-6-webapp.yaml`

YAML to create WebApp connecting to Hazelcast Cloud.

Can be run before or after `kubernetes-7-data-producers.yaml`. Before would be usual, but there is no dependency
between these two.

### 6=. `kubernetes-7-data-producers.yaml`

YAML to create a stream of transactions.

Can be run before or after `kubernetes-6-webapp-and-monitoring.yaml`. After would be usual, but there is no dependency
between these two.

### 7=. `kubernetes-8-polyglot-clients.yaml`

Optional step to run clients in non-Java languages, to poll the data.

## Bonus

If running on Google Cloud, there is a script `gke.sh` that should do all these steps for you, selection option 5.A.


