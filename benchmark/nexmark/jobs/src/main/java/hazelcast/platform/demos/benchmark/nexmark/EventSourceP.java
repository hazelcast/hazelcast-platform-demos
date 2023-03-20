/*
 * Copyright (c) 2008-2023, Hazelcast, Inc. All Rights Reserved.
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

import com.hazelcast.cluster.Address;
import com.hazelcast.function.BiFunctionEx;
import com.hazelcast.jet.core.AbstractProcessor;
import com.hazelcast.jet.core.AppendableTraverser;
import com.hazelcast.jet.core.EventTimePolicy;
import com.hazelcast.jet.core.ProcessorMetaSupplier;
import com.hazelcast.jet.core.ProcessorSupplier;
import com.hazelcast.jet.core.Watermark;
import com.hazelcast.jet.pipeline.Sources;
import com.hazelcast.jet.pipeline.StreamSource;

import javax.annotation.Nonnull;

import static com.hazelcast.jet.impl.JetEvent.jetEvent;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

/**
 * <p>A source that generates generic events,
 * {@link hazelcast.platform.demos.benchmark.nexmark.model.Event}, then
 * each of the tests uses a projection function to expand into
 * {@link Auction}, {@link Bid} or {@link Person}.
 * </p>
 * <p>Error and informational messages are written using "{@code System.out.println}",
 * so inspect the logs for their presence.
 * </p>
 */
public class EventSourceP extends AbstractProcessor {

    private static final long THROUGHPUT_REPORTING_PERIOD_MINUTES = 10;
    private static final long THROUGHPUT_REPORT_PERIOD_NANOS = TimeUnit.MINUTES
            .toNanos(THROUGHPUT_REPORTING_PERIOD_MINUTES);
    private static final long HICCUP_REPORT_THRESHOLD_MILLIS = 10;
    private static final long WM_LAG_THRESHOLD_MILLIS = 20;
    private static final String PREFIX = EventSourceP.class.getSimpleName();

    private final long itemsPerSecond;
    private final long startTimeNanos;
    private final long nanoTimeMillisToCurrentTimeMillis = determineTimeOffset();
    private final long wmGranularity;
    private final long wmOffset;
    private final BiFunctionEx<? super Long, ? super Long, ?> createEventFn;
    private String name;
    private int globalProcessorIndex;
    private int totalParallelism;
    private long emitPeriod;

    private final AppendableTraverser<Object> traverser = new AppendableTraverser<>(2);
    private long emitSchedule;
    private long lastReport;
    private long counterAtLastReport;
    private long lastCallNanos;
    private long counter;
    private long lastEmittedWm;
    private long nowNanos;
    private long warmUpEndMillis;
    private long warmUpEndNanos;

    <T> EventSourceP(long startTimeMillis, long itemsPerSecond, EventTimePolicy<? super T> eventTimePolicy,
            BiFunctionEx<? super Long, ? super Long, ? extends T> createEventFn) {
        this.startTimeNanos = MILLISECONDS.toNanos(startTimeMillis + nanoTimeMillisToCurrentTimeMillis);
        this.warmUpEndMillis = startTimeMillis + BenchmarkBase.WARM_UP_MILLIS;
        System.out.printf(
                "NEXMark.%s:EventSourceP@%s : warm-up without reporting until after %s%n",
                PREFIX, LocalTime.now().toString(),
                new java.util.Date(this.warmUpEndMillis).toString());
        this.warmUpEndNanos = this.startTimeNanos + MILLISECONDS.toNanos(BenchmarkBase.WARM_UP_MILLIS);
        this.itemsPerSecond = itemsPerSecond;
        this.createEventFn = createEventFn;
        wmGranularity = eventTimePolicy.watermarkThrottlingFrameSize();
        wmOffset = eventTimePolicy.watermarkThrottlingFrameOffset();
    }

    @Override
    protected void init(Context context) {
        name = context.vertexName();
        totalParallelism = context.totalParallelism();
        globalProcessorIndex = context.globalProcessorIndex();
        emitPeriod = SECONDS.toNanos(1) * totalParallelism / itemsPerSecond;
        emitSchedule = startTimeNanos + SECONDS.toNanos(1) * globalProcessorIndex / itemsPerSecond;
        lastReport = emitSchedule;
        lastCallNanos = emitSchedule;
    }

    public static <T> StreamSource<T> eventSource(String name, long eventsPerSecond, long initialDelayMs,
            BiFunctionEx<? super Long, ? super Long, ? extends T> createEventFn) {
        return Sources.streamFromProcessorWithWatermarks(name, true,
                eventTimePolicy -> ProcessorMetaSupplier.of((Address ignored) -> {
                    long startTime = System.currentTimeMillis() + initialDelayMs;
                    return ProcessorSupplier
                            .of(() -> new EventSourceP(startTime, eventsPerSecond, eventTimePolicy, createEventFn));
                }));
    }

