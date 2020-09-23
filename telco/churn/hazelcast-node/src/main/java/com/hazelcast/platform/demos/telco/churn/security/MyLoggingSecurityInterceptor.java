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

package com.hazelcast.platform.demos.telco.churn.security;

import java.security.AccessControlException;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.security.Credentials;
import com.hazelcast.security.Parameters;
import com.hazelcast.security.SecurityInterceptor;

/**
 * <p>This class is called before, and after, operations to enable you
 * to reject them with custom logic. Here we just use it for logging.
 * </p>
 */
public class MyLoggingSecurityInterceptor implements /*XXX HazelcastInstanceAware,*/ SecurityInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyLoggingSecurityInterceptor.class);

    private HazelcastInstance hazelcastInstance;

    /**
     * <p>Logging specific operations, here just what is happening on topics.
     * </p>
     * FIXME Add 'sqlService'
     *
     * @param credentials From the client
     * @param objectType Which server service, "{@code mapService}", "{@code topicService}", etc
     * @param objectName The map name for "{@code mapService}", topic name for "{@code topicService}", etc
     * @param methodName Operation, eg "{@code put}" or "{@code put}" for "{@code mapService}"
     * @param parameters Possibly an empty list, depends on the operation type
     * @throws AccessControlException If we wished to reject based on custom logic, eg time of day
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void before(Credentials credentials, String objectType,
                    String objectName, String methodName,
                    Parameters parameters) throws AccessControlException {

        if (objectType.endsWith("topicService")) {
            LOGGER.warn("THIS.HZ = {}", this.hazelcastInstance);
            LOGGER.warn("before({},{}, {}, {}, {})", credentials, objectType,
            objectName, methodName, parameters);
            Iterator iterator = parameters.iterator();
            while (iterator.hasNext()) {
                Object param = iterator.next();
                LOGGER.warn("  ==> PARAMS {}", param);
            }
        }
            /*XXX
            // No constants for "hz:impl:mapService" and "get" ?
            if (objectType.equals("hz:impl:mapService")
                            && objectName.equals("hamlet")
                            && methodName.equals("get")) {
                    if (parameters.length() == 1) {
                            Object key = parameters.get(0);
                            if (key instanceof Integer) {
                                    if (((Integer) key) % 2 == 0) {
                                            String message =
                                                String.format("Key '%s' rejected, EVEN, only ODD allowed", key);
                                            throw new AccessControlException(message);
                                    }
                            }
                    }
            }*/
    }

    /**
     * <p><i>After</i> interception is less useful, since the activity has
     * been allowed to happen, mainly for logging or adjusting
     * in-flight credentials.
     * </p>
     */
    @Override
    public void after(Credentials credentials, String objectType,
                    String objectName, String methodName,
                    Parameters parameters) {
    }

}
