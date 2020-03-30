# Hazelcast Platform Demo Applications - Machine Learning - Reference Implementation

A sample application demonstrating how Python code can be invoked as part of a Jet
streaming job, using a minimal Python module.

This example uses open-source Hazelcast only.

## Description

This text is just the quick reference to help you build and deploy the application.

## Building

To build for running from the command line, use:

```
mvn clean install
```

To build a containerized image, if you have Docker available, activate the `release` build profile:

```
mvn clean install -Prelease
```

The `release` build profile adds extra steps to build the container image.

## Modules

This example consists of two modules, `hazelcast-node` and `python-job`.

### 1. `hazelcast-node`

This module starts one Hazelcast process where Jet is running purely in "_embedded_" mode. You can run several such processes to make a cluster.

_Embedded_ mode here means that the Jet processing engine is embedded in the IMDG node that stores
the input and/or output data used by the Jet processing.

Keeping processing and storage together is the simplest choice, which is why it is used here.
It is equally possible for Jet to pull data from external IMDG nodes and push to other IMDG nodes.

#### Networking

Following the _cloud-first_ approach, the default configuration of this module is for Kubernetes discovery.

In the file [hazelcast.yml](./hazelcast-node/src/main/resources/hazelcast.yml) discovery is set up
to use Kubernetes in DNS mode.

If you don't want to use Kubernetes, you can override to use discovery on localhost
or via Docker.

#### RandomXYGenerator

The first Hazelcast node to start will auto-start the Jet job [RandomXyGenerator](./hazelcast-node/src/main/java/com/hazelcast/platform/demos/ml/ri/RandomXYGenerator.java).

Only the first Hazelcast node in the cluster starts this job, but execution of this job is duplicated onto other
nodes joining the cluster, so that each node in the cluster is running the job.

What this job does is randomly create _X_ &amp; _Y_ co-ordinates in a map named '_points_', using the _X_
value as the key. To avoid running out of space, the map is configured to automatically discard old entries.

The purpose of this job is to generate input data for the `python-job` to process. This is as a separate
job for clarity, all jobs need input source(s) but the source isn't relevant to the Python processing.

### 2. `python-job`

Module `python-job` is a client of the Hazelcast grid that deploys Jet processing jobs to the grid
for execution.

In fact, in this module are 2 Jet processing jobs, and you select which one to deploy with a
command line argument.

The first is [Pi1Job](./python-job/src/main/java/com/hazelcast/platform/demos/ml/ri/Pi1Job.java) which runs
[pi1.py](./python-job/src/main/resources/pi1.py)

The second is [Pi2Job](./python-job/src/main/java/com/hazelcast/platform/demos/ml/ri/Pi2Job.java) which runs
[pi2.py](./python-job/src/main/resources/pi2.py)

Refer to the link at the top of this page to find out the exact details of what they do.

For here, three things are important.

Firstly, the Python code is not all the Jet job does. The Python code is merely executed as one stage
in a pipeline, to transform output from the previous stage into input for the next stage.