    private static long determineTimeOffset() {
        long milliTime = System.currentTimeMillis();
        long nanoTime = System.nanoTime();
        return NANOSECONDS.toMillis(nanoTime) - milliTime;
    }

    /**
     * <p>The complete stage produces the events, checks for
     * problems with events being slow, and reports performance.
     * </p>
     *
     * @return False, never exhausted
     */
    @Override
    public boolean complete() {
        nowNanos = System.nanoTime();
        emitEvents();
        detectAndReportHiccup();
        reportThroughput();
        return false;
    }

    /**
     * <p>Create {@link Event} object using the provided function</p>
     */
    private void emitEvents() {
        if (!emitFromTraverser(traverser)) {
            return;
        }
        if (emitSchedule > nowNanos) {
            maybeEmitWm(nanoTimeToCurrentTimeMillis(nowNanos));
            emitFromTraverser(traverser);
            return;
        }
        do {
            long timestamp = nanoTimeToCurrentTimeMillis(emitSchedule);
            long seq = counter * totalParallelism + globalProcessorIndex;
            Object event = createEventFn.apply(seq, timestamp);
            traverser.append(jetEvent(timestamp, event));
            counter++;
            emitSchedule += emitPeriod;
            maybeEmitWm(timestamp);
        } while (emitFromTraverser(traverser) && emitSchedule <= nowNanos);
    }

    /**
     * <p>Insert watermarks (timestamps) into the event stream.</p>
     * <p>With error reporting.</p>
     * <p>If late, report a problem to the log.
     * </p>
     */
    private void maybeEmitWm(long timestamp) {
        if (timestamp < lastEmittedWm + wmGranularity) {
            return;
        }
        long wmToEmit = timestamp - (timestamp % wmGranularity) + wmOffset;
        long nowMillis = nanoTimeToCurrentTimeMillis(nowNanos);
        long wmLag = nowMillis - wmToEmit;
        if (wmLag > WM_LAG_THRESHOLD_MILLIS && nowMillis > this.warmUpEndMillis) {
            System.out.printf(
                    "NEXMark.%s:maybeEmitWm@%s : %s#%d => %,d behind real time%n",
                    PREFIX, LocalTime.now().toString(),
                    name, globalProcessorIndex,
                    wmLag);
        }
        traverser.append(new Watermark(wmToEmit));
        lastEmittedWm = wmToEmit;
    }

    /**
     * <p>Error reporting.</p>
     * <p>If late, report a problem to the log.
     * </p>
     */
    private void detectAndReportHiccup() {
        long millisSinceLastCall = NANOSECONDS.toMillis(nowNanos - lastCallNanos);
        if (millisSinceLastCall > HICCUP_REPORT_THRESHOLD_MILLIS && nowNanos > this.warmUpEndNanos) {
            System.out.printf(
                    "NEXMark.%s:detectAndReportHiccup@%s : %s#%d => %,d ms%n",
                    PREFIX, LocalTime.now().toString(),
                    name, globalProcessorIndex,
                    millisSinceLastCall);
        }
        lastCallNanos = nowNanos;
    }

    /**
     * <p>Informational reporting.</p>
     * <p>Periodically report throughput, per processor. With multiple processors it is difficult
     * in this module to determine if the total throughput is the same as the requested level. To
     * do this, use {@link SourceBenchmark}.</p>
     * <p>Throughput is reported machine readable and human readable. For the former, wish
     * to sum all processors for the same timestamp to determine overall throughput.
     * </p>
     */
    private void reportThroughput() {
        long nanosSinceLastReport = nowNanos - lastReport;
        if (nanosSinceLastReport < THROUGHPUT_REPORT_PERIOD_NANOS) {
            return;
        }
        lastReport = nowNanos;
        long itemCountSinceLastReport = counter - counterAtLastReport;
        counterAtLastReport = counter;
        double throughput = itemCountSinceLastReport / ((double) nanosSinceLastReport
                / BenchmarkBase.ONE_SECOND_AS_NANOS);
        if (nowNanos > this.warmUpEndNanos) {
            System.out.printf(
                    "NEXMark.%s:reportThroughput@%s : %s#%d => %d items in %d seconds => %,.0f items/second%n",
                    PREFIX, LocalTime.now().toString(),
                    name, globalProcessorIndex,
                    itemCountSinceLastReport, TimeUnit.NANOSECONDS.toSeconds(nanosSinceLastReport),
                    throughput);
        }
    }

    private long nanoTimeToCurrentTimeMillis(long nanoTime) {
        return NANOSECONDS.toMillis(nanoTime) - nanoTimeMillisToCurrentTimeMillis;
    }

    @Override
    public boolean tryProcessWatermark(@Nonnull Watermark watermark) {
        throw new UnsupportedOperationException("Source processor shouldn't be asked to process a watermark");
    }
}
