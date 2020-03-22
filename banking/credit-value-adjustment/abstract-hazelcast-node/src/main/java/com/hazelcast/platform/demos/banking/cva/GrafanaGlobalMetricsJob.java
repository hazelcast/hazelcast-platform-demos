/*
 * Copyright (c) 2008-2020, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.platform.demos.banking.cva;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.function.BiPredicateEx;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.accumulator.LongAccumulator;
import com.hazelcast.jet.impl.JobRepository;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.SourceBuilder;
import com.hazelcast.jet.pipeline.StreamSource;
import com.hazelcast.map.IMap;
import com.hazelcast.platform.demos.banking.cva.MapStatsCallable.MyMapStats;
import com.hazelcast.platform.demos.banking.cva.MyConstants.Site;

/**
 * <p>A job to periodically capture statistics, mainly about maps,
 * and publish to Graphite for display on Grafana.
 * </p>
 */
public class GrafanaGlobalMetricsJob {
    private static final String GRAFANA_HOSTNAME = "grafana";
    private static final Logger LOGGER = LoggerFactory.getLogger(GrafanaGlobalMetricsJob.class);
    private static final long LOG_THRESHOLD = 12L;
    private static final String METRIC_PREFIX = GrafanaGlobalMetricsJob.class.getSimpleName();

    /**
     * <p>Log every so often (5 second gap, every 12th = once per minute),
     * but allow everything through.
     * </p>
     */
    private static BiPredicateEx<LongAccumulator, List<GraphiteMetric>> noopLoggerFilter
    = (counter, item) -> {
        counter.subtract(1);
        if (counter.get() <= 0) {
            LOGGER.info("{} statistics collected", item.size());
            counter.set(LOG_THRESHOLD);
        }
        return true;
    };


    /**
     * <p>A simple pipeline, take every statistic created and send
     * to Graphite. This uses a socket, so assumes Graphite is up.
     * </p>
     * <p>A dummy filter in the middle logs periodically, so we know
     * it's doing something.
     * </p>
     */
    public static Pipeline buildPipeline(Site site) {

        Pipeline pipeline = Pipeline.create();

        pipeline
        .readFrom(GrafanaGlobalMetricsJob.mySource(site)).withoutTimestamps()
        //TODO Make this once per node not once per job
        .filterStateful(LongAccumulator::new, noopLoggerFilter)
        //FIXME Replace with Service K8S or Hostname Docker
        .writeTo(MyUtils.buildGraphiteSinkMultiple(GRAFANA_HOSTNAME));

        return pipeline;
    }

    /**
     * <p>Create a stream source that periodically returns a list of
     * metrics to sent to Graphite for display on Grafana.
     * </p>
     *
     * @param site Are we "{@code CVA_SITE1}" or "{@code CVA_SITE2}"
     * @return
     */
    static StreamSource<List<GraphiteMetric>> mySource(Site site) {
        return SourceBuilder.stream(GraphiteIMapMetricGenerator.class.getSimpleName(),
                    context -> new GraphiteIMapMetricGenerator(context.jetInstance()))
                .<List<GraphiteMetric>>fillBufferFn(
                    (graphiteIMapMetricGenerator, buffer) ->
                        graphiteIMapMetricGenerator.fillBufferFn(buffer, site)
                )
                .build();
    }

    /**
     * <p>A Jet source that runs at most every <i>n</i> seconds (controlled by the
     * system constant {@link MyConstants.GRAPHITE_COLLECTION_INTERVAL_SECONDS}).
     * </p>
     * <p>Each time it runs, it scans all maps for their statistics, adds them
     * to a list to send to Graphite, then goes to sleep for the controlled
     * interval.
     * </p>
     * <p>So if the interval is 5 seconds then stats will appear more than 5
     * seconds apart, assuming it takes a few milliseconds or nanoseconds to
     * collate. If it takes much longer than that it's a problem.
     * </p>
     */
    static class GraphiteIMapMetricGenerator {
        private final HazelcastInstance hazelcastInstance;

        GraphiteIMapMetricGenerator(JetInstance jetInstance) {
            this.hazelcastInstance = jetInstance.getHazelcastInstance();
        }

