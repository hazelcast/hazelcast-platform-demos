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

package com.hazelcast.platform.demos.utils;

import java.util.Properties;

/**
 * <p>Slack helpers.
 * </p>
 */
public class UtilsSlack {
    // For validation of properties.
    public static final int REASONABLE_MINIMAL_LENGTH_FOR_SLACK_PROPERTY = 8;

    // Local constant, never needed outside this class
    private static final String SLACK_PROPERTIES = "platform-utils-slack.properties";

    /**
     * <p>Loads the needed properties from a classpath resource.
     * These may not have values if Slack integration is not
     * enabled.
     * </p>
     *
     * @return
     */
    public static Properties loadSlackAccessProperties() {
        return UtilsProperties.loadClasspathProperties(SLACK_PROPERTIES);
    }

}
