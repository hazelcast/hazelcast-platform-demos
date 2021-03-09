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

import java.io.Serializable;
import java.time.Instant;

import org.python.core.PyFloat;
import org.python.core.PyInteger;
import org.python.core.PyString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.platform.demos.banking.cva.MyConstants.Site;

/**
 * <p>A holder object for a metric trio to send to Grafana,
 * a metric name and value, plus the time captured.
 * </p>
 * <p>Fields types from <a href="https://www.jython.org/">Jython</a>.
 * </p>
 */
public class GraphiteMetric implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphiteMetric.class);

    private final Site site;
    private PyString metricName;
    private PyInteger timestamp;
    private PyFloat metricValue;

    /**
     * <p>Force all metrics to be prefixed by the site they came
     * from.
     * </p>
     */
    public GraphiteMetric(Site arg0) {
        this.site = arg0;
    }


    /**
     * <p>Set the metric name, prefixed by site.
     * </p>
     *
     * @param arg0 A String
     */
    public void setMetricName(String arg0) {
        this.metricName = new PyString(this.site.toString() + MyConstants.GRAPHITE_SEPARATOR + arg0);
    }


    /**
     * <p>Set the metric value. These are passed as floats,
     * so take the input value as a String to easily cope
     * with integers, doubles, floats.
     * </p>
     *
     * @param arg0 A String
     */
    public void setMetricValue(String arg0) {
        float f = 0F;
        try {
            f = Float.parseFloat(arg0);
        } catch (NumberFormatException nfe) {
            LOGGER.error("setMetricValue('{}') -> {}", arg0, nfe.getMessage());
        }
        this.metricValue = new PyFloat(f);
    }

    /**
     * <p>Set the metric timestamp. This is in seconds,
     * so take the normal Java timestamp and discard
     * milliseconds.
     * </p>
     *
     * @param arg0 Time in milliseconds.
     */
    public void setTimestamp(long arg0) {
        int i = (int) Instant.ofEpochMilli(arg0).getEpochSecond();
        this.timestamp = new PyInteger(i);
    }

    /**
     * <p>Generated</p>
     */
    public PyString getMetricName() {
        return metricName;
    }

    /**
     * <p>Generated</p>
     */
    public PyFloat getMetricValue() {
        return metricValue;
    }

    /**
     * <p>Generated</p>
     */
    public PyInteger getTimestamp() {
        return timestamp;
    }

    /**
     * <p>Generated</p>
     */
    @Override
    public String toString() {
        return "GraphiteMetric [site=" + site + ", metricName=" + metricName + ", timestamp=" + timestamp
                + ", metricValue=" + metricValue + "]";
    }

}
