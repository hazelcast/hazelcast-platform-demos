#!/bin/bash
kubectl delete -f kubernetes-2-blue-service.yaml
for POD in `kubectl get pods | grep blue | cut -d" " -f1`
do
 CMD="kubectl delete pods $POD --grace-period=0 --force"
 ( $CMD ) &
done
jobs
kubectl delete -f gke-2-blue.yaml
