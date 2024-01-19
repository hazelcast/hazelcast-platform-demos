/*
 * Copyright (c) 2008-2024, Hazelcast, Inc. All Rights Reserved.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.crdt.pncounter.PNCounter;
import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.map.IMap;
import com.hazelcast.platform.demos.utils.UtilsProperties;
import com.hazelcast.platform.demos.utils.UtilsSlack;
import com.hazelcast.query.impl.predicates.EqualPredicate;
import com.hazelcast.sql.SqlResult;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hazelcast.platform.demos.utils.CheckConnectIdempotentCallable;
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

    private static Map<String, List<WsContext>> aggregationsToBeUpdated = new ConcurrentHashMap<>();
    private static Map<String, WsContext> sessions = new ConcurrentHashMap<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationRunner.class);
    private static final Logger LOGGER_TO_IMAP = IMapLoggerFactory.getLogger(TransactionMonitorIdempotentInitialization.class);

    private static final String DRILL_ITEM = "DRILL_ITEM";
    private static final String LOAD_ITEMS = "LOAD_ITEMS";

    private final HazelcastInstance  hazelcastInstance;
    private final TransactionMonitorFlavor transactionMonitorFlavor;
    private final String moduleName;
    private final boolean localhost;
    private final boolean kubernetes;
    private final boolean useViridian;
    private IMap<String, Tuple3<Long, Long, Integer>> aggregateQueryResultsMap;
    private IMap<String, BicInfo> bicsMap;
    private IMap<String, ProductInfo> productsMap;
    private IMap<String, SymbolInfo> symbolsMap;
    private IMap<String, HazelcastJsonValue> transactionsMap;


    /**
     * <p>Stash the Hazelcast instance reference.
     * </p>
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Hazelcast instance must be shared, not cloned")
    public ApplicationRunner(HazelcastInstance arg0, TransactionMonitorFlavor arg1, String arg2,
            boolean arg3) throws Exception {
        this.hazelcastInstance = arg0;
        this.transactionMonitorFlavor = arg1;
        this.moduleName = arg2;
        this.localhost =
           System.getProperty("my.docker.enabled", "").equalsIgnoreCase("false");
        this.kubernetes =
           System.getProperty("my.kubernetes.enabled", "").equalsIgnoreCase("true");
        this.useViridian = arg3;
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
        boolean ok = initialize(ApplicationConfig.getClusterName());
        LOGGER_TO_IMAP.info(String.format("'%s' run() START", this.moduleName));

        this.aggregateQueryResultsMap =
                this.hazelcastInstance.getMap(MyConstants.IMAP_NAME_AGGREGATE_QUERY_RESULTS);
        switch (this.transactionMonitorFlavor) {
        case ECOMMERCE:
            this.productsMap = this.hazelcastInstance.getMap(MyConstants.IMAP_NAME_PRODUCTS);
            break;
        case PAYMENTS:
            this.bicsMap = this.hazelcastInstance.getMap(MyConstants.IMAP_NAME_BICS);
            break;
        case TRADE:
        default:
            this.symbolsMap = this.hazelcastInstance.getMap(MyConstants.IMAP_NAME_SYMBOLS);
            break;
        }

        this.transactionsMap =
                this.hazelcastInstance.getMap(MyConstants.IMAP_NAME_TRANSACTIONS);

        // Be aware of new transactions
        this.transactionsMap.addEntryListener(new TransactionsMapListener(transactionMonitorFlavor), true);

        System.out.println("");
        System.out.println("");
        if (ok) {
            boolean isEnterprise = this.checkEnterprise();
            //XXX UCD - runnables in namespace, and do something with a queue, add MAP LISTENER for HEAP MAP ?
            //XXX UCD - NS1 - Executor : NS2 - Map Listener : NS3 - Queue Listener
            LOGGER.error("isEnterprise=={}", isEnterprise);
            PNCounter updateCounter = this.hazelcastInstance.getPNCounter(MyConstants.PN_UPDATER);
            if (updateCounter.get() == 0L) {
                LOGGER.info("Launching admin runnables");
                TransactionMonitorIdempotentInitializationAdmin.launchAdminRunners(hazelcastInstance,
                        transactionMonitorFlavor, useViridian);
                updateCounter.incrementAndGet();
            } else {
                LOGGER.info("Skip launch admin runnables, PNCounter '{}'=={}",
                        updateCounter.getName(), updateCounter.get());
            }

            ok = demoSql();
        }

        System.out.println("");
        System.out.println("");

        // If SQL is broken, abort
        if (ok) {
            MyUtils.logStuff(this.hazelcastInstance);
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
            javalin.ws(MyConstants.WEBSOCKET_PATH_TRANSACTIONS, wsHandler -> {
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
     * <p>Probe a server to see if it is Enterprise or Open Source.
     * </p>
     *
     * @return
     */
    private boolean checkEnterprise() {
        IExecutorService iExecutorService = hazelcastInstance.getExecutorService("default");

        boolean isEnterprise = false;
        EnterpriseChecker enterpriseChecker = new EnterpriseChecker(useViridian);
        try {
            Future<Boolean> future = iExecutorService.submit(enterpriseChecker);
            Boolean b = future.get();
            if (b == null) {
                LOGGER.error("launchAdminRunners(), future.get() null");
            } else {
                isEnterprise = b;
            }
        } catch (Exception e) {
            LOGGER.error("launchAdminRunners(), future.get()", e);
        }

        return isEnterprise;
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
            LOGGER.trace("Session -> '{}', connect", sessionId);
            sessions.put(sessionId, wsConnectContext);
        };
    }

    /**
     * <p>Handle the end of a browser session, removing it
     * from the stored sessions map and removing it from
     * the places to refresh when specific transactions
     * update.
     * </p>
     *
     * @return Callback handler
     */
    private WsCloseHandler onClose() {
        return wsCloseContext -> {
            String sessionId = wsCloseContext.getSessionId();
            LOGGER.trace("Session -> '{}', close", sessionId);
            sessions.remove(sessionId, wsCloseContext);

            for (Entry<String, List<WsContext>> entry : aggregationsToBeUpdated.entrySet()) {
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
     * <li><p>"<i>LOAD_ITEMS</i>"</p>
     * <p>This is for the aggregated view produced by {@link AggregateQuery}.</p>
     * <p>A JSON object is creating holding the current results of the aggregation,
     * with one element for each transaction key.
     * </p>
     * </li>
     * <li><p>"<i>DRILL_ITEM</i>"</p>
     * <p>This is for the detail view on any item. If the browser user
     * clicks to expand the aggregation for a particular item, this creates
     * a query to the "{@code transactions}" map for all transactions for that item's
     * routing key (eg. stock code for trades).</p>
     * <p>The transactions map is indexed on the column matching the routing key.</p>
     * </li>
     * </ul>
     *
     * @return Callback handler
     */
    private WsMessageHandler onMessage() {
        return wsMessageContext -> {
            String sessionId = wsMessageContext.getSessionId();
            String message = wsMessageContext.message();
            WsContext session = sessions.get(sessionId);

            // Caller wishes an update on the AggregateQuery
            if (LOAD_ITEMS.equals(message)) {
                JSONObject jsonObject = new JSONObject();
                LOGGER.trace("Session -> '{}', load", sessionId);

                switch (this.transactionMonitorFlavor) {
                case ECOMMERCE:
                    loadItemsEcommerce(jsonObject);
                    break;
                case PAYMENTS:
                    loadItemsPayments(jsonObject);
                    break;
                case TRADE:
                default:
                    loadItemsTrade(jsonObject);
                    break;
                }

                session.send(jsonObject.toString());
            }

            // Caller wishes the list of transactions for a particular symbol, eg. "DRILL_SYMBOL AAPL" for Apple
            if (message.startsWith(DRILL_ITEM)) {
                JSONObject jsonObject = new JSONObject();

                String code = message.split(" ")[1];
                LOGGER.trace("Session -> '{}', requested '{}'", sessionId, code);

                aggregationsToBeUpdated.compute(code, (k, v) -> {
                    if (v == null) {
                        v = new ArrayList<>();
                    }
                    v.add(session);
                    return v;
                });

                switch (this.transactionMonitorFlavor) {
                case ECOMMERCE:
                    drillItemsEcommerce(jsonObject, code);
                    break;
                case PAYMENTS:
                    drillItemsPayments(jsonObject, code);
                    break;
                case TRADE:
                default:
                    drillItemsTrade(jsonObject, code);
                    break;
                }

                session.send(jsonObject.toString());
            }
        };
    }

    /**
     * <p>Load all eCommerce items.
     * </p>
     *
     * @param jsonObject
     */
    private void loadItemsEcommerce(JSONObject jsonObject) {
        Map<String, String> allProducts =
                this.productsMap.entrySet().stream().collect(
                        Collectors.toMap(Entry::getKey, entry -> entry.getValue().getItemName()));

        // The screen turns cents to dollars for price but not for volume
        aggregateQueryResultsMap.forEach((key, value) -> {
            jsonObject.append("items", new JSONObject()
                    .put("name", allProducts.get(key))
                    // Item Code
                    .put("key", key)
                    .put("f0", value.f0())
                    .put("f1", String.format("%.2f", value.f1()))
                    .put("f2", String.format("%.2f", value.f2()))
                    // Screen flashes the row if average goes up or down
                    .put("upDownField", String.format("%.2f", value.f2()))
            );
        });
    }

    /**
     * <p>Load all Payment items.
     * </p>
     *
     * @param jsonObject
     */
    private void loadItemsPayments(JSONObject jsonObject) {
        Map<String, String> allBics =
                this.bicsMap.entrySet().stream().collect(
                        Collectors.toMap(Entry::getKey, entry -> entry.getValue().getName()));

        // The screen turns cents to dollars for price but not for volume
        aggregateQueryResultsMap.forEach((key, value) -> {
            jsonObject.append("items", new JSONObject()
                    .put("name", allBics.get(key))
                    // BIC Code
                    .put("key", key)
                    .put("f0", value.f0())
                    .put("f1", String.format("%.2f", value.f1()))
                    .put("f2", String.format("%.2f", value.f2()))
                    // Screen flashes the row if average goes up or down
                    .put("upDownField", String.format("%.2f", value.f2()))
            );
        });
    }

    /**
     * <p>Load all trade items.
     * </p>
     *
     * @param jsonObject
     */
    private void loadItemsTrade(JSONObject jsonObject) {
        Map<String, String> allSymbols =
                this.symbolsMap.entrySet().stream().collect(
                        Collectors.toMap(Entry::getKey, entry -> entry.getValue().getSecurityName()));

        aggregateQueryResultsMap.forEach((key, value) -> {
            jsonObject.append("items", new JSONObject()
                    .put("name", allSymbols.get(key))
                    // Symbol
                    .put("key", key)
                    .put("f0", value.f0())
                    .put("f1", String.format("%.2f", value.f1()))
                    .put("f2", String.format("%.2f", value.f2()))
                    // Screen flashes the row if price goes up or down
                    .put("upDownField", String.format("%.2f", value.f2()))
            );
        });
    }

    /**
     * <p>Drilldown into all E-commerce objects
     * </p>
     *
     * @param jsonObject
     */
    @SuppressWarnings("unchecked")
    private void drillItemsEcommerce(JSONObject jsonObject, String itemCode) {
        jsonObject.put("itemCode", itemCode);

        Collection<HazelcastJsonValue> records = this.transactionsMap.values(new EqualPredicate("itemCode", itemCode));

        records.forEach(transaction -> {
            String transactionJson = transaction.toString();
            jsonObject.append("data", new JSONObject(transactionJson));
        });
    }

    /**
     * <p>Drilldown into all Payments objects
     * </p>
     *
     * @param jsonObject
     */
    @SuppressWarnings("unchecked")
    private void drillItemsPayments(JSONObject jsonObject, String bicCreditor) {
        jsonObject.put("bicCreditor", bicCreditor);

        Collection<HazelcastJsonValue> records = this.transactionsMap.values(new EqualPredicate("bicCreditor", bicCreditor));

        records.forEach(transaction -> {
            String transactionJson = transaction.toString();
            jsonObject.append("data", new JSONObject(transactionJson));
        });
    }

    /**
     * <p>Drilldown into all Trade objects
     * </p>
     *
     * @param jsonObject
     */
    @SuppressWarnings("unchecked")
    private void drillItemsTrade(JSONObject jsonObject, String symbol) {
        jsonObject.put("symbol", symbol);

        Collection<HazelcastJsonValue> records = this.transactionsMap.values(new EqualPredicate("symbol", symbol));

        records.forEach(transaction -> {
            String transactionJson = transaction.toString();
            jsonObject.append("data", new JSONObject(transactionJson));
        });
    }

    /**
     * <p>Find which sessions have an item drilldown open.
     * </p>
     *
     * @param item An e-commerce item code or for trading the stock symbol, "{@code AAPL}" for Apple, etc.
     * @return A list, possibly empty, of sessions
     */
    public static List<WsContext> getContexts(String code) {
        return aggregationsToBeUpdated.get(code);
    }

    /**
     * <p>Test SQL here, as a demo, so as to provide some examples
     * to cut &amp; paste into the web client.
     * </p>
     *
     * @return {@code true} if all worked.
     */
    private boolean demoSql() {
        boolean noFail = true;
        String[][] queries = getQueries();

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
                noFail = false;
                String message = String.format("SQL '%s'", Arrays.asList(query));
                LOGGER.error(message + ": " + e.getMessage());
            }
        }

        LOGGER.info("demoSql() -> {}", noFail);
        return noFail;
    }

    /**
     * <p>Queries to test SQL
     * </p>
     *
     * @return
     */
    private String[][] getQueries() {
        String[][] queries = new String[][] {
            /* Turn some off if you wish Javalin available sooner, and so Kubernetes readiness probe is happy.
             */
            //{ "System",  "SELECT * FROM information_schema.mappings" },
            //{ "System",  "SELECT table_name AS name FROM information_schema.mappings" },
            //{ "IMap",    "SELECT * FROM " + MyConstants.IMAP_NAME_AGGREGATE_QUERY_RESULTS + " LIMIT 5" },
            //{ "IMap",    "SELECT * FROM " + MyConstants.IMAP_NAME_TRANSACTIONS + " LIMIT 5"},
            //{ "IMap",    "SELECT stock FROM " + MyConstants.IMAP_NAME_PORTFOLIOS + " ORDER BY 1 DESC LIMIT 3"},
            { "IMap",    "SHOW DATA CONNECTIONS" },
            { "IMap",    "SHOW JOBS" },
            { "IMap",    "SHOW MAPPINGS" },
            { "IMap",    "SHOW VIEWS" },
        };
        int originalLen = queries.length;
        String[][] additionalQueries;

        switch (this.transactionMonitorFlavor) {
        case ECOMMERCE:
            additionalQueries = new String[][] {
                { "IMap",    "SELECT * FROM " + MyConstants.IMAP_NAME_PRODUCTS + " LIMIT 3" },
                /*{ "IMap",    "SELECT id, itemcode, price FROM " + MyConstants.IMAP_NAME_TRANSACTIONS
                    + " WHERE itemcode LIKE 'H%' LIMIT 3" },
                 */
            };
            break;
        case PAYMENTS:
            additionalQueries = new String[][] {
                { "IMap",    "SELECT * FROM " + MyConstants.IMAP_NAME_BICS + " LIMIT 3" },
            };
            break;
        case TRADE:
        default:
            additionalQueries = new String[][] {
                { "IMap",    "SELECT * FROM " + MyConstants.IMAP_NAME_SYMBOLS + " LIMIT 5" },
                /*{ "IMap",    "SELECT id, symbol, price FROM " + MyConstants.IMAP_NAME_TRANSACTIONS
                    + " WHERE symbol LIKE 'AA%' AND price > 2510 LIMIT 5" },
                 */
                /* Streaming query, if not enough data to exceed LIMIT it waits, forcing pod timeout
                { "Kafka",   "SELECT * FROM " + MyConstants.KAFKA_TOPIC_MAPPING_PREFIX + MyConstants.KAFKA_TOPIC_NAME_TRANSACTIONS
                   + " LIMIT 5"},
                // The next 2 have the same execution plan but are declared differently
                { "Join",    "SELECT * FROM (SELECT id, symbol, \"timestamp\" FROM "
                    + MyConstants.KAFKA_TOPIC_MAPPING_PREFIX + MyConstants.KAFKA_TOPIC_NAME_TRANSACTIONS + ") AS k"
                    + " LEFT JOIN symbols AS s ON k.symbol = s.__key LIMIT 5" },
                { "Join",    "SELECT k.id, k.symbol, k.\"timestamp\", s.* FROM "
                    + MyConstants.KAFKA_TOPIC_MAPPING_PREFIX + MyConstants.KAFKA_TOPIC_NAME_TRANSACTIONS + " AS k"
                    + " LEFT JOIN symbols AS s ON k.symbol = s.__key LIMIT 5" },
                { "Join",    "SELECT * FROM (SELECT id, symbol, \"timestamp\" FROM "
                    + MyConstants.KAFKA_TOPIC_MAPPING_PREFIX + MyConstants.KAFKA_TOPIC_NAME_TRANSACTIONS + ") AS k"
                    + " LEFT JOIN (SELECT * FROM " + MyConstants.IMAP_NAME_SYMBOLS + ") AS s ON k.symbol = s.__key LIMIT 5" },
                 */
            };
            break;
        }

        queries = Arrays.copyOf(queries, originalLen + additionalQueries.length);
        for (int i = 0; i < additionalQueries.length; i++) {
            queries[originalLen + i] = additionalQueries[i];
        }

        return queries;
    }

    /**
     * <p>Ensure serverside set up. Idempotent as triggered by client but client
     * may be restarted several times.
     * </p>
     * @return
     */
    private boolean initialize(String clusterName) throws Exception {
        LOGGER.info("initialize(): -=-=-=-=- START -=-=-=-=-=-");

        String bootstrapServers = System.getProperty(MyConstants.BOOTSTRAP_SERVERS_CONFIG_KEY, "");
        String pulsarAddress = System.getProperty(MyConstants.PULSAR_CONFIG_KEY, "");
        String postgresAddress = System.getProperty(MyConstants.POSTGRES_CONFIG_KEY, "");
        for (String propertyName : List.of(MyConstants.BOOTSTRAP_SERVERS_CONFIG_KEY, MyConstants.CASSANDRA_CONFIG_KEY,
                MyConstants.MARIA_CONFIG_KEY, MyConstants.MONGO_CONFIG_KEY, MyConstants.MYSQL_CONFIG_KEY,
                MyConstants.POSTGRES_CONFIG_KEY, MyConstants.PULSAR_CONFIG_KEY)) {
            String propertyValue = System.getProperty(propertyName, "");
            if (propertyValue.isEmpty()) {
                LOGGER.error("No value for '{}' ", propertyName);
                return false;
            } else {
                LOGGER.debug("Using '{}'=='{}'", propertyName, propertyValue);
            }
        }

        CheckConnectIdempotentCallable.silentCheckCustomClasses(this.hazelcastInstance);
        boolean ok = true;
        if (ok) {
            Properties properties;
            try {
                properties = UtilsProperties.loadClasspathProperties(MyConstants.APPLICATION_PROPERTIES_FILE);
                properties.putAll(UtilsSlack.loadSlackAccessProperties());
            } catch (Exception e) {
                LOGGER.error("No properties:", e);
                properties = new Properties();
            }

            String pulsarOrKafka = properties.getProperty(MyConstants.PULSAR_OR_KAFKA_KEY);
            boolean usePulsar = MyUtils.usePulsar(pulsarOrKafka);
            LOGGER.debug("usePulsar='{}'", usePulsar);
            String kubernetesOrViridian = properties.getProperty(MyConstants.USE_VIRIDIAN);
            boolean useViridian = MyUtils.useViridian(kubernetesOrViridian);
            LOGGER.debug("useViridian='{}'", useViridian);
            TransactionMonitorFlavor transactionMonitorFlavor = MyUtils.getTransactionMonitorFlavor(properties);
            LOGGER.info("TransactionMonitorFlavor=='{}'", transactionMonitorFlavor);

            // Address from environment/command line, others from application.properties file.
            properties.put(MyConstants.POSTGRES_ADDRESS, postgresAddress);
            String ourProjectProvenance = properties.getProperty(MyConstants.PROJECT_PROVENANCE);

            ok &= TransactionMonitorIdempotentInitialization.createNeededObjects(hazelcastInstance,
                    properties, ourProjectProvenance, transactionMonitorFlavor, this.localhost, useViridian);
            ok &= TransactionMonitorIdempotentInitialization.loadNeededData(hazelcastInstance, bootstrapServers,
                    pulsarAddress, usePulsar, useViridian, transactionMonitorFlavor);
            ok &= TransactionMonitorIdempotentInitialization.defineQueryableObjects(hazelcastInstance, bootstrapServers,
                    properties, transactionMonitorFlavor, this.localhost, this.kubernetes, this.useViridian);
            if (ok && !this.localhost) {
                // Don't even try if broken by this point
                ok = TransactionMonitorIdempotentInitialization.launchNeededJobs(hazelcastInstance, bootstrapServers,
                        pulsarAddress, properties, clusterName, transactionMonitorFlavor, this.kubernetes);
            } else {
                LOGGER.info("ok=={}, localhost=={} - no job submission", ok, this.localhost);
            }
        }

        LOGGER.info("initialize(): -=-=-=-=- END, success=={} -=-=-=-=-=-", ok);
        return ok;
    }
}
