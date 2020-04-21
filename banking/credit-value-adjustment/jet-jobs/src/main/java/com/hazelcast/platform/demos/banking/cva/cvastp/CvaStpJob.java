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

package com.hazelcast.platform.demos.banking.cva.cvastp;

import java.util.List;
import java.io.Serializable;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;

import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.function.ComparatorEx;
import com.hazelcast.function.Functions;
import com.hazelcast.jet.aggregate.AggregateOperation1;
import com.hazelcast.jet.aggregate.AggregateOperations;
import com.hazelcast.jet.core.AbstractProcessor;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.jet.datamodel.Tuple4;
import com.hazelcast.jet.pipeline.BatchStage;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.Sources;
import com.hazelcast.platform.demos.banking.cva.MyConstants;
import com.hazelcast.platform.demos.banking.cva.MyUtils;

/**
 * <p>
 * Version 3 architecture for CVA, <i>straight-through-processing</i>.
 * </p>
 * <p>The job looks conceptually like this:
 * </p>
 * <pre>
 *     +------( 1 )------+   +------( 2 )------+   +------( 3 )------+   +------( 4 )------+
 *     |  Map "trades"   |   | Map "ircurves"  |   |  Map "fixings"  |   |  Map "cp_cds"   |
 *     +-----------------+   +-----------------+   +-----------------+   +-----------------+
 *              |                     |                     |                     |
 *              |                     |                     |                     |
 *              |                     |                     |                     |
 *     +------( 5 )------+   +------( 6 )------+            |                     |
 *     |    "trades"     |   |    "ircurves"   |            |                     |
 *     +-----------------+   +-----------------+            |                     |
 *               \                   /                      |                     |
 *                \                 /                       |                     |
 *                 \               /                        |                     |
 *                +------( 7 )------+              +------( 8 )------+            |
 *                |  Trade x Curve  |              |    "fixings"    |            |
 *                +-----------------+              +-----------------+            |
 *                         |                                |                     |
 *                         |      +-------------------------+                     |
 *                         |      |                                               |
 *                +------( 9 )------+                                             |
 *                |  C++ MTM Calc   |                                             |
 *                +-----------------+                                             |
 *                         |                                                      |
 *                         |                                                      |
 *                         |                                                      |
 *                +------( 10)------+                                             |
 *                | MTM To Exposure |                                             |
 *                +-----------------+                                             |
 *                         |                                                      |
 *                         |     +------------------------------------------------+
 *                         |     |                                                |
 *                +------( 11)------+                                             |
 *                |   CVA Exposure  |                                             |
 *                +-----------------+                                             |
 *                         |                                                      |
 *                         |                                                      |
 *                         |                                                      |
 *                +------( 12)------+                                             |
 *                | Trade Grouping  |                                             |
 *                +-----------------+                                             |
 *                         |                                                      |
 *                         |                                                      |
 *                         |                                                      |
 *                +------( 13)------+                                             |
 *                |   CP Grouping   |                                             |
 *                +-----------------+                                             |
 *                         |                                                      |
 *                         |                                                      |
 *                         |                                                      |
 *                +------( 14)------+                                    +------( 15)------+
 *                |   Output sort   |                                    |    CP Reformat  |
 *                +-----------------+                                    +-----------------+
 *                         |       |                                              |
 *                         |       +-----------------------------------------+    |
 *                         |                                                 |    |
 *                +------( 16)------+                                    +------( 17)------+
 *                |    CSV File     |                                    |  MS Excel File  |
 *                +-----------------+                                    +-----------------+
 * </pre>
 * <p>
 * The steps:
 * </p>
 * <ol>
 * <li>
 * <p>
 * The "trades" map source.
 * </p>
 * <p>Read from the trades map (actually "{@code cva_trades}" since we prefix our map names).
 * This gives us all trades, but as a this is a map without implied sorting, the order is
 * not predetermined.
 * </p>
 * </li>
 * <li>
 * <p>
 * The "ircurves" map source.
 * </p>
 * <p>Obtain all curves from the "{@code cva_ircuves}" map in Hazelcast, again this is unordered.
 * </p>
 * </li>
 * <li>
 * <p>
 * The "fixings" map source.
 * </p>
 * <p>Obtain the fixing information from the "{@code cva_fixings}" map in Hazelcast. In this
 * demo there is only one entry stored.
 * </p>
 * </li>
 * <li>
 * <p>
 * The "cp_cds" map source.
 * </p>
 * <p>Obtain the counterparty CDSes from the "{@code cva_cp_cds}" map in Hazelcast. In this
 * demo there are 20 of these, and as reading from a map these are not ordered.
 * </p>
 * </li>
 * <li>
 * <p>
 * Reformatted trades.
 * </p>
 * <p>Extract the fields we want from the trade {@link java.util.Map.Entry}
 * </p>
 * </li>
 * <li>
 * <p>
 * Reformatted curves.
 * </p>
 * <p>Extract the fields we want from the IR curve {@link java.util.Map.Entry}
 * </p>
 * </li>
 * <li>
 * <p>
 * Combine trades with curves.
 * </p>
 * <p>Produce the cartesian product, joining all trades with all curves. For
 * 600,000 trades and 5,000 curves this means 3,000,000,000 combinations.
 * </p>
 * </li>
 * <li>
 * <p>
 * Reformatted fixings.
 * </p>
 * <p>Extract the fields we want from the fixings {@link java.util.Map.Entry}
 * </p>
 * </li>
 * <li>
 * <p>
 * Calculate mark-to-market.
 * </p>
 * <p>Pass the trio of trade, curve and fixing into C++ to calculate the
 * mark-to-market exposure. The fixing is the same for the entire run,
 * whereas the trade and curve vary.
 * </p>
 * </li>
 * <li>
 * <p>
 * Calculate Exposure.
 * </p>
 * <p>Convert the mark-to-market value into an exposure.
 * </p>
 * </li>
 * <li>
 * <p>
 * Calculate CVA Exposure.
 * </p>
 * <p>Use the MTM Exposure and the Counterparty CDS information to convert
 * the MTM Exposure into a CVA Exposure. A near-cache is used on the
 * Counterparty CDS lookup, as there a large number of trades (600,000)
 * but only 20 counterparties.
 * </p>
 * </li>
 * <li>
 * <p>
 * Group by trade.
 * </p>
 * <p>Aggregate the output of the previous stage, producing totals for each
 * trade identifier.
 * </p>
 * </li>
 * <li>
 * <p>
 * Group by counterparty.
 * </p>
 * <p>Aggregate the output of the previous stage, producing totals for each unique
 * counterparty code.
 * </p>
 * </li>
 * <li>
 * <p>
 * Sort by counterparty.
 * </p>
 * <p>The previous step produced one CVA Exposure per trade, now this is grouped again
 * by counterparty to produce the total CVA.
 * </p>
 * </li>
 * <li>
 * <p>
 * Extract and reformat counterparty fields.
 * </p>
 * <p>This stage is similar to step 11, selecting fields from the CP CDS relating to
 * the counterparty for later printing (such as the full name rather than ticker code).
 * This could be done as part of step 11, but would mean then add these fields to the output
 * from that stage to carry down to here, which would not seem so obvious.
 * </p>
 * </li>
 * <li>
 * <p>
 * Create a CSV File to download.
 * </p>
 * <p>This step turns the list of counterparty's CVA Exposure aggregations into an
 * CSV format file, ready for downloading.
 * </p>
 * </li>
 * <li>
 * <p>
 * Create a Excel File to download.
 * </p>
 * <p>This step is the same in concept as the previous, but instead of a CSV file it's an
 * Excel spreadsheet that is produced. The columns in the spreadsheet come both
 * from the calculated CVAs and by a looking to the CP CDS information.
 * </p>
 * </li>
 * </ol>
 * <p>TODO Swap to {@link java.math.BigDecimal}  instead of Java's {@code double}
 * to avoid loss of numeric precision.
 * </p>
 * <p>TODO Compare performance if using
 * <a href="https://developers.google.com/protocol-buffers">Protobuf</a> instead
 * of JSON.
 * </p>
 */
