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

package hazelcast.platform.demos.benchmark.nexmark;

import static com.hazelcast.jet.datamodel.Tuple2.tuple2;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.function.BiFunctionEx;
import com.hazelcast.function.FunctionEx;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.accumulator.LongAccumulator;
import com.hazelcast.jet.accumulator.LongLongAccumulator;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.config.ProcessingGuarantee;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.StreamStage;

import hazelcast.platform.demos.benchmark.nexmark.model.Auction;
import hazelcast.platform.demos.benchmark.nexmark.model.Bid;
import hazelcast.platform.demos.benchmark.nexmark.model.Person;

/**
 * <p>Hazelcast streaming pipeline for NEXMark queries.
 * </p>
 * <p>A base class for the benchmark job types, that provides communal work.
 * Mainly this is the {@link BenchmarkBase#run} method that create the
 * job, and submits it with latency measurement.
 * </p>
 */
public abstract class BenchmarkBase {
    public static final String IMAP_NAME_CURRENT_LATENCIES = "current_latencies";
    public static final String IMAP_NAME_MAX_LATENCIES = "max_latencies";
    public static final String PROP_EVENTS_PER_SECOND = "events_per_second";
    public static final String PROP_KIND = "kind";
    public static final String PROP_NUM_DISTINCT_KEYS = "num_distinct_keys";
    public static final String PROP_PROCESSING_GUARANTEE = "processing_guarantee";
    public static final String PROP_SLIDING_STEP_MILLIS = "sliding_step_millis";
    public static final String PROP_WINDOW_SIZE_MILLIS = "window_size_millis";
    public static final long ONE_SECOND_AS_MILLIS = TimeUnit.SECONDS.toMillis(1);
    public static final long ONE_SECOND_AS_NANOS = TimeUnit.SECONDS.toNanos(1);

    protected static final long NO_ALLOWED_LAG = 0L;
    protected static final long INITIAL_SOURCE_DELAY_MILLIS = 10L;
    // 15 minutes warm up, so JVM is likely to have settled into normal operations
    protected static final long WARM_UP_MILLIS = TimeUnit.MINUTES.toMillis(15L);

    private static final long SNAPSHOT_INTERVAL_MILLIS = 1_000L;
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance();

    /**
     * <p>Using a pair of timestamp as state (first timestamp and last timestamp),
     * examine the incoming timestamp to determine the latency.
     * Re-factored to a separate function for unit testing.
     * </p>
     */
    protected static final BiFunctionEx<LongLongAccumulator, Long, Tuple2<Long, Long>> LATENCY_FN =
            (state, timestamp) -> {
                long lastTimestamp = state.get2();
                if (timestamp <= lastTimestamp) {
                    return null;
                }
                if (lastTimestamp == 0) {
                    // state.startTimestamp = timestamp;
                    state.set1(timestamp);
                }
                long startTimestamp = state.get1();
                // state.lastTimestamp = timestamp;
                state.set2(timestamp);

                // Drop results in warm-up phase
                long offset = timestamp - startTimestamp;
                if (offset < WARM_UP_MILLIS) {
                    return null;
                }

                // very low latencies may be reported as negative due to clock skew
                long latency = System.currentTimeMillis() - timestamp;
                if (latency < 0) {
                    latency = 0;
                }

                return tuple2(offset, latency);
            };


    /**
     * <p>Initiate a test with the given parameters.
     * </p>
     *
     * @param hazelcastInstance
     * @param jobNameSuffix
     * @param now
     * @param params Numeric parameters
     * @param processingGuarantee
     * @return
     */
    public Job run(HazelcastInstance hazelcastInstance, String jobNameSuffix, long now, Map<String, Long> params,
            ProcessingGuarantee processingGuarantee) {

        String kind = this.getClass().getSimpleName();

        Pipeline pipeline = Pipeline.create();
        StreamStage<Tuple2<Long, Long>> latencies = this.addComputation(pipeline, params);
        this.completePipeline(kind, latencies, now, params, processingGuarantee.toString());

        JobConfig jobConfig = new JobConfig();

        String jobName = kind;
        if (!jobNameSuffix.isBlank()) {
            jobName = jobName + jobNameSuffix;
        }
        jobConfig.setName(jobName);
        jobConfig.setProcessingGuarantee(processingGuarantee);
        jobConfig.setSnapshotIntervalMillis(SNAPSHOT_INTERVAL_MILLIS);

        jobConfig.registerSerializer(Auction.class, Auction.AuctionSerializer.class);
        jobConfig.registerSerializer(Bid.class, Bid.BidSerializer.class);
        jobConfig.registerSerializer(Person.class, Person.PersonSerializer.class);

        jobConfig.addClass(this.getClass());
        jobConfig.addClass(BenchmarkBase.class);

        return hazelcastInstance.getJet().newJob(pipeline, jobConfig);
    }

    /**
     * <p>Each of the tests will produce a pair of time offset and latency observed,
     * following the running of the specific test.
     * </p>
     *
     * @param pipeline A new pipeline to attach stages to.
     * @param params   Some common for all job, some unique.
     * @return
     */
    abstract StreamStage<Tuple2<Long, Long>> addComputation(Pipeline pipeline, Map<String, Long> params);

