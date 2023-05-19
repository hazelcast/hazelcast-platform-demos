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

package com.hazelcast.platform.demos.retail.clickstream.job;

import java.io.Serializable;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;

import com.hazelcast.jet.aggregate.AggregateOperation;
import com.hazelcast.jet.aggregate.AggregateOperation1;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Produce two cy aggregations, from published to Pulsar
 * through to predicted, and from ingested from Pulsar through
 * to predicted.
 * </p>
 */
@Slf4j
@Getter
public class AccuracyAggregator implements Serializable {
    private static final long serialVersionUID = 1L;

    private String algorithm;
    private long count;
    private long sumCorrect;

    public AccuracyAggregator(String arg0) {
        this.algorithm = arg0;
    }

    /**
     * <p>An aggregator for a stream of prediction values (0=no buy, 1=buy)
     * for customers that did buy.
     * </p>
     * @return
     */
    public static AggregateOperation1<Entry<String, Integer>,
        AccuracyAggregator,
        Entry<String, Float>> builAccuracyAggregation(String algorithm) {
        return AggregateOperation
                .withCreate(() -> new AccuracyAggregator(algorithm))
                .andAccumulate((AccuracyAggregator accuracyAggregator, Entry<String, Integer> entry)
                        -> accuracyAggregator.accumulate(entry))
                .andCombine(AccuracyAggregator::combine)
                .andExportFinish(AccuracyAggregator::exportFinish);
    }

    /**
     * <p>Accumulate the accuracies.
     * </p>
     */
    public void accumulate(Entry<String, Integer> entry) {
        this.count++;
        if (entry.getValue() == null || entry.getValue() < 0 || entry.getValue() > 1) {
            log.error("accumulate('{}') unknown prediction for key '{}' of  {}",
                    this.algorithm, entry.getKey(), entry.getValue());
        } else {
            this.sumCorrect += entry.getValue();
        }
    }

    /**
     * <p>Combine by summing
     * </p>
     */
    public void combine(AccuracyAggregator that) {
        this.count += that.getCount();
        this.sumCorrect += that.getSumCorrect();
    }

    /**
     * <p>Calculate the averages
     * </p>
     */
    public Entry<String, Float> exportFinish() {
        if (this.count == 0) {
            log.error("exportFinish() this.algorithm=='{}', this.count=={}", this.algorithm, this.count);
            return null;
        }
        Float averageCorrect = (0.0F + this.sumCorrect) / this.count;
        log.trace("exportFinish() this.algorithm=='{}', this.count=={} this.sumCorrect=={} AVG {}",
                this.algorithm, this.count, this.sumCorrect, averageCorrect);
        return new SimpleImmutableEntry<>(this.algorithm, averageCorrect);
    }

}