public class CvaStpJob {

    public static final String JOB_NAME_PREFIX = CvaStpJob.class.getSimpleName();

    private static final Logger LOGGER = LoggerFactory.getLogger(CvaStpJob.class);

    private static final String STAGE_NAME_CVA_EXPOSURE = "cvaExposure";
    private static final String STAGE_NAME_CVA_EXPOSURE_BY_COUNTERPARTY = "cvaExposureByCounterparty";
    private static final String STAGE_NAME_CVA_EXPOSURE_BY_TRADE = "cvaExposureByTrade";
    private static final String STAGE_NAME_EXPOSURE = "exposure";
    private static final String STAGE_NAME_SORTED_CP_CDS = "sortedCpCds";
    private static final String STAGE_NAME_SORTED_CVA_EXPOSURE_BY_COUNTERPARTY = "sortedCvaExposureByCounterparty";
    private static final String STAGE_NAME_TRADE_X_IRCURVES = "tradesXircurves";

     /**
     * <p>
     * Creates the pipeline to execute.
     * </p>
     *
     * @param jobName      For debug logging
     * @param timestamp    Job submit time
     * @param debug        For development, save intermediate results
     * @return
     */
    @SuppressWarnings("unchecked")
    public static Pipeline buildPipeline(String jobName, long timestamp, boolean debug) {
        String timestampStr = MyUtils.timestampToISO8601(timestamp);

        Pipeline pipeline = Pipeline.create();

        boolean debug2 = debug;

        //XXX Temporary test use, use test MTM data.

        BatchStage<Tuple3<String, String, String>> mtm = null;
        if (!debug2) {
        // Step 1 above
        BatchStage<Entry<String, HazelcastJsonValue>> tradesSource =
                pipeline
                .readFrom(Sources.<String, HazelcastJsonValue>map(MyConstants.IMAP_NAME_TRADES));

        // Step 2 above
        BatchStage<Entry<String, HazelcastJsonValue>> ircurvesSource =
                pipeline
                .readFrom(Sources.<String, HazelcastJsonValue>map(MyConstants.IMAP_NAME_IRCURVES));

        // Step 5 above
        BatchStage<String> trades =
                tradesSource.map(entry -> entry.getValue().toString())
                .setName(MyConstants.IMAP_NAME_TRADES + "-json");

        // Step 6 above
        BatchStage<String> ircurves =
                ircurvesSource.map(entry -> entry.getValue().toString())
                .setName(MyConstants.IMAP_NAME_IRCURVES + "-json");

        // Step 7 above
        BatchStage<Tuple2<String, String>> tradesXircurves =
                trades.hashJoin(ircurves,
                        CvaStpUtils.cartesianProduct(),
                        (trade, ircurve) -> Tuple2.tuple2(trade, ircurve.toString())
                        )
                .setName(STAGE_NAME_TRADE_X_IRCURVES);


        //FIXME: Replace with C++, include Fixing as initialisation
        // Step 9 above
            mtm =
                tradesXircurves
                .customTransform("TMP-MTM", CvaStpJob.DummyMTMCalc::new);
        } else {
            mtm =
                    pipeline
                    .readFrom(Sources.<Tuple2<String, String>, HazelcastJsonValue>map("mtms"))
                    .map(entry -> Tuple3.tuple3(entry.getKey().f0(), entry.getKey().f1(), entry.getValue().toString()))
                    ;
        }

        // Step 10 above
        BatchStage<Tuple3<String, String, String>> exposure =
                mtm
                .groupingKey(Tuple3::f0)
                .mapUsingIMap(MyConstants.IMAP_NAME_TRADES, MtmToExposure.CONVERT)
                .setName(STAGE_NAME_EXPOSURE);

        // Step 11 above
        BatchStage<Tuple3<String, String, String>> cvaExposure =
                exposure
                .map(Tuple3::f2)
                .mapUsingIMap(
                        MyConstants.IMAP_NAME_CP_CDS,
                        ExposureToCvaExposure.GET_TICKER_FROM_EXPOSURE,
                        ExposureToCvaExposure.CONVERT)
                .setName(STAGE_NAME_CVA_EXPOSURE);

        // Step 12 above
        AggregateOperation1<Tuple3<String, String, String>, TradeExposureAggregator, Tuple2<String, String>>
            tradeExposureAggregator =
            TradeExposureAggregator.buildTradeExposureAggregator();

        BatchStage<Entry<String, Tuple2<String, String>>> cvaExposureByTrade =
            cvaExposure
            .groupingKey(Tuple3::f0)
            .aggregate(tradeExposureAggregator)
            .setName(STAGE_NAME_CVA_EXPOSURE_BY_TRADE);

        // Step 13 above
        AggregateOperation1<Entry<String, Tuple2<String, String>>, CounterpartyAggregator, Double>
            counterpartyAggregator =
                CounterpartyAggregator.buildCounterpartyAggregation();

        BatchStage<Entry<String, Double>> cvaExposureByCounterparty =
            cvaExposureByTrade
            .groupingKey(entry -> entry.getValue().f0())
            .aggregate(counterpartyAggregator)
            .setName(STAGE_NAME_CVA_EXPOSURE_BY_COUNTERPARTY);

        // Step 14 above, tuple3 is jobname, timestamp, list of cp,cva
        BatchStage<Tuple3<String, Long, List<Entry<String, Double>>>> sortedCvaExposureByCounterparty =
                cvaExposureByCounterparty
                .aggregate(AggregateOperations.mapping(Functions.wholeItem(),
                        AggregateOperations.sorting(new CvaStpJob.MapKeyComparator())))
                .map(list -> Tuple3.tuple3(jobName, timestamp, list))
                .setName(STAGE_NAME_SORTED_CVA_EXPOSURE_BY_COUNTERPARTY);

        //XXX Move to top with other map sources
        // Step 4 above
        BatchStage<Entry<String, HazelcastJsonValue>> cpCdsSource =
                pipeline
                .readFrom(Sources.<String, HazelcastJsonValue>map(MyConstants.IMAP_NAME_CP_CDS));

        // Step 15 above
        BatchStage<List<Entry<String, HazelcastJsonValue>>> cpCdsEntryList =
            cpCdsSource
            .aggregate(AggregateOperations.mapping(Functions.wholeItem(),
                AggregateOperations.sorting(new CvaStpJob.MapKeyComparator())))
            .setName(STAGE_NAME_SORTED_CP_CDS);

        // Step 16 above, tuple3 is jobname, timestamp, list of cp,cva
        sortedCvaExposureByCounterparty
        .map(CsvFileAsByteArray.CONVERT_TUPLE3_TO_BYTE_ARRAY)
        .map(bytes -> new SimpleImmutableEntry<String, byte[]>(timestampStr, bytes))
        .writeTo(Sinks.map(MyConstants.IMAP_NAME_CVA_CSV));

        // Step 17 above
        sortedCvaExposureByCounterparty
        .hashJoin(cpCdsEntryList,
                CvaStpUtils.cartesianProduct(),
                (sortedCvaExposures, cpCdsEntries) ->
                    Tuple4.tuple4(sortedCvaExposures.f0(), sortedCvaExposures.f1(), sortedCvaExposures.f2(),
                            (List<Entry<String, HazelcastJsonValue>>) cpCdsEntries)
                )
        .map(XlstFileAsByteArray.CONVERT_TUPLES_TO_BYTE_ARRAY)
        .map(bytes -> new SimpleImmutableEntry<String, byte[]>(timestampStr, bytes))
        .writeTo(Sinks.map(MyConstants.IMAP_NAME_CVA_XLSX))
        ;

        // Optional stages for debugging.
        if (debug) {
            addDebugStages(pipeline, timestampStr, jobName, mtm, exposure, cvaExposure,
                    cvaExposureByTrade, cvaExposureByCounterparty, sortedCvaExposureByCounterparty);
        }

        return pipeline;
    }


