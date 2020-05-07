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

import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;

import com.hazelcast.jet.pipeline.JoinClause;

/**
 * <p>Helper functions for the {@code CvaStpJob}
 * </p>
 */
public class CvaStpUtils {

    /**
     * <p>
     * A join clause for the cartesian product.
     * </p>
     * <p>
     * The left side extractor ignores the input object and always returns the same
     * value. The right side extractor ignores it's input object and always returns
     * the same value, and this is the same value as the left. So both match,
     * always.
     * </p>
     *
     * @return An "<i>always true</i>" function.
     */
    public static JoinClause<Boolean, Object, Object, Object> cartesianProduct() {
        return JoinClause.onKeys(__ -> true, __ -> true);
    }

    /**
    * <p>Create a String for an Exposure, based on input. Input is all Java
    * fields, unlike counterpart {@link makeExposureStrFromJson} which
    * tolerates a bit of JSON on the input.
    * </p>
    *
    * @param tradeid A String
    * @param curvename A String
    * @param counterparty A String
    * @param exposures Array of doubles
    * @param legfractions Array of doubles
    * @param discountfactors Array of doubles
    * @return A string for the exposure, that can be turned into JSON directory.
     */
    public static String makeExposureStrFromJava(String tradeid, String curvename, String counterparty,
           double[] exposures, double[] legfractions, double[] discountfactors) {

       StringBuilder stringBuilder = new StringBuilder();
       stringBuilder.append("{");
       stringBuilder.append(" \"tradeid\": \"" + tradeid + "\"");
       stringBuilder.append(", \"curvename\": \"" + curvename + "\"");
       stringBuilder.append(", \"counterparty\": \"" + counterparty + "\"");

       stringBuilder.append(", \"exposures\": [");
       for (int i = 0 ; i < exposures.length; i++) {
           if (i > 0) {
               stringBuilder.append(", ");
           }
           stringBuilder.append(exposures[i]);
       }
       stringBuilder.append("]");

       stringBuilder.append(", \"legfractions\": [");
       for (int i = 0 ; i < legfractions.length; i++) {
           if (i > 0) {
               stringBuilder.append(", ");
           }
           stringBuilder.append(legfractions[i]);
       }
       stringBuilder.append("]");

       stringBuilder.append(", \"discountfactors\": [");
       for (int i = 0 ; i < discountfactors.length; i++) {
           if (i > 0) {
               stringBuilder.append(", ");
           }
           stringBuilder.append(discountfactors[i]);
       }
       stringBuilder.append("]");

       stringBuilder.append(" }");

       return stringBuilder.toString();
   }

   /**
    * <p>Create a String for an Exposure, based on input. Note that "discountvalues"
    * on input becomes "discountfactors" on output.
    * Use {@link makeExposureStrFromJava()} to do most of the work.
    * </p>
    *
    * @param tradeid A String
    * @param curvename A String
    * @param counterparty A String
    * @param exposures Array of doubles
    * @param mtmJson From the original MTM
    * @return A string for the exposure, that can be turned into JSON directory.
    */
    public static String makeExposureStrFromJson(String tradeid, String curvename, String counterparty,
            double[] exposures, JSONObject mtmJson) throws JSONException {
        JSONArray legfractionsJson = mtmJson.getJSONArray("legfractions");
        JSONArray discountfactorsJson = mtmJson.getJSONArray("discountvalues");

        double[] legfractions = new double[legfractionsJson.length()];
        for (int i = 0 ; i < legfractions.length ; i++) {
            legfractions[i] = legfractionsJson.getDouble(i);
        }

        double[] discountfactors = new double[discountfactorsJson.length()];
        for (int i = 0 ; i < discountfactors.length ; i++) {
            discountfactors[i] = discountfactorsJson.getDouble(i);
        }

        return makeExposureStrFromJava(tradeid, curvename, counterparty, exposures, legfractions, discountfactors);
    }

    /**
     * <p>Turn some fields into JSON.
     * </p>
     *
     * @param tradeid A String
     * @param curvename A String
     * @param counterparty A String
     * @param netCvaExposure Double
     * @param spreadrates Array of doubles
     * @param hazardrates Array of doubles
     * @param defaultprob Array of doubles
     * @param cvaexposurebyleg Array of doubles
     * @return A string which can be directly turned into JSON
     */
    public static String makeTradeExposureStrFromJava(String tradeid, String curvename, String counterparty,
            double netCvaExposure, double[] spreadrates, double[] hazardrates, double[] defaultprob,
            double[] cvaexposurebyleg) {

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("{");
        stringBuilder.append(" \"tradeid\": \"" + tradeid + "\"");
        stringBuilder.append(", \"curvename\": \"" + curvename + "\"");
        stringBuilder.append(", \"counterparty\": \"" + counterparty + "\"");
        stringBuilder.append(", \"cva\": " + netCvaExposure);

        stringBuilder.append(", \"spreadrates\": [");
        for (int i = 0 ; i < spreadrates.length; i++) {
            if (i > 0) {
                stringBuilder.append(", ");
            }
            stringBuilder.append(spreadrates[i]);
        }
        stringBuilder.append("]");

        stringBuilder.append(", \"hazardrates\": [");
        for (int i = 0 ; i < hazardrates.length; i++) {
            if (i > 0) {
                stringBuilder.append(", ");
            }
            stringBuilder.append(hazardrates[i]);
        }
        stringBuilder.append("]");

        stringBuilder.append(", \"defaultprob\": [");
        for (int i = 0 ; i < defaultprob.length; i++) {
            if (i > 0) {
                stringBuilder.append(", ");
            }
            stringBuilder.append(defaultprob[i]);
        }
        stringBuilder.append("]");

        stringBuilder.append(", \"cvaexposurebyleg\": [");
        for (int i = 0 ; i < cvaexposurebyleg.length; i++) {
            if (i > 0) {
                stringBuilder.append(", ");
            }
            stringBuilder.append(cvaexposurebyleg[i]);
        }
        stringBuilder.append("]");

        stringBuilder.append(" }");
        return stringBuilder.toString();
    }


    /**
     * <p>Escape <em>escaped</em> double quotes in strings.
     * </p>
     *
     * @param s Any old string
     * @return
     */
    public static String escapeQuotes(String s) {
        return s.replace("\"", "\\\"");
    }

}
