/*
 * Copyright (c) 2008-2025, Hazelcast, Inc. All Rights Reserved.
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

/**
 * <p>"Constants", need to match React.js pages but duplicated.
 * </p>
 */
public class MyLocalConstants {

    // Web Sockets
    public static final String WEBSOCKET_ENDPOINT = "hazelcast";
    public static final String WEBSOCKET_DATA_PREFIX = "data";

    // See components/config/Config.js
    public static final String CONFIG_DESTINATION =
            "/" + MyLocalConstants.WEBSOCKET_DATA_PREFIX
            + "/" + "config";
    // See components/logging/Slf4j.js
    public static final String LOGGING_DESTINATION =
            "/" + MyLocalConstants.WEBSOCKET_DATA_PREFIX
            + "/" + "logging";
}
