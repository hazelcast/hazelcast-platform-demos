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

package com.hazelcast.platform.demos.banking.cva.grafana;

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
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.function.BiPredicateEx;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.accumulator.LongAccumulator;
import com.hazelcast.jet.impl.JobRepository;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.SourceBuilder;
import com.hazelcast.jet.pipeline.StreamSource;
import com.hazelcast.map.IMap;
import com.hazelcast.platform.demos.banking.cva.GraphiteMetric;
import com.hazelcast.platform.demos.banking.cva.MapStatsCallable;
import com.hazelcast.platform.demos.banking.cva.MyConstants;
import com.hazelcast.platform.demos.banking.cva.MyConstants.Site;
import com.hazelcast.platform.demos.banking.cva.MyUtils;

/**
 * <p>
 * A job to periodically capture statistics, mainly about maps, and publish to
 * Graphite for display on Grafana.
 * </p>
 */
public class GrafanaGlobalMetricsJob {

    public static final String JOB_NAME = GrafanaGlobalMetricsJob.class.getSimpleName();

    private static final Logger LOGGER = LoggerFactory.getLogger(GrafanaGlobalMetricsJob.class);
    private static final long LOG_THRESHOLD = 12L;
    private static final String METRIC_PREFIX = GrafanaGlobalMetricsJob.class.getSimpleName();
    private static final double PERCENT = 100.0d;

