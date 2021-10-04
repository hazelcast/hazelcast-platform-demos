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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.map.IMap;
import com.hazelcast.query.impl.predicates.EqualPredicate;
import com.hazelcast.sql.SqlResult;

import io.javalin.Javalin;
import io.javalin.core.JavalinServer;
import io.javalin.http.HandlerType;
import io.javalin.websocket.WsCloseHandler;
import io.javalin.websocket.WsConnectHandler;
import io.javalin.websocket.WsContext;
import io.javalin.websocket.WsMessageHandler;

/**
 * <p>The main "{@code run()}" method of the application, called
 * once configuration created.
 * </p>
 */
public class ApplicationRunner {

    private static Map<String, List<WsContext>> symbolsToBeUpdated = new ConcurrentHashMap<>();
    private static Map<String, WsContext> sessions = new ConcurrentHashMap<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationRunner.class);

    private static final String DRILL_SYMBOL = "DRILL_SYMBOL";
    private static final String LOAD_SYMBOLS = "LOAD_SYMBOLS";

    private final HazelcastInstance  hazelcastInstance;
    private final IMap<String, Tuple3<Long, Long, Integer>> aggregateQueryResultsMap;
    private final IMap<String, SymbolInfo> symbolsMap;
    private final IMap<String, HazelcastJsonValue> tradesMap;


    /**
     * <p>Obtain references to the maps that are needed.
     * </p>
     */
    public ApplicationRunner(HazelcastInstance arg0) throws Exception {
        this.hazelcastInstance = arg0;
        this.aggregateQueryResultsMap =
            this.hazelcastInstance.getMap(MyConstants.IMAP_NAME_AGGREGATE_QUERY_RESULTS);
        this.symbolsMap =
            this.hazelcastInstance.getMap(MyConstants.IMAP_NAME_SYMBOLS);
        this.tradesMap =
            this.hazelcastInstance.getMap(MyConstants.IMAP_NAME_TRADES);
    }

    /**
     * <p>Launch a <a href="https://javalin.io/">Javalin</a> web server.
     * This will react to page events on a <a href="https://reactjs.org/">ReactJS</a>,
     * querying Hazelcast IMDG for the data to display on the page. This data has
     * been passed into Hazelcast IMDG by Hazelcast Jet.
     * </p>
     *
     * @throws Exception
     */
    public void run() throws Exception {
        // Be aware of new trades
        tradesMap.addEntryListener(new TradesMapListener(), true);

        System.out.println("");
        System.out.println("");

        boolean ok = demoSql();

        System.out.println("");
        System.out.println("");

        // If SQL is broken, abort
        if (!ok) {
            Javalin javalin = Javalin.create();

            // ReactJS, see src/main/app
            javalin.config
            .addStaticFiles("/app")
            .addSinglePageRoot("/", "/app/index.html");

            // REST
            MyRestController myRestController = new MyRestController(this.hazelcastInstance);
            javalin.addHandler(HandlerType.GET, "/rest/", myRestController.handleIndex());
            javalin.addHandler(HandlerType.GET, "/rest/sql", myRestController.handleSql());

            // Event types to handle
            javalin.ws(MyConstants.WEBSOCKET_PATH_TRADES, wsHandler -> {
                wsHandler.onClose(onClose());
                wsHandler.onConnect(onConnect());
                wsHandler.onMessage(onMessage());
            });

            // Start web server on requested port, and wait for termination (if ever)
            javalin.start(Application.getPort());
            JavalinServer javalinServer = javalin.server();
            if (javalinServer != null) {
                javalinServer.server().join();
            }
        }
    }


    /**
     * <p>Handle the start of a new browser session, stashing
     * the session and connection context in a local map.
     * </p>
     *
     * @return Callback handler
     */
    private WsConnectHandler onConnect() {
        return wsConnectContext -> {
            String sessionId = wsConnectContext.getSessionId();
            LOGGER.debug("Session -> '{}', connect", sessionId);
            sessions.put(sessionId, wsConnectContext);
        };
    }

    /**
     * <p>Handle the end of a browser session, removing it
     * from the stored sessions map and removing it from
     * the places to refresh when specific trade symbols
     * update.
     * </p>
     *
     * @return Callback handler
     */
    private WsCloseHandler onClose() {
        return wsCloseContext -> {
            String sessionId = wsCloseContext.getSessionId();
            LOGGER.debug("Session -> '{}', close", sessionId);
            sessions.remove(sessionId, wsCloseContext);

            for (Entry<String, List<WsContext>> entry : symbolsToBeUpdated.entrySet()) {
                List<WsContext> contexts = entry.getValue();
                contexts.removeIf(context -> context.getSessionId().equals(sessionId));
            }
        };
    }

    /**
     * <p>Callback handler to process messages from ReactJS for a
     * browser session. Only two types currently handled:
     * </p>
     * <ul>
     * <li><p>"<i>LOAD_SYMBOLS</i>"</p>
     * <p>This is for the aggregated view produced by {@link AggregateQuery}.</p>
     * <p>A JSON object is creating holding the current results of the aggregation,
     * with one element for each trade symbol.
     * </p>
     * </li>
     * <li><p>"<i>DRILL_SYMBOL</i>"</p>
     * <p>This is for the detail view on any trading symbol. If the browser user
     * clicks to expand the aggregration for a particular symbom, this creates
     * a query to the "{@code trades}" map for all trades for that symbol.</p>
     * <p>The trades map is indexed on the "{@code symbol}" column.</p>
     * </li>
     * </ul>
     *
     * @return Callback handler
     */
    @SuppressWarnings("unchecked")
    private WsMessageHandler onMessage() {
        return wsMessageContext -> {
            String sessionId = wsMessageContext.getSessionId();
            String message = wsMessageContext.message();
            WsContext session = sessions.get(sessionId);

            // Caller wishes an update on the AggregateQuery
            if (LOAD_SYMBOLS.equals(message)) {
                JSONObject jsonObject = new JSONObject();

                Map<String, String> allSymbols =
                        symbolsMap.entrySet().stream().collect(
                                Collectors.toMap(Entry::getKey, entry -> entry.getValue().getSecurityName()));

                // The screen turns cents to dollars for price but not for volume
                aggregateQueryResultsMap.forEach((key, value) -> {
                    jsonObject.append("symbols", new JSONObject()
                            .put("name", allSymbols.get(key))
                            .put("symbol", key)
                            .put("count", value.f0())
                            .put("volume", volumeToString(value.f1()))
                            .put("price", value.f2())
                    );
                });

                session.send(jsonObject.toString());
            }

            // Caller wishes the list of trades for a particular symbol, eg. "DRILL_SYMBOL AAPL" for Apple
            if (message.startsWith(DRILL_SYMBOL)) {
                JSONObject jsonObject = new JSONObject();

                String symbol = message.split(" ")[1];
                LOGGER.debug("Session -> '{}', requested symbol '{}'", sessionId, symbol);

                // Note that this session now wishes updated if the drill-down list changes (by TradeMapListener)
                symbolsToBeUpdated.compute(symbol, (k, v) -> {
                    if (v == null) {
                        v = new ArrayList<>();
                    }
                    v.add(session);
                    return v;
                });

                // Query IMDG for all trades for the current symbol
                Collection<HazelcastJsonValue> records = tradesMap.values(new EqualPredicate("symbol", symbol));
                records.forEach(trade -> {
                    String tradeJson = trade.toString();
                    jsonObject.put("symbol", symbol);
                    jsonObject.append("data", new JSONObject(tradeJson));
                });

                session.send(jsonObject.toString());
            }

        };
    }


    /**
     * <p>Convert trade volume (quantity * price in cents) to dollars.
     * </p>
     *
     * @param Volume from Tuple3 produced by {@link AggregateQuery}
     * @return Input divided by 100 to 2DP.
     */
    private static String volumeToString(long price) {
        final double oneHundred = 100.00d;
        return String.format("$%,.2f", price / oneHundred);
    }


    /**
     * <p>Find which sessions have a stock symbol drilldown open.
     * </p>
     *
     * @param symbol A stock symbol, "{@code AAPL}" for Apple, etc.
     * @return A list, possibly empty, of sessions
     */
    public static List<WsContext> getContexts(String symbol) {
        return symbolsToBeUpdated.get(symbol);
    }


    /**
     * <p>Test SQL here, as a demo, so as to provide some examples
     * to cut &amp; paste into the web client.
     * </p>
     *
     * @return {@code true} if all worked.
     */
    private boolean demoSql() {
        boolean didFail = false;
        String[][] queries = new String[][] {
            /* Turn off for now, so Javalin available sooner
            { "System",  "SELECT * FROM information_schema.mappings" },
            { "System",  "SELECT mapping_name AS name FROM information_schema.mappings" },
            { "IMap",    "SELECT * FROM " + MyConstants.IMAP_NAME_AGGREGATE_QUERY_RESULTS },
            { "IMap",    "SELECT * FROM " + MyConstants.IMAP_NAME_SYMBOLS },
            { "IMap",    "SELECT * FROM " + MyConstants.IMAP_NAME_TRADES },
            { "IMap",    "SELECT id, symbol, price FROM " + MyConstants.IMAP_NAME_TRADES
                    + " WHERE symbol LIKE 'AA%' AND price > 2510" },
            { "Kafka",   "SELECT * FROM " + MyConstants.KAFKA_TOPIC_MAPPING_PREFIX + MyConstants.KAFKA_TOPIC_NAME_TRADES },
            // The next 2 have the same execution plan but are declared differently
            { "Join",    "SELECT * FROM (SELECT id, symbol, \"timestamp\" FROM kf_trades) AS k"
                    + " LEFT JOIN symbols AS s ON k.symbol = s.__key" },
            { "Join",    "SELECT k.id, k.symbol, k.\"timestamp\", s.* FROM kf_trades AS k"
                    + " LEFT JOIN symbols AS s ON k.symbol = s.__key" },
            // Not yet implmented : "Sub-query not supported on the right side of a join"
            //{ "Join",    "SELECT * FROM (SELECT id, symbol, \"timestamp\" FROM kf_trades) AS k"
            //    + " LEFT JOIN (SELECT * FROM symbols) AS s ON k.symbol = s.__key" },
             */
        };

        int count = 0;
        // Don't break loop on failure, try each to find if more than one fails
        for (String[] query : queries) {
            try {
                TimeUnit.SECONDS.sleep(1);
                System.out.println("");
                count++;
                System.out.printf("(%d) : %s%n", count, query[0]);
                System.out.println(query[1]);
                SqlResult sqlResult = this.hazelcastInstance.getSql().execute(query[1]);
                Tuple3<String, String, List<String>> result =
                        MyUtils.prettyPrintSqlResult(sqlResult);
                if (result.f0().length() > 0) {
                    // Error
                    System.out.println(result.f0());
                } else {
                    // Actual data
                    result.f2().stream().forEach(System.out::println);
                    if (result.f1().length() > 0) {
                        // Warning
                        System.out.println(result.f1());
                    }
                }
                System.out.println("");
            } catch (Exception e) {
                didFail = true;
                String message = String.format("SQL '%s'", Arrays.asList(query));
                LOGGER.error(message + ": " + e.getMessage());
            }
        }

        return didFail;
    }
}
