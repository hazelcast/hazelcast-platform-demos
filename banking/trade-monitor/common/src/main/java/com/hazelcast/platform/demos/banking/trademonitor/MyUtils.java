/*
 * Copyright (c) 2008-2020, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.platform.demos.banking.trademonitor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * <p>Utility functions used in several places.
 * </p>
 */
public class MyUtils {

    /**
     * <p>Read NASDAQ stock symbols and security names from
     * a file on the classpath.
     * </p>
     *
     * @return A map with symbol key and security name as value
     * @throws Exception
     */
    public static Map<String, String> nasdaqListed() throws Exception {
        Map<String, String> result = null;

        try (
             BufferedReader bufferedReader =
                 new BufferedReader(
                     new InputStreamReader(
                             MyUtils.class.getResourceAsStream("/nasdaqlisted.txt"), StandardCharsets.UTF_8));
        ) {
            result =
                    bufferedReader.lines()
                    .filter(line -> !line.startsWith("#"))
                    .map(line -> {
                        String[] split = line.split("\\|");
                        return new SimpleImmutableEntry<String, String>(split[0], split[1]);
                    })
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        }

        return result;
    }

}
