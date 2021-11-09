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
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.jet.datamodel.Tuple4;
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
    public static List<Tuple4<String, String, List<String>, List<String>>> addMappings(HazelcastInstance hazelcastInstance) {
        List<Tuple4<String, String, List<String>, List<String>>> result = new ArrayList<>();

        result.add(MappingDefinitions.addMappingStringString(hazelcastInstance, MyConstants.IMAP_NAME_CONFIG));
        result.add(MappingDefinitions.addMappingIMapLogging(hazelcastInstance));

        return result;
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
    private static Tuple4<String, String, List<String>, List<String>> addMappingStringString(HazelcastInstance hazelcastInstance,
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
        List<String> warnings = new ArrayList<>();
        return Tuple4.<String, String, List<String>, List<String>>
            tuple4(MY_NAME + ".addMappingStringString()", mapName, errors, warnings);
    }

    /**
     * <p>Logging, based on <a href="http://logback.qos.ch/">Logback</a>.</p>
     *
     * @param hazelcastInstance
     * @return
     */
    private static Tuple4<String, String, List<String>, List<String>> addMappingIMapLogging(HazelcastInstance hazelcastInstance) {
        String mapName = MyConstants.IMAP_NAME_LOGGING;
        String sql = "CREATE OR REPLACE MAPPING \"" + mapName + "\""
                + " EXTERNAL NAME \"" + mapName + "\""
                + " ("
                + "    \"memberAddress\" VARCHAR EXTERNAL NAME \"__key.memberAddress\","
                + "    \"timestamp\" BIGINT EXTERNAL NAME \"__key.timestamp\","
                + "    \"level\" VARCHAR EXTERNAL NAME \"this.level\","
                + "    \"message\" VARCHAR EXTERNAL NAME \"this.message\","
                + "    \"threadName\" VARCHAR EXTERNAL NAME \"this.threadName\","
                + "    \"loggerName\" VARCHAR EXTERNAL NAME \"this.loggerName\""
                + ") "
                + "TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = '" + HazelcastJsonValue.class.getCanonicalName() + "',"
                + " 'valueFormat' = 'java',"
                + " 'valueJavaClass' = '" + HazelcastJsonValue.class.getCanonicalName() + "'"
                + " )";
        List<String> errors = MappingDefinitions.executeSqlMapping(hazelcastInstance, sql);
        List<String> warnings = new ArrayList<>();
        return Tuple4.<String, String, List<String>, List<String>>
            tuple4(MY_NAME + ".addMappingIMapLogging()", mapName, errors, warnings);
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
            Utils.addExceptionToError(errors, sql, e);
        }
        return errors;
    }

}
