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

import org.json.JSONObject;

import com.hazelcast.function.Functions;
import com.hazelcast.jet.aggregate.AggregateOperations;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.datamodel.KeyedWindowResult;
import com.hazelcast.jet.pipeline.JournalInitialPosition;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sources;
import com.hazelcast.jet.pipeline.StreamStage;
import com.hazelcast.jet.pipeline.WindowDefinition;

/**
 * <p>A pipeline that reads transactions, groups by key,
 * and counts the occurrences in fifteen sections of the hour,
 * updating a Vector Collection for subsequent searching.
 * </p>
 */
public class VectorCollectionMoments {

    private static final long ONE_HOUR_IN_MS = 60 * 60 * 1_000L;
    private static final long ONE_MOMENT_IN_MS = ONE_HOUR_IN_MS / 40;

    /**
     * <p>Job configuration, naming and required classes.
     * </p>
     */
    public static JobConfig getJobConfig() {
        JobConfig jobConfig = new JobConfig();
        jobConfig.setName(VectorCollectionMoments.class.getSimpleName());
        jobConfig.addClass(VectorCollectionMoments.class);
        return jobConfig;
    }

    /**
     * <pre>
     *                +------( 1 )------+
     *                | Journal Source  |
     *                +-----------------+
     *                         |
     *                         |
     *                         |
     *                +------( 2 )------+
     *                |     Collate     |
     *                +-----------------+
     *                         |
     *                         |
     *                         |
     *                +------( 3 )------+
     *                |   Vector Sink   |
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
     * <p>Stream changes from the "{@code transactions}" map, extract one field to count.
     * </p>
     * </li>
     * <li>
     * <p>
     * Collate
     * </p>
     * <p>Use built-in functions to count per key per time period.
     * </p>
     * </li>
     * <li>
     * <p>
     * Vector sink
     * </p>
     * <p>Custom sink to insert/update.
     * </p>
     * </li>
     * </ol>
     */
    public static Pipeline getPipeline(TransactionMonitorFlavor transactionMonitorFlavor) {
        Pipeline pipeline = Pipeline.create();

        // Find foreign key in different types of JSON
        StreamStage<String> inputForeignKey;
        switch (transactionMonitorFlavor) {
        case ECOMMERCE:
            inputForeignKey = extractEcommerceForeignKey(pipeline);
            break;
        case PAYMENTS:
            inputForeignKey = extractPaymentsForeignKey(pipeline);
            break;
        case TRADE:
        default:
            inputForeignKey = extractTradeForeignKey(pipeline);
            break;
        }

        // Count per key
        StreamStage<KeyedWindowResult<String, Long>> aggregated = inputForeignKey
            .window(WindowDefinition.tumbling(ONE_MOMENT_IN_MS))
            .groupingKey(Functions.wholeItem())
            .aggregate(AggregateOperations.counting());

        aggregated
        .writeTo(VectorCollectionMomentsSink.vectorCollectionMomentsSink());

        return pipeline;
    }

    /**
     * <p>Item code is the foreign key for an ecommerce order
     * </p>
     * <p>This is the "{@code indexColumn1}"</p>
     *
     * @param pipeline
     * @return
     */
    private static StreamStage<String> extractEcommerceForeignKey(
            Pipeline pipeline) {
        return pipeline
                .readFrom(Sources.<Object, Object>mapJournal(
                        MyConstants.IMAP_NAME_TRANSACTIONS,
                        JournalInitialPosition.START_FROM_OLDEST))
                    .withIngestionTimestamps()
                 .map(entry -> {
                     try {
                         JSONObject json = new JSONObject(entry.getValue().toString());
                         return json.getString("itemCode");
                     } catch (Exception e) {
                         System.out.println(VectorCollectionMoments.class.getName()
                                 + ":Exception for '" + entry + "': " + e.getMessage());
                         return null;
                     }
                 });
    }

    /**
     * <p>Choose either creditor bank or debitor bank as foreign key of payment.
     * </p>
     * <p>This is the "{@code indexColumn1}"</p>
     *
     * @param pipeline
     * @return
     */
    private static StreamStage<String> extractPaymentsForeignKey(
            Pipeline pipeline) {
        return pipeline
                .readFrom(Sources.<Object, Object>mapJournal(
                        MyConstants.IMAP_NAME_TRANSACTIONS,
                        JournalInitialPosition.START_FROM_OLDEST))
                    .withIngestionTimestamps()
                 .map(entry -> {
                     try {
                         JSONObject json = new JSONObject(entry.getValue().toString());
                         return json.getString("bicCreditor");
                     } catch (Exception e) {
                         System.out.println(VectorCollectionMoments.class.getName()
                                 + ":Exception for '" + entry + "': " + e.getMessage());
                         return null;
                     }
                 });
    }

    /**
     * <p>Stock symbol is the foreign key for a trade
     * </p>
     * <p>This is the "{@code indexColumn1}"</p>
     *
     * @param pipeline
     * @return
     */
    private static StreamStage<String> extractTradeForeignKey(
            Pipeline pipeline) {
        return pipeline
                .readFrom(Sources.<Object, Object>mapJournal(
                        MyConstants.IMAP_NAME_TRANSACTIONS,
                        JournalInitialPosition.START_FROM_OLDEST))
                    .withIngestionTimestamps()
                 .map(entry -> {
                     try {
                         JSONObject json = new JSONObject(entry.getValue().toString());
                         return json.getString("symbol");
                     } catch (Exception e) {
                         System.out.println(VectorCollectionMoments.class.getName()
                                 + ":Exception for '" + entry + "': " + e.getMessage());
                         return null;
                     }
                 });
    }

}
