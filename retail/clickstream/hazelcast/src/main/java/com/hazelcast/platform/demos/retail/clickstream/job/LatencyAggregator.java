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

package com.hazelcast.platform.demos.retail.clickstream.job;

import java.io.Serializable;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;

import com.hazelcast.jet.aggregate.AggregateOperation;
import com.hazelcast.jet.aggregate.AggregateOperation1;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.datamodel.Tuple3;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Produce two latency aggregations, from published to Pulsar
 * through to predicted, and from ingested from Pulsar through
 * to predicted.
 * </p>
 */
@Slf4j
@Getter
public class LatencyAggregator implements Serializable {
    private static final long serialVersionUID = 1L;

    private String algorithm;
    private long count;
    private long sumPublishDiff;
    private long sumIngestDiff;

    /**
     * <p>Accumulate prediction and derive the average latencies.
     * </p>
     * @return
     */
    public static AggregateOperation1<Entry<String, Tuple3<Long, Long, Long>>,
        LatencyAggregator,
        Entry<String, Tuple2<Float, Float>>> buildLatencyAggregation() {
        return AggregateOperation
                .withCreate(() -> new LatencyAggregator())
                .andAccumulate((LatencyAggregator latencyAggregator, Entry<String, Tuple3<Long, Long, Long>> entry)
                        -> latencyAggregator.accumulate(entry))
                .andCombine(LatencyAggregator::combine)
                .andExportFinish(LatencyAggregator::exportFinish);
    }

    /**
     * <p>Accumulate the latencies
     * </p>
     */
    public void accumulate(Entry<String, Tuple3<Long, Long, Long>> arg0) {
        if (this.algorithm == null) {
            this.algorithm = arg0.getKey();
        }
        this.count++;
        // Publish timestamp, ingest timestamp, prediction timestamp is tuple
        long publishDiff = arg0.getValue().f2() - arg0.getValue().f0();
        long ingestDiff = arg0.getValue().f2() - arg0.getValue().f1();
        if (publishDiff < 0 || ingestDiff < 0) {
            log.error("accumulate() negative time for {}", arg0);
            return;
        }
        this.sumIngestDiff = this.sumIngestDiff + ingestDiff;
        this.sumPublishDiff = this.sumPublishDiff + publishDiff;
    }

    /**
     * <p>Combine by summing
     * </p>
     */
    public void combine(LatencyAggregator that) {
        if (this.algorithm == null) {
            this.algorithm = that.getAlgorithm();
        } else {
            if (!this.algorithm.equals(that.getAlgorithm())) {
                log.error("combine() - this=='{}', that=='{}'", this.algorithm, that.getAlgorithm());
                return;
            }
        }
        this.count += that.getCount();
        this.sumIngestDiff += that.getSumIngestDiff();
        this.sumPublishDiff += that.getSumPublishDiff();
    }

    /**
     * <p>Calculate the averages
     * </p>
     */
    public Entry<String, Tuple2<Float, Float>> exportFinish() {
        if (this.algorithm == null || this.count == 0) {
            log.error("exportFinish() this.algorithm=='{}', this.count=={}", this.algorithm, this.count);
            return null;
        }
        Float averagePublishToPrediction = (0.0F + this.sumPublishDiff) / this.count;
        Float averageIngestToPrediction = (0.0F + this.sumIngestDiff) / this.count;
        Tuple2<Float, Float> tuple2 =
                Tuple2.<Float, Float>tuple2(averagePublishToPrediction, averageIngestToPrediction);
        return new SimpleImmutableEntry<>(this.algorithm, tuple2);
    }

}
