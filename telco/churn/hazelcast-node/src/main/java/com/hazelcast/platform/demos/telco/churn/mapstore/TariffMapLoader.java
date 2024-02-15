/*
 * Copyright (c) 2008-2024, Hazelcast, Inc. All Rights Reserved.
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

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.map.MapLoader;
import com.hazelcast.platform.demos.telco.churn.domain.Tariff;
import com.hazelcast.platform.demos.telco.churn.domain.TariffMetadata;
import com.hazelcast.platform.demos.telco.churn.domain.TariffRepository;

/**
 * <p>Load a {@link Tariff} object from MySql and turn it into JSON.
 * </p>
 */
public class TariffMapLoader implements MapLoader<String, HazelcastJsonValue> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TariffMapLoader.class);

    private TariffRepository tariffRepository;

    TariffMapLoader(TariffRepository arg0) {
        this.tariffRepository = arg0;
    }

    /**
     * <p>Retrieve a domain object, and turn it into JSON.
     * Validate it has the expected fields, but don't reject if not.
     * </p>
     */
    @Override
    public HazelcastJsonValue load(String key) {
        LOGGER.trace("load('{}')", key);

        HazelcastJsonValue result = null;

        try {
            Tariff tariff = this.tariffRepository.findById(key).get();

            if (tariff != null) {
                JSONObject json = new JSONObject(tariff);
                MyMapHelpers.validate(json, TariffMetadata.getFieldNames());
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
            int year = LocalDate.now().getYear();
            List<String> results = this.tariffRepository.findThisYearsTariffs(year);

            if (results.size() == 0) {
                LOGGER.error("loadAllKeys() -> {} for year {}, was preload-legacy run?",
                        results.size(), year);
            } else {
                LOGGER.debug("loadAllKeys() -> {}", results.size());
            }

            return results;
        } catch (Exception e) {
            LOGGER.error("loadAllKeys()", e);
            return Collections.emptyList();
        }
    }

}
