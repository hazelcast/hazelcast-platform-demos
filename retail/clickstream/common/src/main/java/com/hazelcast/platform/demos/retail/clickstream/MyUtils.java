/*
 * Copyright (c) 2008-2025, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.platform.demos.retail.clickstream;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;

import org.python.core.PyFloat;
import org.python.core.PyInteger;
import org.python.core.PyList;
import org.python.core.PyString;
import org.python.modules.cPickle;

import com.hazelcast.jet.accumulator.LongAccumulator;
import com.hazelcast.jet.core.DAG;
import com.hazelcast.jet.core.Edge;
import com.hazelcast.jet.core.Vertex;
import com.hazelcast.jet.core.processor.DiagnosticProcessors;
import com.hazelcast.jet.pipeline.GeneralStage;
import com.hazelcast.jet.pipeline.ServiceFactories;
import com.hazelcast.jet.pipeline.Sink;
import com.hazelcast.jet.pipeline.SinkBuilder;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.python.PythonServiceConfig;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Shared utility methods
 * </p>
 */
@Slf4j
public class MyUtils {

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
     * <p>Finds the Maven build from the classpath.
     * </p>
     *
     * @return
     * @throws Exception
     */
    public static String getBuildTimestamp() throws Exception {
        Properties properties = MyUtils.loadProperties("application.properties");
        String buildTimestamp = properties.getProperty("my.build-timestamp");
        if (buildTimestamp == null || buildTimestamp.length() == 0) {
            throw new RuntimeException("Could not find 'my.build-timestamp' in 'application.properties'");
        }
        return buildTimestamp;
    }

    /**
     * <p>Docker or Kubernetes version of Pulsar service URL
     * </p>
     */
    public static String getPulsarServiceUrl() {
        StringBuilder stringBuilder = new StringBuilder("pulsar://");
        String pulsarList = System.getProperty("my.pulsar.list", "");
        if (pulsarList.length() == 0) {
            throw new RuntimeException("No 'my.pulsar.list' provided");
        }
        String[] connections = pulsarList.split(",");
        for (int i = 0 ; i < connections.length ; i++) {
            if (i > 0) {
                stringBuilder.append(",");
            }
            String[] hostPort = connections[i].split(":");
            stringBuilder.append(hostPort[0]);
            if (System.getProperty("my.kubernetes.enabled", "").equals("true")
                    && !hostPort[0].endsWith(".default.svc.cluster.local")) {
                stringBuilder.append(".default.svc.cluster.local");
            }
            // Default port if not provided
            if (hostPort.length == 1) {
                stringBuilder.append(":6650");
            } else {
                stringBuilder.append(":").append(hostPort[1]);
            }
        }

        return stringBuilder.toString();
    }

    /**
     * <p>Docker or Kubernetes version of singleton Graphite host,
     * embedded in the Grafana image.
     * </p>
     *
     * @return
     */
    public static String getGraphiteHost() {
        if (System.getProperty("my.kubernetes.enabled", "").equals("true")) {
            return "clickstream-grafana.default.svc.cluster.local";
        } else {
            String graphiteHost = System.getProperty("my.graphite.host", "");
            if (graphiteHost.length() == 0) {
                log.error("No Graphite host");
            }
            return graphiteHost;
        }
    }

