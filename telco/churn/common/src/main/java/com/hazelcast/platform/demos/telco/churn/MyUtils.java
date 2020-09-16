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

package com.hazelcast.platform.demos.telco.churn;

import java.util.Locale;

/**
 * <p>Utility functions that may be useful to more than one module.
 * </p>
 */
public class MyUtils {
    private static final String ALPHABET_UC = "abcdefghijklmnopqrstuvwxyz".toUpperCase(Locale.ROOT);
    private static final String ALPHABET_LC = ALPHABET_UC.toLowerCase(Locale.ROOT);
    private static final String[] ALPHABETS = { ALPHABET_UC, ALPHABET_LC };
    private static final int ALPHABET_LENGTH = ALPHABET_UC.length();
    private static final int HALF_ALPHABET_LENGTH = ALPHABET_LENGTH / 2;

    /**
     * <p>The classic 13 character rotation encryption.
     * "{@code a}" maps to "{@code n}", and "{@code n}" maps
     * back to "{@code a}".
     * </p>
     */
    public static String rot13(String input) {
        if (input == null) {
            return null;
        }

        char[] output = new char[input.length()];

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            for (String alphabet : ALPHABETS) {
                int pos = alphabet.indexOf(c);
                if (pos != -1) {
                    pos = (pos + HALF_ALPHABET_LENGTH) % ALPHABET_LENGTH;
                    c = alphabet.charAt(pos);
                }
            }
            output[i] = c;
        }

        return new String(output);
    }
}
