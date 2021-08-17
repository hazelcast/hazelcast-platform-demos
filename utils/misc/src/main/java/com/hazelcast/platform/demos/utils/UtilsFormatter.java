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

package com.hazelcast.platform.demos.utils;

/**
 * <p>Helpful utilities for formatting.
 * </p>
 */
public class UtilsFormatter {

    /**
     * <p>Make a String safe to include in JSON.
     * </p>
     *
     * @param input
     * @return
     */
    public static String safeForJsonStr(String input) {
        return input.replaceAll("\"", "'");
    }

    /**
     * <p>Make a String safe for HTML characters.
     * </p>
     *
     * @param input
     * @return
     */
    public static String htmlUnescape(String input) {
        return input.replaceAll("&lt;", "<").replaceAll("&gt;", ">");
    }

    /**
     * XXX
     *
     * @param input
     * @return
     */
    public static String makeUTF8(String input) {
        if (input == null) {
            return null;
        }

        // First pass - charset replacements
        char[] firstPass = new char[input.length()];

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            switch (c) {
            case '‘':
            case '’':
                c = '\'';
                break;
            case '“':
            case '”':
                c = '"';
                break;
            default:
                break;
            }

            firstPass[i] = c;
        }

        // Placeholder for any replacements
        String secondPass = new String(firstPass);
        //.replaceAll("%", "%%");

        String thirdPass = htmlUnescape(secondPass);

        return thirdPass;
    }

}
