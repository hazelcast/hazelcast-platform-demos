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

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;

import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.function.FunctionEx;
import com.hazelcast.jet.datamodel.Tuple4;

/**
 * <p>Prepare job output as an Excel spreadsheet to download,
 * one line of objects per row that will go in the spreadsheet.
 * Extra columns compared to {@link CsvFileAsByteArray} plus
 * stores Java objects to retain type information.
 * </p>
 */
public class XlstDataAsObjectArrayArray {

    private static final Logger LOGGER = LoggerFactory.getLogger(XlstDataAsObjectArrayArray.class);

    /**
     * <p>Columns to add to the spreadsheet from the CVAs (all of them!)
     * and their pretty-print labels.
     * </p>
     */
    private static final List<String> CVA_COLUMNS =
            List.of("counterparty", "cva");
    private static final List<String> CVA_COLUMNS_LABELS =
            List.of("CounterParty Code", "CVA");

    /**
     * <p>Columns to add to the spreadsheet from the counterparty CDS JSON,
     * and their pretty-print labels.
     * </p>
     */
    private static final List<String> CP_CDS_COLUMNS =
            List.of("shortname", "date", "redcode", "tier");
    private static final List<String> CP_CDS_COLUMNS_LABELS =
            List.of("Name", "Date", "Red Code", "Tier");

    /**
     * <p>All columns in the spreadsheet.
     * </p>
     */
    private static final List<String> COLUMNS =
            Stream.concat(CVA_COLUMNS.stream(), CP_CDS_COLUMNS.stream()).collect(Collectors.toList());
    private static final List<String> COLUMNS_LABELS =
            Stream.concat(CVA_COLUMNS_LABELS.stream(), CP_CDS_COLUMNS_LABELS.stream()).collect(Collectors.toList());

    /**
     * <p>A function to convert a tuple4 of job name, timestamp,
     * a sorted list of CVAs and a sorted list of the corresponding
     * counterparty CDSes into a two-dimensional array.
     * </p>
     */
    public static final FunctionEx<Tuple4<String, Long, List<Entry<String, Double>>,
        List<Entry<String, HazelcastJsonValue>>>, Object[][]>
        CONVERT_TUPLES_TO_STRING_ARRAY_ARRAY =
                (Tuple4<String, Long, List<Entry<String, Double>>, List<Entry<String, HazelcastJsonValue>>> tuple4) -> {

                List<Entry<String, Double>> cvaList = tuple4.f2();
                List<Entry<String, HazelcastJsonValue>> cpCdsList = tuple4.f3();

                Object[][] result = new Object[1 + cvaList.size()][COLUMNS.size()];

                result[0] = COLUMNS_LABELS.toArray();

                for (int i = 0; i < cvaList.size(); i++) {
                    result[i + 1] = getFields(cvaList.get(i), cpCdsList.get(i));
                }

                return result;
            };


    /**
     * <p>Extract the required data fields. For the CVA entry, it's both fields.
     * For the Counterparty CDS, it's the named fields in the JSON Object.
     * </p>
     *
     * @param cvaEntry Counterparty code and amount pair
     * @param cpCdsEntry Counterparty code and JSON
     * @return
     */
    private static Object[] getFields(Entry<String, Double> cvaEntry, Entry<String, HazelcastJsonValue> cpCdsEntry) {

        List<Object> result = new ArrayList<>();
        result.add(cvaEntry.getKey());
        result.add(cvaEntry.getValue());

        if (!cvaEntry.getKey().equals(cpCdsEntry.getKey())) {
            // Should never occur unless someone changes the sort ordering
            LOGGER.error("Key mismatch, '{}'!='{}'", cvaEntry.getKey(), cpCdsEntry.getKey());
            return result.toArray();
        }

        // For easier field lookup
        String jsonStr = cpCdsEntry.getValue().toString();
        JSONObject json = null;
        try {
            json = new JSONObject(jsonStr);
        } catch (JSONException e) {
            LOGGER.error(cpCdsEntry.getKey(), e);
            for (int i = 0 ; i < CP_CDS_COLUMNS.size(); i++) {
                result.add("?");
            }
            return result.toArray();
        }

        // Find the named fields
        for (String fieldName : CP_CDS_COLUMNS) {
            try {
                Object field = json.get(fieldName);
                if (field instanceof String) {
                    result.add(field);
                } else {
                    LOGGER.error("{},{} field type {} not handled", cpCdsEntry.getKey(), fieldName, field.getClass());
                }
            } catch (JSONException e) {
                LOGGER.error(cpCdsEntry.getKey() + "," + fieldName, e);
                result.add("?");
            }
        }

        return result.toArray();
    }

}
