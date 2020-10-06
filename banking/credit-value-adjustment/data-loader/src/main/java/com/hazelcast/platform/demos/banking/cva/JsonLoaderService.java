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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

    private static final int MAX_DUPLICATES_TO_LOG = 10;
    private static final int PERIODIC_PROGRESS_INTERVAL = 50_000;

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private HazelcastInstance hazelcastInstance;

    private int duplicates;
    private int errors;
    private int read;
    private boolean stopEarly;
    private int threshold;
    private int written;

    public JsonLoaderService() {
        this.threshold = Application.getThreshold();
    }

    /**
     * <p>Read through the provided file, trying to parse each line as JSON
     * and write into the provided map. Find the key as an element in the JSON.
     * </p>
     * <p>If the file is a Zip, look for the files inside and process them.</p>
     */
    public boolean load(String mapName, String inputFileName, String keyFieldName,
            boolean okIfPartial) {
        IMap<String, HazelcastJsonValue> iMap = this.hazelcastInstance.getMap(mapName);

        this.duplicates = 0;
        this.errors = 0;
        this.read = 0;
        this.stopEarly = false;
        this.written = 0;

        Resource resource = this.applicationContext.getResource("classpath:" + inputFileName);

        if (inputFileName.endsWith(".zip")) {
            this.handleZipFile(resource, inputFileName, keyFieldName, iMap);
        } else {
            this.handleNormalFile(resource, inputFileName, keyFieldName, iMap);
        }

        if (this.duplicates == 0 && this.errors == 0) {
            LOGGER.info("File '{}', read {} lines, wrote {} entries into map '{}'",
                    inputFileName, this.read, this.written, iMap.getName());
        } else {
            LOGGER.warn("File '{}', read {} lines, wrote {} entries into map '{}', {} duplicates {} errors",
                    inputFileName, this.read, this.written, iMap.getName(),
                    this.duplicates, this.errors);
        }

        if (this.stopEarly && !okIfPartial) {
            LOGGER.warn("Partial load for '{}' map may cause issues, "
                    + "eg. for 'stage.mapUsingIMap(\"{}\", etc)'",
                    inputFileName, iMap.getName());
        }

        return (this.errors == 0);
    }


    /**
     * <p>Read each line of the input file, parse it as JSON and find the named
     * field. Used the named field as the key to insert into Hazelcast's map.
     * Assumes the JSON does not continue over the line.
     * </p>
     *
     * @param resource A zip file
     * @param inputFileName The file's name, for logging
     * @param keyFieldName The name of the key field in JSON
     * @param iMap The map to insert the JSON lines into
     */
    private void handleNormalFile(Resource resource, String inputFileName, String keyFieldName,
            IMap<String, HazelcastJsonValue> iMap) {
        try (BufferedReader bufferedReader =
                new BufferedReader(
                        new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line = bufferedReader.readLine();
            if (line != null) {
                this.read++;
            }
            while (!this.stopEarly && line != null) {
                if (!line.startsWith("#")
                        && (!line.contains("curvescenario") || line.contains("curvescenario0206"))) {
                    try {
                        HazelcastJsonValue value = new HazelcastJsonValue(line);

                        // Needed until https://github.com/hazelcast/hazelcast/issues/15140
                        JSONObject json = new JSONObject(value.toString());
                        String key = json.get(keyFieldName).toString();

                        this.testDuplicate(iMap, key, line);
                        this.tryWrite(iMap, key, value);

                    } catch (Exception exception) {
                        this.errors++;
                        LOGGER.error("Line {} of {}: '{}' for '{}'", this.read, inputFileName,
                                exception.getMessage(), line);
                    }
                }
                if (!this.stopEarly) {
                    line = bufferedReader.readLine();
                    if (line != null) {
                        this.read++;
                    }
                    if (this.read % PERIODIC_PROGRESS_INTERVAL == 0) {
                        LOGGER.debug("Line {} of {} ... processing", this.read, inputFileName);
                    }
                }
            }
        } catch (IOException e) {
            this.errors++;
            LOGGER.debug("Processing line {} of {} ...", this.read, inputFileName);
        }
    }

    /**
     * <p>ZIP friendly version of {@link #handleNormalFile()}, which takes
     * a ZIP file and applies the same kind of processing to each file in
     * the ZIP. There should really only be one.
     * </p>
     *
     * @param resource A zip file
     * @param inputFileName The file's name, for logging
     * @param keyFieldName The name of the key field in JSON
     * @param iMap The map to insert the JSON lines into
     */
    private void handleZipFile(Resource resource, String inputFileName, String keyFieldName,
            IMap<String, HazelcastJsonValue> iMap) {
        Scanner scanner = null;
        try (BufferedInputStream bufferedInputStream =
                new BufferedInputStream(resource.getInputStream());
             ZipInputStream zipInputStream = new ZipInputStream(bufferedInputStream)) {
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                    scanner = new Scanner(zipInputStream, StandardCharsets.UTF_8);
                    while (!this.stopEarly && scanner.hasNextLine()) {
                        if (scanner.ioException() != null) {
                            throw scanner.ioException();
                        }
                        String line = scanner.nextLine();
                        this.read++;
                        if (!line.startsWith("#") && !line.contains("\"settlement_date\":\"0\"")) {
                            try {
                                HazelcastJsonValue value = new HazelcastJsonValue(line);

                                // Needed until https://github.com/hazelcast/hazelcast/issues/15140
                                JSONObject json = new JSONObject(value.toString());
                                String key = json.get(keyFieldName).toString();

                                this.testDuplicate(iMap, key, line);
                                this.tryWrite(iMap, key, value);

                            } catch (Exception exception) {
                                this.errors++;
                                LOGGER.error("Line {} of {}: '{}' for '{}'", this.read, inputFileName,
                                        exception.getMessage(), line);
                            }
                        }
                        if (this.read % PERIODIC_PROGRESS_INTERVAL == 0) {
                            LOGGER.debug("Processing line {} of {} ...", this.read, inputFileName);
                        }
                    }
                    zipEntry = zipInputStream.getNextEntry();
            }
            if (scanner != null) {
                scanner.close();
            }
        } catch (IOException e) {
            this.errors++;
            LOGGER.error("Problem reading '" + inputFileName + "'", e);
        }
    }

    /**
     * <p>Write an entry unless we have hit any threshold defined.</p>
     *
     * @param iMap The Hazelcast map
     * @param key Entry key
     * @param value Entry value
     */
    private void tryWrite(IMap<String, HazelcastJsonValue> iMap, String key, HazelcastJsonValue value) {
        if (this.threshold > 0) {
            if (this.written < this.threshold) {
                iMap.set(key, value);
                this.written++;
            }
            if (this.written == this.threshold) {
                this.stopEarly = true;
            }
        } else {
            iMap.set(key, value);
            this.written++;
        }
    }

    /**
     * <p>Common code to test if a map contains an item we are due to
     * insert. Input should be unique, but data loader may be run
     * twice by mistake, so only log the first few duplicates.
     * </p>
     *
     * @param iMap Target map to test
     * @param key Key to test presence
     * @param line Value to log some of
     */
    private void testDuplicate(IMap<String, HazelcastJsonValue> iMap, String key, String line) {
        if (iMap.containsKey(key)) {
            this.duplicates++;
            if (this.duplicates <= MAX_DUPLICATES_TO_LOG) {
                if (line.length() > MyConstants.HALF_SCREEN_WIDTH) {
                    LOGGER.warn("Duplicate key '{}' on '{} ......", key,
                            line.substring(0, MyConstants.HALF_SCREEN_WIDTH));
                } else {
                    LOGGER.warn("Duplicate key '{}' on '{}'", key, line);
                }
            }
        }
    }

}
