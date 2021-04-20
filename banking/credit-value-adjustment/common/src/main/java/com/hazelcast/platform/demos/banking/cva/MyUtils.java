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

package com.hazelcast.platform.demos.banking.cva;

import java.io.BufferedOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.python.core.PyList;
import org.python.core.PyString;
import org.python.core.PyTuple;
import org.python.modules.cPickle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.config.NetworkConfig;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.core.JobStatus;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.pipeline.Sink;
import com.hazelcast.jet.pipeline.SinkBuilder;
import com.hazelcast.platform.demos.banking.cva.MyConstants.Site;

/**
 * <p>Utility functions that may be useful to more than one module.
 * </p>
 */
public class MyUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyUtils.class);

    // Primitive int takes 4 bytes to transmit
    private static final int INT_SIZE = 4;
    // Protocol 2 == Python 2.3 upwards. No constant for this apparantly.
    private static final int PROTOCOL_2 = 2;
    private static final int PORT_RANGE = 1000;

    /**
     * <p>A sink that saves an instance of our {@link GraphiteMetric}
     * class to a remote Graphite database for displaying on Grafana
     * dashboards.
     * </p>
     * <p>When the job starts, a {@link java.net.Socket.Socket Socket}
     * is opened to the provided Graphite host.
     * </p>
     * <p>Graphite accepts metrics being sent in plain text. However,
     * for efficiency and best practice we use
     * <a href="https://docs.python.org/3/library/pickle.html">Python Pickle</a>
     * to transmit the metric.
     * </p>
     * <p>To use Pickle, we use <a href="https://www.jython.org/">Jython</a>,
     * which currently (March 2020) has no Python 3 implementation.
     * </p>
     *
     * @param host The location of Graphite/Grafana, port 2004 assumed
     * @return A sink to save to that location.
     */
    protected static Sink<? super GraphiteMetric> buildGraphiteSinkSingleton(String host) {
        return SinkBuilder.sinkBuilder(
                        "graphite",
                        context -> {
                            Socket socket = new Socket(host, MyConstants.GRAPHITE_PORT);
                            return Tuple2.tuple2(socket, new BufferedOutputStream(socket.getOutputStream()));
                            }
                        )
                .receiveFn((Tuple2<Socket, BufferedOutputStream> tuple2, GraphiteMetric graphiteMetric) -> {
                    PyString payload = cPickle.dumps(getAsListFromSingleton(graphiteMetric), PROTOCOL_2);
                    byte[] header = ByteBuffer.allocate(INT_SIZE).putInt(payload.__len__()).array();

                    tuple2.f1().write(header);
                    tuple2.f1().write(payload.toBytes());
                })
                .flushFn(tuple2 -> tuple2.f1().flush())
                .destroyFn(tuple2 -> {
                    tuple2.f1().close();
                    tuple2.f0().close();
                 })
                .preferredLocalParallelism(1)
                .build();
    }

    /**
     * <p>Similar to {@link #buildGraphiteSinkSingleton()} except a more
     * efficient version taking a list of {@link GraphiteMetric} objects.
     * All that changes from the above is "{@code receiveFn()}".
     * </p>
     * <p>It would be possible, though not as type safe to combine this
     * method with the above, and look at the type of the passed item
     * to determine whether a single metric or list of metrics is to
     * be sent across the socket.
     * </p>
     *
     * @param host The location of Graphite/Grafana, port 2004 assumed
     * @return A sink to save to that location.
     */
    public static Sink<List<GraphiteMetric>> buildGraphiteSinkMultiple(String host) {
        return SinkBuilder.sinkBuilder(
                        "graphite",
                        context -> {
                            Socket socket = new Socket(host, MyConstants.GRAPHITE_PORT);
                            return Tuple2.tuple2(socket, new BufferedOutputStream(socket.getOutputStream()));
                            }
                        )
                .receiveFn((Tuple2<Socket, BufferedOutputStream> tuple2, List<GraphiteMetric> graphiteMetrics) -> {
                    PyString payload = cPickle.dumps(getAsListFromList(graphiteMetrics), PROTOCOL_2);
                    byte[] header = ByteBuffer.allocate(INT_SIZE).putInt(payload.__len__()).array();

                    tuple2.f1().write(header);
                    tuple2.f1().write(payload.toBytes());
                })
                .flushFn(tuple2 -> tuple2.f1().flush())
                .destroyFn(tuple2 -> {
                    tuple2.f1().close();
                    tuple2.f0().close();
                 })
                .preferredLocalParallelism(1)
                .build();
    }

    /**
     * <p>A metric list build from a singleton metric.
     * </p>
     */
    private static PyList getAsListFromSingleton(GraphiteMetric graphiteMetric) {
        return getAsListFromList(Collections.singletonList(graphiteMetric));
    }


    /**
     * <p>A metric list build from a singleton metric.
     * </p>
     */
    private static PyList getAsListFromList(List<GraphiteMetric> graphiteMetrics) {
        PyList list = new PyList();
        for (GraphiteMetric graphiteMetric : graphiteMetrics) {
            PyTuple metric = new PyTuple(graphiteMetric.getMetricName(),
                    new PyTuple(graphiteMetric.getTimestamp(), graphiteMetric.getMetricValue()));
            list.add(metric);
        }
        return list;
    }

    /**
     * <p>When trying to run two clusters on the same host,
     * keep the port ranges apart.
     * </p>
     * <p>Put the first cluster on the default port, to make
     * it easy for Management Center to connect.
     * </p>
     *
     * @param site "{@code SITE1}" or "{@code SITE2}"
     * @return 5701 or 6701.
     */
    public static int getLocalhostBasePort(Site site) {
        int multiplier = (site == Site.SITE1) ? 0 : 1;
        return NetworkConfig.DEFAULT_PORT + multiplier * PORT_RANGE;
    }

    /**
     * <p>Handle two args on the command line, the site and a
     * numeric (eg. port). Either, neither or both may be
     * specified, and if both allow any order.
     * </p>
     *
     * @param args A String array of unknown size
     * @param prefix For logging output
     * @return An int and a {@link Site}
     */
    public static Tuple2<Integer, Site> twoArgsIntSite(String[] args, String prefix) {
        if (args.length > 2) {
            String[] ignored = Arrays.copyOfRange(args, 2, args.length);
            LOGGER.warn("{}: Ignoring {} excess arg{}: {}",
                    prefix, ignored.length, (ignored.length == 1 ? "" : "s"), ignored);
        }

        Integer integerValue = null;
        boolean intUsed = false;
        Site siteValue = null;
        boolean siteUsed = false;

        for (String arg : Arrays.copyOf(args,  2)) {
            if (arg != null && arg.length() > 0) {
                // Try as numeric
                if (!intUsed) {
                    try {
                        integerValue = Integer.parseInt(arg);
                        intUsed = true;
                    } catch (NumberFormatException ignored) {
                    }
                }

                // Try as Site ENUM
                if (!siteUsed) {
                    if (arg.equals(Site.SITE1.toString())) {
                        siteValue = Site.SITE1;
                        siteUsed = true;
                    }
                    if (arg.equals(Site.SITE2.toString())) {
                        siteValue = Site.SITE2;
                        siteUsed = true;
                    }
                }
            }
        }

        return Tuple2.tuple2(integerValue, siteValue);
    }


    /**
     * <p>Test if another job is doing the same thing at the same
     * time, to avoid swamping the system. We don't want
     * "{@code CALCULATE_EXPOSURE@10AM}" to run if
     * "{@code CALCULATE_EXPOSURE@9AM}" is still going.
     * </p>
     * <p>This method isn't bulletproof. It relies on jobs having
     * a name and this following a naming pattern.
     * </p>
     * <p>Also, since we don't (and don't want to) suspend processing
     * while we decide, we may spot at job that is running now but
     * actually finishes by the time we want to submit our clone.
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
     * <p>Create a single statistic to send to Graphite/Grafana in a bundle.
     * </p>
     *
     * @param site "{@code CVA-SITE1}" or "{@code CVA-SITE2}"
     * @param one First tier
     * @param two Second tier
     * @param three Third tier
     * @param four Fourth tier
     * @param valueStr Value are passed as strings
     * @param when Timestamp
     * @return A metric to send to Grafana
     */
    public static GraphiteMetric createGraphiteMetric4Tier(Site site, String one, String two, String three, String four,
            String valueStr, long when) {
        GraphiteMetric graphiteMetric = new GraphiteMetric(site);
        graphiteMetric.setMetricName(one + MyConstants.GRAPHITE_SEPARATOR
                + two + MyConstants.GRAPHITE_SEPARATOR
                + three + MyConstants.GRAPHITE_SEPARATOR
                + four);
        graphiteMetric.setMetricValue(valueStr);
        graphiteMetric.setTimestamp(when);
        return graphiteMetric;
    }

}
