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
import com.hazelcast.platform.demos.telco.churn.domain.Customer;
import com.hazelcast.platform.demos.telco.churn.domain.CustomerMetadata;
import com.hazelcast.platform.demos.telco.churn.domain.CustomerRepository;

/**
 * <p>Load a {@link Customer} object from Mongo and turn it into JSON.
 * Save it back when it changes.
 * </p>
 */
public class CustomerMapStore implements MapStore<String, HazelcastJsonValue> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerMapStore.class);

    private CustomerRepository customerRepository;
    private String modifierFilter;

    CustomerMapStore(CustomerRepository arg0, String arg1) {
        this.customerRepository = arg0;
        this.modifierFilter = arg1;
    }

    /**
     * <p>The domain object from Mongo is JSON, but
     * validate the fields still.
     * </p>
     */
    @Override
    public HazelcastJsonValue load(String key) {
        LOGGER.trace("load('{}')", key);

        HazelcastJsonValue result = null;

        try {
            Customer customer = this.customerRepository.findById(key).get();

            if (customer != null) {
                JSONObject json = new JSONObject(customer);
                MyMapHelpers.validate(json, CustomerMetadata.FIELD_NAMES);
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
    public Map<String, HazelcastJsonValue> loadAll(Collection<String> keys) {
        int expectedSize = keys.size();
        LOGGER.trace("loadAll({})", expectedSize);

        Map<String, HazelcastJsonValue> result = new HashMap<>();
        for (String key : keys) {
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
    public Iterable<String> loadAllKeys() {
        LOGGER.trace("loadAllKeys()");

        try {
            List<String> resultsProjection = this.customerRepository.findOnlyId();
            List<String> results = new ArrayList<>(resultsProjection.size());

            for (Object result : resultsProjection) {
                JSONObject json = new JSONObject(result.toString());
                // The projection prefixes the projected field name with "_"
                results.add(json.get("_" + CustomerMetadata.ID).toString());
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
     * <p>Customers shouldn't be deleted from Hazelcast
     * and certainly shouldn't be deleted from system of
     * record.
     * </p>
     */
    @Override
    public void delete(String key) {
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
    public void deleteAll(Collection<String> keys) {
        int expectedSize = keys.size();
        LOGGER.trace("deleteAll({})", expectedSize);
        for (String key : keys) {
            this.delete(key);
        }
    }

    /**
     * <p>Reformat and save.
     * </p>
     */
    @Override
    public void store(String key, HazelcastJsonValue value) {
        LOGGER.debug("store({}, {})", key, value);
        //FIXME ONLY SAVE IF INCOMING MODIFIED SAME AS PARM!
        long now = System.currentTimeMillis();
        LOGGER.error("store needs to add '{}' and '{}'", this.modifierFilter, now);
    }

    /**
     * <p>Use easy iterating store, could do bulk store
     * if available.
     * </p>
     *
     * @param entries
     */
    @Override
    public void storeAll(Map<String, HazelcastJsonValue> entries) {
        int expectedSize = entries.size();
        LOGGER.trace("storeAll({})", expectedSize);
        for (Map.Entry<String, HazelcastJsonValue> entry : entries.entrySet()) {
            this.store(entry.getKey(), entry.getValue());
        }
    }

}
