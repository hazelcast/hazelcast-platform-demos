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

package com.hazelcast.platform.demos.telco.churn.domain;

import java.util.Collections;
import java.util.List;

/**
 * <p>Fields expected in a tariff JSON record.
 * </p>
 */
public class TariffMetadata {

    public static final String ID = "id";
    public static final String YEAR = "year";
    public static final String NAME = "name";
    public static final String INTERNATIONAL = "international";
    public static final String RATE_PER_MINUTE = "ratePerMinute";

    protected static final List<String> FIELD_NAMES =
            List.of(ID, YEAR, NAME,
                    INTERNATIONAL, RATE_PER_MINUTE
                    );

    public static List<String> getFieldNames() {
        return Collections.unmodifiableList(FIELD_NAMES);
    }
}