Secondly, the Python code is held in the [resources](./python-job/src/main/resources) folder.
In this example, this is a location within the bundled Jar file, referenced by the
[getPythonServiceConfig](./python-job/src/main/java/com/hazelcast/platform/demos/ml/ri/MyUtils.java#L50).

The Python code can be anywhere that the `python-job` module can find it at run time.

Bundling the Python code inside the Jar file allows this code to be found when run as a Docker image
or in Kubernetes.

Finally, the file [requirements.txt](./python-job/src/main/resources/requirements.txt) held with the Python
code. This specifies any additional libraries to be loaded before the Python job starts, such as "_NumPy_"
and "_SciPy_". It is used to pre-load these libraries on each Hazelcast node that is to run Python, before
the job starts.

## Running -- Localhost

Two scripts are provided to run on localhost :
[localhost-hazelcast-node.sh](./src/main/scripts/localhost-hazelcast-node.sh)
and [localhost-python-job.sh](./src/main/scripts/localhost-python-job.sh).

Run at least one copy of `localhost-hazelcast-node.sh` to run a grid node.
The script will try to derive the correct IP address for TCP connectivity.

If you run two or more, they will cluster together. But remember they're running
on the same machine so this doesn't gain you much - the instances will be competing
for the same CPUs.

Once the grid is up, run `localhost-python-job.sh` to send the Python job to the
grid to execute. This takes the optional argument `pi1.py` or `pi2.py` to chose
which variant of the Python job to submit. You can submit each once.

Output from the Python job will appear where it runs, in the logs of the grid
nodes.

## Running -- Docker

Two scripts are provided to run on Docker :
[docker-hazelcast-node.sh](./src/main/scripts/docker-hazelcast-node.sh)
and [docker-python-job.sh](./src/main/scripts/docker-python-job.sh).

The `docker-hazelcast-node.sh` script will start one Docker container running
the Hazelcast node image. This will be bound to your host machine's network
rather than the Docker network, so it accessible from outside.

The Docker container binds to the Hazelcast default port (5701), so you can't
run two such containers on the same machine unless you amend the ports.

Once the grid is up, use the `docker-python-job.sh` with optional arguments
`pi1.py` or `pi2.py` to launch the selected Python job.

As before, processing output appears in the Hazelcast node system output.

## Running -- Kubernetes

Three deployment files are provided to run on Kubernetes :
[kubernetes-hazelcast-node.yaml](./src/main/scripts/kubernetes-hazelcast-node.yaml),
[kubernetes-python-job-pi1.yaml](./src/main/scripts/kubernetes-python-job-pi1.yaml)
and [kubernetes-python-job-pi2.yaml](./src/main/scripts/kubernetes-python-job-pi2.yaml).

The `kubernetes-hazelcast-node.yaml` creates a Hazelcast service and stateful set
in the default namespace. The stateful set has a replica size of 2, so you should
see two pods created and from their logs see these Hazelcast nodes form a cluster.

To launch the Python processing use `kubernetes-python-job-pi1.yaml` or
`kubernetes-python-job-pi2.yaml`, or both.

Each of the job variants `pi1.py` and `pi2.py` are launched as separate jobs
with preset arguments rather than use the command line, just for simplicity.

## Running -- Lifecycle

The process that sends the Python job to the grid is just a client. It will
connect to the grid, stream the Python code to the grid, and disconnect.

Execution of the Python code is infinite, so there is no point to the
deployment client in waiting for completion.

## Running -- Expected output

The job `RandomXYGenerator` logs every 100,000th point it produces. As this job
is autostarted, you should see a line like this periodically (the frequency will
depend on how fast you machine produces points):

```
17:16:29.742 [ INFO] [c.h.j.i.c.W.RandomXYGenerator/loggerSink#0] (0.6155604707201013, 0.48772579126259485)
```

The job `Pi1Job` logs every 5 seconds. If this job has been submitted, you will
see output like this:

```
***************************************************************    
Topic 'pi' : Job 'Pi1Job' : Value '3.1536595996336576'    
***************************************************************    
```

The job `Pi2Job` also logs every 5 seconds. The output is the exact same apart
from the name of the job that is cited:

```
***************************************************************    
Topic 'pi' : Job 'Pi2Job' : Value '3.156316310652143'    
***************************************************************    
```

Jobs `Pi1Job` and `Pi2Job` are trying to calculate Pi, and the approximation
will get more accurate the longer they are left running.

## Summary

This is an example showing how Python can become a stage in a Jet pipeline.

The Python module will probably be more complex, but needs to follow a similar
structure.

It will have a handler method (here "`def handle(points)`") that takes a list of
string inputs, runs each through your business logic, and produces a list
of string outputs.

Your Python code is just a collection of interpreted files that are sent from
the host where the Jet job is submitted to all hosts where the Jet job is
executed.

Jet will arrange that the actual input is distributed across multiple Python
instances to exploit the available CPUs on your host machines, and to collate
the output for later stages of the job.

Python is easy to deploy, it's just a step in the processing pipeline.
