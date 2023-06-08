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

package hazelcast.platform.demos.banking.transactionmonitor;

import java.util.Locale;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastInstance;

/**
 * <p>Mongo specific initialization
 * </p>
 * <p>Invoked by the overlarge {@link TransactionMonitorIdempotentInitialization}
 * </p>
 */
public class TransactionMonitorIdempotentInitializationMongo {
    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionMonitorIdempotentInitializationMongo.class);

    /**
     * <p>Define Mongo connection via SQL
     * </p>
     */
    static boolean defineMongo(HazelcastInstance hazelcastInstance, Properties properties,
            TransactionMonitorFlavor transactionMonitorFlavor) {

        try {
            String uri = MyUtils.buildMongoURI(properties);
            String database = "transaction-monitor-" + transactionMonitorFlavor.toString().toLowerCase(Locale.ROOT);

            String definition1 = "CREATE DATA CONNECTION IF NOT EXISTS "
                + MyConstants.MONGO_DATACONNECTION_CONFIG_NAME
                + " TYPE Mongo SHARED "
                + " OPTIONS ( "
                + " 'connectionString' = '" + uri + "'"
                + ",'database' = '" + database + "'"
                + " )";

            String definition2 = "CREATE MAPPING IF NOT EXISTS " + MyConstants.MONGO_COLLECTION
                    + " DATA CONNECTION " + MyConstants.MONGO_DATACONNECTION_CONFIG_NAME;

            boolean ok = true;
            ok = TransactionMonitorIdempotentInitialization.define(definition1, hazelcastInstance);
            if (ok) {
                ok &= TransactionMonitorIdempotentInitialization.define(definition2, hazelcastInstance);
            }
            return ok;
        } catch (Exception e) {
            LOGGER.error("defineMongo()", e);
            return false;
        }
    }

}
