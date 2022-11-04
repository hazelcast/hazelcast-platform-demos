# Hazelcast Platform Demo Applications - Benchmark - NEXMark

NEXMark - *Niagara Extension to XMark*

In an earlier work, [see here](https://hazelcast.com/blog/billion-events-per-second-with-millisecond-latency-streaming-analytics-at-giga-scale/),
Hazelcast Jet ran NEXMark at 1 billion events per second on 45 "`c5.4xlarge`" machine types on AWS.
"`c5.4xlarge`" machines have 16 vCPUs and 32GB of RAM.

This is the next iteration of this benchmark, upgraded to Hazelcast Platform (the combination of Hazelcast IMDG and Hazelcast Jet)
and converted to run on Java 17 and Kubernetes.

## NEXMark

The original [XMark](https://projects.cwi.nl/xmark/) is a benchmarking project for XML.

This was converted to a streaming version, [NEXMark](https://datalab.cs.pdx.edu/niagara/NEXMark/)
with a Java implementation you can download from [here](https://datalab.cs.pdx.edu/niagara/NEXMark/NEXMarkGen.tgz).

## Hazelcast's original benchmark

The original benchmark, where performance was tried up to the billion events per second is
[here](https://github.com/hazelcast/big-data-benchmark/tree/master/nexmark-jet/src/main/java/com/hazelcast/jet/benchmark/nexmark).

This was NEXMark's Java converted to run with Hazelcast Jet in 2021.

Since then, Hazelcast Jet and Hazelcast IMDG merged to form the Hazelcast Platform, so the benchmark would need further
adjustment and we wanted to achieve even higher throughput.

## New benchmark

TODO XXX FIXME
TODO XXX FIXME
TODO XXX FIXME

## Modules

There are 6 modules in this example, alphabetically:

```
grafana/
hazelcast-node/
jobs/
management-center/
prometheus/
webapp/
```

In logical order:

### 1. `management-center`

Builds a Hazelcast Management Center, configured with connectivity to the
Hazelcast grid as a stateful set in Kubernetes.

### 2. `prometheus`

Builds a Prometheus instance, configured to pull stats from the above
`management-center` module.

### 3. `grafana`

Builds a Grafana instance, with Hazelcast dashboard included, configured to connect to
the Prometheus instance from the above `prometheus` module.

### 4. `hazelcast-node`

Builds a Hazelcast server node.

This is intended to be as simple as possible, extended with some logging to confirm
how it has been configured.

### 5. `jobs`

Contains the processing jobs for NEXMark, the 8 queries coded as Hazelcast streaming
pipeline jobs.

### 6. `webapp`

A Hazelcast client of the grid of server nodes, with a web UI to submit the
benchmark jobs.

## Build

You will need a license key for monitoring, see [here](https://github.com/hazelcast/hazelcast-platform-demos#build-instructions).

From the top level directory, build using:

```
mvn clean install -Prelease
```

Using the `-Prelease` profile builds Docker images. This demo is intended for running on multiple pod
in Kubernetes.

## Tuning configurables

TODO XXX FIXME
TODO XXX FIXME
TODO XXX FIXME

## Deployment scripts

There are 3 files to deploy the example to Kubernetes. You will likely need to amend the image
repository specifications to match what you have.

They're named with numbers: `kubernetes-1.yaml`, `kubernetes-2.yaml` and `kubernetes-3.yaml`.

This is just a suggested ordering, you can run them in any order. It doesn't make a lot
of sense to start the client before the servers, but the client will try to reconnect
so it'll work.

If you start activity (`kubernetes-2.yaml` / `kubernetes-3.yaml`) while the Hazelcast
grid is scaling to the size of it's stateful set, you will create the partition
table so each node will then trigger a migration as it joins. This is fine, but will
slow down the scale up.

If you run benchmarking while the grid is scaling up or down, the results
will be distorted.

### 1. [kubernetes-1.yaml](./src/main/scripts/kubernetes-1.yaml)

There are tunables in the `env` section for the Hazelcast stateful set.

As configured, this creates a stateful set of size 1. You should scale this to the size you need

### 2. [kubernetes-2.yaml](./src/main/scripts/kubernetes-2.yaml)

This creates Hazelcast's Management Center, Prometheus and Grafana.

### 3. [kubernetes-3.yaml](./src/main/scripts/kubernetes-3.yaml)

This creates a Hazelcast web client to allow you to initiate benchmarking interactively.

## Passwords

The logon/password for Management Center and Grafana are set in the top level
[pom.xml](https://github.com/hazelcast/hazelcast-platform-demos/blob/master/pom.xml).

## Running a benchmark

### Running a benchmark on AWS EC2

To run on AWS it's easiest to create and configure on machine instance, then
clone this to make all the others ready configured.

When creating instances, if they are given a name scripting can find them easily.

#### 1. AWS base image

First, create a Ubuntu Server instance, (22.04 LTS) of machine type "*c5.4xlarge*".

Once live, this needs configured with Java, the Hazelcast code and config.

Install Java 17, following the same mechanism as used for [hazelcast-node](./hazelcast-node/Dockerfile):

```
ssh ubuntu@12.34.56.78 sudo apt-get update
ssh ubuntu@12.34.56.78 sudo apt-get install -y wget
ssh ubuntu@12.34.56.78 wget -q https://download.java.net/java/GA/jdk17/0d483333a00540d886896bac774ff48b/35/GPL/openjdk-17_linux-x64_bin.tar.gz
ssh ubuntu@12.34.56.78 tar xf openjdk-17_linux-x64_bin.tar.gz
ssh ubuntu@12.34.56.78 sudo update-alternatives --install /usr/bin/java java /home/ubuntu/jdk-17/bin/java 1
ssh ubuntu@12.34.56.78 java -version
```

Copy the scripts, config files and code onto the machine:

```
scp ./src/main/scripts/hazelcast.xml ubuntu@12.34.56.78:
scp ./src/main/scripts/hazelcast-client.xml ubuntu@12.34.56.78:
scp ./src/main/scripts/ubuntu-hazelcast-node.sh ubuntu@12.34.56.78:
scp ./src/main/scripts/ubuntu-management-center.sh ubuntu@12.34.56.78:
scp ./src/main/scripts/ubuntu-webapp.sh ubuntu@12.34.56.78:
scp ./hazelcast-node/target/nexmark-hazelcast-node-?.?-jar-with-dependencies.jar ubuntu@12.34.56.78:
scp ./webapp/target/nexmark-webapp-?.?.jar ubuntu@12.34.56.78:
```

(You'll need to add your connection credentials, and use the correct IP address obviously.)

Then *stop* the machine and use it to create an "*AMI*" image.

#### 2. AWS "oldest" member

Create one instance of the new image for the oldest member.

Machine type should be "*c5.4xlarge*" to recreate the original benchmark.

On this machine, start a Hazelcast node, passing the *external* IP address twice
of the oldest member. The first IP is the oldest member and the second IP is
the current member, which here are the same.

```
ssh ubuntu@12.34.56.78 ./ubuntu-hazelcast-node.sh 12.34.56.78 12.34.56.78
```

#### 3. AWS other members

Create as many other instances off the new image for the other nodes in the
cluster. For example, to repeat the 45 node cluster benchmark, create 44 other
nodes.

The machine type should be the same as the oldest member in the previous step.

On each, start the Hazelcast member passing it the external IP of the oldest member
and it's own external IP. The first IP gives it the cluster to join and the
second is so it can itself be reached.

```
ssh ubuntu@78.56.34.12 ./ubuntu-hazelcast-node.sh 12.34.56.78 78.56.34.12
```

You will likely want to script this!

If you used a name when creating the instances, you can find their IP with a command like:

```
aws ec2 describe-instances --filters Name=tag:Name,Values=yourname | grep PublicIpAddress
```

#### 4. AWS WebApp

Create another instance for the web application. It doesn't have to be the same machine
type as the member nodes, but this is simplest.

Start the web application, with the address of the any member, such as the "oldest" member.

```
ssh ubuntu@33.44.55.66 ./ubuntu-webapp.sh 12.34.56.78
```

Then go to the URL for that instance's IP - [http://33.44.55.66:8080/](https://hazelcast.com/)

#### 5. AWS Management Center (optional)

Create one last instance for the Management Center. Again, it doesn't have to be the
same machine type as the member nodes except for simplicity.

Download the version of Management Center appropriate for the benchmark from
[here](https://hazelcast.com/open-source-projects/downloads/), unpack it,
and go to the directory that contains the Management Center jar file.

Use this command to copy it to the host and start.

```
scp ./hazelcast-management-center-*.jar ubuntu@22.44.66.88:
ssh ubuntu@22.44.66.88 ./ubuntu-management-center.sh
```

Then go to the URL for that instance's IP - [http://22.44.66.88:8080](https://hazelcast.com/)

You need to do two further steps.

Firstly, add your license key in the administration section. See [here](https://hazelcast.com/contact/)
if you need one.

Second, add a cluster connection. The cluster is called "nexmark" and you can pass
the IP of any of the members for it to connect.


### Running a benchmark on Kubernetes


