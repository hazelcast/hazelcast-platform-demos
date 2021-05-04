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

package com.hazelcast.platform.demos.banking.cva.cvastp;

import java.util.List;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Map.Entry;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.cluster.Cluster;
import com.hazelcast.cluster.Member;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.function.ComparatorEx;
import com.hazelcast.function.FunctionEx;
import com.hazelcast.function.Functions;
import com.hazelcast.jet.aggregate.AggregateOperation1;
import com.hazelcast.jet.aggregate.AggregateOperations;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.jet.datamodel.Tuple4;
import com.hazelcast.jet.grpc.GrpcService;
import com.hazelcast.jet.grpc.GrpcServices;
import com.hazelcast.jet.pipeline.BatchStage;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.ServiceFactories;
import com.hazelcast.jet.pipeline.ServiceFactory;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.Sources;
import com.hazelcast.platform.demos.banking.cva.InputMessage;
import com.hazelcast.platform.demos.banking.cva.JetToCppGrpc;
import com.hazelcast.platform.demos.banking.cva.MyConstants;
import com.hazelcast.platform.demos.banking.cva.MyUtils;
import com.hazelcast.platform.demos.banking.cva.OutputMessage;

import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;

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
 *     +------( 5 )------+   +------( 6 )------+   +------( 7 )------+            |
 *     |    "trades"     |   |    "ircurves"   |   |    "fixings"    |            |
 *     +-----------------+   +-----------------+   +-----------------+            |
 *               \                   /                      |                     |
 *                \                 /                       |                     |
 *                 \               /                        |                     |
 *                +------( 8 )------+                       |                     |
 *                |  Trade x Curve  |                       |                     |
 *                +-----------------+                       |                     |
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
 *                |    CSV File     |                                    | MS Excel Prepare|
 *                +-----------------+                                    +-----------------+
 *                                                                                |
 *                                                +-------------------------------+
 *                                                |                               |
 *                                      +------( 18)------+              +------( 19)------+
 *                                      |  MS Excel Live  |              |  MS Excel File  |
 *                                      +-----------------+              +-----------------+
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
 * Reformatted fixings.
 * </p>
 * <p>Extract the fields we want from the fixings {@link java.util.Map.Entry}
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
 * Create Excel File content.
 * </p>
 * <p>This step is the same in concept as the previous, but instead of a CSV file it's
 * an array of array representing the rows and columns that will go in an Excel spreadsheet.
 * The columns in the spreadsheet come both from the calculated CVAs and by a looking to the
 * CP CDS information.
 * </p>
 * </li>
 * <li>
 * <p>
 * Store Excel File content for Excel Live Connect.
 * </p>
 * <p>This step saves the Excel content, an array of arrays, into an IMap. Logically
 * this is close the CSV format, except there are different (extra) columns.
 * </p>
 * </li>
 * <li>
 * <p>
 * Create a Excel File to download.
 * </p>
 * <p>Turn the array of array of data into an actual Excel spreadsheet, and store
 * this in binary form in an IMap.
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
 * <p>TODO Move fixings to initialisation of C++.
 * </p>
 * <p>TODO With change to batching, determine batch size.
 * </p>
 */
public class CvaStpJob {

    public static final String JOB_NAME_PREFIX = CvaStpJob.class.getSimpleName();

    private static final Logger LOGGER = LoggerFactory.getLogger(CvaStpJob.class);

