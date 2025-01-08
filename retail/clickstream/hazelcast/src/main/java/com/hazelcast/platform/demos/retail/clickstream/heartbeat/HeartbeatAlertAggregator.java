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

package com.hazelcast.platform.demos.retail.clickstream.heartbeat;

import java.io.Serializable;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;

import org.json.JSONObject;

import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.jet.aggregate.AggregateOperation;
import com.hazelcast.jet.aggregate.AggregateOperation1;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Find the latest heartbeat in a window. Assume all heartbeats
 * come from a single remote cluster, ignore our own messages.
 * </p>
 */
@Getter
@Slf4j
public class HeartbeatAlertAggregator implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final int SECONDS_TO_MILLIS = 1000;

    private String member;
    private String localClusterName;
    private long timestampSeconds;

    HeartbeatAlertAggregator(String localClusterName) {
        this.localClusterName = localClusterName;
    }

    public static AggregateOperation1<
        Entry<String, HazelcastJsonValue>,
        HeartbeatAlertAggregator,
        Entry<Long, String>> buildHeartbeatAlertAggregation(String localClusterName) {
        return AggregateOperation
            .withCreate(() -> new HeartbeatAlertAggregator(localClusterName))
            .andAccumulate((HeartbeatAlertAggregator heartbeatAlertAggregator,
                    Entry<String, HazelcastJsonValue> entry)
                    -> heartbeatAlertAggregator.accumulate(entry.getValue()))
            .andCombine(HeartbeatAlertAggregator::combine)
            .andExportFinish(HeartbeatAlertAggregator::exportFinish);
    }

    /**
     * <p>Capture the timestamp and possibly use.
     * </p>
     *
     * @param hazelcastJsonValue
     * @return
     */
    public HeartbeatAlertAggregator accumulate(HazelcastJsonValue hazelcastJsonValue) {
        try {
            JSONObject jsonObject = new JSONObject(hazelcastJsonValue.toString());
            long epochSecond = jsonObject.getLong("epoch_second");
            String member = jsonObject.getString("member");
            String cluster = jsonObject.getString("site");
            if (!cluster.equals(this.localClusterName) && epochSecond > this.timestampSeconds) {
                this.timestampSeconds = epochSecond;
                this.member = member;
            }
        } catch (Exception e) {
            log.error("accumulate(" + hazelcastJsonValue + ")", e);
        }
        return this;
    }

    /**
     * <p>Take the largest timestamp when combining aggregators.
     * Fields are primitives, no null.
     * </p>
     *
     * @param that
     * @return
     */
    public HeartbeatAlertAggregator combine(HeartbeatAlertAggregator that) {
        if (that.getTimestampSeconds() > this.timestampSeconds) {
            this.timestampSeconds = that.getTimestampSeconds();
            this.member = that.getMember();
        }
        return this;
    }

    /**
     * <p>Result when window ends.
     * </p>
     */
    public Entry<Long, String> exportFinish() {
        return new SimpleImmutableEntry<>(this.timestampSeconds * SECONDS_TO_MILLIS, this.member);
    }
}
