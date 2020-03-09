# Hazelcast Platform Demo Applications - Machine Learning - Reference Implementation

A sample application demonstrating how Python code can be invoked as part of a Jet
streaming job, using a minimal Python module.

This example uses open-source Hazelcast only.

## Description

This text is just the quick reference to help you build and deploy the application.

For details of what it does, how and why, refer to <a href="_blank">TODO</a>.

## Building

To build for running from the command line, use:

```
mvn clean install
```

To build a containerized image, assuming you have Docker available, activate the `release` build profile:

```
mvn clean install -Prelease
```

The `release` build profile adds ewxtra steps to build the container image.

## Modules

This example consists of two modules, `hazelcast-node` and `python-job`.

### 1. `hazelcast-node`

This module starts a Hazelcast process where Jet is running purely in "_embedded_" mode.

_Embedded_ mode here means that the Jet processing engine is embedded in the IMDG node that stores
the input and/or output data used by the Jet processing.

Keeping processing and storage together is the simplest choice, which is why it is used here.
It is equally possible for Jet to pull data from external IMDG nodes and push to other IMDG nodes.

#### Networking

Following the _cloud first_ approach, the default configuration of this module is for Kubernetes discovery.

In the file [hazelcast.yml](./hazelcast-node/src/main/resources/hazelcast.yml) discovery is set up
to use Kubernetes in DNS mode.

If you don't want to use Kubernetes, you can override to use discovery on localhost (127.0.0.1)
or via Docker.

#### RandomXYGenerator

TODO

### 2. `python-job`

TODO

## Running -- Localhost

TODO Don't run two on Localhost

## Running -- Docker

TODO Can't run two on Docker / Localhost

## Running -- Kubernetes

TODO
