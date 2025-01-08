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

package hazelcast.platform.demos.banking.transactionmonitor;

import java.util.Collection;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

/**
 * <p>Provides an instance of an {@link IMapLogger}, pre-configured with
 * it a reference to its Hazelcast instance.
 * </p>
 */
public class IMapLoggerFactory implements ILoggerFactory {

    private static HazelcastInstance hazelcastInstance;
    private static Level level = Level.INFO;

    /**
     * <p>In normal usage there will only be one Hazelcast instance in the current JVM,
     * so use this. If more than one is found, default to non-Hazelcast logging.
     * </p>
     */
    public static synchronized Logger getLogger(Class<?> klass) {
        if (hazelcastInstance == null) {
            Collection<HazelcastInstance> serverInstances = Hazelcast.getAllHazelcastInstances();
            Collection<HazelcastInstance> clientInstances = HazelcastClient.getAllHazelcastClients();

            int instances = serverInstances.size() + clientInstances.size();
            if (instances == 1) {
                hazelcastInstance = serverInstances.iterator().hasNext()
                        ? serverInstances.iterator().next() : clientInstances.iterator().next();
            } else {
                Logger logger = LoggerFactory.getLogger(klass);
                logger.error("Hazelcast Instances found: {} instead of 1, default to standard logger",
                        instances);
                return logger;
            }
        }

        return new IMapLogger(klass.getSimpleName(), hazelcastInstance, level);
    }

    public static void setLevel(Level arg0) {
        level = arg0;
    }

    @Override
    public Logger getLogger(String name) {
        return LoggerFactory.getLogger(name);
    }

}
