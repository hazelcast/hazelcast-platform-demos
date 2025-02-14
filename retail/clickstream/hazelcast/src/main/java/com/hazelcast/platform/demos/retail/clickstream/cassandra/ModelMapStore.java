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

package com.hazelcast.platform.demos.retail.clickstream.cassandra;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;

import com.hazelcast.map.MapStore;
import com.hazelcast.platform.demos.retail.clickstream.MyUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Model storage, no pre-loading.
 * </p>
 */
@Slf4j
public class ModelMapStore implements MapStore<String, String> {

    private final ModelRepository modelRepository;

    public ModelMapStore(ModelRepository arg0, String arg1) {
        this.modelRepository = arg0;
        log.trace("ModelVaultMapStore({}) to {}", this.modelRepository, arg1);
    }

    /**
     * <p>Load one key. Should only be called on demand.
     * </p>
     */
    @Override
    public String load(String arg0) {
        log.trace("load('{}')", arg0);

        String result = null;
        try {
            Model model = this.modelRepository.findById(arg0).get();

            if (model != null) {
                result = model.getPayload();
            }
        } catch (NoSuchElementException nsse) {
            // Not found is ok, shouldn't be an exception
        } catch (Exception exception) {
            log.error("load('{}'), EXCEPTION({}): {}", arg0,
                    exception.getClass().getSimpleName(),
                    exception.getMessage());
        }

        log.debug("load('{}') -> {}", arg0, MyUtils.truncateToString(result));
        return result;
    }

    /**
     * <p>Per-partition pre-loader helper, should not be called
     * since pre-loading is deactivated.
     * </p>
     */
    @Override
    public Map<String, String> loadAll(Collection<String> arg0) {
        int expectedSize = arg0.size();
        log.error("loadAll({})", expectedSize);
        return Collections.emptyMap();
    }

    /**
     * <p>Models are paged in on demand, not pre-loaded.
     * </p>
     */
    @Override
    public Iterable<String> loadAllKeys() {
        log.trace("loadAllKeys()");
        return Collections.emptyList();
    }

    /**
     * <p>Individual delete, deliberately not implemented, models
     * should not be deleted via the MapStore.
     */
    @Override
    public void delete(String arg0) {
        log.error("delete('{}')", arg0);
    }

    /**
     * <p>Bulk delete, not expected to be called for model store.
     * </p>
     */
    @Override
    public void deleteAll(Collection<String> arg0) {
        int size = arg0.size();
        log.error("deleteAll({})", size);
        arg0.stream().forEach(key -> this.delete(key));
    }

    /**
     * <p>Individual save.
     * </p>
     */
    @Override
    public void store(String arg0, String arg1) {
        log.debug("store('{}')", arg0);
        try {
            Model model = new Model();
            model.setId(arg0);
            model.setPayload(arg1);

            this.modelRepository.save(model);
        } catch (Exception exception) {
            log.error("store('{}'), EXCEPTION: {}", arg0, exception.getMessage());
        }
    }

    /**
     * <p>Bulk store, not expected to be called for model store.
     * </p>
     */
    @Override
    public void storeAll(Map<String, String> arg0) {
        int size = arg0.size();
        log.error("storeAll({})", size);
        arg0.entrySet().stream().forEach(entry -> this.store(entry.getKey(), entry.getValue()));
    }

}
