/*
 * Copyright (c) 2008-2022, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.platform.demos.retail.clickstream;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.jet.datamodel.Tuple4;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Add mapping definitions so Management Center knows how to query
 * even if there is no data in the map.
 * </p>
 */
@Slf4j
public class MapDefinitions {

    public static void addMappings(HazelcastInstance hazelcastInstance) {
        addMappingAlert(hazelcastInstance);
        addMappingCheckout(hazelcastInstance);
        addMappingClickstream(hazelcastInstance);
        addMappingConfig(hazelcastInstance);
        addMappingDigitalTwin(hazelcastInstance);
        addMappingHeartbeat(hazelcastInstance);
        addMappingModelSelection(hazelcastInstance);
        addMappingModelVault(hazelcastInstance);
        addMappingOrdered(hazelcastInstance);
        addMappingRetrainingAssessment(hazelcastInstance);
        addMappingRetrainingControl(hazelcastInstance);
        addMappingPrediction(hazelcastInstance);
    }

    private static void addMappingAlert(HazelcastInstance hazelcastInstance) {
        // IMap is <Long, String>
        addMappingLongString(hazelcastInstance, MyConstants.IMAP_NAME_ALERT);
    }

    private static void addMappingCheckout(HazelcastInstance hazelcastInstance) {
        String mapping = "CREATE OR REPLACE MAPPING \""
                + MyConstants.IMAP_NAME_CHECKOUT
                + "\" ("
                + "  __key VARCHAR,"
                + "    \"publishTimestamp\" OBJECT EXTERNAL NAME \"this.f0\","
                + "    \"ingestTimestamp\" OBJECT EXTERNAL NAME \"this.f1\","
                + "    action OBJECT EXTERNAL NAME \"this.f2\""
                + ") "
                + "TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = '" + String.class.getCanonicalName() + "',"
                + " 'valueFormat' = 'java',"
                + " 'valueJavaClass' = '" + Tuple3.class.getCanonicalName() + "'"
                + " )";
        addMapping(hazelcastInstance, mapping);
    }

    private static void addMappingClickstream(HazelcastInstance hazelcastInstance) {
        String mapping = "CREATE OR REPLACE MAPPING \""
                + MyConstants.IMAP_NAME_CLICKSTREAM
                + "\" ("
                + "    \"key\" VARCHAR EXTERNAL NAME \"__key.key\","
                + "    \"publishTimestamp\" BIGINT EXTERNAL NAME \"__key.publishTimestamp\","
                + "    \"ingestTimestamp\" BIGINT EXTERNAL NAME \"__key.ingestTimestamp\","
                + "    this VARCHAR"
                + ") "
                + "TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = '" + ClickstreamKey.class.getCanonicalName() + "',"
                + " 'valueFormat' = 'java',"
                + " 'valueJavaClass' = '" + String.class.getCanonicalName() + "'"
                + " )";
        addMapping(hazelcastInstance, mapping);
    }

    private static void addMappingDigitalTwin(HazelcastInstance hazelcastInstance) {
        String mapping = "CREATE OR REPLACE MAPPING \""
                + MyConstants.IMAP_NAME_DIGITAL_TWIN
                + "\" ("
                + "  __key VARCHAR,"
                + "    \"publishTimestamp\" OBJECT EXTERNAL NAME \"this.f0\","
                + "    \"ingestTimestamp\" OBJECT EXTERNAL NAME \"this.f1\","
                + "    actions OBJECT EXTERNAL NAME \"this.f2\""
                + ") "
                + "TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = '" + String.class.getCanonicalName() + "',"
                + " 'valueFormat' = 'java',"
                + " 'valueJavaClass' = '" + Tuple3.class.getCanonicalName() + "'"
                + " )";
        addMapping(hazelcastInstance, mapping);
    }

    private static void addMappingConfig(HazelcastInstance hazelcastInstance) {
        // IMap is <String, String>
        addMappingStringString(hazelcastInstance, MyConstants.IMAP_NAME_CONFIG);
    }

    private static void addMappingHeartbeat(HazelcastInstance hazelcastInstance) {
        String mapping = "CREATE OR REPLACE MAPPING \""
                + MyConstants.IMAP_NAME_HEARTBEAT
                + "\" ("
                + "  __key VARCHAR,"
                + "    site VARCHAR,"
                + "    heartbeat_timestamp VARCHAR,"
                // "member" is reserved word
                + "    \"member\" VARCHAR,"
                + "    \"address\" VARCHAR,"
                + "    \"id\" VARCHAR,"
                + "    epoch_second BIGINT,"
                + "    build_timestamp VARCHAR"
                + ") "
                + "TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = '" + String.class.getCanonicalName() + "',"
                + " 'valueFormat' = 'json-flat',"
                + " 'valueJavaClass' = '" + HazelcastJsonValue.class.getCanonicalName() + "'"
                + " )";
        addMapping(hazelcastInstance, mapping);
    }

    private static void addMappingModelSelection(HazelcastInstance hazelcastInstance) {
        // IMap is <String, String>
        addMappingStringString(hazelcastInstance, MyConstants.IMAP_NAME_MODEL_SELECTION);
    }

