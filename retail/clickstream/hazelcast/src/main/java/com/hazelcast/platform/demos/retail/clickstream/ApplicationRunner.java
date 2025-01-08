/*
 * Copyright (c) 2008-2025, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.platform.demos.retail.clickstream;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.core.DAG;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.python.DagPythonMetaSupplier;
import com.hazelcast.jet.python.DagPythonProcessor;
import com.hazelcast.jet.python.DagPythonSupplier;
import com.hazelcast.map.IMap;
import com.hazelcast.platform.demos.retail.clickstream.heartbeat.HeartbeatAlert;
import com.hazelcast.platform.demos.retail.clickstream.heartbeat.HeartbeatAlertAggregator;
import com.hazelcast.platform.demos.retail.clickstream.job.ClickstreamHandler;
import com.hazelcast.platform.demos.retail.clickstream.job.DigitalTwinMetaSupplier;
import com.hazelcast.platform.demos.retail.clickstream.job.DigitalTwinProcessor;
import com.hazelcast.platform.demos.retail.clickstream.job.DigitalTwinSupplier;
import com.hazelcast.platform.demos.retail.clickstream.job.ModelMetaSupplier;
import com.hazelcast.platform.demos.retail.clickstream.job.ModelProcessor;
import com.hazelcast.platform.demos.retail.clickstream.job.ModelSupplier;
import com.hazelcast.platform.demos.retail.clickstream.job.MyMergeProcessor;
import com.hazelcast.platform.demos.retail.clickstream.job.PulsarIngest;
import com.hazelcast.platform.demos.retail.clickstream.job.RandomForestPrediction;
import com.hazelcast.platform.demos.retail.clickstream.job.RetrainingAssessmentListener;
import com.hazelcast.platform.demos.retail.clickstream.job.RetrainingControl;
import com.hazelcast.platform.demos.retail.clickstream.job.RetrainingLaunchListener;
import com.hazelcast.platform.demos.retail.clickstream.job.StatisticsAccuracyByOrder;
import com.hazelcast.platform.demos.retail.clickstream.job.StatisticsLatency;
import com.hazelcast.platform.demos.utils.UtilsSlackSQLJob;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Periodically log to the console, so there is something to
 * confirm member is ok.
 * </p>
 */
@Slf4j
@Configuration
public class ApplicationRunner {
    private static final int FIVE = 5;

    @Autowired
    private HazelcastInstance hazelcastInstance;
    @Autowired
    private MyProperties myProperties;

    /**
     * <p>Output once a minute, with more output every 5th minute.
     * </p>
     */
    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
            log.info("-=-=-=-=- START '{}' START -=-=-=-=-=-",
                this.hazelcastInstance.getName());

            String cluster1Name = System.getProperty("CLUSTER1_NAME");
            String clusterName = this.hazelcastInstance.getConfig().getClusterName();

            int currentSize = this.hazelcastInstance.getCluster().getMembers().size();
            if (currentSize == 1) {
                MapDefinitions.addMappings(this.hazelcastInstance);
            }
            if (this.myProperties.getInitSize() != currentSize) {
                if (this.myProperties.getInitSize() > currentSize) {
                    log.info("Cluster size {}, initializing at {}", currentSize, this.myProperties.getInitSize());
                }
            } else {
                log.info("Cluster size {}, -=-=-=-=- initialize by '{}' -=-=-=-=-=-",
                                               currentSize, this.hazelcastInstance.getName());
                this.createMaps();
                this.launchJobs(clusterName, cluster1Name);
                log.info("Cluster size {}, -=-=-=-=- initialized by '{}' -=-=-=-=-=-",
                        currentSize, this.hazelcastInstance.getName());
            }
            this.addListeners(clusterName, cluster1Name);

