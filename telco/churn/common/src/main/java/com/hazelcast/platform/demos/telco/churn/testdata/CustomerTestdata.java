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

package com.hazelcast.platform.demos.telco.churn.testdata;

/**
 * <p>Test data for customer creation.
 * </p>
 */
public class CustomerTestdata {

    // https://en.wikipedia.org/wiki/List_of_most_popular_given_names#Americas
    private static final String[] FIRST_NAMES = {
            "James", "John", "Robert", "Michael", "William",
            "Mary", "Patricia", "Linda", "Barbara", "Elizabeth"
    };
    // https://en.wikipedia.org/wiki/List_of_most_common_surnames_in_North_America#United_States_(American)
    private static final String[] LAST_NAMES = {
            "Smith", "Johnson", "Williams", "Brown", "Jones",
            "Miller", "Davis", "Garcia", "Rodriguez", "Wilson"
    };

    public static String[] getFirstNames() {
        return FIRST_NAMES.clone();
    }
    public static String[] getLastNames() {
        return LAST_NAMES.clone();
    }

}
