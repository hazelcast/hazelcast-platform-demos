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

package com.hazelcast.platform.demos.banking.trademonitor;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>A DTO for returning the results of the Sql call via Javalin
 * to REST and ultimately Node.js
 * </p>
 */
public class SqlDto {

    private final String error;
    private final String warning;
    private final List<String> rows = new ArrayList<>();

    public SqlDto(String arg0, String arg1, List<String> arg2) {
        this.error = arg0;
        this.warning = arg1;
        this.rows.addAll(arg2);
    }

    public String getError() {
        return error;
    }

    public String getWarning() {
        return warning;
    }

    public List<String> getRows() {
        return new ArrayList<>(this.rows);
    }

    @Override
    public String toString() {
        return "SqlDto [error=" + error + ", warning=" + warning + ", rows=" + rows + "]";
    }

}