    /**
     * <p>
     * Log every so often (5 second gap, every 12th = once per minute), but allow
     * everything through.
     * </p>
     */
    private static BiPredicateEx<LongAccumulator, List<GraphiteMetric>> noopLoggerFilter = (counter, item) -> {
        counter.subtract(1);
        if (counter.get() <= 0) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("{} statistics collected, {}", item.size(), item);
            } else {
                LOGGER.debug("{} statistics collected", item.size());
            }
            counter.set(LOG_THRESHOLD);
        }
        return true;
    };

    /**
     * <p>
     * A simple pipeline, take every statistic created and send to Graphite. This
     * uses a socket, so assumes Graphite is up.
     * </p>
     * <p>
     * A dummy filter in the middle logs periodically, so we know it's doing
     * something.
     * </p>
     */
    public static Pipeline buildPipeline(Site site, String grafanaURL) {

        Pipeline pipeline = Pipeline.create();

        pipeline.readFrom(GrafanaGlobalMetricsJob.mySource(site)).withoutTimestamps()
                // TODO Make this once per node not once per job
                .filterStateful(LongAccumulator::new, noopLoggerFilter)
                .writeTo(MyUtils.buildGraphiteSinkMultiple(grafanaURL));

        return pipeline;
    }

    /**
     * <p>
     * Create a stream source that periodically returns a list of metrics to sent to
     * Graphite for display on Grafana.
     * </p>
     *
     * @param site Are we "{@code site1}" or "{@code site2}"
     * @return
     */
    static StreamSource<List<GraphiteMetric>> mySource(Site site) {
        return SourceBuilder
                .stream(GraphiteIMapMetricGenerator.class.getSimpleName(),
                        context -> new GraphiteIMapMetricGenerator(context.jetInstance()))
                .<List<GraphiteMetric>>fillBufferFn(
                        (graphiteIMapMetricGenerator, buffer)
                        -> graphiteIMapMetricGenerator.fillBufferFn(buffer, site))
                .build();
    }

    /**
     * <p>
     * A Jet source that runs at most every <i>n</i> seconds (controlled by the
     * system constant {@link MyConstants.GRAPHITE_COLLECTION_INTERVAL_SECONDS}).
     * </p>
     * <p>
     * Each time it runs, it scans all maps for their statistics, adds them to a
     * list to send to Graphite, then goes to sleep for the controlled interval.
     * </p>
     * <p>
     * So if the interval is 5 seconds then stats will appear more than 5 seconds
     * apart, assuming it takes a few milliseconds or nanoseconds to collate. If it
     * takes much longer than that it's a problem.
     * </p>
     */
    static class GraphiteIMapMetricGenerator {
        private final HazelcastInstance hazelcastInstance;

        GraphiteIMapMetricGenerator(JetInstance jetInstance) {
            this.hazelcastInstance = jetInstance.getHazelcastInstance();
        }

        /**
         * <p>
         * Create a list of statistics for maps, the same ones for all maps found except
         * system internal ones.
         * </p>
         *
         * @param buffer Jet's buffer
         * @param site   Prefix for statistics
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
                final AtomicLong reads = new AtomicLong(0);
                final AtomicLong writes = new AtomicLong(0);
                final AtomicLong deletes = new AtomicLong(0);
                final AtomicLong nearCacheStatsPresent = new AtomicLong(0);
                final AtomicLong nearCacheHits = new AtomicLong(0);
                final AtomicLong nearCacheMisses = new AtomicLong(0);

                // Collect intermediate results from each member to add to total
                MapStatsCallable mapStatsCallable = new MapStatsCallable(mapName);
                IExecutorService executorService = this.hazelcastInstance.getExecutorService("default");

                Map<?, Future<HazelcastJsonValue>> results = executorService.submitToAllMembers(mapStatsCallable);
                this.processFutures(results, reads, writes, deletes, nearCacheStatsPresent, nearCacheHits, nearCacheMisses);

                // Create metric objects from the totals
                result.add(MyUtils.createGraphiteMetric4Tier(site, METRIC_PREFIX, "map", mapName, "size",
                        String.valueOf(size), now));
                result.add(MyUtils.createGraphiteMetric4Tier(site, METRIC_PREFIX, "map", mapName, "reads",
                        String.valueOf(reads.get()), now));
                result.add(MyUtils.createGraphiteMetric4Tier(site, METRIC_PREFIX, "map", mapName, "writes",
                        String.valueOf(writes.get()), now));
                result.add(MyUtils.createGraphiteMetric4Tier(site, METRIC_PREFIX, "map", mapName, "deletes",
                        String.valueOf(deletes.get()), now));

                if (nearCacheStatsPresent.get() > 0) {
                    long nearCacheTotal = nearCacheHits.get() + nearCacheMisses.get();
                    double percentage = 0d;
                    if (nearCacheTotal > 0) {
                        percentage = nearCacheHits.get() * PERCENT / nearCacheTotal;
                    }
                    result.add(MyUtils.createGraphiteMetric4Tier(site, METRIC_PREFIX, "map", mapName, "near_cache_percent",
                            String.format("%.1f", percentage), now));
                }
            }
            buffer.add(result);

            // Wait before running again.
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(MyConstants.GRAPHITE_COLLECTION_INTERVAL_SECONDS));
        }

        /**
         * <p>For each {@link HazelcastJsonValue} returned per member, update the stats with the
         * matching names to get global figures for the cluster.
         * </p>
         *
         * @param results Future of Hazelcast remote Callable execution
         * @param reads Atomic counter
         * @param writes Atomic counter
         * @param deletes Atomic counter
         * @param nearCacheStatsPresent Atomic counter
         * @param nearCacheHits Atomic counter
         * @param nearCacheMisses Atomic counter
         */
        private void processFutures(Map<?, Future<HazelcastJsonValue>> results, AtomicLong reads, AtomicLong writes,
                AtomicLong deletes, AtomicLong nearCacheStatsPresent, AtomicLong nearCacheHits, AtomicLong nearCacheMisses) {
            results.values().forEach(future -> {
                String str = "";
                try {
                    str = future.get().toString();
                    JSONObject jsonObject = new JSONObject(str);
                    reads.addAndGet(jsonObject.getLong("reads"));
                    writes.addAndGet(jsonObject.getLong("writes"));
                    deletes.addAndGet(jsonObject.getLong("deletes"));
                    if (jsonObject.has("near_cache")) {
                        JSONObject nearCacheJsonObject = jsonObject.getJSONObject("near_cache");
                        nearCacheHits.addAndGet(nearCacheJsonObject.getLong("hits"));
                        nearCacheMisses.addAndGet(nearCacheJsonObject.getLong("misses"));
                        nearCacheStatsPresent.incrementAndGet();
                    }
                } catch (JSONException exception) {
                    LOGGER.debug(str, exception);
                } catch (InterruptedException | ExecutionException ignored) {
                }
            });
        }
    }

}
