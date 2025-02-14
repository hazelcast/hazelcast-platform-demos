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

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.sql.SqlResult;

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
    @Autowired
    private MyProperties myProperties;

    /**
     * <p>Provide a URL for Kubernetes to test the client is alive.
     * </p>
     *
     * @return Any String, doesn't matter, so why not the build timestamp.
     */
    @GetMapping(value = "/k8s")
    public String k8s() {
        LOGGER.trace("k8s()");
        return myProperties.getBuildTimestamp();
    }

    /**
     * <p>Return the size of the important maps, also available in Management
     * Center, except we may not have permissions</p>
     *
     * @return
     */
    @GetMapping(value = "/mapSizes", produces = MediaType.APPLICATION_JSON_VALUE)
    public String mapSizes() {
        LOGGER.trace("mapSizes()");

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{ \"sizes\": [");

        // Alphabetical order, handling security error
        Set<String> mapNames = new TreeSet<>(MyConstants.IMAP_NAMES);
        int count = 0;
        for (String mapName : mapNames) {
            stringBuilder.append("{ \"name\": \"" + mapName + "\",");
            try {
                int size = this.hazelcastInstance.getMap(mapName).size();
                stringBuilder.append("\"error\": \"\", \"size\": " + size + " }");
            } catch (Exception e) {
                stringBuilder.append("\"error\": \""
                        + MyUtils.safeForJsonStr(e.getMessage()) + "\", \"size\": -1 }");
            }
            count++;
            if (count < mapNames.size()) {
                stringBuilder.append(", ");
            }
        }

        stringBuilder.append("] }");
        return stringBuilder.toString();
    }

    /**
     * <p>Run an SQL query via REST
     * </p>
     *
     * @param query Already HTML decoded.
     * @return
     */
    @GetMapping(value = "/sql", produces = MediaType.APPLICATION_JSON_VALUE)
    public String sql(@RequestParam("query") String query) {
        LOGGER.info("sql(query '{}')", query);

        try {
            SqlResult sqlResult = this.hazelcastInstance.getSql().execute(query);
            Tuple3<String, String, List<String>> result =
                    MyUtils.prettyPrintSqlResult(sqlResult);

            // Turn tuple3 into JSON
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("{ ");
            stringBuilder.append(" \"error\": \"")
                .append(MyUtils.safeForJsonStr(result.f0())).append("\"");
            stringBuilder.append(", \"warning\": \"")
                .append(MyUtils.safeForJsonStr(result.f1())).append("\"");
            stringBuilder.append(", \"rows\": [");
            for (int i = 0; i < result.f2().size(); i++) {
                if (i > 0) {
                    stringBuilder.append(",");
                }
                stringBuilder.append(" \"" + MyUtils.safeForJsonStr(result.f2().get(i)) + "\"");
            }
            stringBuilder.append(" ] }");

            LOGGER.trace("sql(query '{}') => '{}'", query, stringBuilder);
            return stringBuilder.toString();
        } catch (Exception e) {
            LOGGER.error(String.format("sql(query '%s')", query), e);

            return "{ \"error\": \""
                    + MyUtils.safeForJsonStr(e.getMessage()) + "\""
                    + ", \"warning\": \"\""
                    + ", \"rows\" : [] }";
        }
    }
}
