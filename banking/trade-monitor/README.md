# Hazelcast Platform Demo Applications - Banking - Trade Monitor

An example showing continuous aggregation of stock market trades, providing
a UI for inspection of the aggregated values with the ability to drill-down
to see the trades contained in each aggregation.

This example uses open-source Hazelcast only.

Input is simulated and injected into a Kafka topic. Hazelcast reads from
this topic to present via  a reactive web UI, but is not aware how of
where the data originated,

## Description

The text here is just a quick reference for building and deploying.

To find out what it really does, how, and why, refer to <a href="_blank">TODO Link to website goes here TODO</a>.

## Building

For a standard build to run on your host machine, use:

```
mvn clean install
```

If you have Docker, and want to run Docker images or Kubernetes, use:

```
mvn clean install -Prelease
```

The `-Prelease` flag activates the release quality build profile, which will assume the
Docker exists and build containers for deployment.

## Modules

There are 8 modules in this example, alphabetically:

```
common/
hazelcast-node/
kafdrop/
kafka-broker/
topic-create/
trade-producer/
webapp/
zookeeper/
```

These are described below, a partial ordering on the way you should
understand and execute them.

### 1. `common`

The `common` module is not a deployed executable. As the name suggests, it is a common dependency
for many of the other modules in the project, to hold communal objects such as logging configuration
and the definition of constants.

### 2. `zookeeper`

The `zookeeper` module is only used for containerized deployments (ie. Docker and Kubernetes).

It exists only to create a predictable and repeatable copy of Zookeeper to connect to. You can
and should replace this with your own Zookeeper installation for more realistic experiments.

Part of Zookeeper's role is maintain a persistent disk based configuration store. For a real
deployment this would need to be an externally mounted volume, which draws questions of whether
this module really belongs as a container.

Since this example isn't directly concerned with the optimal production set-up of Zookeeper, it is
also deployed here un-clustered (ie. only one copy runs).

### 3. `kafka-broker`

Similar to `zookeeper`, the `kafka-broker` module is used only for containerized deployments.

It creates one Kafka broker which connects to the `zookeeper` module, and multiple should be
started to create a more realistic Kafka broker deployment.

As for `zookeeper`, this is questionable production deployment pattern. Persistent volumes
are required, which means external storage and this negates much of the purpose of
containerization.

Unlike `zookeeper`, multiple `kafka-broker` modules will be deployed. However, this is not
for resilience but for scaling and partitioning.

### 4. `topic-create`

TODO

### 5. `kafdrop`

TODO

### 6. `trade-producer`

TODO

### 7. `hazelcast-node`

TODO

### 8. `webapp`

TODO

## Running -- Localhost

TODO

## Running -- Docker

TODO

## Running -- Kubernetes

TODO

## Running -- Lifecycle

TODO

## Running -- Expected Output

TODO

## Summary

TODO

