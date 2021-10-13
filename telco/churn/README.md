# Hazelcast Platform Demo Applications - Telco - Churn Prediction

This example shows continuous updates of customer churn prections, in an environment with multiple
legacy integration points.

You will need a Enterprise license to run it.

Register [here](https://hazelcast.com/contact/) to request the evaluation license keys you
need, and put them in your `settings.xml` file as described in [Repository top-level README.md](../../README.md).
Be sure to mention this is for "Telco - Churn Predication" example so you get a license with the correct capabilities.

# What is churn ?

"Churn" is the turnover of a customers in a business, when an existing customer stops
becoming a customer.

While a leaving customer can always be replaced with a new customer, this isn't a
good strategy. It is typically claimed as costing 10 times as much to attract a
new customer as to retain an old customer. Plus the supply of customers is finite,
you can run out of people to become new customers whereas those who left may not
be keen to return.

So what this demo is about is identifying customers who might churn, so that
we can do something about it.

# Technologies in the demo

This demo features a range of popular technologies in addition to Hazelcast.

Messaging - [Apache Kafka](https://kafka.apache.org/)
and for this version, [Apache Zookeeper](https://zookeeper.apache.org/)

SQL database - [MySql](https://www.mysql.com/)

NoSQL database - [Apache Cassandra](https://cassandra.apache.org/)

NoSql database - [MongoDB](https://www.mongodb.com/)

Charting - [Grafana](https://grafana.com/)

# What does this demo do ?

This demo is about two main things, CDC and ML/AI, desribed in detail in the following sections.

## CDC (Change Data Capture)

CDC is Change Data Capture, the process of propating changes made to one copy of data to another
copy of data held in a different technology.

## ML/AI

ML/AI is Machine Learning/Artificial Intelligence, the process of developing logic that can be
trained with existing input and output data to be able to predict the output for unseen input.

# Understanding CDC (Change Data Capture)

In this demo, data is loaded into Hazelcast from Cassandra, MongoDB and MySql, and may be
amended in any of these four places.

For instance, this means a data record exists in Cassandra and the same record is held
in Hazelcast. When one copy of the data record is amended, we use CDC to ensure the same
amentment is applied to the other copy of the data record, bringing them into line.

This is CDC, and it is important to note this is both asynchronous and two-way.

It is asynchronous, a change made to one copy is not immediately made to the other copy.
Temporarily the two copies will deviate, perhaps only for a few milliseconds but it could
be much longer.

It is two-way, changes made be made to both copies independently, and if this occurs
at the same time, some way is required to merge the two together in a sensible way.
Rejection is not really possible, given the asynchronous nature of the updates, when
one system is aware of a data change that it would like to reject it has already been
committed in the other system.

## CDC *from* Hazelcast

Hazelcast loads data at start-up from Cassandra, Mongo and MySql into distributed maps.
As configured, all data found is loaded into Hazelcast, not a subset.

The same mechanism is used for all three, a [MapStore](https://docs.hazelcast.org/docs/4.1.1/manual/html-single/index.html#loading-and-storing-persistent-data)

When data is changed in Hazelcast's map, the MapStore's `store(K,V)` method is called
to save the same change to the place it was loaded from. This can be configured to happen
immediately the data changes in Hazelcast or on a deferred basis.

So this is essentially a Change Data Capture process. Change is captured by Hazelcast
and passed to user provided code to ensure it is applied to the other store.

In this demo, Hazelcast's copy of Cassandra and Mongo data *can* be changed and so
needs saved back. The data loaded from MySql isn't appropriate to amend in Hazelcast,
so saving this back isn't required. It would be trivial to add.

So, CDC exists for call data records in Hazelcast as these are persisted in Cassandra,
and for customer records in Hazelcase as these are persisted in Mongo. CDC isn't
required for tariff records in Hazelcast as these aren't amended so don't need saved
to MySql. CDC isn't appropriate for sentiment records in Hazelcast as these are not
stored elsewhere, always derived from scratch on a reload.

## CDC *from* Cassandra

Call records are persisted in Cassandra. A call was made from one telephone number
to another, at a certain data, for a certain duration. 

These are loaded to Cassandra by the `preload-legacy` module, and updated subsequently
by the `update-legacy` module.

Call records would largely be thought of as historical and immutable, but certain
fields may be valid to update afterwards. For example, the call would usually be
billable but we may wish to change this to a free call as a way to credit an
otherwise disappointed customer.

### Debezium

CDC is implemented using Debezium. Cassandra saves log records to disk periodically
for changed tables, a Debezium process on the same host reads these files and sends the changed
records to a Kafka topic.

### Hazelcast Jet

Jet reads from the Kafka topic written by Debezium, so duplicating these changes
into Hazelcast's memory.

## CDC from* MongoDB 

Customer records are persisted in MongoDB. A customer has a first and last name,
plus other fields such as private notes recorded by call center staff.

Again, these are loaded to Mongo by `preload-legacy` module and updated subsequently
by the `update-legacy` module.

### Debezium

CDC is again implemented using Debezium, but this time using Kafka Conneect.
Kafka Connect runs as a separate process, on a different host from Mongo,
and connects to Mongo to retrieve changed records from Mongo's internal
buffers and publishes these to a Kafka topic.

### Hazelcast Jet

Again, Hazelcast Jet reads the Kafka topic written by Debezium, and applies the
same changes into Hazelcast's memory.

## CDC *from* MySql

Tariff records are persisted in MySql. The tariff dictates the price paid for
domestic and international calls.

These are loaded to MySql by `preload-legacy` and can be updated subsequently
by running the `update-legacy` module.

Existing tariffs would realistically not change, only new ones added and
very infrequently. However, they are changed here to demonstrate how it
can be done.

### Debezium & Hazelcast Jet

Hazelcast Jet has a a Debezium source that directly connects to the MySql
instance to read change records. This is a simpler approach than the above,
there is no need for Debezium to run separately or to use a Kafka topic
to stage the records.

## Avoiding CDC loops

Data changed in Hazelcast is duplicated into Cassandra (or Mongo). Data changed in
Cassandra, Mongo or MySql is duplicated into Hazelcast.

So, in this demo, it is possible for data to change in Cassandra, have Debezium send it
to Hazelcast, have Hazelcast send it to Cassandra, have Debezium send it to Hazelcast,
have Hazelcast sent it to Cassandra, and so on in an infinite loop.

In practice, this does not occur becuase of data provenance filtering.

A data record changed in Cassandra is tagged as changed in Cassandra. It is applied
to Hazelcast's copy, but excluded from saving again to Cassandra by Hazelcast's
map store.

A data record changed in Hazelcast is tagged as changed in Hazelcast. If seen when
reading from Debezium, it is excluded from applying again to Hazelcast.

## Handling CDC double-updates

Double-updates are not possible in this demo. The `data-feed` module results in
*inserts* of _new_ call records to save to Cassandra, whereas the `update-legacy`
module results in *updates* to _existing_ call records. So no individual data
record for a call is actually updated in two places concurrently.

However, it is clearly possible in principle. A customer could have their
date-of-birth amended in Mongo and their address amended in Hazelcast.
This requires merging the changes together on a field by field basis.

Even then, some changes may not be logical. If a customer has their first
name changed on one system and their last name changed on another system,
it's two fields so the changes could be merged. But if both parts of the
name are amended, is it even the same customer ?

# Understanding the Machine Learning / Artificial Intelligence module

The core of the demo is that customer activity (a call record) along with their
previous state (a sentiment record) is fed into an ML/AI module in Python to determine
their new state (also a sentiment record).

There are three key parts to this:

## Live Assessment

Customer activity is assessed live. We are aware of events impacting the customer
almost as soon as it happens.

By _live_ what we mean is nearly immediately, as soon as the call record is
passed from whatever system provides it it gets passed to ML/AI. It can't be
true real-time, but is our best approximation and way better than an overnight
batch.

For example, in a cellphone call the recipient may be in a poor coverage area
(as determined by the mast ID on the call record) resulting in the call failing
at the outset or midway. Naturally the caller might try the call again, and perhaps
suffer a series of failed calls. The caller would be unlikely to know where the
recipient would be located, and even less likely to know if this is a poor
coverage area.

What the caller would experience is a series of a dropped calls, which would
make them dissatisfied with the service.

An overnight batch could identify this, but any attempts to appease the customer
would have followed all the failures. The customer would have made a series of call
attempts with an expectation of success that was not met.

Live alerting could trigger live investigation to communicate the reason
to the customer (poor network coverage for the recipient) while they are
still attempting their calls. So from the customers perspective, their
expectations are lowered and hence their likelihood for disattisfaction
with the service provided is lowered also.

## Externalised State

The ML/AI is pre-trained, non-evolving and stateless.

This allows scaling. Call volumes fluctuate through the day and from day
to day. The number of ML/AI modules operating in parallel on call data
can be scaled up and down to make efficient use of hardware.

Being pre-trained and non-evolving, a new ML/AI module clone brought
on-stream as a result of scale up would give consistent results compared
to one that had been running for some time.

Equally the stateless aspect would allow the same data to be fed into
two versions of the ML/AI logic. This would enable a new version of
the logic to be deployed alongside the old version, so both versions
run concurrently meaning no loss of service. If the new version is
determined to be better, the old version can be retired, or vice versa.

Essentially this would double the number of ML/AI modules deployed,
requiring a scale-up and would also require a way to distinguish
the output of old from new in determining if the new version is
really better.

## ML/AI model in Python

In the real world, the ML/AI logic would be substantial in size,
and take many days or weeks to train using historical call data
records and customer outcomes.

In this demo, the purpose is to show that such a model can be
deployed and executed, so a Python module is coded
[here](https://github.com/hazelcast/hazelcast-platform-demos/tree/master/telco/churn/jet-jobs/src/main/resources/python) that does this determination. The logic is a stub,
using a random number generator to adjust sentiment for dropped
calls.

# Understanding the rest of the demo

There are many modules in the demo, but it demonstrates the realistic complexities
of integrating multiple components.

The modules are, alphabetically.

## `cassandra`

This module runs a single unclustered Cassandra database instance, and also on the
same host a Debezium process to feed Cassandra CDC records to Kafka.

## `data-feed`

The `data-feed` module creates new call data records for the existing customers,
at the rate of 1 per second, to simulate live customer activity.

## `hazelcast-node`

The `hazelcast-node` module is a node in the Hazelcast cluster. One or more of
these can be started to form a cluster of the corresponding size.

Once the configured expected size is reached, data is loaded into Hazelcast
memory from Cassandra, Mongo and MySql. An expected size is needed to be sure
the Hazelcast cluster is big enough to store that data that will be loaded.

## `grafana`

This creates a Grafana system for live charting, connecting to Prometheus
as the source of it's data, which in turn retrieves this from Hazelcast's
Management Center.

## `java-client`

The `java-client` module mainly hosts a ReactJS browser application using
web sockets. This allows data in Hazelcast to be queried using SQL,
and shows alerts produced for customers at risk of churn.

Additionally, as this client starts, it runs some basic querying of data
in the Hazelcast cluster to confirm this is useable.

## `jet-jobs`

The `jet-jobs` module is a Hazelcast client that connects to the Hazelcast cluster
to submit the various different Jet jobs -- for reading from Kafka, CDC from MySql,
CDC from Cassandra and CDC from Mongo.

## `kafdrop`

[Kafdrop](https://github.com/obsidiandynamics/kafdrop) is a web UI that
allows confirmation of what data is in the Kafka topics in the demo.

It is not necessary for the operation of the demo, it is there just to
show data passing between systems.

## `kafka-connect`

The `kafka-connect` module is needed for CDC from Mongo.

While the Debezium process publishing CDC data for Cassandra is part of the Cassandra
image, the Debezium process for Mongo is run in a separate host.

Debezium for Cassandra reads files, so must run on the Cassandra host. Debezium
for Mongo connects to the Mongo, so is regarded as a Mongo client, not run on the
Mongo host.

## `kafka`

The `kafka` module ceates a single Kafka Broker. The demo uses a Kafka cluster of 3 such brokers,
that connect to `zookeeper` for centralised congifuration.

## `management-center`

The `management-center` module is Hazelcast's Management Center. It allows you to monitor
the health of the Hazelcast cluster, and to run SQL commands, amongst other things.

You can run the demo without it, but since `management-center` feeds data to `prometheus`
which in turn feeds to `grafana`, if you omit one, omit all three.

## `mongo`

This module creates a singleton cluster of MongoDB.

Only one MongoDB process will run, but this is a cluster of size of 1 rather than a standalone Mongo.
Mongo clustering is required for CDC.

## `mysql`

This module creates a single MySql database.

## `preload-legacy`

The `preload-legacy` module will create test data in Cassandra, Mongo and MySql
as a one-off process to run once these datastores are available.

## `prometheus`

This creates a Prometheus database.

Prometheus captures and stores data from Hazelcast's Management Center, and is used
as a source of data for charts in Grafana.

## `slack-integration`

The demo can produce alerts to the [Slack](https://slack.com/intl/en-gb/) application.

This is an optional module that submits a Jet job that will also republish the churn
alerts to the Slack application.

It is optional, as you may not have Slack or wish to use it for alerting.

## `topic-create`

The `topic-create` module creates topics in Kafka prior to their use by `data-feed`
and `kafka-connect`.

Kafka topics are created on demand if needed, but this module is used if non-default
configuration is required.

## `update-legacy`

The `update-legacy` moodule will update the test data in Cassandra, Mongo and MySql,
to generate CDC records.

Some records are updated more than once to ensure that sufficient volume of changes
exist to trigger CDC output.

## `zookeeper`

This creates a single `zookeeper` module, as the version of Kafka use relies on
Zookeeper for centralised configuration and so the Kafka brokers can find each other
to form a Kafka cluster.

# Running the demo

Build the example using the `release` profile:

```
mvn clean install -Prelease
```

The release profile both compiles the code, and converts them into Docker images for
running in Docker or Kubernetes. It is not possible to run the demo on localhost directly.

In [src/main/scripts](https://github.com/hazelcast/hazelcast-platform-demos/tree/master/telco/churn/src/main/scripts)
there are the necessary scripts to run the demo in Docker and the YAML files
to run in Kubernetes.

Although the Docker version omits a few optional modules to keep resource usage down,
you'll still need a powerful machine.

*Remember*, this is a non-interactive demo. Modules will publish alerts when likely
customer churn is detected based on generated data records. The client module subscribes
to this publishing topic, and will see the alert, if the client is started before the
alerts come out. If you or your machine is slow to create the client, it may be all
alerts have been produced by the time the client subscribes to such notifications.

## Kubernetes

There are 5 Kubernetes YAML scripts to run in order. You may need to adjust the
image repository name to match your instance.

Run the first YAML, then once it done run the second, then the third, then
you can do the fourth and fifth together.

Some of the YAML files start jobs in a staggered fashion, you need to wait for
them all to be shown as "_Running_" or "_Completed_" to progress to the next YAML file.

### `kubernetes-1-west.yaml`

The first script creates pods for the following 5 images.

```
image: "hazelcast-platform-demos/churn-zookeeper"
image: "hazelcast-platform-demos/churn-kafka-broker"
image: "hazelcast-platform-demos/churn-kafdrop"
image: "hazelcast-platform-demos/churn-topic-create"
image: "hazelcast-platform-demos/churn-data-feed"
```

Zookeeper is first, a central configuration for this version of Kafka. Here a single deploymenty is used, not
robust enough for production use, but this not the point of this demo.

3 instances of the Kafka Brokers are started, as a "_StatefulSet_". Other values could be used, but note the code
assumes three when calculating the partition identifier for the Kafka topic.

The `topic-create` job creates topics, such as "`_calls_`", that the demo needs and to ensure they have the correct
configuration. Other topics, such as "`__consumer_offsets`" can be created on demand, as a specific configuration
is not needed.

The last item is `data-feed`, which provides the simulated input feed of call records coming in from the outside
world.

A staggered start is used, to ensure the pods are created in the correct sequence where one such as Kafka Broker
depends on another such as Zookeeper.

### `kubernetes-2-south.yaml`

The second script creates pods for these 6 images.

```
image: "hazelcast-platform-demos/churn-cassandra"
image: "hazelcast-platform-demos/churn-kafka-connect"
image: "hazelcast-platform-demos/churn-mongo"
image: "hazelcast-platform-demos/churn-mysql"
image: "hazelcast-platform-demos/churn-preload-legacy"
image: "hazelcast-platform-demos/churn-update-legacy"
```

Three are databases. `Cassandra` is a single Cassandra database, `Mongo` is a Mongo cluster (with one instance)
and `MySql` is a single MySql database.

The `Kafka Connect` module starts a Debezium connector to read from Mongo and write to a Kafka topic. This
approach is not needed for Cassandra as it's Debezium connector is part of the same pod, but still writes
to Kafka. This approach is not needed for MySql, as it's Debezium connector is part of Hazelcast.

As the name implies, the `Preload Legacy` module creates the data in the legacy databases, Cassandra,
Mongo and MySql, before the main part of the demonstration starts.

The `Update Legacy` job runs later, to apply changes to the data in the legacy databases after the
system is live. You don't need to wait for this job to start or finish before starting Hazelcast
with the next script.

### `kubernetes-3-hazelcast.yaml`

The third script only uses 1 image.

```
image: "hazelcast-platform-demos/churn-hazelcast-node"
```

This creates a "_StatefulSet_" of 2 Hazelcast pods, which will join together to form a cluster.
More or less pods could consistute the stateful set if you prefer.

Hazelcast is configured to load a subset of data from the three legacy databases into memory once
the cluster reaches the expect size of 2.

For Cassandra and Mongo, the subset is 100%, so actually all their data is loaded.
For MySql only 50% of the data is loaded.

### `kubernetes-4-east.yaml`

The fourth script uses 5 of the images.

```
image: "hazelcast-platform-demos/churn-prometheus"
image: "hazelcast-platform-demos/churn-grafana"
image: "hazelcast-platform-demos/churn-management-center"
image: "hazelcast-platform-demos/churn-jet-jobs"
image: "hazelcast-platform-demos/churn-slack-integration"
```

`Management Center` is the Hazelcast Management Center. It is pre-configured with the connection
details for the Hazelcast cluster started by `kubernetes-3-south.yaml` script. The Management
Center will run successfully if that cluster is not available, but since that is of limited
use, the script ordering here ensures the cluster is ready first.

`Prometheus` is a database that collects values from the Hazelcast Management Center. Although
it has a web UI, it is not exposed here.

`Grafana` is popular dashboard tool. It is configured here with a dashboard presenting Hazelcast
information, collected from Prometheus which in turn collects from Hazelcast Management Center.

The logons/passwords for the Hazelcast Management Center &amp; Grafana are set in the properties section of
[pom.xml](https://github.com/hazelcast/hazelcast-platform-demos/blob/master/telco/churn/pom.xml).

`Jet Jobs` submits jobs to the Hazelcast cluster for data ingestion from Kafka topics taking the
data feed of calls from "outside world", CDC changes from Kafka topics for Cassandra and Mongo,
and a direct CDC feed of changes from MySql.

`Slack Integration` is option, and makes the Slack chat tool an extra destination for alerts.
If you have Slack, alerts from the demo will appear on the Slack conversation channel of your
choice.

### `kubernetes-5-north.yaml`

The last script only uses 1 image.

```
image: "hazelcast-platform-demos/churn-java-client"
```

The last script starts a Hazelcast web client, connecting to the Hazelcast grid.
You can start this when the grid is ready (`kubernetes-3-hazelcast.yaml`), so can
do this in parallel with running `kubernetes-4-east.yaml`.

The image is `churn-java-client`, and this is accessed via a _LoadBalancer_ (although there is only
one web client).

The browser creates a ReactJS page that updates as alerts are received, at the bottom of the
page in reverse order. So the newest alert is at the top of the HTML table.

Also in this window is a refreshing panel with the size of the Hazelcast maps. You can use
this to watch the map sizes increase as the data is loaded, or you can do the same from
the Management Center.

At the top is a window that allows you to submit SQL. Try `SELECT * FROM sentiment` to
query the "_sentiment_" map in the Hazelcast cluster.

## Docker

There are Docker equivalents for the Kubernetes pods above. For simplicity, some of
the pods aren't used, but even so a powerful Docker set-up will be needed.

The scripts to run, in this order, are:

- `docker-zookeeper.sh`
- `docker-kafka0.sh`
- `docker-kafka1.sh`
- `docker-kafka2.sh`
- `docker-topic-create.sh`
- `docker-cassandra.sh`
- `docker-mongo.sh`
- `docker-mysql.sh`
- `docker-kafka-connect.sh`
- `docker-preload-legacy.sh`
- `docker-hazelcast-node1.sh`
- `docker-jet-jobs.sh`
- `docker-slack-integration.sh`
- `docker-java-client.sh`
- `docker-update-legacy.sh`

The images invoked, and therefore their function in the demo, are the exact same as the Kubernetes
equivalents.

It should mostly be obvious when the Docker image is ready for us, although for `Cassandra` you
should allow 60 seconds for it to become fully available.

# Optional - "Slack" Integration

[Slack](https://slack.com) is a team messaging application, which supports one-to-one
messaging from team member to another team member, and one-to-many message from a team
member to a group of team members on a selected topic.

In the demo, if you submit the job in the `slack-integration` module, alerts generated
for the identification of churn risk customers will be posted to your Slack installation.
So you can see alerts appearing in the desktop Slack application or on your smartphone
Slack application.

It is optional in the demo as you need a Slack installation and may not have one.

## If you do have Slack

You need to refer to Slack documentation here, as the instructions might vary
over time. Here is the very high level.

On `https://api.slack.com/apps` select "_Create New App_".

On "_App Home_" in the *Features* menu, create a bot token.

On "_OAuth & Permissions_" in the *Features* menu, create OAuth tokens for the
later use.

Then use "_Install App_" in the *Settings* menu to add the new application to
your workspace. 

Finally, on the channel(s) you wish to post messages to, select "_Details_" and
then "_Add apps_" to add your application as a channel member.

### `settings.xml`

Slack will give you an OAuth access token, which goes in your Maven `settings.xml`
file.

```
	<my.slack.bot.user.oath.access.token>GOES HERE</my.slack.bot.user.oath.access.token>
	<my.slack.bot.channel.name>GOES HERE</my.slack.bot.channel.name>
	<my.slack.bot.channel.id>GOES HERE</my.slack.bot.channel.id>
```

The channel name is fairly obvious.

The channel ID is less visible. Using the desktop or browser version, select "_copy link_"
on the channel itself in the left bar, paste it into a text editor, and the channel ID
is the last part of the URL.

## If you don't have Slack

The Slack part of the application is optional, you don't need to run it.

The code that is used to post a message to Slack is just a REST call,
that does a HTTP POST to a specific URL with a JSON string as a payload.
The code is [here](https://github.com/hazelcast/hazelcast-platform-demos/blob/master/telco/churn/slack-integration/src/main/java/com/hazelcast/platform/demos/telco/churn/MySlackSink.java).

If you don't have Slack but have something similar, you can always try extending
the application to integrate in a similar way.

# Notes - Cassandra 3.11.4 and Debezium 1.4

CDC for Cassandra is based on Debezium 1.4 which is valid with Cassandra 3.11.4.

Cassandra 3 flushes the CDC log file to disk on a size threshold, not
a time threshold. See [CASSANDRA-12148](https://issues.apache.org/jira/browse/CASSANDRA-12148)
where this is a feature added in Cassandra 4.

This line in the code [custom-entrypoint.sh](https://github.com/hazelcast/hazelcast-platform-demos/blob/master/telco/churn/cassandra/src/main/resources/custom-entrypoint.sh#L11) configures the log file size
down from default 32MB to 1MB to make the output more frequent.
The `update-legacy` module updates Cassandra data in a loop to ensure enough changes
are made to trigger the output of the CDC log.

# Notes - Security

The Hazelcast cluster in this demo is secured, using Hazelcast's
Enterprise Security.

The first server to start forms a cluster. Other servers must pass correct credentials
to be allowed to join this cluster, and so in turn receive a share of the data to host.

Client applications (`management-center`, `java-client`, `jet-jobs` & `slack-integration`)
must also pass valid credentials to be able connect to this cluster, but being
clients they do not host data. 

Instead, clients send and recieve data from the cluster, and this is subject to
additional role-based authentication controls.

For example, the `java-client` module uses the "_churn-java-client_" security principal.
This allows it to read data in the "_customer_"
map but not change it, and allows it to insert, read, update and delete data in
the "_sentiment_" map.

# Summary

This demo shows the integration of complex systems with multiple
moving parts in multiple technologies, that runs in a largely non-interactive
manner.

Existing storage solutions are shown using the sample applications Cassandra,
MongoDB and MySql. Change Data Capture (CDC) demonstrates how data held both
in these systems and in Hazelcast can be updated in one and have the updates
propogate to the other, keeping the dual copies eventually consistent.

ML/AI is deployed to actively assess customer activity as it occurs, to
give a prioritized view of which customers are most likely to abandon
their service. This presumptive determination of abandonment allows for
corrective action to stop loss of customers, and prioritization allows
this to be done in a cost-effective manner.

Data is Hazelcast is secured using a role-based mechanism. Different parts
of the system are allowed to view and amend selected parts of the data,
and not view or amend other parts.

Fundamentally, Hazelcast here acts as a central integration point, storing
key data in memory for fast access and analysis, without the need to
immediately or ever discard existing disk-backed storage technologies.
