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

package com.hazelcast.platform.demos.banking.cva;

import java.io.BufferedOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

import org.python.core.PyList;
import org.python.core.PyString;
import org.python.core.PyTuple;
import org.python.modules.cPickle;

import com.hazelcast.config.NetworkConfig;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.pipeline.Sink;
import com.hazelcast.jet.pipeline.SinkBuilder;
import com.hazelcast.platform.demos.banking.cva.MyConstants.Site;

/**
 * <p>Utility functions that may be useful to more than one module.
 * </p>
 */
public class MyUtils {

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
    protected static Sink<List<GraphiteMetric>> buildGraphiteSinkMultiple(String host) {
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
     * @param site "{@code CVA_SITE1}" or "{@code CVA_SITE2}"
     * @return 5701 or 6701.
     */
    public static int getLocalhostBasePort(Site site) {
        int multiplier = (site == Site.CVA_SITE1) ? 0 : 1;
        return NetworkConfig.DEFAULT_PORT + multiplier * PORT_RANGE;
    }

}
