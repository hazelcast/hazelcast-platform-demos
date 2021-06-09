# Remote job submission

Brief instructions for remote job submissions.

Find the IP addresss assigned to `kubernetes-hazelcast-node-extra.yaml` by the Kubernetes cluster.

Check and submit using the Jet command line:

```
~/Downloads/hazelcast-jet-4.5/bin/jet -t grid@123.456.789.0 list-jobs
~/Downloads/hazelcast-jet-4.5/bin/jet -t grid@123.456.789.0 submit target/trade-monitor-remote-job-sub-1-5.0.jar
~/Downloads/hazelcast-jet-4.5/bin/jet -t grid@123.456.789.0 list-jobs
```

Connect to a pod and see what is running:

```
kubectl exec --stdin --tty trade-monitor-grid-hazelcast-0 -- /bin/bash
ps -elf
```
