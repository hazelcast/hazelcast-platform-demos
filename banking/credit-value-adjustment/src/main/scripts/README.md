# Kubernetes specific instructions

Use these scripts, in sequence.

```
kubernetes-1-cpp.yaml
kubernetes-2-create-configmap.sh
kubernetes-3-cpp-tester.yaml
kubernetes-3-cpp-tester2.yaml
kubernetes-3-cpp-tester3.yaml
kubernetes-3-grafana-prometheus.yaml
kubernetes-3-hazelcast-node-site1.yaml
kubernetes-3-hazelcast-node-site2.yaml
kubernetes-3-management-center.yaml
kubernetes-4-data-loader-site1.yaml
kubernetes-4-data-loader-site2.yaml
kubernetes-5-webapp-site1.yaml
kubernetes-5-webapp-site2.yaml
```

## Steps

### 1. `kubernetes-1-cpp.yaml`

YAML to create C++ pods and load balancer. As least as many C++ pods as (Jet CPUs x pods).

### 2. `kubernetes-2-create-configmap.sh`

Shell script to create a YAML ConfigMap with IP for C++ Load Balancer

### 3=. `kubernetes-3-cpp-tester.yaml`, `kubernetes-3-cpp-tester2.yaml` and `kubernetes-3-cpp-tester3.yaml` (optional)

Tester scripts to confirm C++ is working.

### 3=. `kubernetes-3-grafana-prometheus.yaml`, `kubernetes-3-hazelcast-node-site1.yaml`, `kubernetes-3-hazelcast-node-site2.yaml`
and `kubernetes-3-management-center.yaml`

Creates two clusters, with monitoring. Omit if using Viridian.

### 4. `kubernetes-4-data-loader-site1.yaml` or `kubernetes-4-data-loader-site2.yaml`

Use either one to load test data into one cluster, WAN replicated to the other.
Only use `kubernetes-4-data-loader-site1.yaml` if Viridian.

### 5. `kubernetes-5-webapp-site1.yaml` and `kubernetes-5-webapp-site2.yaml`

Start clients connected to each cluster.
Only use `kubernetes-5-webapp-site1.yaml` if Viridian.





