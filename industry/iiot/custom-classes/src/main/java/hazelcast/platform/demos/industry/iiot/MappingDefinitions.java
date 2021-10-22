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

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.map.IMap;

/**
 * <p>Define mappings for SQL using "{@code CREATE OR REPLACE MAPPING ...}"
 * so idempotent.
 * </p>
 */
public class MappingDefinitions {
    private static final String MY_NAME = MappingDefinitions.class.getSimpleName();

    /**
     * <p>For each mapping, return class name, the SQL and a possibly empty list of
     * error messages. An empty list means the individual SQL worked.
     * </p>
     *
     * @param hazelcastInstance
     * @return
     */
    public static List<Tuple3<String, String, List<String>>> addMappings(HazelcastInstance hazelcastInstance) {
        List<Tuple3<String, String, List<String>>> result = new ArrayList<>();

        result.add(MappingDefinitions.addMappingConfig(hazelcastInstance));

        return result;
    }

    /**
     * <p>The application config {@link IMap}.
     */
    private static Tuple3<String, String, List<String>> addMappingConfig(HazelcastInstance hazelcastInstance) {
        return MappingDefinitions.addMappingStringString(hazelcastInstance, MyConstants.IMAP_NAME_CONFIG);
    }

    /**
     * <p>Communal definition for "{@code IMap<String, String>}".
     * External name and internal name the same, as an {@link IMap}, so inside Hazelcast.
     * </p>
     *
     * @param hazelcastInstance
     * @param mapName
     * @return
     */
    private static Tuple3<String, String, List<String>> addMappingStringString(HazelcastInstance hazelcastInstance,
            String mapName) {
        String sql = "CREATE OR REPLACE MAPPING \"" + mapName + "\""
                + " EXTERNAL NAME \"" + mapName + "\""
                + " ("
                + "    __key VARCHAR,"
                + "    this VARCHAR"
                + ") "
                + "TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = '" + String.class.getCanonicalName() + "',"
                + " 'valueFormat' = 'java',"
                + " 'valueJavaClass' = '" + String.class.getCanonicalName() + "'"
                + " )";

        List<String> errors = MappingDefinitions.executeSqlMapping(hazelcastInstance, sql);
        return Tuple3.<String, String, List<String>>tuple3(MY_NAME + ".addMappingStringString()", mapName, errors);
    }

    /**
     * <p>Attempt to run SQL mapping. No data is returned on success.
     * </p>
     *
     * @param hazelcastInstance The node to run against
     * @param sql The string to try, no parameters allowed.
     * @return A list of errors, empty list indicates successful execution.
     */
    static List<String> executeSqlMapping(HazelcastInstance hazelcastInstance, String sql) {
        List<String> errors = new ArrayList<>();
        try {
            hazelcastInstance.getSql().execute(sql);
        } catch (Exception e) {
            errors.add(sql);
            errors.add(e.getMessage());
            for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                errors.add(stackTraceElement.toString());
            }
        }
        return errors;
    }

}
