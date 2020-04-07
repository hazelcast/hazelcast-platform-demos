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

package com.hazelcast.platform.demos.banking.cva.cvastp;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;

import com.hazelcast.jet.aggregate.AggregateOperation;
import com.hazelcast.jet.aggregate.AggregateOperation1;
import com.hazelcast.jet.datamodel.Tuple4;

/**
 * <p>Collate the CVA totals per counterparty.
 * </p>
 */
public class CvaByCounterpartyTotalizer implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(CvaByCounterpartyTotalizer.class);

    private double cva;
    private String counterparty;
    private String shortname;

    /**
     * <p>Helper function to build this class into an aggregate operation.
     * </p>
     *
     * @return An {@code AggregateOperation1} that works on a single input source.
     */
    public static AggregateOperation1<Tuple4<String, String, String, String>, CvaByCounterpartyTotalizer, String>
        buildCvaByCounterpartyAggregation() {
        return AggregateOperation
                .withCreate(CvaByCounterpartyTotalizer::new)
                .andAccumulate((CvaByCounterpartyTotalizer exposureAverager,
                        Tuple4<String, String, String, String> tuple4)
                        -> exposureAverager.accumulate(tuple4))
                .andCombine(CvaByCounterpartyTotalizer::combine)
                .andDeduct(CvaByCounterpartyTotalizer::deduct)
                .andExportFinish(CvaByCounterpartyTotalizer::exportFinish);
    }

    /**
     * <p>Sum the "{@code cva}" field. As we're capturing other input, we
     * can't use {@link com.hazelcast.jet.AggregateOperations.html#summingDouble}
     * built-in.
     * </p>
     *
     * @param tuple4 A quadruple of Trade, Counterparty code and name, and CDS
     * @return The current accumulator
     */
    public CvaByCounterpartyTotalizer accumulate(Tuple4<String, String, String, String> tuple4) {
        try {
            JSONObject cds = new JSONObject(tuple4.f3());
            this.cva += cds.getDouble("cva");
            this.counterparty = tuple4.f1();
            this.shortname = tuple4.f2();

        } catch (JSONException e) {
            LOGGER.error(tuple4.f0() + "," + tuple4.f1(), e);
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
    public CvaByCounterpartyTotalizer combine(CvaByCounterpartyTotalizer that) {
        this.cva += that.getCva();
        if (this.counterparty == null) {
            this.counterparty = that.getCounterparty();
        }
        if (this.shortname == null) {
            this.shortname = that.getShortname();
        }

        return this;
    }

    /**
     * <p>Merge out another running total for the current key.
     * </p>
     *
     * @param that Another instance of this class, tracking the same key
     * @return The combination downdated.
     */
    public CvaByCounterpartyTotalizer deduct(CvaByCounterpartyTotalizer that) {
        this.cva -= that.getCva();
        return this;
    }

    /**
     * <p>Produce the total, all input exhausted.
     * </p>
     *
     * @return CSV
     */
    public String exportFinish() {
        return this.counterparty + "," + this.shortname + "," + this.cva;
    }

    // --- Getters, standard coding. Setters not needed ---

    /**
     * <p>Counterparty</p>
     * @return A string
     */
    public String getCounterparty() {
        return counterparty;
    }

    /**
     * <p>CVA</p>
     * @return A double
     */
    public Double getCva() {
        return cva;
    }

    /**
     * <p>Shortname</p>
     * @return A string
     */
    public String getShortname() {
        return shortname;
    }

}
