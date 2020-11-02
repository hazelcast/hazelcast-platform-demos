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

package com.hazelcast.platform.demos.telco.churn.domain;

import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

/**
 * <p>Java representation of a table in Cassandra,
 * created by the <i>cassandra</i> module,
 * from it's "{@code src/main/resources/cql/churn.cql}"
 * and populated by the <i>preload-legacy</i> module.
 * </p>
 */
@Table(value = "cdr")
public class CallDataRecord {

    @PrimaryKeyColumn(value = "id", type = PrimaryKeyType.PARTITIONED)
    private String id;
    @Column(value = "caller_telno")
    private String callerTelno;
    @Column(value = "caller_mast_id")
    private String callerMastId;
    @Column(value = "callee_telno")
    private String calleeTelno;
    @Column(value = "callee_mast_id")
    private String calleeMastId;
    @Column(value = "start_timestamp")
    private Long startTimestamp;
    @Column(value = "duration_seconds")
    private Integer durationSeconds;
    @Column(value = "successful")
    private Boolean successful;

    // Generated code below

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getCallerTelno() {
        return callerTelno;
    }
    public void setCallerTelno(String callerTelno) {
        this.callerTelno = callerTelno;
    }
    public String getCallerMastId() {
        return callerMastId;
    }
    public void setCallerMastId(String callerMastId) {
        this.callerMastId = callerMastId;
    }
    public String getCalleeTelno() {
        return calleeTelno;
    }
    public void setCalleeTelno(String calleeTelno) {
        this.calleeTelno = calleeTelno;
    }
    public String getCalleeMastId() {
        return calleeMastId;
    }
    public void setCalleeMastId(String calleeMastId) {
        this.calleeMastId = calleeMastId;
    }
    public Long getStartTimestamp() {
        return startTimestamp;
    }
    public void setStartTimestamp(Long startTimestamp) {
        this.startTimestamp = startTimestamp;
    }
    public Integer getDurationSeconds() {
        return durationSeconds;
    }
    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }
    public Boolean getSuccessful() {
        return successful;
    }
    public void setSuccessful(Boolean successful) {
        this.successful = successful;
    }

    @Override
    public String toString() {
        return "CallDataRecord [id=" + id + ", callerTelno=" + callerTelno + ", callerMastId=" + callerMastId
                + ", calleeTelno=" + calleeTelno + ", calleeMastId=" + calleeMastId + ", startTimestamp="
                + startTimestamp + ", durationSeconds=" + durationSeconds + ", successful=" + successful + "]";
    }

}
