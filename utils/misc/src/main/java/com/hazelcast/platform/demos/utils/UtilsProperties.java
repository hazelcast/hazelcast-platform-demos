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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Helpful utilities for property files, since not insisting
 * helpful Spring Framework must be used.
 * </p>
 */
public class UtilsProperties {
    private static final Logger LOGGER = LoggerFactory.getLogger(UtilsProperties.class);

    /**
     * <p>Load from a file, should be in "{@code src/main/resources}".
     * </p>
     * <p>"{@code spotbugs}" doesn't like
     * <pre>
     * try (InputStream inputStream = classLoader.getResourceAsStream(fileName)) {
     * </pre></p>
     *
     * @param fileName
     * @param classLoader
     * @return Properties, not null but may be empty.
     */
    public static Properties loadClasspathProperties(String fileName, ClassLoader classLoader) {
        LOGGER.trace("loadClasspathProperties('{}', classLoader='{}')", fileName, classLoader.getName());
        Properties properties = new Properties();
        InputStream inputStream = classLoader.getResourceAsStream(fileName);
        try {
            properties.load(inputStream);
            if (properties.isEmpty()) {
                String message = String.format("%s.loadClasspathProperties('%s') is empty",
                        UtilsProperties.class.getSimpleName(), fileName);
                LOGGER.warn(message);
            }
        } catch (Exception e) {
            String message = String.format("%s.loadClasspathProperties('%s')",
                    UtilsProperties.class.getSimpleName(), fileName);
            LOGGER.error(message, e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    String message = String.format("%s.loadClasspathProperties('%s')",
                            UtilsProperties.class.getSimpleName(), fileName);
                    LOGGER.error(message, e);
                }
            }
        }

        return properties;
    }

    /**
     * <p>Convenience method to use current class
     * </p>
     *
     * @param fileName
     * @return
     */
    public static Properties loadClasspathProperties(String fileName) {
        return loadClasspathProperties(fileName, UtilsProperties.class.getClassLoader());
    }
}
