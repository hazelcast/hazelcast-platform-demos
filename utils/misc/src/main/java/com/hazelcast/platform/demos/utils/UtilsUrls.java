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

package com.hazelcast.platform.demos.utils;

/**
 * <p>Helpers for URL building.
 * </p>
 */
public class UtilsUrls {

    /**
     * <p>Build "{@code pulsar://127.0.0.1:6650}" etc.
     * </p>
     *
     * @param pulsarList
     * @return
     * @throws Exception
     */
    public static String getPulsarServiceUrl(String pulsarList) {
        StringBuilder stringBuilder = new StringBuilder("pulsar://");
        String[] connections = pulsarList.split(",");
        for (int i = 0 ; i < connections.length ; i++) {
            if (i > 0) {
                stringBuilder.append(",");
            }
            String[] hostPort = connections[i].split(":");
            stringBuilder.append(hostPort[0]);
            if (System.getProperty("my.kubernetes.enabled", "").equals("true")
                    && !hostPort[0].endsWith(".default.svc.cluster.local")) {
                stringBuilder.append(".default.svc.cluster.local");
            }
            // Default port if not provided
            if (hostPort.length == 1) {
                stringBuilder.append(":6650");
            } else {
                stringBuilder.append(":").append(hostPort[1]);
            }
        }

        return stringBuilder.toString();
    }

}