            int count = 0;
            while (this.hazelcastInstance.getLifecycleService().isRunning()) {
                TimeUnit.MINUTES.sleep(1);
                count++;
                String countStr = String.format("%05d", count);
                log.info("-=-=-=-=- {} '{}' {} -=-=-=-=-=-",
                        countStr, this.hazelcastInstance.getName(), countStr);
                if (count % FIVE == 0) {
                    try {
                        this.logSizes();
                        this.logJobs();
                    } catch (Exception e) {
                        log.error("count==" + count, e);
                    }
                }
            }
        };
    }


    /**
     * <p>Touch/create maps so visible on Management Center, and map loaders fire.
     * </p>
     */
    private void createMaps() {
        for (String iMapName : MyConstants.IMAP_NAMES) {
            this.hazelcastInstance.getMap(iMapName);
        }
        // Cache derived config for re-use
        String graphiteHost = MyUtils.getGraphiteHost();
        this.hazelcastInstance.getMap(MyConstants.IMAP_NAME_CONFIG)
            .set(MyConstants.CONFIG_MAP_KEY_GRAPHITE, graphiteHost);
    }

    /**
     * <p>Launch jobs, but only for one cluster, demonstrates WAN.
     * </p>
     */
    private void launchJobs(String clusterName, String cluster1Name) {
        String graphiteHost = this.hazelcastInstance
                .getMap(MyConstants.IMAP_NAME_CONFIG)
                .get(MyConstants.CONFIG_MAP_KEY_GRAPHITE).toString();
        log.info("------------------------------------");

        // Jobs that run on both clusters
        this.launchClickstreamHandlerJob();
        this.launchHeartbeatJob(clusterName);
        this.launchPulsarIngestJob();

        // Prediction on "blue"
        if (clusterName.equals(cluster1Name)) {
            this.launchRandomForestPredictionJob();
            this.launchSlackSQLJob();
            this.launchStatisticsAccuracyByOrderJob(clusterName, graphiteHost);
            this.launchStatisticsLatencyJob(clusterName, graphiteHost);
        }

        // Retraining on "green"
        if (!clusterName.equals(cluster1Name)) {
            this.launchRetrainingControlJob();
        }

        log.info("------------------------------------");
    }

    /**
     * <p>Check incoming heartbeats.
     * </p>
     */
    private void launchHeartbeatJob(String localClusterName) {
        Pipeline pipelineHeartbeatAlert = HeartbeatAlert.buildPipeline(localClusterName);

        JobConfig jobConfigHeartbeatAlert = new JobConfig();
        String jobNameHeartbeatAlert = HeartbeatAlert.class.getSimpleName();
        jobConfigHeartbeatAlert.addClass(HeartbeatAlert.class);
        jobConfigHeartbeatAlert.addClass(HeartbeatAlertAggregator.class);
        jobConfigHeartbeatAlert.setName(jobNameHeartbeatAlert);

        this.launchPipelineJob(pipelineHeartbeatAlert, jobConfigHeartbeatAlert);
    }

    /**
     * <p>Ingest from Pulsar into {@link IMap}
     * </p>
     */
    private void launchPulsarIngestJob() {
        Pipeline pipelinePulsarIngest = PulsarIngest.buildPipeline();

        JobConfig jobConfigPulsarIngest = new JobConfig();
        String jobNamePulsarIngest = PulsarIngest.class.getSimpleName();
        jobConfigPulsarIngest.addClass(PulsarIngest.class);
        jobConfigPulsarIngest.setName(jobNamePulsarIngest);

        this.launchPipelineJob(pipelinePulsarIngest, jobConfigPulsarIngest);
    }

    /**
     * <p>Process clickstream into relevant events
     */
    private void launchClickstreamHandlerJob() {
        Pipeline pipelineClickstreamHandler = ClickstreamHandler.buildPipeline();

        JobConfig jobConfigClickstreamHandler = new JobConfig();
        String jobNameClickstreamHandler = ClickstreamHandler.class.getSimpleName();
        jobConfigClickstreamHandler.addClass(ClickstreamHandler.class);
        jobConfigClickstreamHandler.setName(jobNameClickstreamHandler);

        this.launchPipelineJob(pipelineClickstreamHandler, jobConfigClickstreamHandler);
    }

    /**
     * <p>ML evaluation using Random Forest mechanism
     */
    private void launchRandomForestPredictionJob() {
        DAG dagRandomForestPrediction = RandomForestPrediction.buildDAG();

        JobConfig jobConfigRandomForestPrediction = new JobConfig();
        String jobNameRandomForestPrediction = RandomForestPrediction.class.getSimpleName();
        jobConfigRandomForestPrediction.addClass(RandomForestPrediction.class);
        jobConfigRandomForestPrediction.addClass(DagPythonMetaSupplier.class);
        jobConfigRandomForestPrediction.addClass(DagPythonProcessor.class);
        jobConfigRandomForestPrediction.addClass(DagPythonSupplier.class);
        jobConfigRandomForestPrediction.addClass(DigitalTwinMetaSupplier.class);
        jobConfigRandomForestPrediction.addClass(DigitalTwinProcessor.class);
        jobConfigRandomForestPrediction.addClass(DigitalTwinSupplier.class);
        jobConfigRandomForestPrediction.addClass(ModelMetaSupplier.class);
        jobConfigRandomForestPrediction.addClass(ModelProcessor.class);
        jobConfigRandomForestPrediction.addClass(ModelSupplier.class);
        jobConfigRandomForestPrediction.addClass(MyMergeProcessor.class);
        jobConfigRandomForestPrediction.setName(jobNameRandomForestPrediction);

        this.launchDAGJob(dagRandomForestPrediction, jobConfigRandomForestPrediction);
    }

    /**
     * <p>SQL to/from Slack.
     * </p>
     */
    private void launchSlackSQLJob() {
        // Shouldn't fail but no reason to abandon if it does
        try {
            Object projectName = this.myProperties.getProjectName();

            UtilsSlackSQLJob.submitJob(hazelcastInstance,
                    projectName == null ? "" : projectName.toString());
        } catch (Exception e) {
            log.error("launchSlackSQLJob:" + UtilsSlackSQLJob.class.getSimpleName(), e);
        }
    }

    /**
     * <p>Periodic retraining
     */
    private void launchRetrainingControlJob() {
        long buildTimestamp = MyUtils.parseTimestamp(this.myProperties.getBuildTimestamp());
        Pipeline pipelineRetrainingControl = RetrainingControl.buildPipeline(buildTimestamp);

        JobConfig jobConfigRetrainingControl = new JobConfig();
        String jobNameRetrainingControl = RetrainingControl.class.getSimpleName();
        jobConfigRetrainingControl.addClass(RetrainingControl.class);
        jobConfigRetrainingControl.setName(jobNameRetrainingControl);

        this.launchPipelineJob(pipelineRetrainingControl, jobConfigRetrainingControl);
    }

    /**
     * <p>Accuracy statistics to Grafana/Graphite
     */
    private void launchStatisticsAccuracyByOrderJob(String clusterName, String graphiteHost) {
        Pipeline pipelineStatisticsAccuracyByOrder = StatisticsAccuracyByOrder.buildPipeline(clusterName, graphiteHost);

        JobConfig jobConfigStatisticsAccuracyByOrder = new JobConfig();
        String jobNameStatisticsAccuracyByOrder = StatisticsAccuracyByOrder.class.getSimpleName();
        jobConfigStatisticsAccuracyByOrder.addClass(StatisticsAccuracyByOrder.class);
        jobConfigStatisticsAccuracyByOrder.setName(jobNameStatisticsAccuracyByOrder);

        this.launchPipelineJob(pipelineStatisticsAccuracyByOrder, jobConfigStatisticsAccuracyByOrder);
    }

    /**
     * <p>Latency statistics to Grafana/Graphite
     */
    private void launchStatisticsLatencyJob(String clusterName, String graphiteHost) {
        Pipeline pipelineStatisticsLatency = StatisticsLatency.buildPipeline(clusterName, graphiteHost);

        JobConfig jobConfigStatisticsLatency = new JobConfig();
        String jobNameStatisticsLatency = StatisticsLatency.class.getSimpleName();
        jobConfigStatisticsLatency.addClass(StatisticsLatency.class);
        jobConfigStatisticsLatency.setName(jobNameStatisticsLatency);

        this.launchPipelineJob(pipelineStatisticsLatency, jobConfigStatisticsLatency);
    }

    /**
     * <p>Launch job on startup, should only occur once and hence not fail.
     * </p>
     *
     * @param pipeline
     * @param jobConfig
     */
    private void launchPipelineJob(Pipeline pipeline, JobConfig jobConfig) {
        try {
            // Fails if job exists with same job name, unlike "newJobIfAbsent"
            Job job =
                    this.hazelcastInstance.getJet().newJob(pipeline, jobConfig);
            log.info("launchJob() -> {}", job);
        } catch (Exception e) {
            log.error("launchJob() for " + jobConfig, e);
        }
    }

    /**
     * <p>DAG equivalent of job launch wrapper
     * </p>
     *
     * @param dag
     * @param jobConfig
     */
    private void launchDAGJob(DAG dag, JobConfig jobConfig) {
        try {
            // Fails if job exists with same job name, unlike "newJobIfAbsent"
            Job job =
                    this.hazelcastInstance.getJet().newJob(dag, jobConfig);
            log.info("launchJob() -> {}", job);
        } catch (Exception e) {
            log.error("launchJob() for " + jobConfig, e);
        }
    }

    /**
     * <p>Listen for local events, on this node only for selected
     * maps.
     * </p>
     */
    private void addListeners(String clusterName, String cluster1Name) {
        MyLoggingListener myLoggingListener = new MyLoggingListener();
        this.hazelcastInstance.getMap(MyConstants.IMAP_NAME_ALERT)
            .addEntryListener(myLoggingListener, true);

        // Retraining on "green"
        if (!clusterName.equals(cluster1Name)) {
            RetrainingAssessmentListener retrainingAssessmentListener = new RetrainingAssessmentListener();
            this.hazelcastInstance.getMap(MyConstants.IMAP_NAME_RETRAINING_ASSESSMENT)
                .addLocalEntryListener(retrainingAssessmentListener);
            RetrainingLaunchListener retrainingLaunchListener = new RetrainingLaunchListener();
            this.hazelcastInstance.getMap(MyConstants.IMAP_NAME_RETRAINING_CONTROL)
                .addLocalEntryListener(retrainingLaunchListener);
        }
    }

    /**
     * <p>"{@code size()}" is a relatively expensive operation, but we
     * only run this every few minutes.
     * </p>
     */
    private void logSizes() {
        Set<String> mapNames = this.hazelcastInstance
                .getDistributedObjects()
                .stream()
                .filter(distributedObject -> (distributedObject instanceof IMap))
                .filter(distributedObject -> !distributedObject.getName().startsWith("__"))
                .map(distributedObject -> distributedObject.getName())
                .collect(Collectors.toCollection(TreeSet::new));

        mapNames
        .forEach(name -> {
            IMap<?, ?> iMap = this.hazelcastInstance.getMap(name);
            log.info("MAP '{}'.size() => {}", iMap.getName(), iMap.size());
        });
    }

    /**
     * <p>Jobs in name order.
     * </p>
     */
    private void logJobs() {
        Map<String, Job> jobs = new TreeMap<>();
        this.hazelcastInstance
            .getJet()
            .getJobs()
            .stream()
            .forEach(job -> {
                if (job.getName() == null) {
                    if (job.isLightJob()) {
                        // Concurrent SQL doesn't have a name set.
                        log.warn("logJobs(), job.getName()==null for light job {}", job);
                    } else {
                        log.error("logJobs(), job.getName()==null for {}", job);
                    }
                } else {
                    jobs.put(job.getName(), job);
                }
            });

        jobs
        .forEach((key, value) -> {
            log.info("JOB '{}' => {}", key, value.getStatus());
        });
    }
}
