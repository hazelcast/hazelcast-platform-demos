# Hazelcast Platform Demo Applications - Banking - Credit Value Adjustment

[Screenshot01]: src/site/markdown/images/screenshot01.png "Image screenshot01.png"

This example is "_straight-through processing_" of Risk, and uses commercial Hazelcast features.

You will need a Jet Enterprise license.

Register [here](https://hazelcast.com/download/) to request the evaluation license keys you
need, and put them in your `settings.xml` file as described in [Repository top-level README.md](../../README.md).
Be sure to mention this is for Credit Value Adjustment so you get a license with the correct capabilities.

## 3 billion intermediate results!

*NOTE* By default the Jet job in this example will invoke 3,000,000,000 pricing calculations from C++.

Make sure you have sufficient hardware if you're going to run the full volume.

## Description

This section is a very brief overview of Credit Value Adjustment, and the architecture for running high-speed "Risk" calculations. For more details, refer to <a href="_blank">TODO Link to website goes here once ready TODO</a>.

### Credit Value Adjustment (CVA)

The application here is doing credit value adjustment for interest rate swaps. Swaps that are in
profit have a credit value to record as an incoming cash flow. That credit value must be adjusted to factor in the
possibility that the other participant in the trade, the counterparty, may default and we won't receive some or all of
the expected credit value.

So for a trade, we need to run simulation scenarios to determine the potential profit for that trade. All trades
for a counterparty then need grouped together, since if the counterparty defaults all their trades with us will
be affected.

### Some numbers

The data for this project is in the `src/main/resources` folder for the `data-loader` module.

There are 600,000 single-currency interest-rate swap trades, 5000 interest rate curves, 20 counterparties and 1 set of fixing
dates in the input files. These are held in JSON format.

Each of the 600,000 trades is with one of the 20 counterparties, and is priced by C++ for each of the 5,000 curves, to
derive it's mark-to-market value in that scenario. Hence there are 3,000,000,000 of these (600,000 * 5,000) intermediate
results. The intermediate results are then aggregated to produce the risk for each counterparty.

The final result is a single array of 20 values, the risk for each of the 20 counterparties, aggregated from 3,000,000,000
intermediate calculations. 

## Straight-Through Processing

By default, the intermediate calculations are not saved.

This is the "_straight-through processing_". The billions of intermediate results are streamed through aggregators,
only the final aggregated results are saved. Although caches are fast, there is still a performance cost to saving
results which are only needed briefly.

When the job is initiated, an option is available to save the intermediate results for later review and verification.

## Limiting Execution Size

All trades are evaluated against all interest rate curves. To limit run-time for initial testing, the `data-loader`
module takes a parameter to limit the amount of data loaded, which reduces the number of combinations and therefore
reduces the run time.

## Building

To build, run:

```
mvn clean install -Prelease
```

The C++ pricer module requires a Docker image, and will take a substantial time to build on the first run,
perhaps as much as an hour. To be successful, you will need to ensure your Docker runtime has sufficient
capacity. Changing from the default 2GB to 8GB of memory and restarting the Docker daemon has been seen
to be necessary.

## Modules

This example creates two Hazelcast clusters, named "*site1*" and "*site2*" to demonstrate WAN replication
between sites for Disaster Recovery and other failovers. It is not necessary to run both to run the calculations.

### 1. `common`

The main item of interest in the `common` module is `src/main/proto/JetToCpp.proto`.

This is a Protobuf3 definition of the gRPC communications between Jet and C++.

### 2. `cva-cpp`

This module contains the C++ code that executes the pricing.

It has has a gRPC server that responds to incoming requests on a specific port (50001).

Each incoming request is one or usually more (default 100) lines of trades and curves
to price. One output is given for each input.

For normal gRPC use, this module would be accessed via  load balancer.
It is only available as a Docker image, as it is based on a customerized Ubuntu image.

### 3. `abstract-hazelcast-node`

This module contains code common the Hazelcast sewrvers in cluster "*site1*" and "*site2*".
It will set up some configuration based on whether it deduces it is running Kubernetes or outside.

In a containerized environment, Docker or Kubernetes, the Grafana job will be initiated if not
already running.

In a Kubernetes environment, WAN replication will be enabled via the custom code in
the `MyLocalWANDiscoveryStrategy.java` file. This will probe the Kubernetes DNS server and
determine if another cluster is running, and initiate a WAN connection to it.

### 4. `hazelcast-node-site1`

This module is the Hazelcast server for "*site1*".

It is built pre-set as "*site1*" rather than parameterised to demonstrate one build mechanism. The `webapp`
module demonstrates the alternative approach using parameterisation.

### 5. `hazelcast-node-site2`

This module is the Hazelcast server for "*site2*". 

### 6. `jet-jobs`

The Jet processing jobs for the CVA application are defined in their own module.

Job "_GrafanaGlobalMetricsJob_" sends some metrics to Grafana.

Job "_CvaStpJob_" is the main job in the CVA application, launching CVA for straight through processing.
It has 4 parameters:
* *calcDate* -
    This is the date for the calculations.
* *batchSize* -
    Calculations are passed by Jet to C++ in batches. Configuring the size here enables this to be optimised
    for the network capacity and C++ processing speed.
* *parallelism* -
    This controls how many C++ worker nodes each Jet node will communicate with concurrently, and again this
    is configurable to optimise for the deployment architecture.
* *debug* -
    Enabling this flag adds additional stages to the processing pipeline to save the intermediate results
    to maps with the naming prefix "debug_". These are sink stages, not inserted into the main pipeline
    log as intermediate stages.

### 7. `abstract-hazelcast-client`

This module is the common code for clients of the Hazelcast grids, and mainly just sets up the
configuration for connectivity.

### 8. `data-loader`

The `data-loader` is a Hazelcast client that connects to a Hazelcast cluster,
reads the JSON data in its `src/main/resources` folder and inserts this data
into maps with corresponding names. Once data is loaded it shuts down.

It takes two parameters, for example "_./localhost-data-loader.sh 100 site2_".

The first parameter is the count of the maximum number of records to upload from
each file. There are 600,000 trades but specifying 100 means only the first 100
are loaded.

The second parameter is the cluster to connect to, "*site1*" or "*site2*".

### 9. `management-center`

This module extends the Management Center's existing Docker image with preset
configuration to make it simpler to connect to this application's Hazelcast
clusters in Docker and Kubernetes environments.

For running on _localhost_, just download and use the Management Center from
[here](https://hazelcast.org/imdg/download/#hazelcast-imdg-management-center) and
configure manually. "*site1*" will be available on "*localhost:5701*" and "*site2*"
on "*localhost:6701*".

The build for this module pre-configures the license and the logon/password
for you. See [management-center/README.md](./management-center/README.md).

### 10. `grafana`

This module creates a Grafana image for Docker, with a special statistics panel
for the CVA application imported.

### 11. `webapp`

This module is a web front-end, the main user-experience for the CVA application.
Note CVA is compute heavy, so not particularly visual or interactive.

When started, it presents a single page with some information and actions.
All dynamic data automatically refreshes, so you don't need to refresh the page
to see updates.

In the top left a panel shows the fixing dates, and has a form that allows you
to submit the CVA job. The run date is locked, but you can select if debug
saving is enabled, the batch size and parallelism. Refer to the `jet-jobs`
module to see what these flags mean.

In the top middle is a panel showing the size of the most important maps for
the example. This information is also available in the Management Center.

In the top right a panel shows job output available for download. This is all
downloadable output, so includes output from jobs that ran in another site
and were shared by WAN replication. In other words, this is a superset of the
downloads in the last panel.

Across the bottle is a table showing the Jet jobs that have run or are running,
and their status. For CVA jobs that have completed successfully, a download link
enables you to get the results as a CSV file or Excel spreadsheet.

### 12. `prometheus`

This module creates a Prometheus image for Docker, pre-configured to connect
to the Management Center in Kubernetes environments only.

## Running -- sequence

There is a partial sequence to running, and some optional modules depending on the environment
and machine capacity.

* *grafana* &amp; *cva-cpp* -
These should be started first.
 *grafana* is only needed when using Docker or Kubernetes.
 *cva-cpp* is always needed.

* *hazelcast-node-site1* &amp; *hazelcast-node-site2* -
 These should be started second.
 Run as many of each as you want. If you don't wish to use WAN, one cluster is enough,
and *WAN* is only available in Kubernetes.

* *data-loader*, *prometheus*, *management-center* &amp; *webapp* -
 These should be started last.
Prometheus and Management Center are optional.

## Running -- Localhost

As above, the mandatory module `cva-cpp` must run in Docker or Kubernetes. However, the rest of the
example can run on localhost if you have enough compute capacity. `grafana` is ignored on localhost, 
and if you want to run Management Center the standard build from the Hazelcast website is all that's
needed.

Start the following commands, located in `src/main/scripts`

* *docker-cva-cpp.sh*
* *localhost-hazelcast-node-site1.sh*
* *localhost-data-loader.sh 100*
* *localhost-webapp.sh*

This will start one C++ worker node, one Hazelcast node for *site1*, do a limited data load capped
at 100 records, and start the web application.

Then you can navigate http://localhost:8084 to access the web application.

The parameter 100 for the data loader caps the items loaded, the first 100 curves and the first
100 trades. Therefore there are only 10,000 combinations of intermediate results for the CVA
calculation. Even then, on a low grade machine such as a laptop it might take 3 minutes or so
to process the entire run.

To close, use `docker ps` and `docker kill` to shut down the Docker contain for C++.
Then `docker container prune` to release retainedresources.

## Running -- Docker

Start the following commands, located in `src/main/scripts`

* *docker-cva-cpp.sh*
* *docker-grafana.sh*
* *docker-hazelcast-node-site1.sh*
* *docker-data-loader.sh 100*
* *docker-webapp.sh*
* *docker-management-center.sh*

This is essentially the same sequence but different script names as for running on localhost.
The difference is that "`docker-grafana.sh`" is needed so a Grafana server is running, as
stats are captured for containerized environments. Also "`docker-management-center.sh`"
is added, here at the end, but it can be anywhere in the sequence.

This uses Docker networking, so be sure to use `docker container prune` to release resources
prior to each run.

The Web UI is available as http://localhost:8081/. Internally it uses and logs port 8080, but
externally this is mapped to port 8081. This is to allow you to run other applications on
port 8080, such as the Management Center.

Grafana is available http://localhost:80/. Log in with user "_admin_", password "_admin_". In
the top menu, select the "_CVA Map Stats_" dashboard.

### Running -- Docker -- Management Center

There are two options for running the Management Center in Docker.

You can take the official image from the Docker hub, https://hub.docker.com/r/hazelcast/management-center/
being careful to take the version at the top of [this file](./management-center/Dockerfile), and
then run it as a Docker image. This is unconfigured, so you need to set up a user and password,
and add connections to "_site1_" and "_site2_".

Alternatively, the script "`docker-management-center.sh`" runs a pre-configured Management
Center, but it is pre-configured for Kubernetes not for Docker. This is pre-configured with
the user "_admin_" and password "_password1_", and with connections for "_site1_" and "_site2_"
that need amended.

In either case you need to find your host machine's IP address. A command such as 
"`ifconfig | grep -w inet`" provides the network IPs, but you need to ignore localhost.

So if your IP 192.168.1.2, the connection for "_site1_" is "_192.168.1.2:5701_", the
connection for "_site2_" is "_192.168.1.2:6701_".

*Note* the password in pre-configured Management Center is different from pre-configured Grafana,
as Management Center has stricter rules for allowable passwords. However, neither are production
strength, and you shouldn't put them on public Github repositories either. This is just an
example, not a practice to copy.

## Running -- Kubernetes

Kubernetes is slightly more complicated, as it is a full enterprise-grade product. However, the steps
are much the same.

### Running -- Kubernetes -- YAML

In `src/main/scripts` there are YAML files for potential customization and then use.

The scripts `kubernetes-cva-cpp.yaml`, `kubernetes-hazelcast-node-site1.yaml` and `kubernetes-hazelcast-node-site2.yaml`
are for stateful sets. All are set as 2, the minimum reasonable stateful set size. If you have sufficient hardware,
you can scale up and run time for processing will go down. 

The scripts `kubernetes-data-loader-site1.yaml` and `kubernetes-data-loader-site2.yaml` have this line:

```
          - name: "MY_THRESHOLD"
            value: "500"
```

This is the threshold for the amount of data loaded. Here it would mean 500 curves and 500 trades is the limit, making
500 * 500 = 250,000 calculations to be run. You can reduce the number or increase the number to vary the run time.

### Running -- Kubernetes -- Sequence

Three sets of Kubenetes deployments will set up the CVA example.

First, Grafana and Prometheus together and also C++, independent of the first two. 

```
kubectl create -f kubernetes-grafana-prometheus.yaml -f kubernetes-cpp.yaml
```

Second, the two Hazelcast clusters.

```
kubectl create -f kubernetes-hazelcast-node-site1.yaml -f kubernetes-hazelcast-node-site2.yaml
```

Lastly, the data loader for one of the two clusters, the web UI for both of the two clusters, and the Management Center.
Because of WAN replication, data loaded into one cluster will be replicated into the second.

```
kubectl create -f kubernetes-data-loader-site1.yaml -f kubernetes-webapp-site1.yaml -f kubernetes-webapp-site2.yaml  -f kubernetes-management-center.yaml
```
## Running -- Start Processing

Viewing the web UI in a browser, for example for "_site1_":

![Image of Hazelcast site 1 UI][Screenshot01] 

This shows four tables.

The Fixings table has a "Submit" button to launch the processing job.

The Map Sizes table shows the data stored in Hazelcast maps, the data loaded as input, and any output from job
runs.

The Downloads table shows results available for download, with links to do so. There will be no results before
a job completes.

The Jet Jobs table shows which processing jobs are running or have completed.

## Running -- Expected Output

Each run of the Jet job "_CvaStpJob_" produces one array of values, for the counterparties and the risk value
derived for each. 

This is stored in the map "_cva_data_", and used as the basis to create a single item in maps "_cva_csv_" 
and "_cva_xlsx_" which is the same array reformatted as a CSV file and a Excel file respectively. The key for
all three maps is the job run date as a string, for example "2016-01-07@2020-06-30T09-06-03" representing a run
with a calculation date of 2016-01-07 and run on 2020-06-30 at 9:06am.

Downloading the CSV should look like

```
ABD,670.5446422902135
AHI,263794.3274578084
AHZ,12901.117973960541
AJK,87590.48230872302
ALUMLQJH,0.0
APB,21231.23794486853
AXTM,38382.9382510774
BAM,1954.4232334783503
BBDMFG,17269.583709286322
BTV,5345.503106886422
H-PitKKX,19347.947833792157
HA-PacCorp,19186.126716549883
HTAyC,67462.90245070258
IQC,39355.557618121165
JBV,95.52423603052165
JJA,10648.800115573642
M,84396.15064428787
MQ,95137.85199258443
VFA,52097.31822236367
ZSX,21776.002502479965
```

with the same for Excel except as an Excel spreadsheet which has some extra columns with more counterparty
information.

There will be a line for each of the 20 counterparties. The risk value derived will depend on the number of
trades in the input, and may be zero if the counterparty has no trades or no credit risk (all debit values, we
owe the counterparty). The calculation is deterministic, so repeating for the same input will give the same
output.

## Summary

The CVA example here presents an alternative architecture for Risk processing.

Input data and final output data are held in Hazelcast maps, as these are very fast and scalable stores.

Calculations are offloaded to C++ worker modules, using gRPC sockets.

Intermediate results, all the Mark-To-Market values are not saved, instead they are streamed direct from
C++ into aggregators to produce the final results.

Input data is JSON, and this is converted to Protobuf for interaction via gRPC. The final results are available
in a familiar format, Excel or CSV.
