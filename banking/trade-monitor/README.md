# Hazelcast Platform Demo Applications - Banking - Trade Monitor

An example showing continuous aggregation of stock market trades, providing
a UI for inspection of the aggregated values with the ability to drill-down
to see the trades contained in each aggregation.

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

## Running -- Localhost

## Running -- Docker

## Running -- Kubernetes