    private static final String STAGE_NAME_CVA_EXPOSURE = "cvaExposure";
    private static final String STAGE_NAME_CVA_EXPOSURE_BY_COUNTERPARTY = "cvaExposureByCounterparty";
    private static final String STAGE_NAME_CVA_EXPOSURE_BY_TRADE = "cvaExposureByTrade";
    private static final String STAGE_NAME_EXPOSURE = "exposure";
    private static final String STAGE_NAME_MTM = "mtm-cpp";
    private static final String STAGE_NAME_OBJECT_ARRAY_ARRAY = "Object[][]";
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
     * @param calcDate     For C++
     * @param loadBalancer Front for C++ servers
     * @param port         For C++ servers
     * @param batchSize    For C++, how many requests to send
     * @param parallelism  Ratio of C++ workers to Jet node
     * @param debug        For development, save intermediate results
     * @return
     */
    public static Pipeline buildPipeline(String jobName, long timestamp, LocalDate calcDate,
            String loadBalancer, int port, int batchSize, int parallelism, boolean debug) {
        String timestampStr = MyUtils.timestampToISO8601(timestamp);
        String calcDateStr = CvaStpUtils.escapeQuotes("{\"calc_date\":\"" + calcDate + "\"}");

        Pipeline pipeline = Pipeline.create();

        // Step 1 above, provides map entries for trades
        BatchStage<Entry<String, HazelcastJsonValue>> tradesSource =
                pipeline.readFrom(Sources.<String, HazelcastJsonValue>map(MyConstants.IMAP_NAME_TRADES));

        // Step 2 above, provides map entries for curves
        BatchStage<Entry<String, HazelcastJsonValue>> ircurvesSource =
                pipeline.readFrom(Sources.<String, HazelcastJsonValue>map(MyConstants.IMAP_NAME_IRCURVES));

        // Step 3 above, provides map entries for fixings
        BatchStage<Entry<String, HazelcastJsonValue>> fixingsSource =
                pipeline.readFrom(Sources.<String, HazelcastJsonValue>map(MyConstants.IMAP_NAME_FIXINGS));

        // Step 4 above, provides map entries for counterparty credit default swaps
        BatchStage<Entry<String, HazelcastJsonValue>> cpCdsSource =
                pipeline.readFrom(Sources.<String, HazelcastJsonValue>map(MyConstants.IMAP_NAME_CP_CDS));

        // Step 5 above, provides JSON trades as unsorted strings
        BatchStage<String> trades =
                tradesSource.map(entry -> entry.getValue().toString())
                .setName(MyConstants.IMAP_NAME_TRADES + "-json");

        // Step 6 above, provides JSON curves as unsorted strings
        BatchStage<String> ircurves =
                ircurvesSource.map(entry -> entry.getValue().toString())
                .setName(MyConstants.IMAP_NAME_IRCURVES + "-json");

        // Step 7 above, provides JSON fixings as unsorted strings
        BatchStage<String> fixings =
                fixingsSource.map(entry -> entry.getValue().toString())
                .setName(MyConstants.IMAP_NAME_FIXINGS + "-json");

        // Step 8 above, join 5 & 6, provides all pairs of trades joined to curves
        BatchStage<Tuple2<String, String>> tradesXircurves =
                trades.hashJoin(ircurves, CvaStpUtils.cartesianProduct(),
                        (trade, ircurve) -> Tuple2.tuple2(trade, ircurve.toString()))
                .setName(STAGE_NAME_TRADE_X_IRCURVES);

        // Step 9 above, provides trio of trade, curve and MTM
        BatchStage<Tuple3<String, String, String>> mtm =
                callCppForMtm(loadBalancer, port, batchSize, parallelism,
                        tradesXircurves, fixings, calcDateStr);

        // Step 10 above, provides trio of trade, curve, exposure
        BatchStage<Tuple3<String, String, String>> exposure =
                convertMtmToExposure(mtm);

        // Step 11 above, provides trio of trade, curve, CVA exposure
        BatchStage<Tuple3<String, String, String>> cvaExposure =
                convertExposureToCvaExposure(exposure);

        // Step 12 above, provides counterparty and exposure sum per trade
        BatchStage<Entry<String, Tuple2<String, String>>> cvaExposureByTrade =
                convertCvaExposureToCvaExposureByTrade(cvaExposure);

        // Step 13 above, provides counterparty and total exposure per counterparty
        BatchStage<Entry<String, Double>> cvaExposureByCounterparty =
                sumCvaExposoreByTradeByCounterparty(cvaExposureByTrade);

        // Step 14 above, collect all counterparty exposures into a single list
        BatchStage<Tuple3<String, Long, List<Entry<String, Double>>>> sortedCvaExposureByCounterparty =
                collateCvaExposoreByTradeByCounterparty(jobName, timestamp, cvaExposureByCounterparty);

        // Step 15 above, collects all counterparty CDS into a single list
        BatchStage<List<Entry<String, HazelcastJsonValue>>> cpCdsEntryList =
                collateCpCds(cpCdsSource);

        // Step 16 above, saves the counterparty exposures into a CSV file for download
        saveAsCsvForLaterDownload(sortedCvaExposureByCounterparty, calcDate, timestampStr);

        // Step 17 above, provides a 2-dimensional array ready for Excel usage
        BatchStage<Object[][]> excelDataContent =
                makeObjectForExcel(sortedCvaExposureByCounterparty, cpCdsEntryList);

        // Step 18 above, provides Excel live connect value
        excelDataContent
        .map(bytes -> new SimpleImmutableEntry<String, Object[][]>(calcDate + "@" + timestampStr, bytes))
        .writeTo(Sinks.map(MyConstants.IMAP_NAME_CVA_DATA));

        // Step 19 above, provides Excel spreadsheet for download
        saveAsExcelForLaterDownload(excelDataContent, jobName, timestamp, timestampStr, calcDate);

        // Optional stages for debugging, impacting performance and memory usage
        if (debug) {
            addDebugSaveStages(calcDate, timestampStr, mtm, exposure, cvaExposure,
                    cvaExposureByTrade, cvaExposureByCounterparty);
            addDebugLogStages(jobName, sortedCvaExposureByCounterparty);
        }

        return pipeline;
    }


