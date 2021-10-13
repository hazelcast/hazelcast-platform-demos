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

package com.hazelcast.platform.demos.retail.clickstream;

/**
 * <p>"Constants", need to match React.js pages but duplicated.
 * </p>
 */
public class MyLocalConstants {

    // Web Sockets
    public static final String WEBSOCKET_ENDPOINT = "hazelcast";
    public static final String WEBSOCKET_INFO_PREFIX = "info";

    // See App.js
    public static final String CLUSTER_DESTINATION =
            "/" + MyLocalConstants.WEBSOCKET_INFO_PREFIX
            + "/" + "cluster";
    // See components/members/Alerts.js
    public static final String ALERTS_DESTINATION =
            "/" + MyLocalConstants.WEBSOCKET_INFO_PREFIX
            + "/" + "alerts";
    // See components/members/Members.js
    public static final String MEMBERS_DESTINATION =
            "/" + MyLocalConstants.WEBSOCKET_INFO_PREFIX
            + "/" + "members";
    // See components/data/Data.js
    public static final String DATA_DESTINATION =
            "/" + MyLocalConstants.WEBSOCKET_INFO_PREFIX
            + "/" + "data";
}
