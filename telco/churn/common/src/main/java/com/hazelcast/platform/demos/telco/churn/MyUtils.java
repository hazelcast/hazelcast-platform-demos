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

package com.hazelcast.platform.demos.telco.churn;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
//FIXME import java.util.List;
import java.util.Locale;

import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.core.JobStatus;
//FIXME import com.hazelcast.sql.SqlColumnMetadata;
//FIXME import com.hazelcast.sql.SqlResult;
//FIXME import com.hazelcast.sql.SqlRow;
//FIXME import com.hazelcast.sql.SqlRowMetadata;

/**
 * <p>Utility functions that may be useful to more than one module.
 * </p>
 */
public class MyUtils {
    public static final String NEWLINE = System.getProperty("line.separator");

    private static final String ALPHABET_UC = "abcdefghijklmnopqrstuvwxyz".toUpperCase(Locale.ROOT);
    private static final String ALPHABET_LC = ALPHABET_UC.toLowerCase(Locale.ROOT);
    private static final String[] ALPHABETS = { ALPHABET_UC, ALPHABET_LC };
    private static final int ALPHABET_LENGTH = ALPHABET_UC.length();
    private static final int HALF_ALPHABET_LENGTH = ALPHABET_LENGTH / 2;

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
     * @param jetInstance To access the job registry
     * @return The first match, or null if none.
     */
    public static Job findRunningJobsWithSamePrefix(String prefix, JetInstance jetInstance) {
        Job match = null;

        for (Job job : jetInstance.getJobs()) {
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

        char[] output = new char[input.length()];

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            switch (c) {
            case '‘':
            case '’':
            case '“':
            case '”':
                c = '\'';
                break;
            default:
                break;
            }

            output[i] = c;
        }

        return new String(output);
    }

    /**
     * <p>Pretty-print an SQL result.
     * </p>
     *
     * @param sqlResult
     * @return
     *FIXME needs IMDG 4.1
    public static String prettyPrintSqlResult(SqlResult sqlResult) {
        StringBuilder stringBuilder = new StringBuilder();
        String format = "%15s";

        // Column headers in capitals
        SqlRowMetadata sqlRowMetaData = sqlResult.getRowMetadata();
        List<SqlColumnMetadata> sqlColumnMetadata = sqlRowMetaData.getColumns();
        int i = 0;
        for (SqlColumnMetadata sqlColumnMetadatum : sqlColumnMetadata) {
            if (i != 0) {
                stringBuilder.append(',');
            }
            stringBuilder.append(String.format(format, sqlColumnMetadatum.getName().toUpperCase(Locale.ROOT)));
            i++;
        }
        stringBuilder.append(NEWLINE);

        int count = 0;
        for (SqlRow sqlRow : sqlResult) {
            count++;
            for (int j = 0; j < sqlColumnMetadata.size() ; j++) {
                if (j != 0) {
                    stringBuilder.append(',');
                }
                stringBuilder.append(String.format(format, sqlRow.getObject(j).toString()));
            }
            stringBuilder.append(NEWLINE);
        }

        stringBuilder.append("[").append(count).append(count == 1 ? " row]" : " rows]").append(NEWLINE);
        return stringBuilder.toString();
    }
    */

}
