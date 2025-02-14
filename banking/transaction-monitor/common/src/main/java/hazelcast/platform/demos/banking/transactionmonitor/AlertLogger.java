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

package hazelcast.platform.demos.banking.transactionmonitor;

import com.hazelcast.jet.pipeline.JournalInitialPosition;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.Sources;

/**
 * <p>
 * A simple job to read streaming changes and log them.
 * </p>
 *
 * <pre>
 *                +------( 1 )------+
 *                | Journal Source  |
 *                +-----------------+
 *                         |
 *                         |
 *                         |
 *                +------( 2 )------+
 *                |      Format     |
 *                +-----------------+
 *                         |
 *                         |
 *                         |
 *                +------( 3 )------+
 *                |    Log Sink     |
 *                +-----------------+
 * </pre>
 * <p>
 * The steps:
 * </p>
 * <ol>
 * <li>
 * <p>
 * Map journal source
 * </p>
 * <p>
 * Stream changes from the "{@code alertsLog}" map, only the new value from
 * writes.
 * </p>
 * </li>
 * <li>
 * <p>
 * Reformat
 * </p>
 * <p>
 * Turn an {@link java.util.Map.Entry} into a {@link String}
 * </p>
 * </li>
 * <li>
 * <p>
 * Sink
 * </p>
 * <p>
 * Write data to standard output.
 * </p>
 * </li>
 * </ol>
 */
public class AlertLogger {

    public static Pipeline buildPipeline() {

        return Pipeline.create()
                .readFrom(
                        Sources.mapJournal(MyConstants.IMAP_NAME_ALERTS_LOG, JournalInitialPosition.START_FROM_OLDEST))
                .withoutTimestamps().map(entry -> entry.getKey() + "==" + entry.getValue()).writeTo(Sinks.logger())
                .getPipeline();
    }
}