    private static void addMappingModelVault(HazelcastInstance hazelcastInstance) {
        // IMap is <String, String>
        addMappingStringString(hazelcastInstance, MyConstants.IMAP_NAME_MODEL_VAULT);
    }

    private static void addMappingOrdered(HazelcastInstance hazelcastInstance) {
        String mapping = "CREATE OR REPLACE MAPPING \""
                + MyConstants.IMAP_NAME_ORDERED
                + "\" ("
                + "  __key VARCHAR,"
                + "    \"publishTimestamp\" OBJECT EXTERNAL NAME \"this.f0\","
                + "    \"ingestTimestamp\" OBJECT EXTERNAL NAME \"this.f1\","
                + "    action OBJECT EXTERNAL NAME \"this.f2\""
                + ") "
                + "TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = '" + String.class.getCanonicalName() + "',"
                + " 'valueFormat' = 'java',"
                + " 'valueJavaClass' = '" + Tuple3.class.getCanonicalName() + "'"
                + " )";
        addMapping(hazelcastInstance, mapping);
    }

    private static void addMappingPrediction(HazelcastInstance hazelcastInstance) {
        String mapping = "CREATE OR REPLACE MAPPING \""
                + MyConstants.IMAP_NAME_PREDICTION
                + "\" ("
                + "    \"key\" VARCHAR EXTERNAL NAME \"__key.key\","
                + "    \"algorithm\" VARCHAR EXTERNAL NAME \"__key.algorithm\","
                + "    \"version\" OBJECT EXTERNAL NAME \"this.f0\","
                + "    \"publishTimestamp\" OBJECT EXTERNAL NAME \"this.f1\","
                + "    \"ingestTimestamp\" OBJECT EXTERNAL NAME \"this.f2\","
                + "    \"egestTimestamp\" OBJECT EXTERNAL NAME \"this.f3\","
                + "    prediction OBJECT EXTERNAL NAME \"this.f4\""
                + ") "
                + "TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = '" + PredictionKey.class.getCanonicalName() + "',"
                + " 'valueFormat' = 'java',"
                + " 'valueJavaClass' = '" + Tuple4.class.getCanonicalName() + "'"
                + " )";
        addMapping(hazelcastInstance, mapping);
    }

    private static void addMappingRetrainingAssessment(HazelcastInstance hazelcastInstance) {
        String mapping = "CREATE OR REPLACE MAPPING \""
                + MyConstants.IMAP_NAME_RETRAINING_ASSESSMENT
                + "\" ("
                + "  __key VARCHAR,"
                + "    this DOUBLE"
                + ") "
                + "TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = '" + String.class.getCanonicalName() + "',"
                + " 'valueFormat' = 'java',"
                + " 'valueJavaClass' = '" + Double.class.getCanonicalName() + "'"
                + " )";
        addMapping(hazelcastInstance, mapping);
    }

    private static void addMappingRetrainingControl(HazelcastInstance hazelcastInstance) {
        String mapping = "CREATE OR REPLACE MAPPING \""
                + MyConstants.IMAP_NAME_RETRAINING_CONTROL
                + "\" ("
                + "  __key BIGINT,"
                + "    action VARCHAR,"
                + "    \"timestamp\" BIGINT,"
                + "    \"count\" BIGINT,"
                + "    \"start\" BIGINT,"
                + "    \"end\" BIGINT,"
                + "    \"previous\" BIGINT"
                + ") "
                + "TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = '" + Long.class.getCanonicalName() + "',"
                + " 'valueFormat' = 'json-flat',"
                + " 'valueJavaClass' = '" + HazelcastJsonValue.class.getCanonicalName() + "'"
                + " )";
        addMapping(hazelcastInstance, mapping);
    }

    /**
     * <p>A map holding "{@code Entry<Long, String>}".
     * </p>
     *
     * @param hazelcastInstance
     * @param name
     */
    private static void addMappingLongString(HazelcastInstance hazelcastInstance, String name) {
        String mapping = "CREATE OR REPLACE MAPPING \""
                + name
                + "\" ("
                + "    __key BIGINT,"
                + "    this VARCHAR"
                + ") "
                + "TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = '" + Long.class.getCanonicalName() + "',"
                + " 'valueFormat' = 'java',"
                + " 'valueJavaClass' = '" + String.class.getCanonicalName() + "'"
                + " )";
        addMapping(hazelcastInstance, mapping);
    }

    /**
     * <p>A map holding "{@code Entry<String, String>}".
     * </p>
     *
     * @param hazelcastInstance
     * @param name
     */
    private static void addMappingStringString(HazelcastInstance hazelcastInstance, String name) {
        String mapping = "CREATE OR REPLACE MAPPING \""
                + name
                + "\" ("
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
        addMapping(hazelcastInstance, mapping);
    }

    /**
     * <p>Define a mapping
     * </p>
     *
     * @param mapping A string which should be a mapping.
     */
    private static void addMapping(HazelcastInstance hazelcastInstance, String mapping) {
        log.trace("addMapping( '{}' )", mapping);
        try {
            hazelcastInstance.getSql().execute(mapping);
        } catch (Exception e) {
            log.error(mapping, e);
        }
    }

}
