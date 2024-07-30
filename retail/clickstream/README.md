# Hazelcast Platform Demo Applications - Retail - Clickstream Analysis

This is a cloud-based demo.

It has too many moving parts to run on a laptop, although remote job submission can be run from your laptop.

## Description

This is a demonstration of online and offline ML analysis of a "clickstream", a stream of
button clicks.

In E-commerce, a website can track user behaviour, the buttons the user clicks on and the
items they may hover their mouse over as part of their navigation during the familiar
"_add to basket_" and "_proceed to checkout_" phases of online shopping.

These behavioural events occur on the users browser, won't happen all at once, and may
happen more than once (eg. "_review basket_"). If these events are sent to the E-commerce
company, they can be analysed to derive insights about that customer and customers in
general.

In this example, ML/AI analyses the clickstream in two ways, online and offline.

What we do for "online" is live prediction of the user behaviour. As the user is shopping,
an ML moodel can analyse their behaviour as it happens, to predict if they are likely to buy before
the reach the payment stage. This is clearly advantageous. If we can detect that a user
is on the borderline of buy or not-buy, we can perhaps offer a discount to make the first
outcome more likely.

What we do for "offline" is produce the model to use for "online", by analysing historical
data. If we can the clicks from purchases and from non-purchases, we produce an ML model
that understands the relative signficance of each kind of button click. Some buttons
indicate a user might buy, some that a user might not buy, and some are irrelevant.

Consider for example "_sort basket_". It would seem reasonable to assume that if the
user is reviewing the basket in this way, they are looking for an item to remove as
the total cost is too high. It would seem another reasonable assumption that this behaviour suggests the
user is looking for an item to remove to bring the cost under a certain threshold.
And from this we might conclude they are going to spend, so a discount might not
be needed. Historical analysis would confirm are this assumptions are true.
true. 

### Digital Twin

