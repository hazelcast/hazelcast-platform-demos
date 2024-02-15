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

package com.hazelcast.platform.demos.banking.cva.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hazelcast.aggregation.Aggregators;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.platform.demos.banking.cva.MyConstants;

/**
 * <p>For Excel live connect, return an HTML table for the latest
 * available CVA.
 * </p>
 */
@RestController
public class MyExcelController {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyExcelController.class);
    private static final String NEWLINE = System.getProperty("line.separator");

    @Autowired
    private HazelcastInstance hazelcastInstance;

    /**
     * <p>Find the latest value, and return in HTML form.
     * </p>
     *
     * @return An HTML table in String form
     */
    @GetMapping(value = "/excel", produces = MediaType.TEXT_PLAIN_VALUE)
    public String index() {
            LOGGER.info("index()");

            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("<table>" + NEWLINE);

            String latestKey = "";

            try {
                IMap<String, Object[][]> cvaDataMap
                    = this.hazelcastInstance.getMap(MyConstants.IMAP_NAME_CVA_DATA);

                latestKey = cvaDataMap.aggregate(Aggregators.comparableMax("__key"));
                LOGGER.trace("latest key '{}'", latestKey);

                Object[][] data = cvaDataMap.get(latestKey);

                // Table header
                Object[] header = data[0];
                stringBuilder.append(" <tr>");
                for (int i = 0; i < header.length; i++) {
                    stringBuilder.append("<th>" + header[i] + "</th>");
                }
                stringBuilder.append("</tr>" + NEWLINE);

                // Table data
                for (int i = 1; i < data.length ; i++) {
                    Object[] datum = data[i];
                    stringBuilder.append(" <tr>");
                    for (int j = 0; j < datum.length; j++) {
                        stringBuilder.append("<td>" + datum[j] + "</td>");
                    }
                    stringBuilder.append("</tr>" + NEWLINE);
                }

            } catch (Exception e) {
                LOGGER.error("latestKey=='" + latestKey + "'", e);
            }

            stringBuilder.append("</table>");
            return stringBuilder.toString();
    }

}
