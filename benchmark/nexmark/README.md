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

This is the next iteration of the benchmark. The code isn't really any different from before.

What's different is we push beyond one billion events per second, repeating the original AWS benchmark at higher volume.

And, we also try the same on Kubernetes, to see the impact that extra layer of complexity brings to performance.

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

Contains the processing jobs for NEXMark, the various queries coded as Hazelcast streaming pipeline jobs.

### 6. `webapp`

A Hazelcast client of the grid of server nodes, with a web UI to submit the benchmark jobs.

## Build

You will need a license key for monitoring, see [here](https://github.com/hazelcast/hazelcast-platform-demos#build-instructions).

From the top level directory, build using:

```
mvn clean install -Prelease
```

Using the `-Prelease` profile builds Docker images. This is needed for running on multiple pods in Kubernetes.

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

As configured, this creates a stateful set of size 1. You should scale this to the size you need.

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

##### 1a. Add Java to the base image

SSH to the base image machine as user `ubuntu` and run one of these two commands:

```
sudo apt-get update
sudo apt-get install -y wget
wget -q https://download.java.net/java/GA/jdk17/0d483333a00540d886896bac774ff48b/35/GPL/openjdk-17_linux-x64_bin.tar.gz
tar xf openjdk-17_linux-x64_bin.tar.gz
sudo update-alternatives --install /usr/bin/java java /home/ubuntu/jdk-17/bin/java 1
java -version
```

or

```
sudo apt-get update
sudo apt install openjdk-17-jdk
java -version
```

The first mechanism installs a specific version of Java 17. The second installs the latest version of Java 17,
and since the latest may obviously change over time, this could have a minor effect on results.

##### 1b. Add the test code to the base image

SCP the following files to the home directory for user `ubuntu`:

```
src/main/scripts/ubuntu-hazelcast-node.sh
src/main/scripts/ubuntu-management-center.sh
src/main/scripts/ubuntu-webapp.sh
src/main/scripts/hazelcast.xml
src/main/scripts/hazelcast-client.xml
hazelcast-node/target/nexmark-hazelcast-node-*.*-jar-with-dependencies.jar
webapp/target/nexmark-webapp-*.*.jar
```

These are scripts to start Hazelcast, the Management Center and the Web App, plus the config files
for Hazelcast and the Webapp, and the code for Hazelcast and the Webapp.


##### 1c. Add Management Center to the base image

If you intend use the Management Center, SCP the Management Center jar file (`hazelcast-management-center-*.*.*.jar`)
from the Management Center distribution to the `ubuntu` user's home directory.

##### 1d. Save the base image

Then *stop* the instance and use it to create an "*AMI*" image.

#### 2. Server instances

Use the AMI image (Ubuntu 22.04 LTS plus java and the test code) to create as many
instances as the intended Hazelcast cluster size. For example, 45 to repeat the original
test.

Machine type should be "*c5.4xlarge*" to recreate the original benchmark.
Networking should allow TCP connectivity, as this is how the nodes find each other,
and external access.

##### 2a. Find the IPs

Assuming there are a lot of instances in the cluster, it's easiest to use the command
line to find their external IP addresses.

If you used a name when creating the instances, you can find their IP with a command like:

```
aws ec2 describe-instances --filters Name=tag:Name,Values=yourname | grep PublicIpAddress
```

would list the details for all instances named `yourname`.

##### 2b. Start 1 member

From the list of IPs, pick the first external IP address to use for the first member.

SSH to it's host as user `ubuntu`, and run this command

```
./ubuntu-hazelcast-node.sh 12.34.56.78 12.34.56.78
```

This runs the script to start a Hazelcast server passing two arguments.

The first argument is external IP of the first server. 

The second argument is the external IP of this server.

Since this server is the first server, those two arguments are the same.

(Obviously, IP address `12.34.56.78` is just an example.)

There is a log file created on the host, `log-hazelcast-node` that you can inspect to check the process has started cleanly.

##### 2c. Start the other members

Once the first node has started, you can start all the others, in sequence or all at once.

For each, SSH to the host as user `ubuntu` and run this command

```
./ubuntu-hazelcast-node.sh 12.34.56.78 34.56.78.90
```

As before, the first argument is the exteranl IP of that first server and the
second is the external IP of the current server.

The current server isn't the first server, so these two arguments will differ.

You will likely want to script this!

Again, check `log-hazelcast-node` to see the process starts correctly and connects
to the others.


#### 3. Webapp client instance

Create an instance from the image for the webapp client with external access
enabled. 

The client doesn't do much, but for consistency select `c5.4xlarge` machine type.

In addition to TCP, ensure HTTP is enabled, since it's a webapp.

Once started, connect as user `ubuntu` and run this command:

```
./ubuntu-webapp.sh 12.34.56.78
```

This starts the client, with the argument being the IP address of any server
in the cluster. The client should connect to that one server and be informed
where the others are.

There is a `log-webapp` file created where you can check the client has started cleanly.

Assuming the client's external IP address is 56.78.90.12, then you can go to
[http://56.78.90.12:8080/](https://hazelcast.com/) and use the web panel to
submit the job you wish.


#### 5. Management Center instance

If you have a license, you should run the Management Center, as this enables
you to stop jobs so others can be run, and to monitor what is happening.

Create a machine instance as for the webapp, with TCP and HTTP enabled,
and again `c5.4xlarge` type for simplicity/consistency.

Log on to the machine as user `ubuntu`, and use the script

```
./ubuntu-management-center.sh
```

This takes no arguments. Again, there is a `log-management-center` file produced
to confirm it starts correctly.

Assuming the Management Center's external IP address is 78.90.12.34, then you can go to
[http://78.90.12.34:8080/](https://hazelcast.com/).

From the Management Center's web interface, there are two steps.

Firstly, add your license key in the administration section. See [here](https://hazelcast.com/contact/)
if you need one.

Second, add a cluster connection. The cluster is called "nexmark" and you can pass
the IP of any of the members for it to connect.



