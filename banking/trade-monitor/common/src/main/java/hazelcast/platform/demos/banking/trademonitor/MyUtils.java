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

package hazelcast.platform.demos.banking.trademonitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.jet.python.PythonServiceConfig;
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

    /**
     * <p>Configuration for the Python runner. Where to find the Python module,
     * which is presumed to have a "{@code handle()}" function.
     *
     * @param name The job name, eg. "{@code pi1}", used as a folder prefix
     * @param handler The function in the Python file to call
     * @return Python configuration for use in a Jet job.
     */
    public static PythonServiceConfig getPythonServiceConfig(String name, String handler) throws Exception {
        String subdir = "python";
        File temporaryDir = MyUtils.getTemporaryDir(subdir, name);

        PythonServiceConfig pythonServiceConfig = new PythonServiceConfig();
        pythonServiceConfig.setBaseDir(temporaryDir.toString());
        pythonServiceConfig.setHandlerFunction(handler);
        pythonServiceConfig.setHandlerModule(name);

        System.out.printf("Python module '%s%s.py', calling function '%s()'%n",
                "classpath:src/main/resources" + File.separator + subdir + File.separator,
                pythonServiceConfig.handlerModule(),
                pythonServiceConfig.handlerFunction());

        return pythonServiceConfig;
    }

    /**
     * <p>Python files are in "/src/main/resources" and hence in the classpath,
     * for easy deployment as a Docker image. Copy these to the main filesystem
     * to make it easier for the Python service to find them and stream them
     * to the cluster.
     * <p>
     *
     * @param sourceDirectory The directory in "classpath:src/main/resources"
     * @param name The job name, eg. "{@code noop}", used as a folder prefix
     * @return A folder containing the Python code copied from the classpath.
     */
    protected static File getTemporaryDir(String sourceDirectory, String name) throws Exception {

        Path targetDirectory = Files.createTempDirectory(name);
        targetDirectory.toFile().deleteOnExit();

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        String[] resourcesToCopy = { name + ".py", "requirements.txt" };
        for (String resourceToCopy : resourcesToCopy) {
            String relativeResourceToCopy = sourceDirectory + File.separator + resourceToCopy;
            try (InputStream inputStream = classLoader.getResourceAsStream(relativeResourceToCopy)) {
                if (inputStream == null) {
                    throw new RuntimeException(relativeResourceToCopy + ": not found in Jar");
                } else {
                    LOGGER.trace("{}", relativeResourceToCopy);
                    Path targetFile = Paths.get(targetDirectory + File.separator + resourceToCopy);
                    Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        return targetDirectory.toFile();
    }

    /**
     * <p>Loads a properties file from the classpath.
     * </p>
     *
     * @param filename
     * @return
     */
    public static Properties loadProperties(String filename) throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        try (InputStream inputStream = classLoader.getResourceAsStream(filename)) {
            if (inputStream == null) {
                throw new RuntimeException(filename + ": not found in Jar");
            } else {
                Properties properties = new Properties();
                properties.load(inputStream);
                return properties;
            }
        }
    }

    /**
     * <p>Determine input source, assume Kafka
     * </p>
     * @param s
     * @return
     */
    public static boolean usePulsar(String s) {
        if (s == null || s.isBlank()) {
            return false;
        } else {
            return s.toLowerCase(Locale.ROOT).equals("pulsar");
        }
    }
}