As part of each user's shopping, we receive a stream of clicks of various types,
see the list [here](https://github.com/hazelcast/hazelcast-platform-demos/blob/master/retail/clickstream/common/src/main/java/com/hazelcast/platform/demos/retail/clickstream/CsvField.java).

These arrive as they happen, but Hazelcast accumulates these for each user, to form
a "_Digital Twin_", the electronic impression we have build up for the user.

### Checkout

The key action for E-commerce is the "_checkout_" button. When the user clicks on this button,
for the most part they have completed selection and are moving to payment.

Note the "_checkout_" button is the click to *go* to the checkout page. We have to then
render this page and show it to them in the usual way.

So, we need to decide if we are offering a discount as this page is presented to the user.
For MVC style applications this can be as the view is formed, for reactive style a
discount can be injected. Either way the discount needs to be available almost instantly,
so the page can show the discount factored into the first price the user sees.

It will obviously be more successful to offer the discount at the first viewing of the
checkout page than a follow-up discount email hours later, but the latter remains a
valid fallback.

## Retraining cycle

In this example, two cluster are used to segregate workload. Model training occurs
in the "_green_" Hazelcast cluster.

Model training consists of two activities that repeat.

The first activity is training. This takes a block of input, the digital twin and whether a purchase
resulted, and passes this into (Python) ML. The output of this is a trained model.

The second activity is validation. This takes the next block of input, passes the digital twin
into the ML model which produces a prediction, then compares the prediction against whether a
purchase resulted. This gives an accuracy score for this newly produced model.

Repeating these activities give a series of models, and if we would obviously use the one
with the highest accuracy.

Periodic retraining is necessary as user behaviour may change over time. For example, if we release
a new release of the website, the buttons may be moved on the page. This in turns changes the
relative significance of which ones are clicked and how this correlates to purchasing.

Doing it this way means the retraining is automatic.

Retraining is essentially an "offline" activity. There is usually no urgency to it, so little
need to scale the "_green_" cluster if clickstream volumes increase, as training uses the
same volume of input to produce a model and can ignore excess.

## Live updating

In this example, two cluster are used to segregate workload. Model execution occurs
in the "_blue_" Hazelcast cluster.

What this means in reality is multiple Hazelcast nodes form the "_blue_" cluster, and
the clickstream is split using the browser identifier across the available nodes.
Each browser's behaviour can be assessed independently.

The configuration for "_blue_" is for a 5 node cluster, and this will support a
certain transaction volume. If this could, for example, deal with 100,000 decisions
per second and you temporarily need 50% because of some busy period such as Black
Friday, just add 50% more nodes (3!). Workload scales linearly, so you just scale
up and down to the level you need.

What "_blue_" is really doing is streaming digital twin details into an ML model
when the user requests the checkout page. If "_green_" produces a better ML model,
this is passed to "_blue_" which updates the running model.

So in essence, "_green_" will make it known if a better model is available, and
"_blue_" will upload it without pausing prediction processing. Hence the "_blue_"
cluster always runs the best prediction available.

### Latency and Accuracy

Two key metrics are captured by the "_blue_" cluster.

*Latency* measures the time between the user requesting the "_checkout_"
page and the prediction being made. It's important to produce the prediction
quickly. If this is slowing down because of increased input volume, it's an
indication to scale up the "_blue_" cluster.

*Accuracy* measures the actual accuracy of the prediction, as a check-and-balance
against the validation stage of training. Purchases are compared against predictions.
If this accuracy deviates from the predicted accuracy, it's an indication user
behaviour has changed in a way that model training hasn't observed.

## Building

From the top-level, run the following:

```
mvn clean install -Prelease
```

This will build all modules as Docker images, which can be deployed to Kubernetes.

## Modules

There are 12 modules in this example:

```
cassandra
common
grafana
hazelcast
job-decisiontree
job-gaussian
management-center
prometheus
pulsar
pulsar-feed
pulsar-manager
webapp
```

### 1. `cassandra`

A one-node Apache Cassandra cluster. This is used as a store for trained models.

### 2. `common`

Common code shared amongst the Java modules. It does not directly run.

### 3. `grafana`

A Grafana node that runs for monitoring the behaviour of the live prediction, the
latency and accuracy measurements referenced above.

### 4. `hazelcast`

This module is the processing for both the "_blue_" and "_green_" clusters. It is part of both, but
adjusts behaviour depending on the cluster name.

### 5. `job-decisiontree`

An optional module that can be submitted from the command line, to add "_Decision Tree_" ML
prediction, using a model prepared external to Hazelcast.

Data scientists may prefer to prepare the models manually, rather than use the automatic retraining cycle.

### 6. `job-gaussian`

Another optional module that can be submitted from the command line, to add "_Gaussian_" ML
prediction, using a model prepared external to Hazelcast.

Data scientists may prefer to prepare the models manually, rather than use the automatic retraining cycle.

### 7. `management-center`

Hazelcast's Management Center, to monitor the "_blue_" and "_green_" clusters.

All maps used are available for querying, for example `SELECT * FROM prediction LIMIT 50`.

### 8. `prometheus`

Prometheus is used to export statistics from Hazelcast's Management Center for presenting on Grafana.

### 9. `pulsar`

A one-node Apache Pulsar cluster. Pulsar is used as the mechanism to transmit clickstream events
from browsers to Hazelcast.

### 10. `pulsar-feed`

A job that writes clickstream events to Pulsar.

This uses downloaded data from [Kaggle](https://www.kaggle.com/). See
[pulsar-feed/src/main/resources](https://github.com/hazelcast/hazelcast-platform-demos/blob/master/retail/clickstream/pulsar-feed/src/main/resources/training_sample.csv) for the data used.

Although this is downloaded data, it is real data that is replayed in the demo over the course of about an hour.

### 11. `pulsar-manager`

Apache Pulsar's monitoring tool, for inspecting the Pulsar topic.

### 12. `webapp`

An optional web application, presenting some information about the running system. Much of this information is also available
on Hazelcast's Management Center.

This web application will by default connect to the "_blue_" cluster, and display some statistics about the data in blue,
the "_blue_" cluster connection information, and any alerts produced by the "_blue_" cluster.

Should the "_blue_" cluster go offline, this web application will automatically reconnect to the "_green_" cluster.

## Running - Kubernetes

Four scripts are provided for running in Kubernetes:

```
kubernetes-1.yaml
kubernetes-2-blue.yaml
kubernetes-2-green.yaml
kubernetes-3.yaml
```

These may need adjusting for the "_image:_" and "_imagePullPolicy:_" settings appropriate to your deployment.

Unsurprisingly, `kubernetes-1.yaml` should be run first. It creates the prerequisites for Hazelcast, the modules
`cassandra`, `grafana`, `management-center`, `prometheus` and `pulsar`. Strictly speaking, only really `cassandra`
needs to be there, but it preferable to create the other services that may be needed at this point.

The scripts `kubernetes-2-blue.yaml` and `kubernetes-2-green.yaml` scripts create the "_blue_" and "_green_" clusters.
You can start them in parallel or one after the other. The two clusters will join together using WAN replication once both are online.

Lastly, `kubernetes-3.yaml` should be started once the Hazelcast clusters are online. This mainly produces the
clickstream from `pulsar-feed`, but also starts `webapp` and `pulsar-manager` optional elements.

## Running - Command Line Job Submission

The script `kubernetes-2-blue.yaml` defines an external port to access the "_blue_" cluster from outside Kubernetes.
This will be available from the ingress information on your Kubernetes cluster.

Once you have this IP address, a command such as

```
hazelcast-enterprise-5.0/bin/hz-cli -t blue@1.2.3.4 list-jobs
```

will run the [hz-cli](https://download.hazelcast.com/download.jsp?version=hazelcast-5.0&p=) command line tool to list running jobs. This is a good way to check connectivity.
Note this uses *no security*, so it not suitable for production usage.

Then you can submit either or both of the Gaussian and Decision Tree ML predictions.

```
hazelcast-enterprise-6.0.0/bin/hz-cli -t blue@1.2.3.4 submit job-gaussian/target/clickstream-job-gaussian-6.0-jar-with-dependencies.jar
hazelcast-enterprise-6.0.0/bin/hz-cli -t blue@1.2.3.4 submit job-decisiontree/target/clickstream-job-decisiontree-6.0-jar-with-dependencies.jar

```
