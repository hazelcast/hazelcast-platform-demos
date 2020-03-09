# Hazelcast Platform Demo Applications - Machine Learning - Reference Implementation

A sample application demonstrating how Python code can be invoked as part of a Jet
streaming job, using a minimal Python module.

This example uses open-source Hazelcast only.

## Description

## Building

To build for running from the command line, use:

```
mvn clean install
```

To build a containerized image, and assuming you have Docker running, activate the `release` build profile:

```
mvn clean install -Prelease
```

## Modules

This example consists of two modules.

### `hazelcast-node`

This module starts a single Hazelcast process where Jet is running purely in "_embedded_" mode.

_Embedded_ mode here means that the Jet processing engine is embedded in the IMDG node that stores
the input and/or output data used by the Jet processing.

Keeping processing and storage together is the simplest choice, which is why it is used here.
It is equally possible for Jet to pull data from external IMDG nodes and push to other IMDG nodes.

#### Networking

Following the _cloud first_ approach, the configuration of this module is for Kubernetes discovery.

In the file [hazelcast.yml](./hazelcast-node/src/main/resources/hazelcast.yml) discovery is set up
to use Kubernetes in DNS mode.

#### RandomXYGenerator


### `python-job`

## Running -- Localhost

## Running -- Docker

## Running -- Kubernetes

