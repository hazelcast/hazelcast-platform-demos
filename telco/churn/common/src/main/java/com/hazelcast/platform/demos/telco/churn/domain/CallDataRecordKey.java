/*
 * Copyright (c) 2008-2023, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.platform.demos.telco.churn.domain;

import java.io.Serializable;

import com.hazelcast.partition.PartitionAware;

/**
 * <p>The primary key for call data record in Hazelcast is
 * a tuple of the caller id and call data record id.
 * </p>
 * <p>This enables all data records for the same caller id
 * to be hosted together.
 * </p>
 * <p>Although classes such as {@link Tuple2} exist for
 * this purpose, we use CSV format for simplicity.
 * </p>
 */
public class CallDataRecordKey implements PartitionAware<String>, Serializable {
    private static final long serialVersionUID = 1L;

    private String csv;

    // No arg constructor needed for java.io.Serializable
    public CallDataRecordKey() {
        this.csv = ",";
    }
    public CallDataRecordKey(String arg0) {
        this.csv = arg0;
    }

    /**
     * <p>Keys are distinguished based on both fields
     * but routed based on first, the caller id.
     * </p>
     */
    @Override
    public String getPartitionKey() {
        return this.csv.split(",")[0];
    }

    // Generated code below

    public String getCsv() {
        return csv;
    }
    public void setCsv(String csv) {
        this.csv = csv;
    }

    @Override
    public String toString() {
        return "CallDataRecordKey [csv=" + csv + "]";
    }

}
