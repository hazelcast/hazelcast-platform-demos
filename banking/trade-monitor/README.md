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

The `-Prelease` flag activates the release quality build profile, which will assume that
Docker exists and build container images for deployment.

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
for many of the other modules in the project, to hold shared items such as logging configuration
and the definition of constants.

### 2. `zookeeper`

The `zookeeper` module is only used for containerized deployments (ie. Docker and Kubernetes)
and *does not* represent a production quality Zookeeper deployment.

Zookeeper is not part of Hazelcast, but it's part of the external ecosystem this demonstration
needs as a data source.

For running on localhost, it is assumed you will already have Zookeeper running.

To facilitate running in a containerized environment, the `zookeeper` module provides an adequate
Zookeeper image to use. This Zookeeper is un-clustered (only one copy runs) and does not use
persistent volumes, so if it is stopped all data is lost. This is ideal for a demonstration,
but obviously not for production use.

### 3. `kafka-broker`

Similar to `zookeeper`, the `kafka-broker` module is used only for containerized deployments
and again *does not* represent a production quality Kafka deployment.

Kafka is not part of Hazelcast, but for the same reason as Zookeeper it's needed as the
external data source for the demonstration.

For running on localhost, it assumed you have Kafka running and connected to Zookeeper.

For running in a containerized environment, the `kafka-broker` module produces an adequate
image to use as a convenience. This is deployed in parallel (multiple copies run), but this
is for scaling not resilience. Kafka stored in the image is not on persistent volumes. This
is a useful simplification for a demonstration, and again not suitable for production.

### 4. `topic-create`

The `topic-create` module is only for containerized deployments.

It creates a container image, which if run will create the necessary Kafka topic ("`trades`")
used by the Trade Monitor applicaiton in Hazelcast.

It is not needed when running on localhost, the Kafka command line is used instead to
create the topic.

### 5. `kafdrop`

The `kafdrop` module is optional.

[Kafdrop](https://github.com/obsidiandynamics/kafdrop) is an open-source tool with an
appealing web UI for browsing a Kafka deployment.

It is used here as a way to independently browse the input to the Trade Monitor. This
could equally be done with the Kafka's command line "`kafka-console-consumer.sh`" tool,
although items are written to the topic at a high rate, so command line output tends
to flood the screen.

Kafdrop itself is unchanged in this module. 

### 6. `trade-producer`

The `trade-producer` module is the source of the stock market trades that the Trade
Monitor application is monitoring.

The companies being traded are real, the trades are not.

What this module does is random select from 3000 companies listed on the New York
Stock Exchange ( [NASDAQ](https://www.nasdaq.com/) ), and generate trades for
these companies. The trades have random prices and random quantities.

Trades are generated at a default rate of 300 per second to the Kafka topic named
"`trades`". 

You can generated millions of trades this way, depending what rate you set for
generation, how long you leave the `trade-producer` running for, and how much
disk space Kafka has for retention.

This is part of the purpose of this demonstration. Millions of trades can be
processed in seconds, depending on how many machines you have and how many
CPUs each has.

Trades have a random [UUID](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/UUID.html)
as their key on the Kafka topic. The main trade details are the value on the Kafka topic, structured as
JSON but written as a string.

Most of the trade fields should be intuitive. The "`symbol`" field is the lookup code for the stock symbol
on [NASDAQ](https://www.nasdaq.com/).
For example, the symbol "_FB_" is [Facebook](https://www.nasdaq.com/market-activity/stocks/fb).

### 7. `hazelcast-node`

TODO

#### Configuration

The Hazelcast node is mainly configured from the
[hazelcast.yml](./hazelcast-node/src/main/resources/hazelcast.yml) file.

Following the _cloud-first_ approach, the networking section of this file configures for Kubernetes.
This assumes DNS based discovery, for a service named "`trade-monitor-service.default.svc.cluster.local`"
and with some REST endpoints enabled to that Kubernetes probes can determine if the node is healthy.
This network is overridden by the scripts described below to run in Docker or localhost.

Also defined is an unordered index on the "`symbol`" field in the "`trades`" map. 
The index improves the query speed when looking up stock market trades by their
string symbol.

#### Ingest Trades

[IngestTrades](./hazelcast-node/src/main/java/com/hazelcast/platform/demos/banking/trademonitor/IngestTrades.java#L62)
is a Jet job that is automatically initiated when the Hazelcast node starts.

This job is a simple upload, or _ingest_ of data from Kafka into Hazelcast.

The input stage of the pipeline is a Kafka source, with the topic name "`trades`".

The output stage of the pipeline is an [IMap](https://docs.hazelcast.org/docs/4.0/javadoc/), also called "`trades`".

What is read from Kafka is written directly into Hazelcast, without enrichment, depletion, filtering or any
sophisticated stream processing.

So the effect of this job is to make trades written to Kafka visible in Hazelcast unchanged.

#### Aggregate Query

[AggregateQuery](./hazelcast-node/src/main/java/com/hazelcast/platform/demos/banking/trademonitor/AggregateQuery.java#L87)
is a separate Jeb job that is also automatically initiated when the Hazelcast node starts.

It has the same input as the `Ingest Trades` job, namely the Kafka "`trades`" topic.

TODO why two jobs.

### 8. `webapp`

TODO

## Running -- Localhost

TODO
kafka_2.13-2.4.0

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
3 node cluster, 10 million in 30 seconds

