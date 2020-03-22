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

package com.hazelcast.platform.demos.banking.cva;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.map.IMap;

/**
 * <p>Load a file with one JSON object per line into a map.
 * </p>
 */
@Service
public class JsonLoaderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonLoaderService.class);

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private HazelcastInstance hazelcastInstance;

    /**
     * <p>Read through the provided file, trying to parse each line as JSON
     * and write into the provided map. Find the key as an element in the JSON.
     * </p>
     */
    public boolean load(String mapName, String inputFileName, String keyFieldName) {
        IMap<String, HazelcastJsonValue> iMap = this.hazelcastInstance.getMap(mapName);

        int errors = 0;
        int read = 0;
        int written = 0;
        try {
            Resource resource = this.applicationContext.getResource("classpath:" + inputFileName);

            try (BufferedReader bufferedReader =
                    new BufferedReader(
                            new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    if (!line.startsWith("#")) {
                        try {
                            HazelcastJsonValue value = new HazelcastJsonValue(line);

                            // Needed until https://github.com/hazelcast/hazelcast/issues/15140
                            JSONObject json = new JSONObject(value.toString());
                            String key = json.get(keyFieldName).toString();

                            iMap.set(key, value);
                            written++;
                        } catch (Exception exception) {
                            errors++;
                            LOGGER.error("Line {} of {}: '{}' for '{}'", read, inputFileName,
                                    exception.getMessage(), line);
                        }
                    }
                    read++;
                }
            }
        } catch (IOException e) {
           errors++;
           LOGGER.error("Problem reading '" + inputFileName + "'", e);
        }

        if (errors == 0) {
            LOGGER.info("File '{}', read {} lines, wrote {} entries into map '{}'",
                    inputFileName, read, written, iMap.getName());
        } else {
            LOGGER.warn("File '{}', read {} lines, wrote {} entries into map '{}', {} errors",
                    inputFileName, read, written, iMap.getName(), errors);
        }
        return (errors == 0);
    }

}
