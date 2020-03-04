# Hazelcast Platform Demo Applications - Machine Learning - Reference Implementation

TODO - Add X,Y generator, log on all nodes, run at a faster speed
TODO - Update names on Kubernetes scriopts or use namespace?

minikube start --cpus=8 --memory=12g
kubectl config get-contexts
eval $(minikube docker-env)

docker pull library/python:3.7-buster
docker pull library/openjdk:11-jre-slim