        /**
         * <p>Create a list of statistics for maps, the same ones for
         * all maps found except system internal ones.
         * </p>
         *
         * @param buffer Jet's buffer
         * @param site Prefix for statistics
         */
        void fillBufferFn(SourceBuilder.SourceBuffer<List<GraphiteMetric>> buffer, Site site) {

            List<GraphiteMetric> result = new ArrayList<>();
            long now = System.currentTimeMillis();

            Set<String> mapNames = this.hazelcastInstance.getDistributedObjects().stream()
                    .filter(distributedObject -> distributedObject instanceof IMap)
                    .map(distributedObject -> distributedObject.getName())
                    .filter(name -> !name.startsWith(JobRepository.INTERNAL_JET_OBJECTS_PREFIX))
                    .collect(Collectors.toCollection(TreeSet::new));

            for (String mapName : mapNames) {
                IMap<?, ?> iMap = this.hazelcastInstance.getMap(mapName);

                // Metric values to collect for this map
                int size = iMap.size();
                final AtomicLong read = new AtomicLong(0);
                final AtomicLong write = new AtomicLong(0);
                final AtomicLong delete = new AtomicLong(0);

                // Collect intermediate results from each member to add to total
                MapStatsCallable mapStatsCallable = new MapStatsCallable(mapName);
                IExecutorService executorService = this.hazelcastInstance.getExecutorService("default");

                Map<?, Future<MyMapStats>> results = executorService.submitToAllMembers(mapStatsCallable);
                results.values().forEach(future -> {
                        MyMapStats myMapStats;
                        try {
                            myMapStats = future.get();
                            read.addAndGet(myMapStats.getRead());
                            write.addAndGet(myMapStats.getWrite());
                            delete.addAndGet(myMapStats.getDelete());
                        } catch (InterruptedException | ExecutionException ignored) {
                        }
                });

                // Create metric objects from the totals
                GraphiteMetric graphiteMetricSize = new GraphiteMetric(site);
                graphiteMetricSize.setMetricName(METRIC_PREFIX + MyConstants.GRAPHITE_SEPARATOR
                        + "map" + MyConstants.GRAPHITE_SEPARATOR
                        + mapName + MyConstants.GRAPHITE_SEPARATOR + "size");
                graphiteMetricSize.setMetricValue(String.valueOf(size));
                graphiteMetricSize.setTimestamp(now);

                GraphiteMetric graphiteMetricRead = new GraphiteMetric(site);
                graphiteMetricRead.setMetricName(METRIC_PREFIX + MyConstants.GRAPHITE_SEPARATOR
                        + "map" + MyConstants.GRAPHITE_SEPARATOR
                        + mapName + MyConstants.GRAPHITE_SEPARATOR + "read");
                graphiteMetricRead.setMetricValue(String.valueOf(read.get()));
                graphiteMetricRead.setTimestamp(now);

                GraphiteMetric graphiteMetricWrite = new GraphiteMetric(site);
                graphiteMetricWrite.setMetricName(METRIC_PREFIX + MyConstants.GRAPHITE_SEPARATOR
                        + "map" + MyConstants.GRAPHITE_SEPARATOR
                        + mapName + MyConstants.GRAPHITE_SEPARATOR + "write");
                graphiteMetricWrite.setMetricValue(String.valueOf(write.get()));
                graphiteMetricWrite.setTimestamp(now);

                GraphiteMetric graphiteMetricDelete = new GraphiteMetric(site);
                graphiteMetricDelete.setMetricName(METRIC_PREFIX + MyConstants.GRAPHITE_SEPARATOR
                        + "map" + MyConstants.GRAPHITE_SEPARATOR
                        + mapName + MyConstants.GRAPHITE_SEPARATOR + "delete");
                graphiteMetricDelete.setMetricValue(String.valueOf(delete.get()));
                graphiteMetricDelete.setTimestamp(now);

                // Add the collected statistics to what will be sent to Graphite for this map
                result.add(graphiteMetricSize);
                result.add(graphiteMetricRead);
                result.add(graphiteMetricWrite);
                result.add(graphiteMetricDelete);
            }

            buffer.add(result);

            // Wait before running again.
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(MyConstants.GRAPHITE_COLLECTION_INTERVAL_SECONDS));
        }
    }
}
