/*
 * Copyright (c) 2008-2021, Hazelcast, Inc. All Rights Reserved.
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
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;

import com.hazelcast.jet.aggregate.AggregateOperation;
import com.hazelcast.jet.aggregate.AggregateOperation1;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.datamodel.Tuple3;

/**
 * <p>An aggregator to summarise CVA Exposures on a per trade basis.
 * Logically this is highly similar to summarising CVA Exposures on a
 * per counterparty basis. However, different fields are captured,
 * so it's difficult to make one class that does both.
 * </p>
 */
public class TradeExposureAggregator implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(TradeExposureAggregator.class);

    private int count;
    private String counterparty;
    private String curvename;
    private double[] cvaexposurebyleg;
    private double[] defaultprob;
    private double netCvaExposure;
    private double[] hazardrates;
    private double[] spreadrates;
    private String tradeid;

    /**
     * <p>Define an aggregator that can uses this class (2nd argument) to process a stream
     * of CVA Exposures (1st argument) and return a single summary object (3rd argument).
     * The summary object is really JSON but passed here as a String for convenience.
     * </p>
     *
     * @return
     */
    public static AggregateOperation1<Tuple3<String, String, String>, TradeExposureAggregator, Tuple2<String, String>>
        buildTradeExposureAggregator() {
        return AggregateOperation
                .withCreate(TradeExposureAggregator::new)
                .andAccumulate((TradeExposureAggregator tmpAggregator, Tuple3<String, String, String> tuple3)
                        -> tmpAggregator.accumulate(tuple3))
                .andCombine(TradeExposureAggregator::combine)
                .andExportFinish(TradeExposureAggregator::exportFinish);
    }

    /**
     * <p>Add the incoming exposure into the current aggregator object. If this
     * is the first seen (count is zero) or first by collating sequence on the
     * curve, capturing specific fields such as hazard rates from that object.
     * For other fields, such as CVA itself, add to the total.
     * </p>
     *
     * @param tuple3 A trio of trade, curve and exposure
     * @return An accumulator object
     */
    public TradeExposureAggregator accumulate(Tuple3<String, String, String> tuple3) {
        try {
            JSONObject exposure = new JSONObject(tuple3.f2());
            JSONArray cvaexposurebylegJson = exposure.getJSONArray("cvaexposurebyleg");

            if (this.count == 0 || this.curvename.compareTo(tuple3.f1()) > 0) {
                this.lowestThis(tuple3.f0(), tuple3.f1(), exposure);

                if (this.count == 0) {
                    this.cvaexposurebyleg = new double[cvaexposurebylegJson.length()];
                }
            }

            for (int i = 0 ; i < this.cvaexposurebyleg.length ; i++) {
                this.cvaexposurebyleg[i] += cvaexposurebylegJson.getDouble(i);
            }

            this.netCvaExposure += exposure.getDouble("cva");
            this.count++;

        } catch (JSONException e) {
            LOGGER.error(tuple3.f0() + "," + tuple3.f1(), e);
        }
        return this;
    }

    /**
     *  <p>Use the incoming JSON object as the basis for setting the
     *  six fields that are taken from the first exposure by
     *  curvename.
     *  </p>
     *
     * @param arg0 Tuple3.f0() - trade
     * @param arg1 Tuple3.f1() - counterparty
     * @param arg2 Tuple3.f2() - exposure
     * @throws JSONException
     */
    private void lowestThis(String arg0, String arg1, JSONObject arg2) throws JSONException {
        this.counterparty = arg2.getString("counterparty");
        this.curvename = arg1;

        JSONArray defaultprobJson = arg2.getJSONArray("defaultprob");
        this.defaultprob = new double[defaultprobJson.length()];
        for (int i = 0 ; i < this.defaultprob.length ; i++) {
            this.defaultprob[i] = defaultprobJson.getDouble(i);
        }

        JSONArray hazardratesJson = arg2.getJSONArray("hazardrates");
        this.hazardrates = new double[hazardratesJson.length()];
        for (int i = 0 ; i < this.hazardrates.length ; i++) {
            this.hazardrates[i] = hazardratesJson.getDouble(i);
        }

        JSONArray spreadratesJson = arg2.getJSONArray("spreadrates");
        this.spreadrates = new double[spreadratesJson.length()];
        for (int i = 0 ; i < this.spreadrates.length ; i++) {
            this.spreadrates[i] = spreadratesJson.getDouble(i);
        }

        this.tradeid = arg0;
    }

    /**
     * <p>The incoming "{@code that}" object is earlier than "{@code this}",
     * so use "{@code that}" as the basis for setting six fields from the
     * earliest record seen.
     * </p>
     *
     * @param that A {@link TradeExposureAggregator} instance.
     */
    private void lowestThat(TradeExposureAggregator that) {
        this.counterparty = that.getCounterparty();
        this.curvename = that.getCurvename();
        this.cvaexposurebyleg = that.getCvaexposurebyleg();
        this.defaultprob = that.getDefaultprob();
        this.hazardrates = that.getHazardrates();
        this.spreadrates = that.getSpreadrates();
        this.tradeid = that.getTradeid();
    }

    public TradeExposureAggregator combine(TradeExposureAggregator that) {
        if (this.count == 0 || this.curvename.compareTo(that.getCurvename()) > 0) {
            this.lowestThat(that);

            if (this.count == 0) {
                this.cvaexposurebyleg = that.getCvaexposurebyleg();
            } else {
                for (int i = 0 ; i < this.cvaexposurebyleg.length ; i++) {
                    this.cvaexposurebyleg[i] += that.getCvaexposurebyleg()[i];
                }
            }
        } else {
            for (int i = 0 ; i < this.cvaexposurebyleg.length ; i++) {
                this.cvaexposurebyleg[i] += that.getCvaexposurebyleg()[i];
            }
        }

        this.netCvaExposure += that.getNetCvaExposure();
        this.count += that.getCount();

        return this;
    }

    /**
     * <p>Input is finished, convert the running totals into averages,
     * and output.
     * <p>
     *
     * @return Counterparty and JSON to send on to the next job stage.
     */
    public Tuple2<String, String> exportFinish() {
        if (count == 0) {
            LOGGER.error("Count 0 for tradeid='" + this.tradeid + "'");
            this.tradeid = "";
            this.counterparty = "";
            this.curvename = "";
            this.cvaexposurebyleg = new double[0];
            this.defaultprob = new double[0];
            this.netCvaExposure = 0;
            this.hazardrates = new double[0];
            this.spreadrates = new double[0];
        } else {
            for (int i = 0; i < this.cvaexposurebyleg.length; i++) {
                this.cvaexposurebyleg[i] = this.cvaexposurebyleg[i] / ((double) this.count);
            }
            this.netCvaExposure = this.netCvaExposure / ((double) count);
        }

        // Curvename is not relevant as the average of all curves, set to empty string rather than omit
        String curvenameNull = "";
        String tradeExposure = CvaStpUtils.makeTradeExposureStrFromJava(this.tradeid, curvenameNull,
                this.counterparty, this.netCvaExposure, this.spreadrates, this.hazardrates, this.defaultprob,
                this.cvaexposurebyleg);

        return Tuple2.tuple2(this.counterparty, tradeExposure);
    }

    // --- Getters, standard coding. Setters not needed ---

    /**
     * <p>Counter</p>
     * @return How many exposures accumulated
     */
    public int getCount() {
        return this.count;
    }

    /**
     * <p>Counterparty</p>
     * @return A string
     */
    public String getCounterparty() {
        return this.counterparty;
    }

    /**
     * <p>Curvename</p>
     * @return A string
     */
    public String getCurvename() {
        return this.curvename;
    }

    /**
     * <p>CVA Exposure by Leg</p>
     * @return Double Array, assume non-zero length
     */
    public double[] getCvaexposurebyleg() {
        if (this.cvaexposurebyleg.length == 0) {
            return new double[0];
        }
        return Arrays.copyOf(this.cvaexposurebyleg, this.cvaexposurebyleg.length);
    }

    /**
     * <p>Default probabilities</p>
     * @return Double Array, assume non-zero length
     */
    public double[] getDefaultprob() {
        if (this.defaultprob.length == 0) {
            return new double[0];
        }
        return Arrays.copyOf(this.defaultprob, this.defaultprob.length);
    }

    /**
     * <p>Hazard rates</p>
     * @return Double Array, assume non-zero length
     */
    public double[] getHazardrates() {
        if (this.hazardrates.length == 0) {
            return new double[0];
        }
        return Arrays.copyOf(this.hazardrates, this.hazardrates.length);
    }

    /**
     * <p>Net CVA Exposure</p>
     * @return Double Array, assume non-zero length
     */
    public double getNetCvaExposure() {
        return this.netCvaExposure;
    }

    /**
     * <p>Spread rates</p>
     * @return Double Array, assume non-zero length
     */
    public double[] getSpreadrates() {
        if (this.spreadrates.length == 0) {
            return new double[0];
        }
        return Arrays.copyOf(this.spreadrates, this.spreadrates.length);
    }

    /**
     * <p>Trade Id</p>
     * @return A string
     */
    public String getTradeid() {
        return this.tradeid;
    }

}
