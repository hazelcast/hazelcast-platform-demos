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

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;

import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.jet.aggregate.AggregateOperation1;
import com.hazelcast.jet.core.AbstractProcessor;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.jet.datamodel.Tuple4;
import com.hazelcast.jet.pipeline.BatchStage;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.Sources;
import com.hazelcast.platform.demos.banking.cva.MyConstants;

/**
 * <p>
 * Version 3 architecture for CVA, <i>straight-through-processing</i>.
 * </p>
 * FIXME Investigate if Protobuf would be faster
 * <p>
 * Code:
 * </p>
 * <p>The job looks conceptually like this:
 * </p>
 * <pre>
 *     +------( 1 )------+   +------( 2 )------+   +------( 3 )------+
 *     |  Map "fixings"  |   | Map "ircurves"  |   |  Map "trades"   |
 *     +-----------------+   +-----------------+   +-----------------+
 *              |                     |                     |
 *              |                     |                     |
 *              |                     |                     |
 *     +------( 4 )------+   +------( 5 )------+   +------( 6 )------+
 *     |"fixings"|   |"ircurves" JSON  |   |  Map "trades"   |
 *     +-----------------+   +-----------------+   +-----------------+
 *                        \           |           /
 *                         \          |          /
 *                          \         |         /
 *                           +------( 4 )------+
 *                           |   3-way join    |
 *                           +-----------------+
 *                                    |
 *                                    |
 *                                    |
 *                           +------( 5 )------+
 *                           | C++ Calculations|
 *                           +-----------------+
 *                                    |
 *                                    |
 *                                    |
 *                           +------( 6 )------+
 *                           |  MTM Averaging  |
 *                           +-----------------+
 *                                    |
 *                                    |
 *                                    |
 *                           +------( 7 )------+
 *                           |  Exposure Calcs |
 *                           +-----------------+
 *                                    |
 *                                    |
 *                                    |
 *                           +------( 8 )------+
 *                           | Counterparty Agg|
 *                           +-----------------+
 *                                    |
 *                                    |
 *                                    |
 *                           +------( 9 )------+
 *                           |   Save Output   |
 *                           +-----------------+
 * </pre>
 * <p>
 * The steps:
 * </p>
 * <ol>
 * <li>
 * <p>
 * XXX
 * </p>
 * <p>
 * </p>
 * </li>
 * <li>
 * <p>
 * XXX
 * </p>
 * <p>
 * </p>
 * </li>
 * <li>
 * <p>
 * XXX
 * </p>
 * <p>
 * </p>
 * </li>
 * <li>
 * <p>
 * XXX
 * </p>
 * <p>
 * </p>
 * </li>
 * <li>
 * <p>
 * XXX
 * </p>
 * <p>
 * </p>
 * </li>
 * </ol>
 * <li>
 * <p>
 * XXX
 * </p>
 * <p>
 * </p>
 * </li>
 * </ol>
 * <li>
 * <p>
 * XXX
 * </p>
 * <p>
 * </p>
 * </li>
 * </ol>
 * <li>
 * <p>
 * XXX
 * </p>
 * <p>
 * </p>
 * </li>
 * </ol>
 * <li>
 * <p>
 * XXX
 * </p>
 * <p>
 * </p>
 * </li>
 * </ol>
 * XXX Is above complete?
 */
public class CvaStpJob {

    public static final String JOB_NAME_PREFIX = CvaStpJob.class.getSimpleName();

    private static final Logger LOGGER = LoggerFactory.getLogger(CvaStpJob.class);

    private static final String STAGE_NAME_AVERAGE_EXPOSURE = "averageExposure";
    private static final String STAGE_NAME_CVA_BY_COUNTERPARTY = "cvaByCounterparty";
    private static final String STAGE_NAME_CVA_EXPOSURE = "cvaExposure";
    private static final String STAGE_NAME_EXPOSURE = "exposure";
    private static final String STAGE_NAME_TRADE_X_IRCURVES = "tradesXircurves";

