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

package hazelcast.platform.demos.banking.transactionmonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.vector.VectorCollection;

/**
 * <p>Standard initialization plus enterprise-only parts.
 * </p>
 */
public class ApplicationInitializerEnterprise {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationInitializerEnterprise.class);

    /**
     * <p>General initialization, then Enterprise specifics
     * </p>
     */
    public static void build(String[] args) throws Exception {
        ApplicationInitializer.build(args);
        HazelcastInstance hazelcastInstance = Hazelcast.getAllHazelcastInstances().iterator().next();

        // Force visibility on Management Center
        VectorCollection<?, ?> transactions =
                VectorCollection.getCollection(hazelcastInstance, MyConstants.VECTOR_COLLECTION_TRANSACTIONS);
        LOGGER.info("VectorCollection<?, ?> transactions = '{}'", transactions.getName());
    }


}
