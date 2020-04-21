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

    // Map names, for eager creation
    public static final String IMAP_NAME_CP_CDS  = "cva_cp_cds";
    public static final String IMAP_NAME_CVA_CSV  = "cva_csv";
    public static final String IMAP_NAME_CVA_XLSX  = "cva_xlsx";
    public static final String IMAP_NAME_IRCURVES  = "cva_ircurves";
    public static final String IMAP_NAME_FIXINGS = "cva_fixings";
    public static final String IMAP_NAME_TRADES  = "cva_trades";

    public static final List<String> IMAP_NAMES =
            List.of(IMAP_NAME_CP_CDS, IMAP_NAME_CVA_CSV, IMAP_NAME_CVA_XLSX,
                    IMAP_NAME_FIXINGS, IMAP_NAME_IRCURVES, IMAP_NAME_TRADES);

    public static final String ITOPIC_NAME_JOB_STATE  = "job_state";

    public static final List<String> ITOPIC_NAMES =
            List.of(ITOPIC_NAME_JOB_STATE);

    /**
     * <p>Live and DR sites, but which is which depends on you.</p>
     */
    public enum Site {
        CVA_SITE1,
        CVA_SITE2
    };

    // For sending data to Grafana/Graphite
    public static final int GRAPHITE_COLLECTION_INTERVAL_SECONDS = 5;
    public static final int GRAPHITE_PORT = 2004;
    public static final char GRAPHITE_SEPARATOR = '.';

    // For logging, prefix for important messages (see "log4j2.yml")
    public static final String BANNER = "*** *** *** ***";
    // For limiting the logging of long lines
    public static final int HALF_SCREEN_WIDTH = 66;

}
