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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map.Entry;

import com.hazelcast.function.FunctionEx;
import com.hazelcast.jet.datamodel.Tuple3;

/**
 * <p>Prepare job output as a CSV file to download
 */
public class CsvFileAsByteArray {

    /**
     * <p>This is the end of line character on the server-side, which will likely
     * be Linux. The client-side that downloads may be Windows.
     * </p>
     * <p>TODO: Should add a platform specific download formatter.
     * </p>
     */
    private static final String NEWLINE = System.lineSeparator();

    /**
     * <p>A function to convert a tuple3 of job name, timestamp, and
     * list of CVA pairs to a CSV file content (not the file itself).
     * </p>
     * <p>TODO: To be consistent with {@link XlstFileAsByteArray} we
     * are passed the job name and timestamp as part of the tuple.
     * These could perhaps be part of a "{@code # comment}" first
     * line in the CSV, but then may break some CSV processors.
     * </p>
     */
    public static final FunctionEx<Tuple3<String, Long, List<Entry<String, Double>>>, byte[]>
        CONVERT_TUPLE3_TO_BYTE_ARRAY =
            (Tuple3<String, Long, List<Entry<String, Double>>> tuple3) -> {
                StringBuilder stringBuilder = new StringBuilder();

                for (Entry<String, Double> entry : tuple3.f2()) {
                    stringBuilder.append(entry.getKey() + "," + entry.getValue() + NEWLINE);
                }

                return stringBuilder.toString().getBytes(StandardCharsets.UTF_8);
            };

}
