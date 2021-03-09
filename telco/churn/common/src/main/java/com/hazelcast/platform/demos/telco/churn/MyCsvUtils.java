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

package com.hazelcast.platform.demos.telco.churn;

import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.platform.demos.telco.churn.domain.Sentiment;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Utility functions for convenient CSV conversion
 * </p>
 * <p><b>Alphabetical order</b></p>
 */
public class MyCsvUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyCsvUtils.class);

    /**
     * <p>Takes a JSON format CallDataRecord</p>
     */
    public static String toCSVCallDataRecord(HazelcastJsonValue hazelcastJsonValue) {
        String empty = ",,,,,,,,,,,";
        if (hazelcastJsonValue == null) {
            LOGGER.error("CallDataRecord is null");
            return empty;
        } else {
            try {
                JSONObject json = new JSONObject(hazelcastJsonValue.toString());
                String calleeMastId = json.getString("calleeMastId");
                String calleeTelno = json.getString("calleeTelno");
                String callerMastId = json.getString("callerMastId");
                String callerTelno = json.getString("callerTelno");
                String callSuccessful = String.valueOf(json.getBoolean("callSuccessful"));
                String createdBy = json.getString("createdBy");
                String createdDate = String.valueOf(json.getLong("createdDate"));
                String durationSeconds = String.valueOf(json.getInt("durationSeconds"));
                String id = json.getString("id");
                String lastModifiedBy = json.getString("lastModifiedBy");
                String lastModifiedDate = String.valueOf(json.getLong("lastModifiedDate"));
                String startTimestamp = String.valueOf(json.getLong("startTimestamp"));

                // Alphabetical order on field name
                return calleeMastId + ","
                        + calleeTelno + ","
                        + callerMastId + ","
                        + callerTelno + ","
                        + callSuccessful + ","
                        + createdBy + ","
                        + createdDate + ","
                        + durationSeconds + ","
                        + id + ","
                        + lastModifiedBy + ","
                        + lastModifiedDate + ","
                        + startTimestamp
                        ;
            } catch (Exception e) {
                LOGGER.error(hazelcastJsonValue.toString(), e);
                return empty;
            }
        }
    }

    /**
     * <p>Takes a JSON format Customer</p>
     * <p><b>Deliberately exclude the "{@code notes}" field.</p>
     * TODO: Should "{@code notes}" be included ?
     */
    public static String toCSVCustomer(HazelcastJsonValue hazelcastJsonValue) {
        String empty = ",,,,,,,";
        if (hazelcastJsonValue == null) {
            LOGGER.error("Customer is null");
            return empty;
        } else {
            try {
                JSONObject json = new JSONObject(hazelcastJsonValue.toString());
                String accountType = json.getString("accountType");
                String createdBy = json.getString("createdBy");
                String createdDate = String.valueOf(json.getLong("createdDate"));
                String firstName = json.getString("firstName");
                String id = json.getString("id");
                String lastModifiedBy = json.getString("lastModifiedBy");
                String lastModifiedDate = String.valueOf(json.getLong("lastModifiedDate"));
                String lastName = json.getString("lastName");

                // Alphabetical order on field name
                return accountType + ","
                        + createdBy + ","
                        + createdDate + ","
                        + firstName + ","
                        + id + ","
                        + lastModifiedBy + ","
                        + lastModifiedDate + ","
                        + lastName
                        ;
            } catch (Exception e) {
                LOGGER.error(hazelcastJsonValue.toString(), e);
                return empty;
            }
        }
    }

    /**
     * <p>For {@link Sentiment}</p>
     */
    public static String toCSVSentiment(Sentiment sentiment) {
        String empty = ",,";
        if (sentiment == null || sentiment.getUpdated() == null) {
            return empty;
        } else {
            // Alphabetical order on field name
            return sentiment.getCurrent() + ","
                    + sentiment.getPrevious() + ","
                    + sentiment.getUpdated();
        }
    }

}
