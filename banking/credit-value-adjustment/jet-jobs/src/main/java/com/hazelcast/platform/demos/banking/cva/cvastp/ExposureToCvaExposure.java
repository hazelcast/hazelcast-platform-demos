/*
 * Copyright (c) 2008-2022, Hazelcast, Inc. All Rights Reserved.
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
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.function.BiFunctionEx;
import com.hazelcast.function.FunctionEx;
import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.platform.demos.banking.cva.MyConstants;

/**
 * <p>Methods to take an Exposure object and Counterparty CDS to calculate
 * the resultant CVA Exposure.
 * </p>
 */
public class ExposureToCvaExposure {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExposureToCvaExposure.class);

    /**
     * <p>Find the lookup key for the map "{@code cp_cds}". The field is
     * known as "{@code ticker}", but it's the counterparty code in the
     * incoming exposure object.
     * </p>
     */
    public static final FunctionEx<String, String> GET_TICKER_FROM_EXPOSURE =
            (String exposure) -> {
                try {
                    return new JSONObject(exposure).getString("counterparty");
                } catch (Exception e) {
                    if (exposure.length() > MyConstants.HALF_SCREEN_WIDTH) {
                        LOGGER.error("No counterparty field: " + exposure.substring(0, MyConstants.HALF_SCREEN_WIDTH), e);
                    } else {
                        LOGGER.error("No counterparty field: " + exposure, e);
                    }
                    return "";
                }
            };

    /**
     * <p>A {@link BiFunctionEx}, so takes two objects and returns a third.
     * </p>
     * <p>Take the Exposure (first argument) and the Counterparty CDS (second argument),
     * and turn this into a CVA Exposure.
     * </p>
     * <p>Try to keep the JSON processing and business logic separate.
     * </p>
     * <p>The return object is trio of the Trade Id, Curve, and the CDS.
     * </p>
     */
    public static final BiFunctionEx<String, HazelcastJsonValue, Tuple3<String, String, String>> CONVERT =
           (String exposure, HazelcastJsonValue cpCds) -> {
               try {
                   // Extract necessary fields
                   JSONObject exposureJson = new JSONObject(exposure);
                   String tradeid = exposureJson.getString("tradeid");
                   String counterparty = exposureJson.getString("counterparty");
                   String curvename = exposureJson.getString("curvename");
                   JSONArray exposuresJson = exposureJson.getJSONArray("exposures");
                   JSONArray discountFactorsJson = exposureJson.getJSONArray("discountfactors");
                   JSONArray legFractionsJson = exposureJson.getJSONArray("legfractions");

                   JSONObject cpCdsJson = new JSONObject(cpCds.toString());
                   JSONArray spreadsJson = cpCdsJson.getJSONArray("spreads");
                   JSONArray spreadPeriodsJson = cpCdsJson.getJSONArray("spread_periods");
                   float recovery = (float) cpCdsJson.getDouble("recovery");

                   // Convert from JSON
                   List<Double> discountFactors = new ArrayList<>();
                   for (int i = 0 ; i < discountFactorsJson.length(); i++) {
                       discountFactors.add(discountFactorsJson.getDouble(i));
                   }
                   List<Double> exposures = new ArrayList<>();
                   for (int i = 0 ; i < exposuresJson.length(); i++) {
                       exposures.add(exposuresJson.getDouble(i));
                   }
                   List<Double> legFractions = new ArrayList<>();
                   for (int i = 0 ; i < legFractionsJson.length(); i++) {
                       legFractions.add(legFractionsJson.getDouble(i));
                   }
                   List<Float> spreads = new ArrayList<>();
                   for (int i = 0 ; i < spreadsJson.length(); i++) {
                       spreads.add((float) spreadsJson.getDouble(i));
                   }
                   List<Float> spreadPeriods = new ArrayList<>();
                   for (int i = 0 ; i < spreadPeriodsJson.length(); i++) {
                       spreadPeriods.add((float) spreadPeriodsJson.getDouble(i));
                   }

                   // Business logic
                   List<Double> spreadRates = getSpreadRates(spreads, spreadPeriods, legFractions);
                   List<Double> hazardRates = getHazardRates(spreadRates, legFractions, recovery);
                   List<Double> defaultProbabilities = getDefaultProbabilities(hazardRates, legFractions);
                   List<Double> cvaExposureByLeg =
                           getCvaExposureByLeg(defaultProbabilities, exposures, discountFactors, recovery);
                   double cvaExposure = getCvaExposureVal(cvaExposureByLeg);

                   // Format for output.
                   StringBuilder stringBuilder = new StringBuilder();
                   stringBuilder.append("{");
                   stringBuilder.append(" \"tradeid\": \"" + tradeid + "\"");
                   stringBuilder.append(", \"curvename\": \"" + curvename + "\"");
                   stringBuilder.append(", \"counterparty\": \"" + counterparty + "\"");
                   stringBuilder.append(", \"cva\": " + cvaExposure);
                   stringBuilder.append(formatDoubleArrayForJSON(",", "spreadrates", spreadRates));
                   stringBuilder.append(formatDoubleArrayForJSON(",", "hazardrates", hazardRates));
                   stringBuilder.append(formatDoubleArrayForJSON(",", "defaultprob", defaultProbabilities));
                   stringBuilder.append(formatDoubleArrayForJSON(",", "cvaexposurebyleg", cvaExposureByLeg));
                   stringBuilder.append(" }");

                   return Tuple3.tuple3(tradeid, curvename, stringBuilder.toString());
               } catch (RuntimeException e) {
                   if (exposure.length() > MyConstants.HALF_SCREEN_WIDTH) {
                       if (cpCds.toString().length() > MyConstants.HALF_SCREEN_WIDTH) {
                           LOGGER.error("No counterparty field: " + exposure.substring(0, MyConstants.HALF_SCREEN_WIDTH)
                           + "," + cpCds.toString().substring(0, MyConstants.HALF_SCREEN_WIDTH), e);
                   } else {
                       LOGGER.error("No counterparty field: " + exposure.substring(0, MyConstants.HALF_SCREEN_WIDTH)
                           + "," + cpCds, e);
                       }
                   } else {
                       LOGGER.error("No counterparty field: " + exposure + "," + cpCds, e);
                   }
                   return Tuple3.tuple3("", "", "");
               }
            };

    /**
     * <p>Find an key's index in a list.
     * <p>
     *
     * @param list A sorted list
     * @param key Item to find in the list
     * @return Position in list
     */
    public static int bisectLeft(List<Double> list, double key) {
        int idx = Math.min(list.size(), Math.abs(Collections.binarySearch(list, key)));
        while (idx > 0 && list.get(idx - 1) >= key) {
            idx--;
        }
        return idx;
    }

    /**
     * <p>Get the spread rates
     * </p>
     *
     * @param spreads
     * @param spreadPeriods
     * @param legFractions
     * @return
     */
    public static List<Double> getSpreadRates(List<Float> spreads, List<Float> spreadPeriods, List<Double> legFractions) {
        /* CDS Spread and periods for the counterparty
         */
        List<Double> cdsPeriods = new ArrayList<>();
        for (int i = 0 ; i < spreadPeriods.size() ; i++) {
            cdsPeriods.add((double) spreadPeriods.get(i));
        }
        List<Double> cdsSpreads = new ArrayList<>();
        for (int i = 0 ; i < spreads.size() ; i++) {
            cdsSpreads.add((double) spreads.get(i));
        }

       /* Use left bisection to get cds rate for the leg fractions
        */
       List<Double> spreadRates = new ArrayList<>();
       for (int i = 0 ; i < legFractions.size(); i++) {
           double fraction = legFractions.get(i);
           spreadRates.add(cdsSpreads.get(bisectLeft(cdsPeriods, fraction)));
       }

       return spreadRates;
    }

    /**
     * <p>For hazard rate use simplified Hull equation:
     * <pre>hazard rate = s / (1 - r)</pre>
     * </p>
     *
     * @param spreadRates
     * @param legFractions
     * @param recoveryRate
     * @return
     */
    public static List<Double> getHazardRates(List<Double> spreadRates, List<Double> legFractions,
            double recoveryRate) {

        List<Double> hazardRates = new ArrayList<>();
         for (double spreadRate : spreadRates) {
             hazardRates.add(spreadRate / (1 - recoveryRate));
         }

         return hazardRates;
    }

    /**
     * <p>From Hazard Rates calculate default probabilities
     * </p>
     *
     * @param hazardRates
     * @param legFractions
     * @return
     */
    public static List<Double> getDefaultProbabilities(List<Double> hazardRates, List<Double> legFractions) {

        List<Double> defaultProbabilities = new ArrayList<>();
        if (hazardRates.size() > 0) {
            //strictly MTM has errored - so this check should not occur
            double defaultProbability =
                   (1.0 - Math.exp(-1.0d * hazardRates.get(0) * legFractions.get(0)));
            defaultProbabilities.add(defaultProbability);
            for (int i = 1; i < legFractions.size(); i++) {
               defaultProbability =
                      Math.exp(-1.0 * hazardRates.get(i - 1) * legFractions.get(i - 1))
                              - Math.exp(-1.0 * hazardRates.get(i) * legFractions.get(i));
               defaultProbabilities.add(defaultProbability);
            }
        }

        return defaultProbabilities;
    }

    /**
     * <p>Calculate CVA exposure across the leg for this trade &amp; scenario
     * </p>
     *
     * @param defaultProbabilities
     * @param exposures
     * @param discountFactors
     * @param recoveryRate
     * @return
     */
    public static List<Double> getCvaExposureByLeg(List<Double> defaultProbabilities, List<Double> exposures,
            List<Double> discountFactors, double recoveryRate) {

        List<Double> cvaExposureByLeg = new ArrayList<>();
        double val;
        for (int i = 0; i < defaultProbabilities.size(); i++) {
            val = (exposures.get(i)
                   * discountFactors.get(i)
                   * defaultProbabilities.get(i)
                   * (1 - recoveryRate));
            cvaExposureByLeg.add(val);
        }

        return cvaExposureByLeg;
    }

    /**
     * <p>Not all financial mathematics is hard! Total the exposures by leg.
     * </p>
     *
     * @param cvaExposureByLeg Array of doubles
     * @return Sum of the array
     */
    public static double getCvaExposureVal(List<Double> cvaExposureByLeg) {
        double cvaExposureVal = 0.0;
        for (int i = 0; i < cvaExposureByLeg.size(); i++) {
            cvaExposureVal += cvaExposureByLeg.get(i);
        }
        return cvaExposureVal;
    }

    /**
     * <p>Helper function to format a list of doubles for JSON parsing.
     * </p>
     *
     * @param comma A comma to prefix if not first field
     * @param fieldName Field name
     * @param doubles List of doubles
     * @return A String that could become JSON
     */
    private static String formatDoubleArrayForJSON(String comma, String fieldName, List<Double> doubles) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(comma);
        stringBuilder.append(" \"" + fieldName + "\": [");
        for (int i = 0 ; i < doubles.size(); i++) {
            if (i > 0) {
                stringBuilder.append(", ");
            }
            stringBuilder.append(doubles.get(i));
        }
        stringBuilder.append("]");

        return stringBuilder.toString();
    }

}