    /**
     * <p>Push a quadruple of calculation date, fixing dates &amp; rates, trade and interest
     * rate curve to the <b>C++</b> calculation, which will return the <i>mark-to-market</i>
     * for the curve scenario.
     * </p>
     * <p>As in, pass a 4-field JSON object through a GRPC connection to a C++ server which
     * returns something, which happens to be the result of the MTM calculations.
     * </p>
     *
     * @param host A load balancer fronting the C++ calculation processes
     * @param port Expect all C++ calculation processes to use the same port
     * @param batchSize How many requests to send in a batch
     * @param parallelism How many C++ workers per Jet node
     * @param tradesXircurves A pair of trade and interest rate curve
     * @param fixings The fixing date/rate to use
     * @param calcDateStr The date for the calculation
     * @return The answer from C++
     *
     * <p>TODO: See general improvements. Calculation date is the same for all invocations
     * so should be used as initialisation. Batching would be more efficient.
     * </p>
     */
    private static BatchStage<Tuple3<String, String, String>> callCppForMtm(String host, int port,
            int batchSize, int parallelism,
            BatchStage<Tuple2<String, String>> tradesXircurves, BatchStage<String> fixings, String calcDateStr) {

        /* A diagnostic service factory to get the calling member to pass to C++.
         */
        ServiceFactory<?, Cluster> clusterService =
                ServiceFactories.sharedService(ctx -> ctx.jetInstance().getCluster());

        /* A service factory to provide a BiDirectional connection to a C++ server,
         * using the provided channel builder and invoking function.
         */
        FunctionEx<? super ManagedChannel,
                ? extends FunctionEx<StreamObserver<OutputMessage>, StreamObserver<InputMessage>>>
             callStubFn = channel -> JetToCppGrpc.newStub(channel)::streamingCall;
        ServiceFactory<?, ? extends GrpcService<InputMessage, OutputMessage>> cppService =
                GrpcServices.bidirectionalStreamingService(
                        () -> CvaStpUtils.getManagedChannelBuilder(host, port), callStubFn);

        /* Make the input to C++ for the service call, and extract the output
         * from the result.
         */
        BatchStage<Tuple3<String, String, String>> mtm =
                tradesXircurves.hashJoin(fixings, CvaStpUtils.cartesianProduct(),
                        (tuple2, fixing) -> Tuple3.tuple3(tuple2.f0(), tuple2.f1(), fixing))
                .mapUsingService(clusterService,
                        (service, tuple3) -> {
                            Member member = service.getLocalMember();

                            String source = member.getAddress().getHost() + ":"
                                    + member.getAddress().getPort();

                            return Tuple4.tuple4(tuple3.f0(), tuple3.f1(), tuple3.f2(), source);
                        })
                .mapUsingServiceAsyncBatched(cppService,
                        batchSize,
                        (service, tuple4List) -> {
                            List<String> jsonStrList = new ArrayList<>();
                            for (Tuple4<String, String, Object, String> tuple4 : tuple4List) {
                                jsonStrList.add(formJsonStr(tuple4, calcDateStr));
                            }

                            InputMessage request =
                                    InputMessage.newBuilder().addAllInputValue(jsonStrList).build();

                            return service.call(request).thenApply(result -> {
                                List<Tuple3<String, String, String>> batch = new ArrayList<>();

                                for (int i = 0 ; i < result.getOutputValueCount(); i++) {
                                    try {
                                        String tradeId =
                                                new JSONObject(tuple4List.get(i).f0()).getString("tradeid");
                                        String curveName =
                                                new JSONObject(tuple4List.get(i).f1()).getString("curvename");

                                        batch.add(Tuple3.tuple3(tradeId, curveName, result.getOutputValue(i)));
                                    } catch (Exception e) {
                                        LOGGER.error(e.getMessage(), e);
                                    }
                                }

                                return batch;
                            });
                        })
                .setLocalParallelism(parallelism)
                .setName(STAGE_NAME_MTM);

        return mtm;
    }


