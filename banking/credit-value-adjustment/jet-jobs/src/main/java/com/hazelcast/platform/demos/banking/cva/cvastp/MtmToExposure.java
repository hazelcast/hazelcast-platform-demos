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

package com.hazelcast.platform.demos.banking.cva.cvastp;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.function.BiFunctionEx;
import com.hazelcast.jet.datamodel.Tuple3;

/**
 * <p>Converts a Mark-To-Market record into an Exposure,
 * using some information from the original Trade.
 * </p>
 */
public class MtmToExposure {

    private static final Logger LOGGER = LoggerFactory.getLogger(MtmToExposure.class);
    private static final double HALF = 0.5d;

    /**
     * <p>A function that takes two inputs:
     * <ol>
     * <li>"{@code Tuple3<String, String, String>}" of TradeId, Curve Name and MTM.
     * </li>
     * <li>"{@code HazelcastJsonValue}" holding a Trade.
     * </li>
     * </ol>
     * and produces one output:
     * <ol>
     * <li>"{@code Tuple3<String, String, String>}" of TradeId, Curve Name and Exposure.
     * </li>
     * </ol>
     */
    public static final BiFunctionEx<Tuple3<String, String, String>, HazelcastJsonValue, Tuple3<String, String, String>> CONVERT =
            (Tuple3<String, String, String> mtm, HazelcastJsonValue trade) -> {
                try {
                    // Fields needed from the Trade
                    JSONObject tradeJson = new JSONObject(trade.toString());
                    int payerReceiverFlag = tradeJson.getInt("payer_receiver_flag");
                    String counterparty = tradeJson.getString("counterparty");

                    // Fields needed from the MTM
                    JSONObject mtmJson = new JSONObject(mtm.f2());
                    JSONArray fixlegamount = mtmJson.getJSONArray("fixlegamount");
                    JSONArray fltlegamount = mtmJson.getJSONArray("fltlegamount");
                    if (fixlegamount.length() == 0 || fixlegamount.length() != fltlegamount.length()) {
                        throw new RuntimeException("fixlegamount/fltlegamount wrong");
                    }

                    // Business logic
                    double[] exposures = calculateExposures(payerReceiverFlag, fixlegamount, fltlegamount);

                    // Format for output
                    String exposureStr = CvaStpUtils.makeExposureStrFromJson(mtm.f0(), mtm.f1(), counterparty,
                            exposures, mtmJson);

                    return Tuple3.tuple3(mtm.f0(), mtm.f1(), exposureStr);
                } catch (RuntimeException e) {
                    LOGGER.error(mtm.f0() + "," + mtm.f1(), e);
                    return null;
                }
            };

    /**
     * <p>Business logic to calculate the exposure array.
     * </p>
     *
     * @param payerReceiverFlag 0 or more usually 1
     * @param fixlegamount Fixed leg amounts
     * @param fltlegamount Floating leg amounts
     * @return An array of exposures as doubles
     * @throws JSONException If param 2 or 3 is corrupt
     */
    protected static double[] calculateExposures(int payerReceiverFlag, JSONArray fixlegamount, JSONArray fltlegamount)
        throws JSONException {

        /* TODO: ASSUMPTION - We assume fixed and float payments of the same frequency
         * A payment is the next value of the amount in the floating leg minus the amount in the fixed leg
         * for a payer swap (if receiver swap then reverse). For example:
         * Assume four fixed leg payments as [0985.83, 0985.83, 1018.33, 0985.83]
         * Assume four float leg payments as [0101.11, 1162.66, 1205.57, 1622.08]
         * Assume payer receiver flag is 1 (i.e payer swap), then the payment will be
         *                                   [-884.72, 176.82, 187.24, 636.25]
         */
        List<Double> payments = new ArrayList<>();
        for (int i = 0; i < fixlegamount.length(); i++) {
            payments.add(payerReceiverFlag * (fltlegamount.getDouble(i) - fixlegamount.getDouble(i)));
        }

        /* Sum the payments as the date advances (across the swap legs).
         * SUM [-884.72, 176.82, 187.24, 636.25] = 115.59
         * SUM [176.82, 187.24, 636.25] = 1000.31
         * SUM [187.24, 636.25] = 823.49
         * SUM [636.25] = 636.25
         * Add a final value of zero at the end (since the payment becomes zero beyond settlement)
         */
        double[] sumpayments = new double[payments.size() + 1];
        double total = 0.0;
        for (int i = payments.size(); i > 0; i--) {
            total += payments.get(i - 1);
            sumpayments[i - 1] = total;
        }
        sumpayments[payments.size()] = 0.0;

        /* If we are out of the money - exposure would be negative, so take max of value or zero
         */
        sumpayments[payments.size()] = 0.0;
        for (int i = 0; i < sumpayments.length; i++) {
            sumpayments[i] = Math.max(sumpayments[i], 0.0);
        }

        /* The final exposures is determined by taking half the amounts from each legs
         * 1st exposure = 0.5 * 0115.59 + 0.5 * 1000.31 = 557.95
         * 2nd exposure = 0.5 * 1000.31 + 0.5 * 0823.49 = 911.90
         * 3rd exposure = 0.5 * 0823.49 + 0.5 * 0636.25 = 729.87
         * 4th exposure = 0.5 * 0636.25 + 0.5 * 0000.00 = 318.13
         */
        double[] exposures = new double[sumpayments.length - 1];
        for (int i = 1; i < sumpayments.length; i++) {
            exposures[i - 1] = (sumpayments[i] + sumpayments[i - 1]) * HALF;
        }

        return exposures;
    }

}
