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

package com.hazelcast.platform.demos.banking.cva;

import java.util.List;

/**
 * <p>Utility constants shared across the modules.
 * </p>
 */
public class MyConstants {

    public static final String IMAP_NAME_CP_CDS  = "cp_cds";
    public static final String IMAP_NAME_IRCURVES  = "ircurves";
    public static final String IMAP_NAME_FIXINGS = "fixings";
    public static final String IMAP_NAME_TRADES  = "trades";

    public static final List<String> IMAP_NAMES =
            List.of(IMAP_NAME_CP_CDS, IMAP_NAME_FIXINGS,
                    IMAP_NAME_IRCURVES, IMAP_NAME_TRADES);

    /**
     * <p>Live and DR sites, but which is which depends on you.</p>
     */
    public enum Site {
        CVA_SITE1,
        CVA_SITE2
    };

    public static final int GRAPHITE_COLLECTION_INTERVAL_SECONDS = 5;
    public static final int GRAPHITE_PORT = 2004;
    public static final char GRAPHITE_SEPARATOR = '.';
}
