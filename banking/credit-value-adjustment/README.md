# Hazelcast Platform Demo Applications - Banking - Credit Value Adjustment

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

* *data-loader*, *management-center* &amp; *webapp* -
 These should be started last.
Management Center is optional.

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

Then you can navigate http://localhost:8085 to access the web application.

The parameter 100 for the data loader caps the items loaded, the first 100 curves and the first
100 trades. Therefore there are only 10,000 combinations of intermediate results for the CVA
calculation. Even then, on a low grade machine such as a laptop it might take 3 minutes or so
to process the entire run.

To close, use `docker ps` and `docker kill` to shut down the Docker contain for C++.
Then `docker container prune` to release retainedresources.

## Running -- Docker

TODO

## Running -- Kubernetes

TODO

## Running -- Expected Output

TODO

### Logs

TODO

## Summary

TODO


