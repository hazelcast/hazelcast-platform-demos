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

package com.hazelcast.platform.demos.telco.churn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.nio.serialization.Portable;
import com.hazelcast.nio.serialization.PortableFactory;
import com.hazelcast.platform.demos.telco.churn.domain.Sentiment;

/**
 * <p>A factory to create objects that are
 * {@link com.hazelcast.nio.serialization.Portable Portable}
 * based on the type code received.
 * </p>
 */
public class MyPortableFactory implements PortableFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyPortableFactory.class);

    @Override
    public Portable create(int typeId) {
        switch (typeId) {
            case MyConstants.CLASS_ID_SENTIMENT:
                return new Sentiment();
            default:
                LOGGER.error("Unknown typeId: {}", typeId);
                return null;
        }
    }

}
