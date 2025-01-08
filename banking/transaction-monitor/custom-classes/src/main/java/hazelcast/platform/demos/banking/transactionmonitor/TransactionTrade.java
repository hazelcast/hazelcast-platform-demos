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

import java.io.Serializable;

/**
 * <p>A Java object representing some (in fact, all) of the
 * JSON fields in the Trade style transaction object read for Kafka.
 * </p>
 * <p>Required until <a href="https://github.com/hazelcast/hazelcast/issues/15140">Issue-15150</a>
 * is addressed.
 * </p>
 */
public class TransactionTrade implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private long timestamp;
    private String symbol;
    private long quantity;
    private double price;

    // Generated getters/setters and toString()

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public long getQuantity() {
        return quantity;
    }

    public void setQuantity(long quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return "TransactionTrade [id=" + id + ", timestamp=" + timestamp + ", symbol=" + symbol + ", quantity=" + quantity
                + ", price=" + price + "]";
    }

}
