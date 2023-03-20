/*
 * Copyright (c) 2008-2023, Hazelcast, Inc. All Rights Reserved.
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

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>From <a href="http://www.nasdaqtrader.com/trader.aspx?id=symboldirdefs">here</a>.
 * </p>
 */
public enum NasdaqMarketCategory {

    GLOBAL_SELECT_MARKET('Q'),
    GLOBAL_MARKET('G'),
    CAPITAL_MARKET('S');

    private static Map<Character, NasdaqMarketCategory> lookup;
    static {
        lookup = Arrays.stream(NasdaqMarketCategory.values())
        .collect(Collectors.<NasdaqMarketCategory, Character, NasdaqMarketCategory>toUnmodifiableMap(
            nasdaqMarketCategory -> nasdaqMarketCategory.c,
            nasdaqMarketCategory -> nasdaqMarketCategory));
    }

    private char c;

    NasdaqMarketCategory(char arg0) {
        this.c = arg0;
    }

    public static NasdaqMarketCategory valueOfMarketCategory(String arg0) {
        return lookup.get(arg0.charAt(0));
    }

    String getCode() {
        return String.valueOf(this.c);
    }

}
