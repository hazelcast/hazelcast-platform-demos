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

package hazelcast.platform.demos.banking.transactionmonitor;

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
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.config.JobConfigArguments;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.jet.datamodel.Tuple4;
import com.hazelcast.jet.python.PythonServiceConfig;
import com.hazelcast.map.IMap;
import com.hazelcast.nio.serialization.genericrecord.GenericRecord;
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
    private static final int CSV_FOURTH = 3;
    private static final int CSV_FIFTH = 4;
    private static final int DEFAULT_CASSANDRA_PORT = 9042;
    private static final int DEFAULT_MARIA_PORT = 3306;
    private static final int ALTERNATIVE_MARIA_PORT = 4306;
    private static final int DEFAULT_MONGO_PORT = 27017;
    private static final int POS4 = 4;
    private static final int POS6 = 6;

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
                    .filter(line -> line.length() > 0 && !line.startsWith("#"))
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
     * <p>Read Bank Identifier Code (BIC) and details about it from
     * a CSV file on the classpath.
     * </p>
     * <p>A BIC code "{@code AAAAGBXX}" would be bank "AAAA", in country "GB"
     * with location "XX". Use the country to derive the currency.
     * </p>
     *
     * @return A map with BIC key and details as value
     * @throws Exception
     */
    public static Map<String, Tuple4<String, Double, String, String>>
        bicList() throws Exception {
        Map<String, Tuple4<String, Double, String, String>> result = null;
        final SortedSet<String> failures = new TreeSet<>();

        try (
             BufferedReader bufferedReader =
                 new BufferedReader(
                     new InputStreamReader(
                             MyUtils.class.getResourceAsStream("/biclist.txt"), StandardCharsets.UTF_8));
        ) {
            result =
                    bufferedReader.lines()
                    .filter(line -> line.length() > 0 && !line.startsWith("#"))
                    .map(line -> {
                        String[] lineSplit = line.split("\\|");
                        String key = lineSplit[CSV_FIRST];
                        String country = key.substring(POS4, POS6);
                        Tuple2<String, Double> currencyPair = Tuple2.tuple2(null, 0d);
                        try {
                            currencyPair = MyUtils.getCurrency(country);
                        } catch (Exception e) {
                            failures.add(country);
                        }
                        Tuple4<String, Double, String, String> tuple4
                            = Tuple4.tuple4(
                                    currencyPair.f0(),
                                    currencyPair.f1(),
                                    lineSplit[CSV_SECOND],
                                    lineSplit[CSV_THIRD]
                                    );
                        return new SimpleImmutableEntry
                                <String, Tuple4<String, Double, String, String>>(key, tuple4);
                    })
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        }

        if (failures.size() != 0) {
            String message = String.format("No currencies derived for %s", failures.toString());
            throw new RuntimeException(message);
        }
        return result;
    }

    /**
     * <p>Read E-commerce stock codes and details about them from
     * a CSV file on the classpath.
     * </p>
     *
     * @return A map with stock code and details as value
     * @throws Exception
     */
    public static Map<String, Tuple3<String, String, Double>>
        productCatalog() throws Exception {
        Map<String, Tuple3<String, String, Double>> result = null;

        try (
             BufferedReader bufferedReader =
                 new BufferedReader(
                     new InputStreamReader(
                             MyUtils.class.getResourceAsStream("/productcatalog.txt"), StandardCharsets.UTF_8));
        ) {
            result =
                    bufferedReader.lines()
                    .filter(line -> line.length() > 0 && !line.startsWith("#"))
                    .map(line -> {
                        String[] split = line.split("\\|");
                        Tuple3<String, String, Double> tuple3
                            = Tuple3.tuple3(split[CSV_SECOND],
                                    split[CSV_THIRD],
                                    Double.parseDouble(split[CSV_FOURTH]));
                        return new SimpleImmutableEntry
                                <String, Tuple3<String, String, Double>>(split[CSV_FIRST], tuple3);
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

    /**
     * <p>Determine target, assume Hazelcast Cloud
     * </p>
     * @param s
     * @return
     */
    public static boolean useHzCloud(String s) {
        if (s == null || s.isBlank()) {
            return true;
        } else {
            return !s.toLowerCase(Locale.ROOT).equals("false");
        }
    }

    /**
     * <p>Extract Postgres properties
     */
    public static Properties getPostgresProperties(Properties properties) throws Exception {
        Properties result = new Properties();
        List<String> names = List.of(
                MyConstants.POSTGRES_ADDRESS, MyConstants.POSTGRES_DATABASE, MyConstants.POSTGRES_SCHEMA,
                MyConstants.POSTGRES_USER, MyConstants.POSTGRES_PASSWORD);
        for (String name : names) {
            String value = properties.getProperty(name, "");
            if (value.isBlank()) {
                String message = String.format("No value for property '%s'", name);
                throw new RuntimeException(message);
            }
            result.put(name, value);
        }
        return result;
    }

    /**
     * <p>Look-up from properties
     * </p>
     *
     * @param properties
     * @return
     */
    public static TransactionMonitorFlavor getTransactionMonitorFlavor(Properties properties) throws Exception {
        String key = MyConstants.TRANSACTION_MONITOR_FLAVOR;
        String value = (properties == null ? "" : properties.getProperty(key, ""));

        for (TransactionMonitorFlavor possible : TransactionMonitorFlavor.values()) {
            if (possible.toString().equalsIgnoreCase(value)) {
                return possible;
            }
        }

        String message = String.format("No match for flavor '%s' in %s",
                value, Arrays.toString(TransactionMonitorFlavor.values()));
        throw new RuntimeException(message);
    }

    /**
     * <p>For a country, find the currency and an relative
     * amount compared to USD. Ie. fix exchange rate.
     * </p>
     *
     * @param country
     * @return
     * @throws Exception
     */
    @SuppressWarnings({"checkstyle:CyclomaticComplexity", "checkstyle:MagicNumber"})
    public static Tuple2<String, Double> getCurrency(String country) throws Exception {
        String currency = null;
        double rate = 0d;
        switch (country) {
        case "BS":
            currency = "BSD";
            rate = 1d;
            break;
        case "CH":
            currency = "CHF";
            rate = 1.08d;
            break;
        case "CZ":
            currency = "CZK";
            rate = 0.044d;
            break;
        case "AT":
        case "BE":
        case "DE":
        case "ES":
        case "FR":
        case "IE":
        case "IT":
        case "LU":
        case "PT":
            currency = "EUR";
            rate = 1.06d;
            break;
        case "GB":
            currency = "GBP";
            rate = 1.20d;
            break;
        case "HK":
            currency = "HKD";
            rate = 0.13d;
            break;
        case "PL":
            currency = "PLN";
            rate = 0.23d;
            break;
        default:
            // None
            break;
        }
        if (country.equals("FR")) {
            currency = "EUR";
        }
        if (currency == null) {
            String message = String.format("No currency for '%s'", Objects.toString(country));
            throw new RuntimeException(message);
        }
        return Tuple2.tuple2(currency, rate);
    }

    /**
     * <p>Turn multi-line XML into JSON
     * </p>
     *
     * @param string
     * @return
     */
    public static String xmlSafeForJson(String input) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");

        String[] lines = input.split(System.lineSeparator());
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                stringBuilder.append(", \"");
            } else {
                stringBuilder.append("\"");
            }

            stringBuilder.append(xmlLineToJsonString(lines[i]));

            stringBuilder.append("\"");
        }

        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    /**
     * <p>Make a line of XML into a safe JSON string.
     * </p>
     *
     * @param line
     * @return
     */
    public static String xmlLineToJsonString(String line) {
        return line.replace("\"", "\\\"");
    }

    public static void logStuff(HazelcastInstance hazelcastInstance) {
        logJobs(hazelcastInstance);
        logMaps(hazelcastInstance);
        logMySqlSlf4j(hazelcastInstance);
    }

    /**
     * <p>Confirm the running jobs to the console.
     * </p>
     */
    private static void logJobs(HazelcastInstance hazelcastInstance) {
        LOGGER.info("~_~_~_~_~");
        LOGGER.info("logJobs()");
        LOGGER.info("---------");
        hazelcastInstance.getJet().getJobs().forEach(job -> {
            try {
                LOGGER.info("Job name '{}', id {}, status {}, submission {} ({})",
                    Objects.toString(job.getName()), job.getId(), job.getStatus(),
                    job.getSubmissionTime(), new Date(job.getSubmissionTime()));
                JobConfig jobConfig = job.getConfig();
                Object originalSql =
                        jobConfig.getArgument(JobConfigArguments.KEY_SQL_QUERY_TEXT);
                if (originalSql != null) {
                    LOGGER.info(" Original SQL: {}", originalSql);
                }
            } catch (Exception e) {
                String message = String.format("logJobs(): %s: %s", job.getId(), e.getMessage());
                LOGGER.warn(message);
            }
        });
        LOGGER.info("---------");
    }

    /**
     * <p>Confirm the maps sizes to the console.
     * </p>
     */
    private static void logMaps(HazelcastInstance hazelcastInstance) {
        Set<String> iMapNames = hazelcastInstance.getDistributedObjects()
                .stream()
                .filter(distributedObject -> distributedObject instanceof IMap)
                .filter(distributedObject -> !distributedObject.getName().startsWith("__"))
                .map(distributedObject -> distributedObject.getName())
                .collect(Collectors.toCollection(TreeSet::new));

        LOGGER.info("~_~_~_~_~");
        LOGGER.info("logMaps()");
        LOGGER.info("---------");
        for (String iMapName : iMapNames) {
            LOGGER.info("IMap: name '{}', size {}",
                    iMapName, hazelcastInstance.getMap(iMapName).size());
        }
        LOGGER.info("---------");
    }

    /**
     * <p>Log the logs saved into an {@link com.hazelcast.map.IMap IMap}
     * if it exists.
     * </p>
     */
    @SuppressWarnings("unchecked")
    private static void logMySqlSlf4j(HazelcastInstance hazelcastInstance) {
        IMap<Object, GenericRecord> mapMySqlSlf4j = null;

        // Do not look up by name, as that force creates
        for (DistributedObject distributedObject : hazelcastInstance.getDistributedObjects()) {
            if (distributedObject instanceof IMap
                    && distributedObject.getName().equals(MyConstants.IMAP_NAME_MYSQL_SLF4J)) {
                mapMySqlSlf4j = (IMap<Object, GenericRecord>) distributedObject;
            }
        }

        LOGGER.info("~_~_~_~_~");
        LOGGER.info("logMySqlSlf4j()");
        LOGGER.info("---------");
        if (mapMySqlSlf4j == null) {
            LOGGER.info("Map '{}' does not currently exist", MyConstants.IMAP_NAME_MYSQL_SLF4J);
        } else {
            LOGGER.info("Map '{}'", MyConstants.IMAP_NAME_MYSQL_SLF4J);
            Set<Entry<Object, GenericRecord>> entrySet = mapMySqlSlf4j.entrySet();
            for (Entry<Object, GenericRecord> entry : entrySet) {
                LOGGER.info("Key '{}', Value '{}'", entry.getKey(), entry.getValue());
            }
            LOGGER.info("[{} entr{}]", entrySet.size(), entrySet.size() == 1 ? "y" : "ies");
        }
        LOGGER.info("---------");
    }

    /**
     * <p>Confirm mappings/views added implicitly or explicitly.
     * </p>
     */
    public static void showMappingsAndViews(HazelcastInstance hazelcastInstance) {
        for (String query : List.of("SHOW MAPPINGS", "SHOW VIEWS")) {
            LOGGER.info("~_~_~_~_~");
            LOGGER.info("{}", query);
            LOGGER.info("---------");
            int count = 0;
            try {
                Iterator<SqlRow> iterator = hazelcastInstance.getSql().execute(query).iterator();
                while (iterator.hasNext()) {
                    count++;
                    LOGGER.info("{}", iterator.next());
                }
                LOGGER.info("[{} row{}]", count, (count == 1 ? "" : "s"));
            } catch (Exception e) {
                LOGGER.error("showMappingsAndViews():" + query, e);
            }
        }
        LOGGER.info("~_~_~_~_~");
    }

    /**
     * <p>To connect...to Cassandra
     * </p>
     */
    public static String buildCassandraURI(Properties properties, String keyspace) throws Exception {
        String myCassandraAddress = System.getProperty(MyConstants.CASSANDRA_CONFIG_KEY, "");
        String hostIp = System.getProperty(MyConstants.HOST_IP, "");
        LOGGER.debug("'{}'=='{}'", MyConstants.CASSANDRA_CONFIG_KEY, myCassandraAddress);
        LOGGER.debug("'{}'=='{}'", MyConstants.HOST_IP, hostIp);

        String uri = "jdbc:cassandra://";
        if (!myCassandraAddress.isBlank()) {
            String[] tokens = myCassandraAddress.split(":");
            uri += tokens[0];
        } else {
            if (!hostIp.isBlank()) {
                String[] tokens = hostIp.split(":");
                uri += tokens[0];
            } else {
                String message = String.format("Missing both '{}' and '{}', need one to connect",
                        MyConstants.HOST_IP, MyConstants.CASSANDRA_CONFIG_KEY);
                throw new RuntimeException(message);
            }
        }

        uri += ":" + DEFAULT_CASSANDRA_PORT;
        uri += "/" + keyspace + "?localdatacenter=datacenter1";

        return uri;
    }

    /**
     * <p>To connect...to MariaDB
     * </p>
     */
    public static String buildMariaURI(Properties properties, String database, boolean isKubernetes) throws Exception {
        String myMariaAddress = System.getProperty(MyConstants.MARIA_CONFIG_KEY, "");
        String hostIp = System.getProperty(MyConstants.HOST_IP, "");
        LOGGER.debug("'{}'=='{}'", MyConstants.MARIA_CONFIG_KEY, myMariaAddress);
        LOGGER.debug("'{}'=='{}'", MyConstants.HOST_IP, hostIp);

        String uri = "jdbc:mariadb://";
        if (!myMariaAddress.isBlank()) {
            String[] tokens = myMariaAddress.split(":");
            uri += tokens[0];
        } else {
            if (!hostIp.isBlank()) {
                String[] tokens = hostIp.split(":");
                uri += tokens[0];
            } else {
                String message = String.format("Missing both '{}' and '{}', need one to connect",
                        MyConstants.HOST_IP, MyConstants.MARIA_CONFIG_KEY);
                throw new RuntimeException(message);
            }
        }

        if (isKubernetes) {
            uri += ":" + DEFAULT_MARIA_PORT;
        } else {
            // Localhost or Docker, Maria on different port so doesn't clash with MySql
            uri += ":" + ALTERNATIVE_MARIA_PORT;
        }
        uri += "/" + database;

        return uri;
    }

    /**
     * <p>To connect...to MongoDB
     * </p>
     */
    public static String buildMongoURI(Properties properties) throws Exception {
        String myMongoAddress = System.getProperty(MyConstants.MONGO_CONFIG_KEY, "");
        String hostIp = System.getProperty(MyConstants.HOST_IP, "");
        String user = properties.getProperty(MyConstants.MONGO_USER, "");
        String password = properties.getProperty(MyConstants.MONGO_PASSWORD, "");

        LOGGER.debug("'{}'=='{}'", MyConstants.MONGO_CONFIG_KEY, myMongoAddress);
        LOGGER.debug("'{}'=='{}'", MyConstants.HOST_IP, hostIp);
        LOGGER.debug("'{}'=='{}'", MyConstants.MONGO_USER, user);
        LOGGER.debug("'{}'=='{}'", MyConstants.MONGO_PASSWORD, password);

        if (user.isBlank() || password.isBlank()) {
            String message = String.format("Missing user/password pair, got '%s'/'%s'", user, password);
            throw new RuntimeException(message);
        }

        int port = DEFAULT_MONGO_PORT;

        String uri = "mongodb://" + user + ":" + password + "@";
        if (!myMongoAddress.isBlank()) {
            String[] tokens = myMongoAddress.split(":");
            uri += tokens[0];
            if (tokens.length > 1) {
                port = Integer.parseInt(tokens[1]);
            }
        } else {
            if (!hostIp.isBlank()) {
                String[] tokens = hostIp.split(":");
                uri += tokens[0];
                if (tokens.length > 1) {
                    port = Integer.parseInt(tokens[1]);
                }
            } else {
                String message = String.format("Missing both '{}' and '{}', need one to connect",
                        MyConstants.HOST_IP, MyConstants.MONGO_CONFIG_KEY);
                throw new RuntimeException(message);
            }
        }
        uri += ":" + port + "/?tls=false";
        return uri;
    }

    /**
     * <p>Retrieve a property, exception if missing.
     * </p>
     *
     * @param properties
     * @param key
     * @return
     * @throws Exception
     */
    public static String ensureGet(Properties properties, String key) throws Exception {
        String value = properties.getProperty(key, "");
        if (value.isBlank()) {
            throw new RuntimeException(String.format("Blank for '{}'", key));
        }
        return value;
    }

}
