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

package com.hazelcast.platform.demos.retail.clickstream;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.Serializable;

import org.python.core.PyFloat;
import org.python.core.PyInteger;
import org.python.core.PyList;
import org.python.core.PyString;
import org.python.core.PyTuple;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>Helper class for sending a single metric to Graphite.
 * </p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "Graphite to be replaced with Prometheus")
public class MyGraphiteMetric implements Serializable {

    private static final long serialVersionUID = 1L;

    private PyString metricName;
    private PyInteger timestamp;
    private PyFloat metricValue;

    /**
     * <p>Creates a singleton list containing this metric
     * for sending.
     * </p>
     */
    public PyList getAsList() {
        PyList pyList = new PyList();
        PyTuple pyTuple = this.getAsItem();
        pyList.add(pyTuple);
        return pyList;
    }

    /**
     * <p>For adding to a list
     * </p>
     *
     * @return
     */
    public PyTuple getAsItem() {
        PyTuple pyTuple = new PyTuple(this.metricName, new PyTuple(this.timestamp, this.metricValue));
        return pyTuple;
    }
}
