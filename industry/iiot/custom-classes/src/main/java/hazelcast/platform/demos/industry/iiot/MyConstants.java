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

package hazelcast.platform.demos.industry.iiot;

import java.util.List;

/**
 * <p>Constants for the app.
 * </p>
 */
public class MyConstants {

    // Map names, for eager creation
    //FIXME Add MapStore
    public static final String IMAP_NAME_SERVICE_HISTORY = "service.history";
    public static final String IMAP_NAME_SYS_CONFIG = "sys.config";
    public static final String IMAP_NAME_SYS_LOGGING = "sys.logging";
    public static final List<String> IMAP_NAMES =
            List.of(IMAP_NAME_SERVICE_HISTORY,
                    IMAP_NAME_SYS_CONFIG, IMAP_NAME_SYS_LOGGING);

    public static final String SLF4J_APPENDER_NAME = "IMAP-" + IMAP_NAME_SYS_LOGGING;
    public static final String SLF4J_LOGGER_NAME = "hazelcast.platform.demos";
}
