/*
 * Copyright (c) 2008-2024, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.platform.demos.telco.churn;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.core.JobStatus;
import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.sql.SqlColumnMetadata;
import com.hazelcast.sql.SqlResult;
import com.hazelcast.sql.SqlRow;
import com.hazelcast.sql.SqlRowMetadata;

import org.springframework.web.util.HtmlUtils;

/**
 * <p>Utility functions that may be useful to more than one module.
 * </p>
 */
public class MyUtils {
    public static final String NEWLINE = System.getProperty("line.separator");
    private static final Logger LOGGER = LoggerFactory.getLogger(MyUtils.class);

    private static final String ALPHABET_UC = "abcdefghijklmnopqrstuvwxyz".toUpperCase(Locale.ROOT);
    private static final String ALPHABET_LC = ALPHABET_UC.toLowerCase(Locale.ROOT);
    private static final String[] ALPHABETS = { ALPHABET_UC, ALPHABET_LC };
    private static final int ALPHABET_LENGTH = ALPHABET_UC.length();
    private static final int HALF_ALPHABET_LENGTH = ALPHABET_LENGTH / 2;
    private static final DecimalFormat PADDED_SIX_ZEROES = new DecimalFormat("000000");
    private static final DecimalFormat PADDED_TEN_ZEROES = new DecimalFormat("0000000000");

    /**
     * <p>The classic 13 character rotation encryption.
     * "{@code a}" maps to "{@code n}", and "{@code n}" maps
     * back to "{@code a}".
     * </p>
     */
    public static String rot13(String input) {
        if (input == null) {
            return null;
        }

        char[] output = new char[input.length()];

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            for (String alphabet : ALPHABETS) {
                int pos = alphabet.indexOf(c);
                if (pos != -1) {
                    pos = (pos + HALF_ALPHABET_LENGTH) % ALPHABET_LENGTH;
                    c = alphabet.charAt(pos);
                }
            }
            output[i] = c;
        }

        return new String(output);
    }

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
     * <p>In the {@link JobConfig} jobs are named with their function
     * and a submission timestamp. Look for the job name, to see if a
     * job is already running, to stop running the same job twice.
     * </p>
     *
     * @param prefix The start of a job name
     * @param hazelcastInstance To access the job registry
     * @return The first match, or null if none.
     */
    public static Job findRunningJobsWithSamePrefix(String prefix, HazelcastInstance hazelcastInstance) {
        Job match = null;

        for (Job job : hazelcastInstance.getJet().getJobs()) {
            if (job.getName() != null && job.getName().startsWith(prefix)) {
                if ((job.getStatus() == JobStatus.STARTING)
                     || (job.getStatus() == JobStatus.RUNNING)
                     || (job.getStatus() == JobStatus.SUSPENDED)
                     || (job.getStatus() == JobStatus.SUSPENDED_EXPORTING_SNAPSHOT)) {
                    return job;
                }
            }
        }

        return match;
    }

    /**
     * <p>Convert characters the user may have typed and look normal into
     * their equivalents.
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

        String thirdPass = HtmlUtils.htmlUnescape(secondPass);

        return thirdPass;
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
            if (count == MyConstants.SQL_RESULT_THRESHOLD) {
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

    /**
     * <p>Create an account id, padded and with the correct prefix.
     * </p>
     *
     * @param Actual account number
     * @return Same, formatted
     */
    public static String getAccountId(int i) {
        return "AC" + PADDED_SIX_ZEROES.format(i);
    }

    /**
     * <p>Manufacture rather than lookup a phone number for an account,
     * using a deterministic creation method.
     * </p>
     *
     * @param i
     * @return
     */
    @SuppressWarnings("checkstyle:magicnumber")
    public static String getTelno(Integer i) {
        String atLeastTen = makeTenDigits(i);

        // Don't want leading zeroes
        atLeastTen = atLeastTen.substring(atLeastTen.length() - 10);
        if (atLeastTen.charAt(0) == '0') {
            atLeastTen = atLeastTen.charAt(9) + atLeastTen;
            if (atLeastTen.charAt(0) == '0') {
                atLeastTen = "5" + atLeastTen;
            }
        }

        // US style telno format
        return "(" + atLeastTen.substring(0, 3) + ")-"
                + atLeastTen.substring(3, 6) + "-"
                + atLeastTen.substring(6, 10);
    }

    /**
     * <p>Generate the mast for the call. Each person only calls
     * from a fixed subset of masts in their area.
     * <p>
     *
     * @param caller
     * @param call
     * @return
     */
    @SuppressWarnings("checkstyle:magicnumber")
    public static String getMastId(int caller, int call) {
        int mast = (caller * caller) + ((call * call) % (caller + 1));

        String atLeastTen = makeTenDigits(mast);

        return "MAST" + atLeastTen.substring(4, 10);
    }

    /**
     * <p>Turn a number into a String, but try to avoid many zeroes
     * but hopefully generating other digits.
     * </p>
     *
     * @param i
     * @return
     */
    private static String makeTenDigits(int i) {
        long l = Integer.toUnsignedLong(String.valueOf(Integer.MAX_VALUE - (i * i)).hashCode());
        l = Math.abs((l * l) + l);

        return PADDED_TEN_ZEROES.format(l);
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
