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

package com.hazelcast.platform.demos.telco.churn.mapstore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.map.MapStore;
import com.hazelcast.platform.demos.telco.churn.domain.CallDataRecord;
import com.hazelcast.platform.demos.telco.churn.domain.CallDataRecordKey;
import com.hazelcast.platform.demos.telco.churn.domain.CallDataRecordKeyProjection;
import com.hazelcast.platform.demos.telco.churn.domain.CallDataRecordMetadata;
import com.hazelcast.platform.demos.telco.churn.domain.CallDataRecordRepository;

/**
 * <p>Load a {@link CallDataRecord} object from Cassandra and turn it into JSON.
 * Save it back when it changes.
 * </p>
 */
public class CallDataRecordMapStore implements MapStore<CallDataRecordKey, HazelcastJsonValue> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CallDataRecordMapStore.class);

    private CallDataRecordRepository callDataRecordRepository;
    private String modifierFilter;

    CallDataRecordMapStore(CallDataRecordRepository arg0, String arg1) {
        this.callDataRecordRepository = arg0;
        this.modifierFilter = arg1;
    }

    /**
     * <p>Try to load a specific key from Cassandra</p>
     */
    @Override
    public HazelcastJsonValue load(CallDataRecordKey key) {
        LOGGER.trace("load('{}')", key);

        HazelcastJsonValue result = null;
        String id = key.getCsv().split(",")[1];

        try {
            CallDataRecord callDataRecord = this.callDataRecordRepository.findById(id).get();

            if (callDataRecord != null) {
                JSONObject json = new JSONObject(callDataRecord);
                MyMapHelpers.validate(json, CallDataRecordMetadata.FIELD_NAMES);
                result = new HazelcastJsonValue(json.toString());
         }

        } catch (Exception exception) {
            LOGGER.error("load('{}'), EXCEPTION: {}", key, exception.getMessage());
        }

        LOGGER.trace("load('{}') -> {}", key, result);
        return result;
    }

    /**
     * <p>Each member is given blocks of keys to load. Depending on the
     * technology it may be feasible to retrieve these in one shot from
     * the database, but here it is coded to retrieve them individually.
     * </p>
     */
    @Override
    public Map<CallDataRecordKey, HazelcastJsonValue> loadAll(Collection<CallDataRecordKey> keys) {
        int expectedSize = keys.size();
        LOGGER.trace("loadAll({})", expectedSize);

        Map<CallDataRecordKey, HazelcastJsonValue> result = new HashMap<>();
        for (CallDataRecordKey key : keys) {
            HazelcastJsonValue json = null;
            try {
                json = this.load(key);
            } catch (Exception exception) {
                LOGGER.error("loadAll({}) for key '" + key + "'", exception);
            }

            if (json != null) {
                result.put(key, json);
            }
        }

        if (result.size() != expectedSize) {
            LOGGER.error("loadAll({}) got only {}", expectedSize, result.size());
        } else {
            LOGGER.trace("loadAll({}) got only {}", expectedSize, result.size());
        }

        return result;
    }

    /**
     * <p>One member calls this to find the subset of primary keys in the
     * database that we wish to load. The keys are then shared across the
     * whole cluster, and each member loads it's allocated keys.
     * </p>
     */
    @Override
    public Iterable<CallDataRecordKey> loadAllKeys() {
        LOGGER.trace("loadAllKeys()");

        try {
            List<CallDataRecordKeyProjection> resultsProjection =
                    this.callDataRecordRepository.findByIdGreaterThan("");
            List<CallDataRecordKey> results = new ArrayList<>(resultsProjection.size());

            for (CallDataRecordKeyProjection callDataRecordKeyProjection : resultsProjection) {
                CallDataRecordKey callDataRecordKey = new CallDataRecordKey();
                callDataRecordKey.setCsv(callDataRecordKeyProjection.getCallerTelno()
                        + "," + callDataRecordKeyProjection.getId());
                results.add(callDataRecordKey);
            }

            if (results.size() == 0) {
                LOGGER.error("loadAllKeys() -> {}, was preload-legacy run?",
                        results.size());
            } else {
                LOGGER.debug("loadAllKeys() -> {}", results.size());
            }

            return results;
        } catch (Exception e) {
            LOGGER.error("loadAllKeys()", e);
            return Collections.emptyList();
        }
    }

    /**
     * <p>CDRs shouldn't be deleted from Hazelcast (could expire out if
     * too many) and certainly shouldn't be deleted from system of
     * record.
     * </p>
     */
    @Override
    public void delete(CallDataRecordKey key) {
        LOGGER.error("delete({}) should not be called", key);
    }

    /**
     * <p>Use easy iterating delete, could do bulk delete
     * if available.
     * </p>
     *
     * @param keys
     */
    @Override
    public void deleteAll(Collection<CallDataRecordKey> keys) {
        int expectedSize = keys.size();
        LOGGER.trace("deleteAll({})", expectedSize);
        for (CallDataRecordKey key : keys) {
            this.delete(key);
        }
    }

    /**
     * <p>Reformat and save.
     * </p>
     */
    @Override
    public void store(CallDataRecordKey key, HazelcastJsonValue value) {
        long now = System.currentTimeMillis();

        CallDataRecord callDataRecord = new CallDataRecord();
        try {
            JSONObject json = new JSONObject(value.toString());
            callDataRecord.setId(json.getString("id"));
            callDataRecord.setCallSuccessful(json.getBoolean("callSuccessful"));
            callDataRecord.setCalleeMastId(json.getString("calleeMastId"));
            callDataRecord.setCalleeTelno(json.getString("calleeTelno"));
            callDataRecord.setCallerMastId(json.getString("callerMastId"));
            callDataRecord.setCallerTelno(json.getString("callerTelno"));
            callDataRecord.setCreatedBy(json.getString("createdBy"));
            callDataRecord.setCreatedDate(json.getLong("createdDate"));
            callDataRecord.setDurationSeconds(json.getInt("durationSeconds"));
            // Regard everything saved by Hazelcast as changed by Hazelcast
            String previousLastModifiedBy = json.getString("lastModifiedBy");
            callDataRecord.setLastModifiedBy(this.modifierFilter);
            callDataRecord.setLastModifiedDate(now);
            callDataRecord.setStartTimestamp(json.getLong("startTimestamp"));

            if ("churn-update-legacy".equals(previousLastModifiedBy)) {
                LOGGER.trace("store({}, {}) not stored as LastModifiedBy='{}'",
                        key, value, previousLastModifiedBy);
            } else {
                LOGGER.debug("store({}, {}) with LastModifiedDate={}, LastModifiedBy='{}'",
                        key, value, now, this.modifierFilter);
                this.callDataRecordRepository.save(callDataRecord);
            }

        } catch (Exception exception) {
            LOGGER.error("store('{}', '{}'), EXCEPTION: {}", key, value, exception.getMessage());
        }
    }

    /**
     * <p>Use easy iterating store, could do bulk store
     * if available.
     * </p>
     *
     * @param entries
     */
    @Override
    public void storeAll(Map<CallDataRecordKey, HazelcastJsonValue> entries) {
        int expectedSize = entries.size();
        LOGGER.trace("storeAll({})", expectedSize);
        for (Map.Entry<CallDataRecordKey, HazelcastJsonValue> entry : entries.entrySet()) {
            this.store(entry.getKey(), entry.getValue());
        }
    }

}
