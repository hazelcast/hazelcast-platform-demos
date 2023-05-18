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

package com.hazelcast.platform.demos.telco.churn.mapstore;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.platform.demos.telco.churn.MyProperties;

/**
 * <p>Utilities to share across all map loaders.
 * </p>
 */
public class MyMapHelpers {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyMapHelpers.class);

    /**
     * <p>Check if the JSON object has the expected fields.
     * </p>
     *
     * @param json
     * @param fieldNames
     */
    public static void validate(JSONObject json, List<String> fieldNames) {
        Set<String> names = new TreeSet<>(fieldNames);

        json.names().forEach(name -> {
            if (fieldNames.contains(name)) {
                names.remove(name);
            } else {
                LOGGER.warn("Unexpected surplus field '{}' in {}", name, json);
            }
        });

        names.forEach(name -> {
            LOGGER.warn("Unexpected missing field '{}' in {}", name, json);
        });
    }

    /**
     * <p>On data records with a "{@code lastModifiedBy}" field,
     * define the value to use for modifications made by Hazelcast.
     * </p>
     *
     * @param myProperties
     * @return
     */
    public static String getModifiedBy(MyProperties myProperties) {
        return myProperties.getProject() + "-" + myProperties.getSite();
    }
}
