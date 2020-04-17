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

package com.hazelcast.platform.demos.banking.cva.controllers;

import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.map.IMap;
import com.hazelcast.platform.demos.banking.cva.MyConstants;

/**
 * <p>A controller for vending out REST requests, all of which
 * are prefixed by "{@code /rest}". So "{@code /rest/one}",
 * "{@code /rest/two}", "{@code /rest/three}" and so on.
 * </p>
 */
@RestController
@RequestMapping("/rest")
public class MyRestController {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyRestController.class);

    @Autowired
    private HazelcastInstance hazelcastInstance;

    /**
     * <p>List the keys of the counterparty CDS map.
     * <p>
     *
     * @return A String which Spring converts into JSON.
     */
    @GetMapping(value = "/test", produces = MediaType.APPLICATION_JSON_VALUE)
    public String test() {
        LOGGER.info("test()");

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{ ");

        // Return something visible, as the map may be empty.
        stringBuilder.append(" \"date\": \"" + new java.util.Date() + "\"");
        stringBuilder.append(", \"username\": \"" + System.getProperty("user.name") + "\"");

        // List all keys for the map as strings
        IMap<String, HazelcastJsonValue> iMap
            = this.hazelcastInstance.getMap(MyConstants.IMAP_NAME_CP_CDS);

        // For any may where KeySet() is manageable and Keys are Comparable this works
        stringBuilder.append(", \"" + iMap.getName() + "\": [");
        Object[] keys = new TreeSet<>(iMap.keySet()).toArray();
        for (int i = 0 ; i < keys.length ; i++) {
            if (i > 0) {
                stringBuilder.append(", ");
            }
            stringBuilder.append("\"" + keys[i] + "\"");
        }
        stringBuilder.append("]");

        stringBuilder.append(" }");
        return stringBuilder.toString();
    }

}
