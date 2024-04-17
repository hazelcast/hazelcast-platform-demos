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

package hazelcast.platform.demos.banking.transactionmonitor;

import java.util.Properties;

import com.hazelcast.platform.demos.utils.UtilsProperties;

/**
 * <p>Constants for this jar.
 * </p>
 */
public class LocalConstants {

    static final String MY_JAR_NAME = extractJarNameFromProperties();

    private static final String PROPERTIES_FILE_NAME = "jar.properties";
    private static final String PROPERTIES_KEY = "my.properties.key";

    /**
     * <p>Extract at run time (server-side) from properties file.
     * </p>
     *
     * @return
     */
    private static String extractJarNameFromProperties() {
        Properties properties = UtilsProperties.loadClasspathProperties(PROPERTIES_FILE_NAME,
                LocalConstants.class.getClassLoader());
        if (properties.size() == 0) {
            return "ERROR_NO_PROPERTIES_LOADED";
        }
        return properties.getProperty(PROPERTIES_KEY, "ERROR_NO_VALUE_FOUND");
    }

}
