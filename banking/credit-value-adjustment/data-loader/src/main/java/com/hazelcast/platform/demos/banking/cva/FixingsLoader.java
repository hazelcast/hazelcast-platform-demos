/*
 * Copyright (c) 2008-2022, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.platform.demos.banking.cva;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * <p>Load fixings data.</p>
 * <p>We don't need "{@code @Order}" annotation, won't matter
 * if this is before or after other maps.
 * </p>
 * <p>Do fixings last, so cannot kick off a run while data loader
 * is running. Only one fixing, but without it there's no run.
 * </p>
 */
@Component
@Order(value = 3)
public class FixingsLoader implements CommandLineRunner {

    private static final boolean PARTIAL_OK = true;

    @Autowired
    private JsonLoaderService jsonLoaderService;

    /**
     * <p>Use the generic JSON loader to upload the file into a map.</p>
     */
    @Override
    public void run(String... args) throws Exception {
        String inputFileName = "fixings.json";
        String keyFieldName = "curvename";
        this.jsonLoaderService.load(MyConstants.IMAP_NAME_FIXINGS, inputFileName, keyFieldName, PARTIAL_OK);
    }

}
