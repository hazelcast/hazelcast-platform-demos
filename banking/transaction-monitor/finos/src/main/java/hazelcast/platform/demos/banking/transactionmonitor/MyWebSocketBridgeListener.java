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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.map.listener.EntryAddedListener;
import com.hazelcast.map.listener.EntryUpdatedListener;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * <p>Listen for map updates and pass on to websocket.
 * </p>
 */
@SuppressWarnings("rawtypes")
@Component
public class MyWebSocketBridgeListener implements EntryAddedListener, EntryUpdatedListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyWebSocketBridgeListener.class);
    private static final int LOG_THRESHOLD = 10;

    private static final String DESTINATION_DATA =
            "/" + MyConstants.WEBSOCKET_FEED_PREFIX
            + "/" + MyConstants.WEBSOCKET_DATA_SUFFIX;

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;
    private int count;

    public MyWebSocketBridgeListener() {
        LOGGER.debug("Data destination: {}", DESTINATION_DATA);
    }

    @Override
    public void entryAdded(EntryEvent arg0) {
        this.handle(arg0);
    }
    @Override
    public void entryUpdated(EntryEvent arg0) {
        this.handle(arg0);
    }

    /**
     * <p>First insert and later updates all sent the same.
     * </p>
     * @param entryEvent Key is String (ignored). Value is some sort of Perspective sub-type.
     */
    @SuppressFBWarnings(value = {"UPM_UNCALLED_PRIVATE_METHOD", "UUF_UNUSED_FIELD"},
            justification = "This is not unused, suppress warning to avoid SpotBugs bug")
    private void handle(EntryEvent entryEvent) {
        Object value = entryEvent.getValue();

        try {
            JSONObject payload = null;
            if (value instanceof PerspectiveEcommerce) {
                payload = this.fromPerspectiveEcommerce((PerspectiveEcommerce) value);
            }
            if (value instanceof PerspectivePayments) {
                payload = this.fromPerspectivePayments((PerspectivePayments) value);
            }
            if (value instanceof PerspectiveTrade) {
                payload = this.fromPerspectiveTrade((PerspectiveTrade) value);
            }

            if (payload != null) {
                if (this.count < LOG_THRESHOLD) {
                    LOGGER.info("Data {} to websocket '{}'", count, payload);
                    System.out.println(payload);
                } else {
                    if (this.count == LOG_THRESHOLD) {
                        LOGGER.info("Data {} to websocket '{}'.", count, payload);
                        LOGGER.info("Next data logging is at TRACE level");
                    } else {
                        LOGGER.trace("Data {} to websocket '{}'", count, payload);
                    }
                }
                this.count++;
                this.simpMessagingTemplate.convertAndSend(DESTINATION_DATA, payload.toString());
            } else {
                LOGGER.error("Unhandled object class {} for {}", value.getClass().getCanonicalName(), value);
            }

        } catch (Exception e) {
            String message = String.format("handle('%s')", value.toString());
            LOGGER.error(message, e);
        }
    }


    /**
     * <p>Map names specific to Ecommerce to standard names for
     * front-end.
     * </p>
     * @param value
     * @return
     */
    private JSONObject fromPerspectiveEcommerce(PerspectiveEcommerce value) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder
        .append("{ \"").append(MyConstants.PERSPECTIVE_JSON_KEY).append("\": \"").append(value.getCode()).append("\"")
        .append(", \"").append(MyConstants.PERSPECTIVE_FIELD_COUNT).append("\": ").append(value.getCount())
        .append(", \"").append(MyConstants.PERSPECTIVE_FIELD_SUM).append("\": ").append(value.getSum())
        .append(", \"").append(MyConstants.PERSPECTIVE_JSON_DERIVED).append("\": ").append(value.getAverage())
        .append(", \"").append(MyConstants.PERSPECTIVE_FIELD_SECONDS).append("\": ").append(value.getSeconds())
        .append(", \"").append(MyConstants.PERSPECTIVE_FIELD_RANDOM).append("\": ").append(value.getRandom())
        .append("}");

        return new JSONObject(stringBuilder.toString());
    }

    /**
     * <p>Map names specific to Payments to standard names for
     * front-end.
     * </p>
     * @param value
     * @return
     */
    private JSONObject fromPerspectivePayments(PerspectivePayments value) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder
        .append("{ \"").append(MyConstants.PERSPECTIVE_JSON_KEY).append("\": \"").append(value.getBic()).append("\"")
        .append(", \"").append(MyConstants.PERSPECTIVE_FIELD_COUNT).append("\": ").append(value.getCount())
        .append(", \"").append(MyConstants.PERSPECTIVE_FIELD_SUM).append("\": ").append(value.getSum())
        .append(", \"").append(MyConstants.PERSPECTIVE_JSON_DERIVED).append("\": ").append(value.getAverage())
        .append(", \"").append(MyConstants.PERSPECTIVE_FIELD_SECONDS).append("\": ").append(value.getSeconds())
        .append(", \"").append(MyConstants.PERSPECTIVE_FIELD_RANDOM).append("\": ").append(value.getRandom())
        .append("}");

        return new JSONObject(stringBuilder.toString());
    }

    /**
     * <p>Map names specific to Trade to standard names for
     * front-end.
     * </p>
     * @param value
     * @return
     */
    private JSONObject fromPerspectiveTrade(PerspectiveTrade value) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder
        .append("{ \"").append(MyConstants.PERSPECTIVE_JSON_KEY).append("\": \"").append(value.getSymbol()).append("\"")
        .append(", \"").append(MyConstants.PERSPECTIVE_FIELD_COUNT).append("\": ").append(value.getCount())
        .append(", \"").append(MyConstants.PERSPECTIVE_FIELD_SUM).append("\": ").append(value.getSum())
        .append(", \"").append(MyConstants.PERSPECTIVE_JSON_DERIVED).append("\": ").append(value.getLatest())
        .append(", \"").append(MyConstants.PERSPECTIVE_FIELD_SECONDS).append("\": ").append(value.getSeconds())
        .append(", \"").append(MyConstants.PERSPECTIVE_FIELD_RANDOM).append("\": ").append(value.getRandom())
        .append("}");

        return new JSONObject(stringBuilder.toString());
    }

}