    /**
     * <p>Create a string that actually holds JSON, to pass to the C++ pricer.
     * Field "{@code debug}" is optional.
     * </p>
     * <p>TODO Moving calcdate, debug and fixing to one per batch.
     * </p>
     *
     * @param tuple4 TradeId, CurveName, Fixing and Jet Member
     * @param calcDateStr Pre-formatted calculation date
     * @return A string holding JSON to parse on the receiver
     */
    private static String formJsonStr(Tuple4<String, String, Object, String> tuple4, String calcDateStr) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("{");
        stringBuilder.append(" \"calcdate\": \""
                + calcDateStr + "\"");
        stringBuilder.append(", \"debug\": \""
                + CvaStpUtils.escapeQuotes(tuple4.f3()) + "\"");
        stringBuilder.append(", \"trade\": \""
                + CvaStpUtils.escapeQuotes(tuple4.f0()) + "\"");
        stringBuilder.append(", \"curve\": \""
                + CvaStpUtils.escapeQuotes(tuple4.f1()) + "\"");
        stringBuilder.append(", \"fixing\": \""
                + CvaStpUtils.escapeQuotes(tuple4.f2().toString()) + "\"");
        stringBuilder.append("}");

        return stringBuilder.toString();
    }


    /**
     * <p>Convert the Mark-To-Market value into an Exposure, using
     * {@link com.hazelcast.platform.demos.banking.cva.cvastp.MtmToExposure}
     * to do the work.
     * </p>
     *
     * @param mtm A trio of trade key, curve key and MTM from C++
     * @return A trio of trade key, curve key and Exposure
     */
    private static BatchStage<Tuple3<String, String, String>> convertMtmToExposure(
            BatchStage<Tuple3<String, String, String>> mtm) {
        return mtm
                .groupingKey(Tuple3::f0)
                .mapUsingIMap(MyConstants.IMAP_NAME_TRADES, MtmToExposure.CONVERT)
                .setName(STAGE_NAME_EXPOSURE);
    }


    /**
     * <p>Convert the exposure to be the CVA exposure, using
     * {@link com.hazelcast.platform.demos.banking.cva.cvastp.ExposureToCvaExposure}.
     * to do the conversion.
     * </p>
     *
     * @param exposure A trio of trade (ignored), curve (ignored) and exposure.
     * @return A trio of trade id (key), curve name (key) and CVA exposure
     */
    private static BatchStage<Tuple3<String, String, String>> convertExposureToCvaExposure(
            BatchStage<Tuple3<String, String, String>> exposure) {
        return exposure
                .map(Tuple3::f2)
                .mapUsingIMap(
                        MyConstants.IMAP_NAME_CP_CDS,
                        ExposureToCvaExposure.GET_TICKER_FROM_EXPOSURE,
                        ExposureToCvaExposure.CONVERT)
                .setName(STAGE_NAME_CVA_EXPOSURE);
    }


    /**
     * <p>Aggregate (summing) CVA Exposures by Trade Id.
     * </p>
     *
     * @param cvaExposure A trio of trade id (key), curve name (key) and CVA exposure
     * @return
     */
    private static BatchStage<Entry<String, Tuple2<String, String>>>
        convertCvaExposureToCvaExposureByTrade(
            BatchStage<Tuple3<String, String, String>> cvaExposure) {

        AggregateOperation1<Tuple3<String, String, String>, TradeExposureAggregator, Tuple2<String, String>>
        tradeExposureAggregator =
        TradeExposureAggregator.buildTradeExposureAggregator();

        return cvaExposure
                .groupingKey(Tuple3::f0)
                .aggregate(tradeExposureAggregator)
                .setName(STAGE_NAME_CVA_EXPOSURE_BY_TRADE);
    }


    /**
     * <p>Take the list of CVA exposure per trade, and group this per counterparty
     * to find the exposure for that counterparty. This should be a large reduction,
     * there are hundreds of thousands of CVA exposures (one per trade, for which
     * there are hundreds of thousands), but only 20 counterparties in this demo.
     * </p>
     *
     * @param cvaExposureByTrade A counterparty and exposure per trade
     * @return The counterparty and it's total exposure
     */
    private static BatchStage<Entry<String, Double>> sumCvaExposoreByTradeByCounterparty(
              BatchStage<Entry<String, Tuple2<String, String>>> cvaExposureByTrade) {
          AggregateOperation1<Entry<String, Tuple2<String, String>>, CounterpartyAggregator, Double>
          counterpartyAggregator =
              CounterpartyAggregator.buildCounterpartyAggregation();

          return cvaExposureByTrade
          .groupingKey(entry -> entry.getValue().f0())
          .aggregate(counterpartyAggregator)
          .setName(STAGE_NAME_CVA_EXPOSURE_BY_COUNTERPARTY);
    }


    /**
     * <p>Collate the counterparty exposures into a single list.
     * </p>
     *
     * @param jobName Not part of the collate, useful for the next stage
     * @param timestamp Not part of the collate, useful for the next stage.
     * @param cvaExposureByCounterparty
     * @return A trio of jobname, timestamp and list
     */
    private static BatchStage<Tuple3<String, Long, List<Entry<String, Double>>>> collateCvaExposoreByTradeByCounterparty(
            String jobName, long timestamp,
            BatchStage<Entry<String, Double>> cvaExposureByCounterparty) {
        return cvaExposureByCounterparty
                .aggregate(AggregateOperations.mapping(Functions.wholeItem(),
                        AggregateOperations.sorting(new CvaStpJob.MapKeyComparator())))
                .map(list -> Tuple3.tuple3(jobName, timestamp, list))
                .setName(STAGE_NAME_SORTED_CVA_EXPOSURE_BY_COUNTERPARTY);
    }


    /**
     * <p>Collate the Counterparty CDS into a single list.
     * </p>
     *
     * @param cpCdsSource Unsorted content from an {@link com.hazelcast.map.IMap}
     * @return A sorted list
     */
    private static BatchStage<List<Entry<String, HazelcastJsonValue>>> collateCpCds(
            BatchStage<Entry<String, HazelcastJsonValue>> cpCdsSource) {
        return cpCdsSource
                .aggregate(AggregateOperations.mapping(Functions.wholeItem(),
                        AggregateOperations.sorting(new CvaStpJob.MapKeyComparator())))
                    .setName(STAGE_NAME_SORTED_CP_CDS);
    }


    /**
     * <p>Turn the counterparty exposures it a CSV file and store it in an
     * {@link com.hazelcast.map.IMap} for later download.
     * <p>
     *
     * @param sortedCvaExposureByCounterparty Data for the CSV
     * @param calcDate For the entry key
     * @param timestampStr For the entry key
     */
    private static void saveAsCsvForLaterDownload(
            BatchStage<Tuple3<String, Long, List<Entry<String, Double>>>> sortedCvaExposureByCounterparty,
            LocalDate calcDate, String timestampStr) {
        sortedCvaExposureByCounterparty
        .map(CsvFileAsByteArray.CONVERT_TUPLE3_TO_BYTE_ARRAY)
        .map(bytes -> new SimpleImmutableEntry<String, byte[]>(calcDate + "@" + timestampStr, bytes))
        .writeTo(Sinks.map(MyConstants.IMAP_NAME_CVA_CSV));
    }


    /**
     * <p>Join the counterparty exposures with the counterparty CDS details to form
     * the content to put in an Excel spreadsheet.
     * </p>
     * <p>Depending on the data, more counterparties may exist that be used,
     * so "{@code sortedCvaExposureByCounterparty}" may be less than
     * "{@code cpCdsEntryList}".
     * </p>
     *
     * @param sortedCvaExposureByCounterparty All calculated counterparty exposures
     * @param cpCdsEntryList All stored counterparties.
     * @return A single 2-dimensional object array
     */
    @SuppressWarnings("unchecked")
    private static BatchStage<Object[][]> makeObjectForExcel(
            BatchStage<Tuple3<String, Long, List<Entry<String, Double>>>> sortedCvaExposureByCounterparty,
            BatchStage<List<Entry<String, HazelcastJsonValue>>> cpCdsEntryList
            ) {
        return sortedCvaExposureByCounterparty
                .hashJoin(cpCdsEntryList,
                        CvaStpUtils.cartesianProduct(),
                        (sortedCvaExposures, cpCdsEntries) ->
                            Tuple4.tuple4(sortedCvaExposures.f0(), sortedCvaExposures.f1(), sortedCvaExposures.f2(),
                                    (List<Entry<String, HazelcastJsonValue>>) cpCdsEntries)
                        )
                .map(XlstDataAsObjectArrayArray.CONVERT_TUPLES_TO_STRING_ARRAY_ARRAY)
                .setName(STAGE_NAME_OBJECT_ARRAY_ARRAY);
    }


    /**
     * <p>Turn the 2-dimensional array of counterparty exposures into an Excel spreadsheet,
     * save in {@link com.hazelcasrt.map.IMap} for later download.
     * </p>
     *
     * @param excelDataContent A 2-dimensional array
     * @param jobName For the spreadsheet name
     * @param timestamp For the spreadsheet name
     * @param timestampStr For the map key
     * @param calcDate For the map key
     */
    private static void saveAsExcelForLaterDownload(BatchStage<Object[][]> excelDataContent, String jobName,
            long timestamp, String timestampStr, LocalDate calcDate) {
        excelDataContent
        .map(objectArrayArray -> Tuple3.tuple3(jobName, timestamp, objectArrayArray))
        .map(XlstFileAsByteArray.CONVERT_TUPLE3_TO_BYTE_ARRAY)
        .map(bytes -> new SimpleImmutableEntry<String, byte[]>(calcDate + "@" + timestampStr, bytes))
        .writeTo(Sinks.map(MyConstants.IMAP_NAME_CVA_XLSX))
        ;
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
    public static void addDebugSaveStages(LocalDate calcDate, String timestampStr,
            BatchStage<Tuple3<String, String, String>> mtm,
            BatchStage<Tuple3<String, String, String>> exposure,
            BatchStage<Tuple3<String, String, String>> cvaExposure,
            BatchStage<Entry<String, Tuple2<String, String>>> cvaExposureByTrade,
            BatchStage<Entry<String, Double>> cvaExposureByCounterparty) {

        String prefix = "debug_";
        String suffix = "_" + calcDate + "@" +  timestampStr;

        /* (1) Save MTMs. Watch out there could be billions
         */
        mtm
        .map(tuple3 -> new SimpleImmutableEntry<String, String>(tuple3.f0() + "," + tuple3.f1(), tuple3.f2())).setName("reformat")
        .writeTo(Sinks.map(prefix + STAGE_NAME_MTM + suffix));

        /* (2) Save Exposures. Same count as MTMs.
         */
        exposure
        .map(tuple3 -> new SimpleImmutableEntry<String, String>(tuple3.f0() + "," + tuple3.f1(), tuple3.f2())).setName("reformat")
        .writeTo(Sinks.map(prefix + STAGE_NAME_EXPOSURE + suffix));

        /* (3) Save CVA Exposures. Same count as MTMs.
         */
        cvaExposure
        .map(tuple3 -> new SimpleImmutableEntry<String, String>(tuple3.f0() + "," + tuple3.f1(), tuple3.f2())).setName("reformat")
        .writeTo(Sinks.map(prefix + STAGE_NAME_CVA_EXPOSURE + suffix));

        /* (4) Save CVA Exposures by Trade. Same count as trades.
         */
        cvaExposureByTrade
        .map(entry -> new SimpleImmutableEntry<String, String>(entry.getKey(), entry.getValue().f1())).setName("reformat")
        .writeTo(Sinks.map(prefix + STAGE_NAME_CVA_EXPOSURE_BY_TRADE + suffix));

        /* (5) Save CVA Exposures by Counterparty. Same count as counterparties.
         */
        cvaExposureByCounterparty
        .writeTo(Sinks.map(prefix + STAGE_NAME_CVA_EXPOSURE_BY_COUNTERPARTY + suffix));
    }


    /**
     * <p>Optional debugging steps, logging to the console.
     * </p>
     */
    public static void addDebugLogStages(String jobName,
            BatchStage<Tuple3<String, Long, List<Entry<String, Double>>>> sortedCvaExposureByCounterparty) {

        /* (1) A single sorted list of CVA Exposures by Counterparty. Log to console
         * as it is saved to a map already.
         */
        String delimiter =  MyConstants.BANNER + " " + jobName + " " + MyConstants.BANNER;
        sortedCvaExposureByCounterparty
        .map(Tuple3::f2)
        .writeTo(Sinks.logger(list -> String.format("%n%s%n%s%n%s%n", delimiter, list, delimiter)));
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
