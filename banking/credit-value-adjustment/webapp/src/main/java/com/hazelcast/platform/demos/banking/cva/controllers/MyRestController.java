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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.map.IMap;
import com.hazelcast.platform.demos.banking.cva.MyConstants;
import com.hazelcast.platform.demos.banking.cva.MyProperties;
import com.hazelcast.platform.demos.banking.cva.cvastp.CvaStpJobSubmitter;

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
    private JetInstance jetInstance;
    @Autowired
    private MyProperties myProperties;

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

    /**
     * <p>Return all fixing dates and rates.
     * <p>
     *
     * @return A String which Spring converts into JSON.
     */
    @GetMapping(value = "/fixings", produces = MediaType.APPLICATION_JSON_VALUE)
    public String fixings() {
        LOGGER.info("fixings()");

        IMap<String, HazelcastJsonValue> fixingsMap
            = this.hazelcastInstance.getMap(MyConstants.IMAP_NAME_FIXINGS);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{ \"fixings\": [");

        TreeMap<String, HazelcastJsonValue> sortedMap = new TreeMap<>();
        sortedMap.putAll(fixingsMap);

        Iterator<Entry<String, HazelcastJsonValue>> iterator = sortedMap.entrySet().iterator();

        // Pretty print some of the JSON
        int count = 0;
        while (iterator.hasNext()) {
            if (count > 0) {
                stringBuilder.append(", ");
            }
            Entry<String, HazelcastJsonValue> entry = iterator.next();
            try {

                StringBuilder innerStringBuilder = new StringBuilder();
                innerStringBuilder.append("{ \"curvename\": \"" + entry.getKey() + "\"");

                JSONObject jsonObject = new JSONObject(entry.getValue().toString());

                // Processed and unprocessed fixing dates
                JSONArray fixingDates = jsonObject.getJSONArray("fixing_dates");
                this.appendFixingDates(innerStringBuilder, fixingDates, true);
                this.appendFixingDates(innerStringBuilder, fixingDates, false);

                // Unprocessed fixing rates
                JSONArray fixingRates = jsonObject.getJSONArray("fixing_rates");
                innerStringBuilder.append(", \"fixing_rates\": [");
                for (int i = 0 ; i < fixingRates.length() ; i++) {
                    if (i > 0) {
                        innerStringBuilder.append(", ");
                    }
                    double fixingRate = fixingRates.getDouble(i);
                    innerStringBuilder.append(fixingRate);
                }
                innerStringBuilder.append(" ]");

                // Past point of possible exceptions, safe to append intermediate result
                stringBuilder.append(innerStringBuilder + "} ");
                count++;
            } catch (JSONException e) {
                LOGGER.error(entry.getKey(), e);
            }
        }

        stringBuilder.append("] }");
        return stringBuilder.toString();
    }

    /**
     * <p>Process fixing dates from JSON, using a flag to determine whether to show
     * "{@code 1483084800}" or "{@code 2017-12-31}".
     *
     * @param innerStringBuilder
     * @param fixingDates Array of longs
     * @param prettyPrint Whether to print as local date or long
     * @throws JSONException
     */
    private void appendFixingDates(StringBuilder innerStringBuilder, JSONArray fixingDates, boolean prettyPrint)
            throws JSONException {
        if (prettyPrint) {
            innerStringBuilder.append(", \"fixing_dates_ccyymmdd\": [");
        } else {
            innerStringBuilder.append(", \"fixing_dates\": [");
        }

        for (int i = 0 ; i < fixingDates.length() ; i++) {
            if (i > 0) {
                innerStringBuilder.append(", ");
            }
            long fixingDate = fixingDates.getLong(i);

            if (prettyPrint) {
                long when =  TimeUnit.MILLISECONDS.convert(fixingDate, TimeUnit.SECONDS);
                LocalDate localDate =
                        Instant.ofEpochMilli(when).atZone(ZoneId.systemDefault()).toLocalDate();
                innerStringBuilder.append("\"" + localDate + "\"");
            } else {
                innerStringBuilder.append(fixingDate);
            }
        }
        innerStringBuilder.append(" ]");
    }


    /**
     * <p>Launch the CVA run for the given calculation date.
     * <p>
     *
     * @return A String which Spring converts into JSON.
     */
    @GetMapping(value = "/cva/run", produces = MediaType.APPLICATION_JSON_VALUE)
    public String cvaRun(@RequestParam("batch_size") int batchSize,
            @RequestParam("calc_date") String calcDateStr,
            @RequestParam("debug") boolean debug,
            @RequestParam("parallelism") int parallelism) {
        LOGGER.info("cvaRun(batch size '{}',calc date '{}',debug '{}',parallelism '{}')",
                batchSize, calcDateStr, debug, parallelism);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{ \"date\": \"" + new Date() + "\"");
        stringBuilder.append(", \"batchSize\": \"" + batchSize + "\"");
        stringBuilder.append(", \"calcDate\": \"" + calcDateStr + "\"");
        stringBuilder.append(", \"debug\": \"" + debug + "\"");
        stringBuilder.append(", \"parallelism\": \"" + parallelism + "\"");

        try {
            LocalDate calcDate = LocalDate.parse(calcDateStr);
            Job job = CvaStpJobSubmitter.submitCvaStpJob(this.jetInstance,
                    calcDate, batchSize, parallelism, debug);

            stringBuilder.append(", \"id\": \"" + job.getId() + "\"");
            stringBuilder.append(", \"name\": \"" + job.getName() + "\"");
            stringBuilder.append(", \"error\": " + false + "");
            stringBuilder.append(", \"error_message\": \"\"");
        } catch (Exception e) {
            stringBuilder.append(", \"id\": \"\"");
            stringBuilder.append(", \"name\": \"\"");
            stringBuilder.append(", \"error\": " + true + "");
            stringBuilder.append(", \"error_message\": \"" + e.getMessage() + "\"");
        }

        stringBuilder.append(" }");
        return stringBuilder.toString();
    }

    /**
     * <p>List available files for download, as help for Swagger and direct REST debugging.
     * Bakes in the URL expected by {@link fileDownload} below.
     * </p>
     *
     * @return A possibly empty string
     */
    @GetMapping(value = "/downloads", produces = MediaType.APPLICATION_JSON_VALUE)
    public String availableDownloads(HttpServletRequest httpServletRequest) {
        LOGGER.info("availableDownloads()");

        String host = httpServletRequest.getServerName();
        int port = httpServletRequest.getServerPort();

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{ \"url_available_at\": \"" + new Date() + "\"");
        stringBuilder.append(", \"urls\": [");

        List<String> mapNames = List.of(MyConstants.IMAP_NAME_CVA_CSV, MyConstants.IMAP_NAME_CVA_XLSX);
        int urlCount = 0;
        for (String mapName : mapNames) {
            IMap<String, ?> iMap =
                this.hazelcastInstance.getMap(mapName);

            for (String key : iMap.keySet()) {
                if (urlCount > 0) {
                    stringBuilder.append(", ");
                }
                String url = "http://" + host + ":" + port + "/rest/download/" + mapName;
                stringBuilder.append("\"" + url + "?key=" + URLEncoder.encode(key, StandardCharsets.UTF_8) + "\"");
                urlCount++;
            }
        }
        stringBuilder.append("] }");

        return stringBuilder.toString();
    }

    /**
     * <p>A rest endpoint to look in a specific map (in the path) for a
     * specific key (in the param), and return this as a CSV file, Excel
     * spreadsheet or unknown download type.
     * <p>
     * <p>Call "{@code /rest/download/abc?key=def}" to try to find the
     * key "{@code def}" in the map "{@code abc}".
     * <p>
     * <p>The key may contain characters that don't work in a path, hence
     * why it is a param.
     * </p>
     *
     * @param requestMapName Should exist, won't be created on demand
     * @param requestKey Should exist, can't be created
     * @return CSV, Excel or bytes
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @GetMapping(value = "/download/{map}")
    public ResponseEntity fileDownload(
            @PathVariable("map") String requestMapName,
            @RequestParam("key") String requestKey) {
        LOGGER.info("fileDownload('{}', '{}')", requestMapName, requestKey);

        // Find mapout with doing lazy-evaluation create
        IMap<String, Object> iMap = null;
        for (DistributedObject distributedObject : this.hazelcastInstance.getDistributedObjects()) {
            if (distributedObject instanceof IMap
                    && distributedObject.getName().equalsIgnoreCase(requestMapName)) {
                iMap = (IMap<String, Object>) distributedObject;
            }
        }
        if (iMap == null) {
            LOGGER.info("fileDownload('{}', '{}'), map not found", requestMapName, requestKey);
            return null;
        }

        // Find first key match
        Object value = iMap.get(requestKey);
        if (value == null) {
            LOGGER.error("fileDownloadCSV('{}'), key not found", requestKey);
            return null;
        }

        try {
            byte[] content;
            MediaType mediaType;
            String suggestedFilename;

            switch (iMap.getName()) {
                case MyConstants.IMAP_NAME_CVA_CSV:
                    content = (byte[]) value;
                    mediaType = new MediaType("text",
                            "csv",
                            StandardCharsets.UTF_8);
                    suggestedFilename = requestKey + ".csv";
                    break;
                case MyConstants.IMAP_NAME_CVA_XLSX:
                    content = (byte[]) value;
                    mediaType = new MediaType("application",
                            "vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            StandardCharsets.UTF_8);
                    suggestedFilename = requestKey + ".xlsx";
                    break;
                default:
                    LOGGER.warn("Unexpected map '{}', data type unknown", iMap.getName());
                    content = value.toString().getBytes(StandardCharsets.UTF_8);
                    mediaType = MediaType.APPLICATION_OCTET_STREAM;
                    suggestedFilename = requestKey;
            }

            return ResponseEntity
                    .ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + suggestedFilename)
                    .contentLength(content.length)
                    .contentType(mediaType)
                    .body(content);

        } catch (Exception e) {
            String prefix = String.format("fileDownload('%s', '%s')", requestMapName, requestKey);
            LOGGER.error(prefix, e);
            return null;
        }
    }

    /**
     * <p>Useful for testing, return any one item from a map.
     * </p>
     *
     * @param mapName Map name, only those that can be specified as a param
     * @return Some JSON
     */
    @GetMapping(value = "/getany/{map}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getAny(
            @PathVariable("map") String mapName) {
        LOGGER.info("getAny('{}')", mapName);

        IMap<?, ?> map = null;
        for (DistributedObject distributedObject : this.hazelcastInstance.getDistributedObjects()) {
            if (distributedObject instanceof IMap && distributedObject.getName().endsWith(mapName)) {
                map = (IMap<?, ?>) distributedObject;
            }
        }

        if (map == null) {
            return "{}";
        }

        // Hopefully not too big for memory
        Set<?> keys = map.keySet();

        if (keys.size() == 0) {
            return "{}";
        } else {
            // Hopefully JSON
            Object key = keys.iterator().next();
            return map.get(key).toString();
        }
    }

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
     * Center, but to help prove Data Loader and/or WAN replication.
     * </p>
     *
     * @return
     */
    @GetMapping(value = "/mapSizes", produces = MediaType.APPLICATION_JSON_VALUE)
    public String mapSizes() {
        LOGGER.trace("mapSizes()");

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{ \"sizes\": [");

        // Alphabetical order
        Set<String> mapNames = new TreeSet<>(MyConstants.IMAP_NAMES);
        // "data" map is internal, used as basis to build CSV and XLSX
        mapNames.remove(MyConstants.IMAP_NAME_CVA_DATA);
        int count = 0;
        for (String mapName : mapNames) {
            int size = this.hazelcastInstance.getMap(mapName).size();
            stringBuilder.append("{ \"name\": \"" + mapName + "\", \"size\": " + size + " }");
            count++;
            if (count < mapNames.size()) {
                stringBuilder.append(", ");
            }
        }

        stringBuilder.append("] }");
        return stringBuilder.toString();
    }

    /**
     * <p>Map keys for download in JSON format, reverse collating sequence.
     * </p>
     *
     * @return
     */
    @GetMapping(value = "/mapKeysForDownload", produces = MediaType.APPLICATION_JSON_VALUE)
    public String mapKeysForDownload() {
        LOGGER.trace("mapKeysForDownload()");

        // Keys for CSV
        IMap<?, ?> csvMap = this.hazelcastInstance.getMap(MyConstants.IMAP_NAME_CVA_CSV);
        TreeSet<String> keyNamesCsv = csvMap.keySet()
        .stream()
        .map(key -> key.toString() + ",.csv")
        .collect(Collectors.toCollection(TreeSet::new));

        // Keys for XLSX
        IMap<?, ?> xlsxMap = this.hazelcastInstance.getMap(MyConstants.IMAP_NAME_CVA_XLSX);
        TreeSet<String> keyNamesXlsx = xlsxMap.keySet()
        .stream()
        .map(key -> key.toString() + ",.xlsx")
        .collect(Collectors.toCollection(TreeSet::new));

        // Combined keys
        TreeSet<String> keyNames = new TreeSet<>(keyNamesCsv);
        keyNames.addAll(keyNamesXlsx);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{ \"downloads\": [");

        // Most recent first
        int count = 0;
        for (String key : keyNames.descendingSet()) {
            String[] tokens = key.split(",");
            stringBuilder.append("{ \"date\": \"" + tokens[0] + "\", ");
            stringBuilder.append("\"kind\": \"" + tokens[1] + "\" }");
            count++;
            if (count < keyNames.size()) {
                stringBuilder.append(", ");
            }
        }

        stringBuilder.append("] }");
        return stringBuilder.toString();
    }

}
