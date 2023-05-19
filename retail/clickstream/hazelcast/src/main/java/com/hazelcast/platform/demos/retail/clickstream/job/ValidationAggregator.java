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
import com.hazelcast.jet.datamodel.Tuple2;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Produce a correctness percentage for an algorithm.
 * </p>
 */
@Getter
@Slf4j
public class ValidationAggregator implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final double ONE_HUNDRED = 100D;

    private final String modelName;
    private long count;
    private long correctCount;

    public ValidationAggregator(String arg0) {
        this.modelName = arg0;
        this.count = 0;
        this.correctCount = 0;
    }

    /**
     * <p>An aggregator that summarises the effectiveness of predictions against reality.
     * </p>
     * @return
     */
    public static AggregateOperation1<Tuple2<Integer, Integer>, ValidationAggregator, Entry<String, Double>>
        buildValidationAggregation(String arg0) {
        return AggregateOperation
                .withCreate(() -> new ValidationAggregator(arg0))
                .andAccumulate((ValidationAggregator validationAggregator, Tuple2<Integer, Integer> tuple2)
                        -> validationAggregator.accumulate(tuple2))
                .andCombine(ValidationAggregator::combine)
                .andExportFinish(ValidationAggregator::exportFinish);
    }

    /**
     * <p>Update tallies
     * </p>
     *
     * @param arg0
     */
    public void accumulate(Tuple2<Integer, Integer> tuple2) {
        this.count++;
        if (tuple2.f0() == tuple2.f1()) {
            this.correctCount++;
        }
    }

    /**
     * <p>Combine accumulators.
     * </p>
     *
     * @param that
     */
    public void combine(ValidationAggregator that) {
        this.correctCount += that.getCorrectCount();
        this.count += that.getCount();
    }

    /**
     * <p>Format result
     * </p>
     *
     * @return
     */
    public Entry<String, Double> exportFinish() {
        if (this.count == 0) {
            log.error("exportFinish() '{}': this.count=={}",
                    this.modelName, this.count);
            return null;
        }
        // Percentage
        double effectiveness = (ONE_HUNDRED * this.correctCount) / this.count;
        return new SimpleImmutableEntry<>(this.modelName, effectiveness);
    }

}
