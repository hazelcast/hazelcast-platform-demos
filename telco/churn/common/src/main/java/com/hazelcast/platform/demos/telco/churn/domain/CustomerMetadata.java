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

package com.hazelcast.platform.demos.telco.churn.domain;

import java.util.List;

/**
 * <p>Fields expected in a customer JSON record.
 * </p>
 */
public class CustomerMetadata {

    public static final String ID = "id";
    public static final String FIRSTNAME = "firstName";
    public static final String LASTNAME = "lastName";
    public static final String ACCOUNT_TYPE = "accountType";
    // Mirror @CreatedBy, @CreatedDate, @LastModifiedBy, @LastModifiedDate
    public static final String CREATED_BY = "createdBy";
    public static final String CREATED_DATE = "createdDate";
    public static final String LAST_MODIFIED_BY = "lastModifiedBy";
    public static final String LAST_MODIFIED_DATE = "lastModifiedDate";

    public static final List<String> FIELD_NAMES =
            List.of(ID, FIRSTNAME, LASTNAME, ACCOUNT_TYPE,
                    CREATED_BY, CREATED_DATE,
                    LAST_MODIFIED_BY, LAST_MODIFIED_DATE
                    );

}
