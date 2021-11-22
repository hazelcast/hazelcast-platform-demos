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

package hazelcast.platform.demos.industry.iiot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MapStoreConfig;
import com.hazelcast.jet.datamodel.Tuple4;
import com.hazelcast.map.IMap;

import hazelcast.platform.demos.industry.iiot.mapstore.ServiceHistoryMapLoader;

/**
 * <p>Idempotent initialization steps to be run on any node
 * after initialization on all node.
 * </p>
 * <p>Ie. member specific runs before cluster specific.
 * </p>
 */
public class InitializerAnyNode extends Tuple4Callable {
    private static final long serialVersionUID = 1L;
    private static final String MY_NAME = InitializerAnyNode.class.getSimpleName();

    private final String[][] config;

    InitializerAnyNode(String[][] arg0) {
        this.config = arg0;
    }

    @Override
    public List<Tuple4<String, String, List<String>, List<String>>> call() throws Exception {
        List<Tuple4<String, String, List<String>, List<String>>> result = new ArrayList<>();

        // Define mappings
        MappingDefinitions.addMappings(super.getHazelcastInstance())
        .stream().forEach(tuple4 -> result.add(tuple4));

        // Add config before touching maps that may have MapStores that use config
        boolean ok = this.populateConfig(result);
        if (ok) {
            this.addMapStores(result);
            // Define needed objects
            this.touchAllIMaps(result);
        }

        return result;
    }

    /**
     * <p>Iterate through the entries to insert to the config map. May
     * be empty if already inserted.
     * </p>
     *
     * @param result
     * @return True if all mandatory config provided
     */
    private boolean populateConfig(List<Tuple4<String, String, List<String>, List<String>>> result) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        String diagnostic = "";

        if (this.config == null) {
            errors.add("config is null");
        } else {
            int count = 0;
            for (String[] pair : this.config) {
                this.populateConfigEntry(pair, count, errors, warnings);
                count++;
            }
            if (errors.size() == 0) {
                diagnostic = "Inserted " + count + " item" + (count == 1 ? "" : "s");
            }
        }

        // Validate what is present
        Map<String, String> configMap = this.getHazelcastInstance().getMap(MyConstants.IMAP_NAME_SYS_CONFIG);
        for (String key : MyConstants.CONFIG_REQUIRED) {
            String value = configMap.get(key);
            if (value == null || MyConstants.CONFIG_VALUE_PLACEHOLDER.equals(value)) {
                errors.add("No value for '" + key + "', got " + value);
            }
        }
        for (String key : MyConstants.CONFIG_OPTIONAL) {
            String value = configMap.get(key);
            if (value == null || MyConstants.CONFIG_VALUE_PLACEHOLDER.equals(value)) {
                errors.add("No value for '" + key + "', got " + value);
            }
        }

        result.add(Tuple4.tuple4(MY_NAME + ".populateConfig()", diagnostic, errors, warnings));
        return (errors.size() == 0);
    }

    /**
     * <p>Insert an individual String/String pair into the config map.
     * </p>
     *
     * @param pair
     * @param count
     * @param errors
     * @param warnings
     */
    private void populateConfigEntry(String[] pair, int count, List<String> errors, List<String> warnings) {
        IMap<String, String> configMap = super.getHazelcastInstance().getMap(MyConstants.IMAP_NAME_SYS_CONFIG);
        if (pair == null) {
            errors.add("config pair " + count + " is null");
            return;
        }
        if (pair.length != 2
                || pair[0] == null || pair[0].length() == 0
                || pair[1] == null || pair[1].length() == 0) {
            errors.add("config pair " + count + " is invalid");
            for (int i = 0; i < pair.length; i++) {
                errors.add("config pair " + count + " item: " + i + " is: " + Objects.toString(pair[i]));
            }
            return;
        }
        // Overwrite is allowed, for idempotent testing
        Object previous = configMap.put(pair[0], pair[1]);
        if (previous != null) {
            warnings.add("Key '" + pair[0] + "' already present");
        }
    }

    /**
     *
     * @param result
     */
    private void addMapStores(List<Tuple4<String, String, List<String>, List<String>>> result) {
        IMap<String, String> configMap = super.getHazelcastInstance().getMap(MyConstants.IMAP_NAME_SYS_CONFIG);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        String diagnostic = "";

        Map<String, String> mongoProperties = configMap.entrySet()
                .stream()
                .filter(entry -> entry.getKey().startsWith(MyConstants.MONGO_PREFIX))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        try {
            ServiceHistoryMapLoader serviceHistoryMapLoader = new ServiceHistoryMapLoader(mongoProperties);

            MapStoreConfig serviceHistoryMapStoreConfig = new MapStoreConfig();
            serviceHistoryMapStoreConfig.setInitialLoadMode(MapStoreConfig.InitialLoadMode.EAGER);
            serviceHistoryMapStoreConfig.setImplementation(serviceHistoryMapLoader);

            MapConfig serviceHistoryMapConfig = new MapConfig(MyConstants.IMAP_NAME_SERVICE_HISTORY);
            serviceHistoryMapConfig.setMapStoreConfig(serviceHistoryMapStoreConfig);

            this.getHazelcastInstance().getConfig().addMapConfig(serviceHistoryMapConfig);

            diagnostic = serviceHistoryMapConfig.getName();
        } catch (Exception e) {
            Utils.addExceptionToError(errors, MyConstants.IMAP_NAME_SERVICE_HISTORY, e);
        }

        result.add(Tuple4.tuple4(MY_NAME + ".addMapStores()", diagnostic, errors, warnings));
    }

    /**
     * <p>Access maps to make them visible on Management Center.
     * </p>
     *
     * @param result
     */
    private void touchAllIMaps(List<Tuple4<String, String, List<String>, List<String>>> result) {
        for (String iMapName : MyConstants.IMAP_NAMES) {
            super.getHazelcastInstance().getMap(iMapName);
            result.add(Tuple4.tuple4(MY_NAME + ".touchAllIMaps()", iMapName, new ArrayList<>(), new ArrayList<>()));
        }
    }

}
