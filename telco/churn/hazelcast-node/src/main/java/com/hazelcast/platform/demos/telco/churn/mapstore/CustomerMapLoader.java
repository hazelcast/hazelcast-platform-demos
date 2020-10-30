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
import com.hazelcast.map.MapLoader;
import com.hazelcast.platform.demos.telco.churn.domain.CustomerRepository;

/**
 * <p>Load a {@link Customer} object from Mongo and turn it into JSON.
 * </p>
 */
public class CustomerMapLoader implements MapLoader<String, HazelcastJsonValue> {

    private CustomerRepository customerRepository;

    CustomerMapLoader(CustomerRepository arg0) {
        this.customerRepository = arg0;
    }

    @Override
    public HazelcastJsonValue load(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, HazelcastJsonValue> loadAll(Collection<String> arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterable<String> loadAllKeys() {
        // TODO Auto-generated method stub
        if (this.customerRepository == null) {
            return Collections.emptyList();
        }
        return null;
    }

}
