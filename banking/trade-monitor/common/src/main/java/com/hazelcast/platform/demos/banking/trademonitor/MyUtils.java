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

package com.hazelcast.platform.demos.banking.trademonitor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.sql.SqlResult;
import com.hazelcast.sql.SqlRow;
import com.hazelcast.sql.SqlRowMetadata;

/**
 * <p>Utility functions used in several places.
 * </p>
 */
public class MyUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyUtils.class);
    private static final int CSV_FIRST = 0;
    private static final int CSV_SECOND = 1;
    private static final int CSV_THIRD = 2;
    private static final int CSV_FIFTH = 4;

    /**
     * <p>Read NASDAQ stock symbols and details about it from
     * a CSV file on the classpath.
     * </p>
     *
     * @return A map with symbol key and security details as value
     * @throws Exception
     */
    public static Map<String, Tuple3<String, NasdaqMarketCategory, NasdaqFinancialStatus>>
        nasdaqListed() throws Exception {
        Map<String, Tuple3<String, NasdaqMarketCategory, NasdaqFinancialStatus>> result = null;

        try (
             BufferedReader bufferedReader =
                 new BufferedReader(
                     new InputStreamReader(
                             MyUtils.class.getResourceAsStream("/nasdaqlisted.txt"), StandardCharsets.UTF_8));
        ) {
            result =
                    bufferedReader.lines()
                    .filter(line -> !line.startsWith("#"))
                    .map(line -> {
                        String[] split = line.split("\\|");
                        Tuple3<String, NasdaqMarketCategory, NasdaqFinancialStatus> tuple3
                            = Tuple3.tuple3(split[CSV_SECOND],
                                    NasdaqMarketCategory.valueOfMarketCategory(split[CSV_THIRD]),
                                    NasdaqFinancialStatus.valueOfFinancialtatus(split[CSV_FIFTH]));
                        return new SimpleImmutableEntry
                                <String, Tuple3<String, NasdaqMarketCategory, NasdaqFinancialStatus>>(split[CSV_FIRST], tuple3);
                    })
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        }

        return result;
    }


    /**
     * <p>Pretty-print an SQL result.
     * </p>
     *
     * @param 3-Tuple of error message, warning message, success rows
     * @return
     */
    public static Tuple3<String, String, List<String>> prettyPrintSqlResult(SqlResult sqlResult) {
        String error = "";
        String warning = "";
        List<String> formatted = new ArrayList<>();

        if (!sqlResult.isRowSet()) {
            LOGGER.error("prettyPrintSqlResult() called for !rowSet");
            error = "sqlResult.isRowSet()==" + sqlResult.isRowSet();
            return Tuple3.tuple3(error, warning, formatted);
        }

        // Capture raw data
        List<String[]> unformatted = new ArrayList<>();

        // Column headers
        SqlRowMetadata sqlRowMetaData = sqlResult.getRowMetadata();
        String[] line = new String[sqlRowMetaData.getColumnCount()];
        for (int i = 0 ; i < line.length; i++) {
            line[i] = sqlRowMetaData.getColumn(i).getName().toUpperCase(Locale.ROOT);
        }
        unformatted.add(line);

        // Main data, break after threshold of lines
        int count = 0;
        for (SqlRow sqlRow : sqlResult) {
            count++;
            line = new String[sqlRowMetaData.getColumnCount()];
            for (int i = 0; i < line.length; i++) {
                line[i] = sqlRow.getObject(i).toString();
            }
            unformatted.add(line);
            if (count == MyConstants.SQL_RESULT_THRESHOLD) {
                sqlResult.close();
                warning = "-- truncated at count " + count;
                break;
            }
        }

        formatted = format(unformatted);
        formatted.add("[" + count + (count == 1 ? " row]" : " rows]"));

        return Tuple3.tuple3("", warning, formatted);
    }


    /**
     * <p>Determine printing width of each column and make each {@code String[]} a
     * {@code String} that is aligned</p>
     *
     * @param unformatted
     * @return
     */
    private static List<String> format(List<String[]> unformatted) {
        List<String> formatted = new ArrayList<>();

        // Find maximum width of each field
        int[] displayWidths = new int[unformatted.get(0).length];
        for (String[] line : unformatted) {
            for (int i = 0 ; i < line.length; i++) {
                if (line[i].length() > displayWidths[i]) {
                    displayWidths[i] = line[i].length();
                }
            }
        }

        // Construct line
        for (String[] line : unformatted) {
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0 ; i < line.length; i++) {
                if (i > 0) {
                    stringBuilder.append(",  ");
                }
                String format = "%-" + (displayWidths[i] + 2) + "s";
                stringBuilder.append(String.format(format, line[i]));
            }
            formatted.add(stringBuilder.toString());
        }

        return formatted;
    }

    /**
     * <p>No double-quotes in a string that will become a
     * JSON string field's value.
     * </p>
     *
     * @param input
     * @return
     */
    public static String safeForJsonStr(String input) {
        return input.replaceAll("\"", "'");
    }

}