    /**
     * <p>Optional debugging steps, saving intermediate results to maps.
     * </p>
     * <p>These use {@link java.lang.String} keys rather than
     * {@link com.hazelcast.jet.datamodel.Tuple3} for example, as it makes them
     * easier to query from the Management Center.
     * </p>
     * <p>These maps may have many many entries.
     * </p>
     */
    public static void addDebugStages(Pipeline pipeline, String timestampStr,
            String jobName,
            BatchStage<Tuple3<String, String, String>> mtm,
            BatchStage<Tuple3<String, String, String>> exposure,
            BatchStage<Tuple3<String, String, String>> cvaExposure,
            BatchStage<Entry<String, Tuple2<String, String>>> cvaExposureByTrade,
            BatchStage<Entry<String, Double>> cvaExposureByCounterparty,
            BatchStage<Tuple3<String, Long, List<Entry<String, Double>>>> sortedCvaExposureByCounterparty) {

        /* (1) Save MTMs. Watch out there could be billions
         */
        mtm
        .map(tuple3 -> new SimpleImmutableEntry<String, String>(tuple3.f0() + "," + tuple3.f1(), tuple3.f2())).setName("reformat")
        // FIXME, Adjust stage name once C++ ready
        .writeTo(Sinks.map("debug_" + timestampStr + "_TMP-MTM"));

        /* (2) Save Exposures. Same count as MTMs.
         */
        exposure
        .map(tuple3 -> new SimpleImmutableEntry<String, String>(tuple3.f0() + "," + tuple3.f1(), tuple3.f2())).setName("reformat")
        .writeTo(Sinks.map("debug_" + timestampStr + "_" + STAGE_NAME_EXPOSURE));

        /* (3) Save CVA Exposures. Same count as MTMs.
         */
        cvaExposure
        .map(tuple3 -> new SimpleImmutableEntry<String, String>(tuple3.f0() + "," + tuple3.f1(), tuple3.f2())).setName("reformat")
        .writeTo(Sinks.map("debug_" + timestampStr + "_" + STAGE_NAME_CVA_EXPOSURE));

        /* (4) Save CVA Exposures by Trade. Same count as trades.
         */
        cvaExposureByTrade
        .map(entry -> new SimpleImmutableEntry<String, String>(entry.getKey(), entry.getValue().f1())).setName("reformat")
        .writeTo(Sinks.map("debug_" + timestampStr + "_" + STAGE_NAME_CVA_EXPOSURE_BY_TRADE));

        /* (5) Save CVA Exposures by Counterparty. Same count as counterparties.
         */
        cvaExposureByCounterparty
        .writeTo(Sinks.map("debug_" + timestampStr + "_" + STAGE_NAME_CVA_EXPOSURE_BY_COUNTERPARTY));

        /* (6) A single sorted list of CVA Exposures by Counterparty. Log to console
         * as it is saved to a map already.
         */
        String delimiter =  MyConstants.BANNER + " " + jobName + " " + MyConstants.BANNER;
        sortedCvaExposureByCounterparty
        .map(Tuple3::f2)
        .writeTo(Sinks.logger(list -> String.format("%n%s%n%s%n%s%n", delimiter, list, delimiter)));
    }


