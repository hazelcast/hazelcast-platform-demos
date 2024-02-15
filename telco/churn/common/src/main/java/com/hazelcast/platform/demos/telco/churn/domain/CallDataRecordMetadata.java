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

package com.hazelcast.platform.demos.telco.churn.domain;

import java.util.List;

/**
 * <p>Fields expected in a call data record in JSON. A successful call
 * is one which is connected and not then dropped.
 * </p>
 */
public class CallDataRecordMetadata {

    public static final String ID = "id";
    public static final String CALLER_TELNO = "callerTelno";
    public static final String CALLER_MAST_ID = "callerMastId";
    public static final String CALLEE_TELNO = "calleeTelno";
    public static final String CALLEE_MAST_ID = "calleeMastId";
    public static final String START_TIMESTAMP = "startTimestamp";
    public static final String DURATION_SECONDS = "durationSeconds";
    public static final String CALL_SUCCESSFUL = "callSuccessful";
    // Mirror @CreatedBy, @CreatedDate, @LastModifiedBy, @LastModifiedDate
    public static final String CREATED_BY = "createdBy";
    public static final String CREATED_DATE = "createdDate";
    public static final String LAST_MODIFIED_BY = "lastModifiedBy";
    public static final String LAST_MODIFIED_DATE = "lastModifiedDate";

    public static final List<String> FIELD_NAMES =
            List.of(ID,
                    CALLER_TELNO, CALLER_MAST_ID,
                    CALLEE_TELNO, CALLEE_MAST_ID,
                    START_TIMESTAMP, DURATION_SECONDS,
                    CALL_SUCCESSFUL,
                    CREATED_BY, CREATED_DATE,
                    LAST_MODIFIED_BY, LAST_MODIFIED_DATE
                    );

}
