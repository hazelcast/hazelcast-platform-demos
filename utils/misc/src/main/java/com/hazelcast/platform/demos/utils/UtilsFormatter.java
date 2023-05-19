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

package com.hazelcast.platform.demos.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.sql.SqlColumnMetadata;
import com.hazelcast.sql.SqlResult;
import com.hazelcast.sql.SqlRow;
import com.hazelcast.sql.SqlRowMetadata;

/**
 * <p>Helpful utilities for formatting.
 * </p>
 */
public class UtilsFormatter {
    private static final Logger LOGGER = LoggerFactory.getLogger(UtilsFormatter.class);

    /**
     * <p>Take a timestamp {@code long} and convert it into an ISO-8601
     * style string, but drop the millisecond accuracy.
     * </p>
     *
     * @see <a href="https://en.wikipedia.org/wiki/ISO_8601">ISO-8601</a>
     * @param timestamp From "{@code System.currentTimeMillis()}" probabaly
     * @return Time as a string
     */
    public static String timestampToISO8601(long timestamp) {
        LocalDateTime localDateTime =
                Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime();

        String timestampStr = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(localDateTime);

        if (timestampStr.indexOf('.') > 0) {
            timestampStr = timestampStr.substring(0, timestampStr.indexOf('.'));
        }

        return timestampStr;
    }

    /**
     * <p>Make a String safe to include in JSON.
     * </p>
     *
     * @param input
     * @return
     */
    public static String safeForJsonStr(String input) {
        String[] tokens = input.replaceAll("\"", "'").split(System.getProperty("line.separator"));
        String result = tokens[0];
        for (int i = 1; i < tokens.length; i++) {
            result += "+" + tokens[i];
        }
        return result;
    }

    /**
     * <p>Make a String safe for HTML characters.
     * </p>
     *
     * @param input
     * @return
     */
    public static String htmlUnescape(String input) {
        return input.replaceAll("&lt;", "<").replaceAll("&gt;", ">");
    }

    /**
     * <p>Ensure string is UTF8.
     * </p>
     *
     * @param input
     * @return
     */
    public static String makeUTF8(String input) {
        if (input == null) {
            return null;
        }

        // First pass - charset replacements
        char[] firstPass = new char[input.length()];

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            switch (c) {
            case '‘':
            case '’':
                c = '\'';
                break;
            case '“':
            case '”':
                c = '"';
                break;
            default:
                break;
            }

            firstPass[i] = c;
        }

        // Placeholder for any replacements
        String secondPass = new String(firstPass);
        //.replaceAll("%", "%%");

        String thirdPass = htmlUnescape(secondPass);

        return thirdPass;
    }

    /**
     * <p>Format SQL for output. Shouldn't produce an error but
     * may produce warning if results truncated.
     * </p>
     *
     * @param sqlResult
     * @return Error, warning and list of actual results.
     */
    public static Tuple3<String, String, List<String>> prettyPrintSqlResult(SqlResult sqlResult) {
        String error = "";
        String warning = "";
        List<String> rows = new ArrayList<>();

        if (!sqlResult.isRowSet()) {
            LOGGER.error("prettyPrintSqlResult() called for !rowSet");
            error = "sqlResult.isRowSet()==" + sqlResult.isRowSet();
            return Tuple3.tuple3(error, "", new ArrayList<>());
        }

        StringBuilder line = new StringBuilder();
        String format = "%15s";

        // Column headers in capitals
        SqlRowMetadata sqlRowMetaData = sqlResult.getRowMetadata();
        List<SqlColumnMetadata> sqlColumnMetadata = sqlRowMetaData.getColumns();
        int i = 0;
        for (SqlColumnMetadata sqlColumnMetadatum : sqlColumnMetadata) {
            if (i != 0) {
                line.append(',');
            }
            line.append(String.format(format, sqlColumnMetadatum.getName().toUpperCase(Locale.ROOT)));
            i++;
        }
        rows.add(line.toString());

        int count = 0;
        for (SqlRow sqlRow : sqlResult) {
            line = new StringBuilder();
            count++;
            for (int j = 0; j < sqlColumnMetadata.size() ; j++) {
                if (j != 0) {
                    line.append(',');
                }
                line.append(String.format(format, sqlRow.getObject(j).toString()));
            }
            rows.add(line.toString());
            if (count == UtilsConstants.SQL_RESULT_THRESHOLD) {
                sqlResult.close();
                warning = "-- truncated at count " + count;
                break;
            }
        }

        line = new StringBuilder();
        line.append("[").append(count).append(count == 1 ? " row]" : " rows]");
        rows.add(line.toString());
        return Tuple3.tuple3("", warning, rows);
    }
}
