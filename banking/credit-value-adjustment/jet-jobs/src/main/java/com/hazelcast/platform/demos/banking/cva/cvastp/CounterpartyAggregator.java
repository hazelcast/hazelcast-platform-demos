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

package com.hazelcast.platform.demos.banking.cva.cvastp;

import java.io.Serializable;
import java.util.Map.Entry;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.jet.aggregate.AggregateOperation;
import com.hazelcast.jet.aggregate.AggregateOperation1;
import com.hazelcast.jet.datamodel.Tuple2;

/**
 * <p>Collate the CVA totals per counterparty.
 * </p>
 */
public class CounterpartyAggregator implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(CounterpartyAggregator.class);

    private double cva;

    /**
     * <p>Helper function to build this class into an aggregate operation.
     * </p>
     *
     * @return An {@code AggregateOperation1} that works on a single input source.
     */
    public static AggregateOperation1<Entry<String, Tuple2<String, String>>, CounterpartyAggregator, Double>
        buildCounterpartyAggregation() {
        return AggregateOperation
                .withCreate(CounterpartyAggregator::new)
                .andAccumulate((CounterpartyAggregator cvaByCounterpartyAggregator,
                        Entry<String, Tuple2<String, String>> entry)
                        -> cvaByCounterpartyAggregator.accumulate(entry.getValue()))
                .andCombine(CounterpartyAggregator::combine)
                .andExportFinish(CounterpartyAggregator::exportFinish);
    }

    /**
     * <p>Sum the "{@code cva}" field. As we're capturing other input, we
     * can't use {@link com.hazelcast.jet.AggregateOperations.html#summingDouble}
     * built-in.
     * </p>
     *
     * @param tuple2 A pair of counterparty and exposure JSON
     * @return The current accumulator
     */
    public CounterpartyAggregator accumulate(Tuple2<String, String> tuple2) {
        try {
            JSONObject cds = new JSONObject(tuple2.f1());
            this.cva += cds.getDouble("cva");

        } catch (JSONException e) {
            LOGGER.error(tuple2.f0(), e);
        }

        return this;
    }

    /**
     * <p>Merge in another running total for the current key.
     * </p>
     *
     * @param that Another instance of this class, tracking the same key
     * @return The combination updated.
     */
    public CounterpartyAggregator combine(CounterpartyAggregator that) {
        this.cva += that.getCva();
        return this;
    }

    /**
     * <p>Produce the total, all input exhausted.
     * </p>
     *
     * @return Counterparty and it's exposure
     */
    public Double exportFinish() {
        return this.cva;
    }

    // --- Getters, standard coding. Setters not needed ---

    /**
     * <p>CVA</p>
     * @return A double
     */
    public Double getCva() {
        return this.cva;
    }

}
