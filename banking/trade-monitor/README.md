# Hazelcast Platform Demo Applications - Banking - Trade Monitor

[Screenshot1]: src/site/markdown/images/Screenshot1.png "Image screenshot1.png"
[Screenshot2]: src/site/markdown/images/Screenshot2.png "Image screenshot2.png"
[Screenshot3]: src/site/markdown/images/Screenshot3.png "Image screenshot3.png"

An example showing continuous aggregation of stock market trades, providing
a UI for inspection of the aggregated values with the ability to drill-down
to see the trades contained in each aggregation.

[Watch The Video](https://hazelcast.com/resources/continuous-query-with-drill-down-demo/)

This example only requires open-source Hazelcast.

There are modules for monitoring which involve licensing, but these are optional.

Input is simulated and injected into a Kafka topic. Hazelcast reads from
this topic to present via  a reactive web UI, but is not aware how of
where the data originated:

![Image of the Trade Monitor expanded view of "ASPS" symbol][Screenshot2]

## Description

The text here is just a quick reference for building and deploying.

To find out what it really does, how, and why, refer to
<a href="https://hazelcast.org/resources/continuous-query-with-drilldown-trade-monitoring-reference-architecture/">here</a>
and 
<a href="https://hazelcast.org/resources/hazelcast-continuous-query-with-drilldown-trade-monitoring-wp/">here</a>.

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

There are 12 modules in this example, alphabetically:

```
common/
grafana/
hazelcast-node/
kafdrop/
kafka-broker/
management-center/
prometheus/
remote-job-sub-1/
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

The `hazelcast-node` is the module where actual work of the Trade Monitor is done, even though
this needs the separate `webapp` module below to visualize.

This module creates a single Hazelcast node with both in-memory data grid (IMDG) and Jet functionality.

IMDG functionality here is the storage of trade data and trade aggregation results in maps.
Specifically, [IMap](https://docs.hazelcast.org/docs/5.0/javadoc/com/hazelcast/map/IMap.html) which is
a distributed map spread across as many Hazelcast nodes as you have. To increase storage capacity,
all you need do is run more Hazelcast nodes, and capacity scales linearly.

Jet functionality here is the processing of trades to aggregate their values to produce running
totals. This aggregation gives us the values per stock symbol for trading volume. Calculating these
aggregations is distributed across the Hazelcast nodes. Adding more Hazelcast nodes spreads the
aggregation across them linearly, allowing the same number of stock symbols to be aggregated
faster or to aggregate a larger number of stock symbols at the previous rate.

Since Jet and IMDG functionality are present in the same node, they must scale together.
Storage need for trades and processing need for aggregation are unlikely to need the same
scaling, so if this deviates substantially running Jet and IMDG functionality in separate
Hazelcast clusters would be the solution.

#### Configuration

The Hazelcast node is mainly configured from the
[hazelcast.yml](./hazelcast-node/src/main/resources/hazelcast.yml) file.

Following the _cloud-first_ approach, the networking section of this file configures for Kubernetes.
This assumes DNS based discovery, for a service named "`trade-monitor-service.default.svc.cluster.local`"
and with some REST endpoints enabled to that Kubernetes probes can determine if the node is healthy.
This network is overridden by the scripts described below to run in Docker or localhost.

Hazelcast is also able to use Zookeeper for discovery, using the
[hazelcast-zookeeper](https://github.com/hazelcast/hazelcast-zookeeper) plugin, although this is not
done here.

Also defined is an unordered index on the "`symbol`" field in the "`trades`" map. 
The index improves the query speed when looking up stock market trades by their
string symbol.

#### Ingest Trades

[IngestTrades](./hazelcast-node/src/main/java/com/hazelcast/platform/demos/banking/trademonitor/IngestTrades.java#L62)
is a Jet job that is automatically initiated when the Hazelcast node starts.

This job is a simple upload, or _ingest_ of data from Kafka into Hazelcast.

The input stage of the pipeline is a Kafka source, with the topic name "`trades`".

The output stage of the pipeline is an [IMap](https://docs.hazelcast.org/docs/5.0/javadoc/com/hazelcast/map/IMap.html), also called "`trades`".

What is read from Kafka is written directly into Hazelcast, without enrichment, depletion, filtering or any
sophisticated stream processing.

So the effect of this job is to make trades written to Kafka visible in Hazelcast unchanged.

#### Aggregate Query

[AggregateQuery](./hazelcast-node/src/main/java/com/hazelcast/platform/demos/banking/trademonitor/AggregateQuery.java#L87)
is a separate Jeb job that is also automatically initiated when the Hazelcast node starts.

It has the same input as the `Ingest Trades` job, namely the Kafka "`trades`" topic.

What this job does differently is grouping and aggregation. All incoming trades are grouped by their
stock symbol, and for each of the 3000 or so symbols a rolling aggregation updates a total of
trade count and trade volume (stock price * trade quantity).

For each trade that comes in, the running total for that trade is updated in the
[IMap](https://docs.hazelcast.org/docs/5.0/javadoc/com/hazelcast/map/IMap.html) called
"`AggregateQuery_results`".

Jet job `AggregatedQuery` processes the same input as Jet job `IngestTrades`, and at the same
time. So they could be merged for efficiency, but here they are kept apart for clarity of understanding.

### 8. `webapp`

The last module in the demo is a web-based UI to display the trade data and trade aggregation
stored in the Hazelcast grid.

`webapp` is a Hazelcast client that connects to the Hazelcast grid nodes, and presents this
information using a reactive interface.

When module is started, if you go to it's home page you will see a list of stock symbol codes,
the stock's real name, the latest price, and the trading volume (stock price * trade quantity).

There are several pages of data here, as there are more than 3000 stock symbol codes. 

The page updates dynamically. As trades are processed, the values shown on screen will change,
and will briefly go green if the price is increasing or red if the price is decreasing.

For every stock symbol, there is an expand/collapse toggle at the left. If you click on this,
you will see a list of all trades for that stock symbol since the beginning of execution of
the demonstration. The trades for that symbol are listed most recent first, but if there are
millions of trades then that means thousands of trades for each stock symbol, so potentially
a very long list.

### 9. `management-center` (optional)

This is Hazelcast's Management Center, for collating, viewing and controlling a cluster.

This is licensed software, but for the purposes of this demo `management-center`, `prometheus`
and `grafana` are an optional trio. If you don't want to obtain a license, skip these three.

Register [here](https://hazelcast.com/download/) to request the evaluation license keys you
need, and put them in your `settings.xml` file as described in [Repository top-level README.md](../../README.md).
Be sure to mention this is for Trade Monitor so you get a license with the correct capabilities.

### 10. `prometheus` (optional)

Prometheus is an open-source time-series database. It connects to `management-center` for its
data, so if you choose not to run `management-center` there's no need to bother with
`prometheus`.

### 11. `grafana` (optional)

Grafana is open-source dashboarding software. If connects to `prometheus` for its data, so
if you don't run `management-center` and so don't run `prometheus`, don't bother with
`grafana` either.

### 12. `remote-job-sub-1` (optional)

The main jobs in this example are started with the cluster automatically.

This module is an optional extra to show how to package and submit from the command line.
The job involvd is an extra, it does not produce data for the web application. You can
browse it's output with the Management Center.

## Running -- sequence

The following sections describe how to run the example on your local machine, on Docker
or Kubernetes. For all, there is a partial ordering on the modules.

1.`zookeeper` Zookeeper must be first, it is the configuration registry used by Kafka.

2.`kafka-broker` Kafka is second, as it uses Zookeeper for configuration.

3.== `topic-create` Kafka starts without topics, this module should be run third to create the needed topic.

3.== `kafdrop` The Kafka browser has to start after Kafka. It doesn't have to start after the topic is
created, but it shows the most if the topic exists and is being written to.

4.== `trade-producer` This writes the trade data to the Kafka topic created.

4.== `hazelcast-node` The Hazelcast node has Jet jobs that read from the Kafka topic created. There doesn't
need to be anything being written to the Kafka topic, but if there is no input there is no output from
a streaming job.

5.== `webapp` The web UI is a client of the Hazelcast cluster, so needs Hazelcast node(s) to be runnning.

5.== `management-center` Connects to the cluster in step 4.

6. `prometheus` Connects to the Hazelcast Management Center in step 5.

7. `grafana` Connects to Prometheus in step 6.

8. `remote-job-sub-1` Launches an additional Jet job from the command line.

Ignoring the partial ordering, the recommended start sequence would be `zookeeper`, `kafka-broker`,
`topic-create`, `kafdrop`, `trade-producer`, `hazelcast-node` and finally `webapp`.

If you add the optional monitoring, then `management-center`, `prometheus` and `grafana`.

## Running -- Localhost

For running directly on your local machine, the assumption is made that you already have Kafka
and Zookeeper running and with an appropriate configuration as described in the following
subsection.

Assuming so, run the [localhost-trade-producer.sh](./localhost-trade-producer.sh) script
to start writing trades to the Kafka topic "`trades`". This takes an optional integer
argument on the command line for the rate at which to generate trades. If not specified
300 will be used, for a rate of 300 random trades per second.

Next run one or more instances of the Hazelcast grid nodes, using the
[localhost-hazelcast-node.sh](./src/main/scripts/localhost-hazelcast-node.sh) script.
These will automatically cluster together, but since each will attempt to use all the
CPUs available, running two or more nodes on the same machine won't give increased
performance.

Finally, [localhost-webapp.sh](./src/main/scripts/localhost-webapp.sh) will start the
web client that displays the results of the Trade Monitor. Access this from:

```
http://localhost:8080/
```

### Kafka &amp; Zookeeper on Localhost
To match the containerized version, you should have one Zookeeper instance on port 2181
and three Kafka broker instances running on ports 9092, 9093 and 9094 using that Zookeeper.

You will also need the "`trades`" topic, which you could create:

```
kafka-topics.sh --zookeeper 127.0.0.1:2181 --create --partitions 3 --replication-factor 1 --topic trades
```

The main code dependency is for Kafka 2.4.0. Using other recent versions of Kafka
has been successful, but this is not exactly guaranteed or adviseable.

If your set-up has to be different for ports, or the number of instances, just modify the
command scripts in the obvious places.

## Running -- Docker

9 scripts are provided to run the various modules as Docker containers.
In sequence:

1. [docker-zookeeper.sh](./src/main/scripts/docker-zookeeper.sh)
2. [docker-kafka0.sh](./src/main/scripts/docker-kafka0.sh)
3. [docker-kafka1.sh](./src/main/scripts/docker-kafka1.sh)
4. [docker-kafka2.sh](./src/main/scripts/docker-kafka2.sh)
5. [docker-topic-create.sh](./src/main/scripts/docker-topic-create.sh)
6. [docker-kafdrop.sh](./src/main/scripts/docker-kafdrop.sh)
7. [docker-trade-producer.sh](./src/main/scripts/docker-trade-producer.sh)
8. [docker-hazelcast-node.sh](./src/main/scripts/docker-hazelcast-node.sh)
9. [docker-webapp.sh](./src/main/scripts/docker-webapp.sh)
10. [docker-management-center.sh](./src/main/scripts/docker-management-center.sh)

You should wait for Zookeeper (1) to have started before starting the three Kafka brokers (2,3,4).

You should wait for Kafka brokers (2,3,4) before starting the container that creates the topic (5).

Kafdrop (6), the Trade Producer (7) and a Hazelcast node (8) can all be started in parallel once the topic exists.

The Web UI (9) is started last, once everything else is ready.

Once started, the `webapp` UI is available as http://localhost:8081/ and `kafdrop` as http://localhost:8083/.

If you chose to start the `management-center` it is on http://localhost:8080.

### Host network

To enable the Docker containers to find each other, a local Docker network named "_trade-monitor_" is created.

The container for Zookeeper takes the name "_zookeeper_" so that the Kafka brokers can refer to it by host name,
as this is simpler than determining the IP address at run time and passing it as an argument.

The containers for the Kafka brokers take the names "_kafka-broker0_", "_kafka-broker1_" and "_kafka-broker2_"
so that the `trade-producer`, `topic-create`, `kafdrop` and `hazelcast-node` modules can find them by host name.

Use the command `docker network inspect trade-monitor` if you really wish to see the details of this networking.

If you have multiple network cards on your host machine the scripts won't be able to deduce which one to use. In this case, reduce the network down to one or update the scripts to hardwire in the IP address to use for the host network.

## Running -- Kubernetes

4 deployment files are provided to run the Trade Monitor in Kubernetes.

1. [kubernetes-zookeeper-kafka.yaml](./src/main/scripts/kubernetes-zookeeper-kafka.yaml)
2. [kubernetes-trade-producer.yaml](./src/main/scripts/kubernetes-trade-producer.yaml)
3. [kubernetes-hazelcast-node.yaml](./src/main/scripts/kubernetes-hazelcast-node.yaml)
4. [kubernetes-webapp-and-monitoring.yaml](./src/main/scripts/kubernetes-webapp-and-monitoring.yaml)

These are deliberately simple Kubernetes deployment file. Resource limits, auto-scaling,
namespaces, etc could all be added to move towards production quality.
For each file, ensure all the created pods report as being healthy ("_1/1_" in the "_READY_" column)
before progressing to the next deployment.

The first will create a pod for Zookeeper, three pods for Kafka brokers, run a job to create the needed topic, and start a pod for Kafdrop.

The second creates a job pod to run the Trade Producer.

The third creates two pods for the Hazelcast cluster to process the trades. This is a stateful set,
you can vary the number of replicas after from 2 down to 1 or up to a larger number.

The last creates a pod for the web UI.

If all looks well, you should something like this listed for the default namespace:

```
$ kubectl get pods
NAME                                       READY   STATUS      RESTARTS   AGE
job-trade-monitor-topic-create-2v2q6               0/1     Completed   0          17m
job-trade-monitor-trade-producer-t2r2r             1/1     Running     0          9s
trade-monitor-grafana-b5bd4545b-7tc2b              1/1     Running     0          8m11s
trade-monitor-hazelcast-node-0                     1/1     Running     0          11m
trade-monitor-hazelcast-node-1                     1/1     Running     0          10m
trade-monitor-kafdrop-6c454cf9c4-pbqc4             1/1     Running     0          17m
trade-monitor-kafka-broker-0                       1/1     Running     0          17m
trade-monitor-kafka-broker-1                       1/1     Running     0          17m
trade-monitor-kafka-broker-2                       1/1     Running     0          16m
trade-monitor-management-center-66b8744ccd-4qlvv   1/1     Running     0          8m11s
trade-monitor-prometheus-74cc965f6d-4kwhh          1/1     Running     0          8m11s
trade-monitor-webapp-86c965c5b4-qvjkd              1/1     Running     0          8m11s
trade-monitor-zookeeper-596bcf6b7c-hhl4z           1/1     Running     0          17m
```

All nodes are at "_READY_" status "_1/1_" (exception the topic creation job which has completed).

Once all are running, use `kubectl get services` to find the location of `kafdrop` and `webapp`.

Finding the external IP address for these will depend on your flavor of Kubernetes.

For example, for this output from Google Kubernetes Engine:

```
$ kubectl get services | egrep 'trade-monitor|EXTERNAL'
NAME                                 TYPE           CLUSTER-IP      EXTERNAL-IP      PORT(S)          AGE
trade-monitor-kafdrop-service        LoadBalancer   10.104.4.224    35.205.164.151   8080:31257/TCP   8m45s
trade-monitor-kafka-broker-service   ClusterIP      None            <none>           9092/TCP         8m45s
trade-monitor-service                ClusterIP      None            <none>           5701/TCP         3m49s
trade-monitor-webapp                 LoadBalancer   10.104.1.250    35.205.91.17     8080:32760/TCP   111s
trade-monitor-zookeeper-service      ClusterIP      10.104.11.197   <none>           2181/TCP         8m45s
```

So here Kafdrop would be available as [http://75.205.164.151:8080/](https://www.youtube.com/watch?v=dQw4w9WgXcQ) and the WebApp as
[http://75.205.91.17:8080/](https://www.youtube.com/watch?v=dQw4w9WgXcQ).

### Tagging for Kubernetes

`mvn clean install -Prelease` builds the necessary images on your local machine. You need them in your Kubernetes instance so the `image:` and `imagePullPolicy:` tags in the YAML allow them to be found.

You can use `docker save` and `docker load` to export from where built and import to where needed.

Or `docker tag` and `docker push` if you have a direct connection to the remote repository.

## Running -- Lifecycle

Apart from `topic-create` which is one-off set-up, all the modules here are continuous rather than batch,
they are intended to run forever.

The Jet jobs are requesting the next unread message from the Kafka topic. If there isn't
one because the `trade-producer` has been paused, that's no different from the stock
market being closed so no trading or from Jet reading from Kafka faster than the
producer can produce.

## Running `remote-job-sub-1` from the command line

If you download Jet from [here](https://jet-start.sh/download), there is a utility
in the `bin` folder called `jet`.

Try this to begin with:

```
hazelcast-jet-4.5/bin/jet -t grid@123.456.789.0 list-jobs
```

The cluster name here is `grid` but you will need to substitue the IP address of one of
the member. Once successful, this should list the running jobs in the cluster,
`AggregateQuery` and `IngestTrades`.

Then do this to launch the additional job

```
hazelcast-jet-4.5/bin/jet -t grid@123.456.789.0 submit target/trade-monitor-remote-job-sub-1-5.0.jar
```

This will send the job from your machine to wherever in the world the cluster
is, so long as it can connect. It may take a few seconds to stream all the
job content.

You can then use the `list-jobs` command again to see three jobs running, and
look at the map output in the `python_sentiment` map on Management Center.

### Extra step for Kubenetes.

By default Kubernetes will not expose your cluster to the outside world,
so you will not be able to connect to it from your desktop.

The additional script `src/main/scripts/kubernetes-hazelcast-node-extra.yaml`
allows access from your desktop to the cluster.

You need to use your Kubernetes console to determine the IP address
of the Load Balancer.

## Running -- Expected Output

Trade Monitor uses web UIs to present the data, but it is also logged.

### `webapp`

For the `webapp` a browser will show something similar to the below:

![Image of the Trade Monitor aggregation view of symbols][Screenshot1]

Here the symbol "ASPS" is temporarily shown in green to reflect a recent price rise,
and "ABCO" in red for a recent price fall.

For "ASPS", clicking on the expand button to it's left (the "^gt;") gives more detail:

![Image of the Trade Monitor expanded view of "ASPS" symbol][Screenshot2]

This now shows all trades for "ASPS", with their individual trade UUID, individual
price and quantity. Collectively there trades sum to the aggregation value.

### `kafdrop`

The `webapp` module displays the output of the Trade Monitor, which is what we
are really interested in.

If the optional module `kafdrop` is run, you can see the raw input, the contents
of the Kafka topic.

![Image of the Kafdrop browsing the "trades" topic][Screenshot3]

### Logs

Trades are produced at a high rate, so logging is configured to log only periodically, the first item, then
every 100,000th after that.

`trade-producer` produces random trades, which appear in the logs like this.

```
14:41:55.933 INFO  main c.h.p.d.b.trademonitor.ApplicationRunner - Wrote 0 => "{"id": "eca117f8-c44e-47d2-aad4-b0d358769456","timestamp": 1583934115727,"symbol": "ENTG","price": 2501,"quantity": 99}" 
```

`hazelcast-node` has two Jet jobs running, each with a [Logger Sink](https://docs.hazelcast.org/docs/jet/4.4/javadoc/com/hazelcast/jet/pipeline/Sinks.html#logger--). 

There will be output from the `IngestTrades` job, which is an upload, so the format is the same as the `trade-producer`.

```
14:42:40.090 INFO  hz.wonderful_goldwasser.jet.blocking.thread-4 c.h.j.i.c.W.IngestTrades/loggerSink#0 - [192.168.0.125]:5701 [trade-monitor] [4.0] 846b4539-e4de-4545-a3cd-9976c1fa0a9f={"id": "846b4539-e4de-4545-a3cd-9976c1fa0a9f","timestamp": 1583934115940,"symbol": "AMSF","price": 2501,"quantity": 378} 
```

And there will be output from the `AggregateQuery` job. As below, the stock symbol is "_WPCS_" and for this a trio of numbers are produced "_(1, 702219, 2499)_" (which means 1 trade for that symbol, volume is 702,219, price $24.99).

```
14:42:40.354 INFO  hz.wonderful_goldwasser.jet.blocking.thread-9 c.h.j.i.c.W.AggregateQuery/loggerSink#0 - [192.168.0.125]:5701 [trade-monitor] [4.0] WPCS=(1, 702219, 2499) 
```

Finally, the `webapp` also produces logs, of trade changes it is listening to. Again, this is the raw trade not the
aggregate, so the format is the same as for `trade-producer` log and the `IngestTrades` Jet job log.

```
14:43:11.181 INFO  trade-monitor-webapp.event-2 c.h.p.d.b.trademonitor.TradesMapListener - Received 1 => "{"id": "5af8cf39-4db7-4663-8b31-1169dd9398ca","timestamp": 1583934191165,"symbol": "MTGE","price": 2501,"quantity": 5729}" 
```

## Summary

This is an example showing real-time aggregation of data from a Kafka topic, giving a live
view of what is happening to an end-user available via a web front-end.

It happens here this is stock market trade data, but in some sense this is just a detail.

The important part is the raw data (the trades) and the aggregated data (the running totals) are maintained
in parallel, so the web front-end can show derived data that is consistent with the raw data. The high
level view is in step with the low level view.

To give some numbers, 3 hosts with 16 CPUs each can process 10,000 items per second, or replay 10,000,000 items
in 20 seconds. If your messages arrive at twice the rate, just have twice the number of hosts.
