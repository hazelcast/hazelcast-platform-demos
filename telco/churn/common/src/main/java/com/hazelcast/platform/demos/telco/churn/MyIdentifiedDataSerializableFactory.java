/*
 * Copyright (c) 2008-2021, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.platform.demos.telco.churn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.nio.serialization.DataSerializableFactory;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import com.hazelcast.platform.demos.telco.churn.security.MyCredentials;

/**
 * <p>A factory to create objects that are
 * {@link com.hazelcast.nio.serialization.IdentifiedDataSerializable IdentifiedDataSerializable}
 * based on the type code received.
 * </p>
 * <p>This is not currently used. It is present to demonstrate how credentials could
 * be provided from non-Java clients.
 * </p>
 */
public class MyIdentifiedDataSerializableFactory implements DataSerializableFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyIdentifiedDataSerializableFactory.class);

    @Override
    public IdentifiedDataSerializable create(int typeId) {
        switch (typeId) {
            case MyConstants.CLASS_ID_MYCREDENTIALS:
                return new MyCredentials();
            default:
                LOGGER.error("Unknown typeId: {}", typeId);
                return null;
        }
    }

}