     /**
     * <p>
     * Creates the pipeline to execute.
     * </p>
     *
     * @param timestampStr For output map names
     * @param debug        For development, save intermediate results
     * @return
     */
    public static Pipeline buildPipeline(String timestampStr, boolean debug) {

        Pipeline pipeline = Pipeline.create();

        // Step XXX above
        BatchStage<Entry<String, HazelcastJsonValue>> tradesSource =
                pipeline
                .readFrom(Sources.<String, HazelcastJsonValue>map(MyConstants.IMAP_NAME_TRADES));

        // Step XXX above
        BatchStage<Entry<String, HazelcastJsonValue>> ircurvesSource =
                pipeline
                .readFrom(Sources.<String, HazelcastJsonValue>map(MyConstants.IMAP_NAME_IRCURVES));

        // Step XXX above
        BatchStage<String> trades =
                tradesSource.map(entry -> entry.getValue().toString())
                .setName(MyConstants.IMAP_NAME_TRADES + "-json");

        // Step XXX above
        BatchStage<String> ircurves =
                ircurvesSource.map(entry -> entry.getValue().toString())
                .setName(MyConstants.IMAP_NAME_IRCURVES + "-json");

        // Step XXX above
        BatchStage<Tuple2<String, String>> tradesXircurves =
                trades.hashJoin(ircurves,
                        CvaStpUtils.cartesianProduct(),
                        (trade, ircurve) -> Tuple2.tuple2(trade, ircurve.toString())
                        )
                .setName(STAGE_NAME_TRADE_X_IRCURVES);


        //FIXME: Replace with C++, include Fixing as initialisation
        //TODO: Explain curvename in key to help debugging
        // Step XXX above
        BatchStage<Tuple3<String, String, String>> mtm =
                tradesXircurves
                .customTransform("TMP-MTM", CvaStpJob.DummyMTMCalc::new);

        //TODO: Explain this doesn't need near-cache
        // Step XXX above
        BatchStage<Tuple3<String, String, String>> exposure =
                mtm
                .groupingKey(Tuple3::f0)
                .mapUsingIMap(MyConstants.IMAP_NAME_TRADES, MtmToExposure.CONVERT)
                .setName(STAGE_NAME_EXPOSURE);

        // Step XXX above
        AggregateOperation1<Tuple3<String, String, String>, ExposureAverager, String> exposureAggregator =
                ExposureAverager.buildExposureAggregation();
        BatchStage<Entry<String, String>> averageExposure =
                exposure
                .groupingKey(Tuple3::f0)
                .aggregate(exposureAggregator)
                .setName(STAGE_NAME_AVERAGE_EXPOSURE);

        //TODO: Explain this does use near-cache
        // Step XXX above
        BatchStage<Tuple4<String, String, String, String>> cvaExposure =
                averageExposure
                .map(Entry::getValue)
                .mapUsingIMap(
                        MyConstants.IMAP_NAME_CP_CDS,
                        ExposureToCds.GET_TICKER_FROM_EXPOSURE,
                        ExposureToCds.CONVERT)
                .setName(STAGE_NAME_CVA_EXPOSURE);

        // Step XXX above
        AggregateOperation1<Tuple4<String, String, String, String>, CvaByCounterpartyTotalizer, String>
            cvaByCounterpartyAggregator =
                CvaByCounterpartyTotalizer.buildCvaByCounterpartyAggregation();
        BatchStage<Entry<String, String>> cvaByCounterparty =
            cvaExposure
            .groupingKey(tuple4 -> tuple4.f1())
            .aggregate(cvaByCounterpartyAggregator)
            .setName(STAGE_NAME_CVA_BY_COUNTERPARTY);

        // Step XXX above
        cvaByCounterparty
            .writeTo(Sinks.map(JOB_NAME_PREFIX + "_" + timestampStr));

        // Optional stages for debugging.
        if (debug) {
            addDebugStages(pipeline, timestampStr, mtm, exposure, averageExposure, cvaExposure, cvaByCounterparty);
        }

        return pipeline;
    }

    /**
     * <p>Optional debugging steps, saving intermediate results to maps.
     * </p>
     * <p>These use {@link java.lang.String} keys rather than
     * {@link com.hazelcast.jet.datamodel.Tuple3} as it makes them
     * easier to query from the Management Center.
     * </p>
     * <p>These maps may have many many entries.
     * </p>
     */
    public static void addDebugStages(Pipeline pipeline, String timestampStr, BatchStage<Tuple3<String, String, String>> mtm,
            BatchStage<Tuple3<String, String, String>> exposure, BatchStage<Entry<String, String>> averageExposure,
            BatchStage<Tuple4<String, String, String, String>> cvaExposure, BatchStage<Entry<String, String>> cvaByCounterparty) {
        // Save MTMs. Watch out there could be billions
        // FIXME, Adjust stage name once C++ ready
        mtm
        .map(tuple3 -> new SimpleImmutableEntry<String, String>(tuple3.f0() + "," + tuple3.f1(), tuple3.f2())).setName("reformat")
        .writeTo(Sinks.map("debug_" + timestampStr + "_TMP-MTM"));

        // Save Exposures. Same count as MTMs.
        exposure
        .map(tuple3 -> new SimpleImmutableEntry<String, String>(tuple3.f0() + "," + tuple3.f1(), tuple3.f2())).setName("reformat")
        .writeTo(Sinks.map("debug_" + timestampStr + "_" + STAGE_NAME_EXPOSURE));

        // Average Exposures. One per Trade
        averageExposure
        .writeTo(Sinks.map("debug_" + timestampStr + "_" + STAGE_NAME_AVERAGE_EXPOSURE));

        // CVA Exposures. One per Trade
        cvaExposure
        .map(tuple4 -> new SimpleImmutableEntry<String, String>(tuple4.f0() + "," + tuple4.f1(), tuple4.f2())).setName("reformat")
        .writeTo(Sinks.map("debug_" + timestampStr + "_" + STAGE_NAME_CVA_EXPOSURE));

        // CVA By Counterparty. One per Counterparty, to the log.
        cvaByCounterparty
        .writeTo(Sinks.logger(entry -> STAGE_NAME_CVA_BY_COUNTERPARTY + "," + entry));
    }

    /**TODO Remove this oncew C++ call available.
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
}