    /**
     * <p>Complete the pipeline with standard processing for all job types. Write each new
     * latency to the current latencies map. If the max latency changes, output this to its map.
     * </p>
     * <p>The tuple input stream has the job offset. We would like the absolute time, but rather
     * than make the tuple input stream use {@link Tuple3} with one field being constant, use the
     * approximate job launch time as a parameter. Submission time and start time will be near
     * enough the same.
     * </p>
     *
     * @param kind For the key of the map.
     * @param latencies A stream of time offsets and latencies, both in milliseconds
     * @param startTime Approximate job start time
     */
    private void completePipeline(String kind, StreamStage<Tuple2<Long, Long>> latencies, long startTime,
            Map<String, Long> params, String processingGuaranteeStr) {
        latencies
        .map(tuple2 -> BenchmarkBase.formMapEntry(kind, startTime, tuple2.f0(), tuple2.f1(),
                params, processingGuaranteeStr))
        .writeTo(Sinks.map(BenchmarkBase.IMAP_NAME_CURRENT_LATENCIES));

        latencies
        .mapStateful(LongAccumulator::new,
                (state, tuple2) -> {
                    long latency = tuple2.f1();
                    if (latency > state.get()) {
                        state.set(latency);
                        return BenchmarkBase.formMapEntry(kind, startTime, tuple2.f0(), tuple2.f1(),
                                params, processingGuaranteeStr);
                    } else {
                        return null;
                    }
        })
        .writeTo(Sinks.map(BenchmarkBase.IMAP_NAME_MAX_LATENCIES));
    }

    private static Tuple2<HazelcastJsonValue, HazelcastJsonValue> formMapEntry(String kind,
            long startTime, long offset, long latency, Map<String, Long> params, String processingGuaranteeStr) {
        StringBuilder keySB = new StringBuilder();
        keySB.append("{\"").append(PROP_KIND).append("\":\"").append(kind).append("\"");
        keySB.append(",\"start_timestamp\":").append(startTime);
        keySB.append(",\"start_timestamp_str\":\"").append(BenchmarkBase.timestampToISO8601(startTime)).append("\"");
        keySB.append(",\"").append(BenchmarkBase.PROP_PROCESSING_GUARANTEE).append("\":\"")
            .append(processingGuaranteeStr).append("\"");
        keySB.append(",\"").append(BenchmarkBase.PROP_EVENTS_PER_SECOND).append("\":\"")
            .append(BenchmarkBase.NUMBER_FORMAT.format(params.get(BenchmarkBase.PROP_EVENTS_PER_SECOND))).append("\"");
        keySB.append(",\"").append(BenchmarkBase.PROP_NUM_DISTINCT_KEYS).append("\":")
            .append(params.get(BenchmarkBase.PROP_NUM_DISTINCT_KEYS));
        keySB.append(",\"").append(BenchmarkBase.PROP_SLIDING_STEP_MILLIS).append("\":")
            .append(params.get(BenchmarkBase.PROP_SLIDING_STEP_MILLIS));
        keySB.append(",\"").append(BenchmarkBase.PROP_WINDOW_SIZE_MILLIS).append("\":")
            .append(params.get(BenchmarkBase.PROP_WINDOW_SIZE_MILLIS));
        keySB.append("}");

        StringBuilder valueSB = new StringBuilder();

        long timestamp = offset + startTime;
        valueSB.append("{\"timestamp\":").append(timestamp);
        valueSB.append(",\"timestamp_str\":\"").append(BenchmarkBase.timestampToISO8601(timestamp)).append("\"");
        valueSB.append(",\"latency_ms\":").append(latency);
        valueSB.append(",\"offset_ms\":").append(offset);
        valueSB.append("}");

        return Tuple2.<HazelcastJsonValue, HazelcastJsonValue>
            tuple2(new HazelcastJsonValue(keySB.toString()), new HazelcastJsonValue(valueSB.toString()));
    }


    /**
     * <p>Determine the latency of processing, the difference between the
     * end of the window and now.
     * </p>
     * <p>Uses an accumulator for a pair of "{@code long}" to emit a
     * pair of offset and latency.
     * </p>
     * <p>Disregard any latency measurement in the warm-up phase.
     * </p>
     *
     * @param <T>
     * @param timestampFn
     * @return
     */
    <T> FunctionEx<StreamStage<T>, StreamStage<Tuple2<Long, Long>>> determineLatency(
            FunctionEx<? super T, ? extends Long> timestampFn) {
        return stage -> stage.map(timestampFn).mapStateful(LongLongAccumulator::new, LATENCY_FN);
    }


    /**
     * <p>Take a timestamp {@code long} and convert it into an ISO-8601
     * style string, but drop the millisecond accuracy.
     * </p>
     *
     * @see <a href="https://en.wikipedia.org/wiki/ISO_8601">ISO-8601</a>
     * @param timestamp From "{@code System.currentTimeMillis()}" probably
     * @return Time as a string
     */
    public static String timestampToISO8601(long timestamp) {
        LocalDateTime localDateTime =
                Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime();

        String timestampStr = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(localDateTime);

        if (timestampStr.indexOf('.') > 0) {
            timestampStr = timestampStr.substring(0, timestampStr.indexOf('.'));
        }

        return timestampStr;
    }
}