    /**
     * <p>Configuration for the Python runner. Where to find the Python module,
     * which is presumed to have a "{@code handle()}" function.
     *
     * @param name The job name, eg. "{@code pi1}", used as a folder prefix
     * @param handler The function in the Python file to call
     * @return Python configuration for use in a Jet job.
     */
    public static PythonServiceConfig getPythonServiceConfig(String name, String handler, ClassLoader classLoader)
            throws Exception {
        String subdir = "python";
        File temporaryDir = MyUtils.getTemporaryDir(subdir, name, classLoader);

        PythonServiceConfig pythonServiceConfig = new PythonServiceConfig();
        pythonServiceConfig.setBaseDir(temporaryDir.toString());
        pythonServiceConfig.setHandlerFunction(handler);
        pythonServiceConfig.setHandlerModule(name);

        log.trace("Python module '{}{}.py', calling function '{}()'",
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
    private static File getTemporaryDir(String sourceDirectory, String name, ClassLoader classLoader) throws Exception {

        Path targetDirectory = Files.createTempDirectory(name);
        targetDirectory.toFile().deleteOnExit();

        String[] resourcesToCopy = { name + ".py", "requirements.txt" };
        for (String resourceToCopy : resourcesToCopy) {
            String relativeResourceToCopy = sourceDirectory + File.separator + resourceToCopy;
            try (InputStream inputStream = classLoader.getResourceAsStream(relativeResourceToCopy)) {
                if (inputStream == null) {
                    throw new RuntimeException(relativeResourceToCopy + ": not found in Jar");
                } else {
                    log.trace("{}", relativeResourceToCopy);
                    Path targetFile = Paths.get(targetDirectory + File.separator + resourceToCopy);
                    Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        return targetDirectory.toFile();
    }

    /**
     * <p>Log every 1st, 2nd, 4th, 8th, 16th...etc up to a maximum interval
     * </p>
     *
     * @param generalStage Any stage, stream or batch
     * @param prefix To distinguish if used more than one per pipeline
     * @param cap Stop logging above this threshold
     */
    public static void addExponentialLogger(GeneralStage<?> streamStage, String prefix, long cap) {
        streamStage
        .mapUsingService(ServiceFactories.sharedService(__ -> {
            Object[] control = new Object[2];
            control[0] = new LongAccumulator(0);
            control[1] = Long.valueOf(1L);
            return control;
        }),
         (control, item) -> {
             LongAccumulator counter = (LongAccumulator) control[0];
             long currentThreshold = (long) control[1];
             counter.add(1L);
             if ((counter.get() < cap) && (counter.get() % currentThreshold == 0)) {
                 if (2 * currentThreshold <= MyConstants.MAX_LOGGING_INTERVAL) {
                     control[1] = Long.valueOf(2 * currentThreshold);
                 }
                 String message = String.format("Item %d -> '%s'", counter.get(), MyUtils.truncateToString(item));
                 return message;
             } else {
                 return null;
             }
         }).setName(prefix + "_log_upto_every_" + MyConstants.MAX_LOGGING_INTERVAL)
        .writeTo(Sinks.logger()).setName(prefix + "-logger");
    }

    /**
     * <p>DAG version of exponential logger above
     * </p>
     */
    public static void addExponentialLogger(DAG dag, Vertex source, long cap, int outbox) {
        String prefix = source.getName();

        Vertex filterFormatter = dag.newVertex(
                prefix + "-formatter",
                () -> new FilterFormatterProcessor(cap)
                );

        Vertex logger = dag.newVertex(
                prefix + "-logger",
                DiagnosticProcessors.writeLoggerP(Object::toString)
                );

        // Input for additional logging will have an edge already presumably
        dag.edge(Edge.from(source, outbox).to(filterFormatter));
        dag.edge(Edge.between(filterFormatter, logger));
    }

    /**
     * <p>For logging, but some objects may be huge.
     * </p>
     *
     * @param o
     * @return
     */
    public static String truncateToString(Object o) {
        String s = Objects.toString(o);
        if (s.length() > MyConstants.MAX_LOGGING_LINE_LENGTH) {
            return s.substring(0, MyConstants.MAX_LOGGING_LINE_LENGTH) + " (truncated)";
        } else {
            return s;
        }
    }

    /**
     * <p>Create a Sink to send to Graphite
     * </p>
     *
     * @param host For socket, port is preset
     * @return
     */
    public static Sink<Entry<String, Float>> buildGraphiteSink(String host) {
        return SinkBuilder.sinkBuilder(
                "graphite",
                __ -> new BufferedOutputStream(new Socket(host, MyConstants.GRAPHITE_PORT).getOutputStream()))
        .<Entry<String, Float>>receiveFn((bufferedOutputStream, entry) -> {

            PyString metricName = new PyString(entry.getKey());
            PyInteger timestamp = new PyInteger(-1);
            PyFloat metricValue = new PyFloat(entry.getValue());
            MyGraphiteMetric myGraphiteMetric = new MyGraphiteMetric(metricName, timestamp, metricValue);

            PyString payload = cPickle.dumps(myGraphiteMetric.getAsList(), 2);
            byte[] header = ByteBuffer.allocate(Integer.BYTES).putInt(payload.__len__()).array();

            bufferedOutputStream.write(header);
            bufferedOutputStream.write(payload.toBytes());
        })
        .flushFn(BufferedOutputStream::flush)
        .destroyFn(BufferedOutputStream::close)
        .preferredLocalParallelism(1)
        .build();
    }

    /**
     * <p>Create a Sink to send to Graphite, as above but for multiple input items.
     * </p>
     *
     * @param host For socket, port is preset
     * @return
     */
    public static Sink<List<Entry<String, Float>>> buildGraphiteBatchSink(String host) {
        return SinkBuilder.sinkBuilder(
                "graphite",
                __ -> new BufferedOutputStream(new Socket(host, MyConstants.GRAPHITE_PORT).getOutputStream()))
        .<List<Entry<String, Float>>>receiveFn((bufferedOutputStream, list) -> {

            PyList pyList = new PyList();
            list.forEach(entry -> {
                PyString metricName = new PyString(entry.getKey());
                PyInteger timestamp = new PyInteger(-1);
                PyFloat metricValue = new PyFloat(entry.getValue());
                MyGraphiteMetric myGraphiteMetric = new MyGraphiteMetric(metricName, timestamp, metricValue);

                pyList.add(myGraphiteMetric.getAsItem());
            });

            PyString payload = cPickle.dumps(pyList, 2);
            byte[] header = ByteBuffer.allocate(Integer.BYTES).putInt(payload.__len__()).array();

            bufferedOutputStream.write(header);
            bufferedOutputStream.write(payload.toBytes());
        })
        .flushFn(BufferedOutputStream::flush)
        .destroyFn(BufferedOutputStream::close)
        .preferredLocalParallelism(1)
        .build();
    }

    /**
     * <p>Convert text version of digital twin to compact for Python. 0 is false.s
     * </p>
     *
     * @param key Prepend if not null
     * @param csv Of "didB,didD,didE,didF"
     * @return "0,1,0,1,1,1"
     */
    public static String[] digitalTwinCsvToBinary(String key, String csv, boolean includeOrdered) {
        int size = CsvField.ordered.ordinal();
        int dataOffset = 0;
        if (key != null) {
            size++;
            dataOffset = 1;
        }
        if (!includeOrdered) {
            size--;
        }
        String[] result = new String[size];

        for (int i = dataOffset ; i < result.length; i++) {
            result[i] = "0";
        }
        if (key != null) {
            result[0] = key;
        }

        if (csv != null) {
            for (String token : csv.split(",")) {
                if (token.length() > 0) {
                    CsvField csvField = CsvField.valueOf(token);
                    if ((csvField == CsvField.ordered && includeOrdered)
                            || csvField != CsvField.ordered) {
                        result[csvField.ordinal() - 1 + dataOffset] = "1";
                    }
                }
            }
        }
        return result;
    }

    /**
     * <p>Parse ISO date from Maven
     * </p>
     *
     * @param buildTimestamp
     * @return
     */
    public static long parseTimestamp(String buildTimestamp) {
        final long millis = 1_000L;
        if (buildTimestamp != null && buildTimestamp.length() > 0) {
            try {
                int len = buildTimestamp.length();
                if (buildTimestamp.charAt(len - 1) == 'Z') {
                    buildTimestamp = buildTimestamp.substring(0, len - 1);
                }
                LocalDateTime when = LocalDateTime.parse(buildTimestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                return when.atZone(ZoneId.systemDefault()).toEpochSecond() * millis;
            } catch (Exception e) {
                log.error("parseTimestamp(" + buildTimestamp + "): {}", e.getMessage());
            }
        }
        return 0;
    }


}
