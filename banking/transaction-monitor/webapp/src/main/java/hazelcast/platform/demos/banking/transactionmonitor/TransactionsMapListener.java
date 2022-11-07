/*
 * Copyright (c) 2008-2022, Hazelcast, Inc. All Rights Reserved.
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

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.map.listener.EntryAddedListener;

import io.javalin.websocket.WsContext;

/**
 * <p>A listener on the "{@code transactions}" map for creation events, new transactions
 * to show on the web panel.
 * </p>
 */
public class TransactionsMapListener implements EntryAddedListener<String, HazelcastJsonValue> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionsMapListener.class);

    private static final long LOG_THRESHOLD = 100_000L;

    private static AtomicInteger count = new AtomicInteger(0);

    /**
     * <p>For any transaction created, broadcast to all listening web socket
     * contexts interested in the transaction's symbol. As the key is the
     * transaction Id which is unique, each transaction incoming will trigger this
     * listener.
     * </p>
     *
     * @param event Key is transaction Id, Value is full transaction incl. Id
     */
    @Override
    public void entryAdded(EntryEvent<String, HazelcastJsonValue> event) {

        HazelcastJsonValue transaction = event.getValue();

        JSONObject jsonObject = new JSONObject(transaction.toString());

        String symbol = jsonObject.getString("symbol");

        if (count.getAndIncrement() % LOG_THRESHOLD == 0) {
            LOGGER.info("Received {} => \"{}\"", count.get() - 1, transaction);
        }

        /* Contexts that have the drill-down view open need this updated
         * if there is a new transaction for the relevant symbol.
         */
        List<WsContext> contexts = ApplicationRunner.getContexts(symbol);
        if (contexts != null && !contexts.isEmpty()) {
            LOGGER.trace("Broadcasting update on '{}' to {} context{}", symbol,
                    contexts.size(), (contexts.size() == 1 ? "" : "s"));

            String message = String.format("{"
                    + "\"symbol\": \"%s\","
                    + "\"data\": \"%s\""
                    + "}",
                    symbol,
                    transaction
            );

            for (WsContext context : contexts) {
                context.send(message);
            }
        }

    }

}
