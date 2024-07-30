# Remote job submission - 1 - Python ML

Brief instructions for remote job submissions.

Find the IP addresss assigned to `transaction-monitor-ecommerce-live-hazelcast-extra` in the Kubernetes cluster.

Check and submit using the Jet command line:

```
~/Downloads/hazelcast-enterprise-6.0.0/bin/hz-cli -t live@123.456.789.0 list-jobs
~/Downloads/hazelcast-enterprise-6.0.0/bin/hz-cli -t live@123.456.789.0 submit target/transaction-monitor-remote-job-sub-1.6.0-jar-with-dependencies.jar
~/Downloads/hazelcast-enterprise-6.0.0/bin/hz-cli -t live@123.456.789.0 list-jobs
```

Connect to a pod and see what is running:

```
kubectl exec --stdin --tty transaction-monitor-ecommerce-live-hazelcast-0 -- /bin/bash
ps -elf
```
