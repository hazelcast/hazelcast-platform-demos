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

package com.hazelcast.platform.demos.telco.churn.testdata;

/**
 * <p>Test data for call tariff bands.
 * </p>
 */
public class TariffTestdata {

    private static final Object[][] TARIFFS = {
            // Year offset, Id, Name, International, Rate
            { 0, "stddom", "Standard Domestic Rate", false, 1.1 },
            { 0, "stdint", "Standard International Rate", true, 5.5 },
            { 1, "stddom", "Standard Domestic Rate", false, 1.3 },
            { 1, "stdint", "Standard International Rate", true, 5.9 },
    };

    public static Object[][] getTariffs() {
        return TARIFFS.clone();
    }

}
