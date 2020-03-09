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

### `python-job`

## Running -- Localhost

## Running -- Docker

## Running -- Kubernetes

