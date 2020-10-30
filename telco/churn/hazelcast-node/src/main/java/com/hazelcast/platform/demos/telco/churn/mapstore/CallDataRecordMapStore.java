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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.map.MapStore;
import com.hazelcast.platform.demos.telco.churn.domain.CallDataRecordRepository;

/**
 * <p>Load a {@link CallDataRecord} object from Cassandra and turn it into JSON.
 * </p>
 */
public class CallDataRecordMapStore implements MapStore<String, HazelcastJsonValue> {

    private CallDataRecordRepository callDataRecordRepository;

    CallDataRecordMapStore(CallDataRecordRepository arg0) {
        this.callDataRecordRepository = arg0;
    }

    @Override
    public HazelcastJsonValue load(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
    @Override
    public void store(String key, Team value) {
            log.info("'{}'::store('{}', '{}')",
                            this.league, key, value);
            this.teamRepository.save(value);
    }

    @Override
    public void storeAll(Map<String, Team> map) {
            log.error("'{}'::storeAll('{}'), not yet implemeted",
                            this.league, map.entrySet());
    }

    @Override
    public void delete(String key) {
            log.error("'{}'::delete('{}'), not yet implemeted",
                            this.league, key);
    }

    @Override
    public void deleteAll(Collection<String> keys) {
            log.error("'{}'::deleteAll('{}'), not yet implemeted",
                            this.league, keys);
    }
    */
    @Override
    public Map<String, HazelcastJsonValue> loadAll(Collection<String> arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterable<String> loadAllKeys() {
        // TODO Auto-generated method stub
        if (this.callDataRecordRepository == null) {
            return Collections.emptyList();
        }
        return null;
    }

    @Override
    public void delete(String arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public void deleteAll(Collection<String> arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public void store(String arg0, HazelcastJsonValue arg1) {
        // TODO Auto-generated method stub
    }

    @Override
    public void storeAll(Map<String, HazelcastJsonValue> arg0) {
        // TODO Auto-generated method stub
    }

}
