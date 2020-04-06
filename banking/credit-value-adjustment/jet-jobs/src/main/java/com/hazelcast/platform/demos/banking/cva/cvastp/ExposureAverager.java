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
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;

import com.hazelcast.jet.aggregate.AggregateOperation;
import com.hazelcast.jet.aggregate.AggregateOperation1;
import com.hazelcast.jet.datamodel.Tuple3;

/**
 * <p>We wish to aggregate all Exposures for a Trade together, and calculate
 * the average exposure.
 * <p>
 */
@SuppressWarnings("serial")
public class ExposureAverager implements Serializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExposureAverager.class);

    private int count;
    private String counterparty;
    private String curvename;
    private double[] exposures;
    private double[] discountfactors;
    private double[] legfractions;
    private String tradeid;

    /**
     * <p>Helper function to build this class into an aggregate operation.
     * </p>
     *
     * @return An {@code AggregateOperation1} that works on a single input source.
     */
    public static AggregateOperation1<Tuple3<String, String, String>, ExposureAverager, String> buildExposureAggregation() {
        return AggregateOperation
                .withCreate(ExposureAverager::new)
                .andAccumulate((ExposureAverager exposureAverager, Tuple3<String, String, String> tuple3)
                        -> exposureAverager.accumulate(tuple3))
                .andCombine(ExposureAverager::combine)
                .andDeduct(ExposureAverager::deduct)
                .andExportFinish(ExposureAverager::exportFinish);
    }

    /**
     * <p>Add one set of exposure values to the running total.
     * If the first set of exposure values, capture values that
     * will be the same across all {@link #accumulate(Tuple3))} calls
     * for this class instance.
     * </p>
     *
     * @param tuple3 A trio of Trade, Curve and Exposure
     * @return The current accumulator
     */
    public ExposureAverager accumulate(Tuple3<String, String, String> tuple3) {
        try {
            JSONObject exposure = new JSONObject(tuple3.f2());
            JSONArray exposuresJson = exposure.getJSONArray("exposures");

            if (this.count == 0) {
                this.firstTimeThis(tuple3.f0(), tuple3.f1(), exposure);

                this.exposures = new double[exposuresJson.length()];
                for (int i = 0 ; i < exposures.length ; i++) {
                    exposures[i] = exposuresJson.getDouble(i);
                }
            } else {
                for (int i = 0 ; i < exposures.length ; i++) {
                    exposures[i] += exposuresJson.getDouble(i);
                }
            }

            this.count++;

        } catch (JSONException e) {
            LOGGER.error(tuple3.f0() + "," + tuple3.f1(), e);
        }
        return this;
    }

    /**
     * <p>All items fed to this accumulator have the same common
     * fields, only capture these once.
     * </p>
     *
     * @param arg0 Tuple3.f0()
     * @param arg1 Tuple3.f2()
     */
    private void firstTimeThis(String arg0, String arg1, JSONObject arg2) throws JSONException {
        this.tradeid = arg0;
        this.curvename = arg1;
        this.counterparty = arg2.getString("counterparty");

        JSONArray legfractionsJson = arg2.getJSONArray("legfractions");
        this.legfractions = new double[legfractionsJson.length()];
        for (int i = 0 ; i < legfractions.length ; i++) {
            legfractions[i] = legfractionsJson.getDouble(i);
        }

        JSONArray discountfactorsJson = arg2.getJSONArray("discountfactors");
        this.discountfactors = new double[discountfactorsJson.length()];
        for (int i = 0 ; i < discountfactors.length ; i++) {
            discountfactors[i] = discountfactorsJson.getDouble(i);
        }
    }

    /**
     * <p>Equivalent to {@link firstTimeThis} but take the fields
     * from the incoming accumulator object.
     * </p>
     *
     * @param that An existing {@link ExposureAverager} instance
     */
    private void firstTimeThat(ExposureAverager that) {
        this.count = that.getCount();
        this.counterparty = that.getCounterparty();
        this.curvename = that.getCurvename();
        this.discountfactors = that.getDiscountfactors();
        this.exposures = that.getExposures();
        this.legfractions = that.getLegfractions();
        this.tradeid = that.getTradeid();
    }

    /**
     * <p>Merge in another running total for the current key.
     * </p>
     *
     * @param that Another instance of this class, tracking the same key
     * @return The combination updated.
     */
    public ExposureAverager combine(ExposureAverager that) {
        if (this.count == 0) {
            this.firstTimeThat(that);
        } else {
            this.count += that.getCount();

            for (int i = 0 ; i < exposures.length ; i++) {
                exposures[i] += that.getExposures()[i];
            }
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
    public ExposureAverager deduct(ExposureAverager that) {
        this.count -= that.count;

        for (int i = 0 ; i < exposures.length ; i++) {
            exposures[i] -= that.getExposures()[i];
        }

        return this;
    }

    /**
     * <p>Average the running totals, and so produce the average exposure.
     * </p>
     *
     * @return A String that can be converted to JSON
     */
    public String exportFinish() {
        if (count == 0) {
            LOGGER.error("Count 0 for tradeid='" + this.tradeid + "',curvename='" + this.curvename + "'");
            this.tradeid = "";
            this.counterparty = "";
            this.curvename = "";
            this.discountfactors = new double[0];
            this.exposures = new double[0];
            this.legfractions = new double[0];
        } else {
            for (int i = 0; i < this.exposures.length; i++) {
                exposures[i] = exposures[i] / ((double) this.count);
            }
        }

        return CvaStpUtils.makeExposureStrFromJava(this.tradeid, this.curvename, this.counterparty,
                this.exposures, this.legfractions, this.discountfactors);
    }

    // --- Getters, standard coding. Setters not needed ---

    /**
     * <p>Counter</p>
     * @return How many exposures accumulated
     */
    public int getCount() {
        return count;
    }

    /**
     * <p>Counterparty</p>
     * @return A string
     */
    public String getCounterparty() {
        return counterparty;
    }

    /**
     * <p>Curvename, only needed for logging exceptions</p>
     * @return A string
     */
    public String getCurvename() {
        return curvename;
    }

    /**
     * <p>Discount Factors</p>
     * @return Double Array, assume non-zero length
     */
    public double[] getDiscountfactors() {
        return discountfactors;
    }

    /**
     * <p>Exposures</p>
     * @return Double Array, assume non-zero length
     */
    public double[] getExposures() {
        return exposures;
    }

    /**
     * <p>Leg Fractions</p>
     * @return Double Array, assume non-zero length
     */
    public double[] getLegfractions() {
        return legfractions;
    }

    /**
     * <p>Trade Id</p>
     * @return A string
     */
    public String getTradeid() {
        return tradeid;
    }

}