    /**FIXME Remove this once C++ call available.
     * <p>Fake an MTM for now, until C++ calcs are slotted in.</p>
     * <p>Expected MTM looks something like:</p>
     * <pre>
     * {"tradeid":"t000000","curvename":"curvescenario0000",
     * "fixlegdates":["1454025600","1461884400","1469746800","1477872000"],
     * "fixlegamount":[327111.094,323555.562,323555.562,334222.219],
     * "fltlegdates":["1454025600","1461884400","1469746800","1477872000"],
     * "fltlegamount":[33109780,777095.75,1283875.62,1233292.38],
     * "discountvalues":[0.999752,0.997813523,0.994621098,0.991563857],
     * "legfractions":[0.0597826093,0.307065219,0.554347813,0.809782624],
     * "haserrored":false,"error":"","computetimemicros":"0"}
     * </pre>
     */
    public static class DummyMTMCalc extends AbstractProcessor {

        @Override
        protected boolean tryProcess(int ordinal, Object item) {
                @SuppressWarnings("unchecked")
                Tuple2<String, String> tuple2 =
                        (Tuple2<String, String>) item;

                StringBuilder stringBuilder = new StringBuilder();

                try {
                    String tradeId = new JSONObject(tuple2.f0()).getString("tradeid");
                    String curveName = new JSONObject(tuple2.f1()).getString("curvename");

                    stringBuilder.append("{");
                    stringBuilder.append(" \"tradeid\": \"" + tradeId + "\"");
                    stringBuilder.append(", \"curvename\": \"" + curveName + "\"");
                    stringBuilder.append(", \"fixlegdates\": [\"1454025600\",\"1461884400\",\"1469746800\",\"1477872000\"]");
                    stringBuilder.append(", \"fixlegamount\": [327111.094,323555.562,323555.562,334222.219]");
                    stringBuilder.append(", \"fltlegdates\": [\"1454025600\",\"1461884400\",\"1469746800\",\"1477872000\"]");
                    stringBuilder.append(", \"fltlegamount\": [33109780,777095.75,1283875.62,1233292.38]");
                    stringBuilder.append(", \"discountvalues\": [0.999752,0.997813523,0.994621098,0.991563857]");
                    stringBuilder.append(", \"legfractions\": [0.0597826093,0.307065219,0.554347813,0.809782624]");
                    stringBuilder.append(", \"haserrored\":false");
                    stringBuilder.append(", \"computetimemicros\":\"0\"");
                    stringBuilder.append(" }");

                    Tuple3<String, String, String> tuple3
                        = Tuple3.tuple3(tradeId, curveName, stringBuilder.toString());

                    return super.tryEmit(tuple3);
                } catch (JSONException jsonException) {
                    LOGGER.error("JSONException: =>" + stringBuilder, jsonException);
                    return true;
                }
        }
    }


    /**
     * <p>Serializable utility class to use in the job to sort map entries by
     * key. Assumes the key is comparable.
     * </p>
     */
    public static class MapKeyComparator implements ComparatorEx<Entry<String, ?>>, Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * <p>Use default collating sequence on the entry keys to decide.
         * </p>
         */
        @Override
        public int compareEx(Entry<String, ?> o1, Entry<String, ?> o2) throws Exception {
            return o1.getKey().compareTo(o2.getKey());
        }
    }
}
