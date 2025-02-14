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
import java.util.Arrays;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * <p>A Java object representing some (in fact, all) of the
 * JSON fields in the Payments ISO20022 style transaction object read for Kafka.
 * </p>
 * <p>Required until <a href="https://github.com/hazelcast/hazelcast/issues/15140">Issue-15150</a>
 * is addressed.
 * </p>
 */
public class TransactionPayments implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private long timestamp;
    private String kind;
    private String bicCreditor;
    private String bicDebitor;
    private String ccy;
    private double amtFloor;
    private String[] xml;

    // Generated getters/setters and toString(), but String[] needs @SuppressFBWarnings(value = "EI_EXPOSE_REP2"...

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
    public String getKind() {
        return kind;
    }
    public void setKind(String kind) {
        this.kind = kind;
    }
    public String getBicCreditor() {
        return bicCreditor;
    }
    public void setBicCreditor(String bicCreditor) {
        this.bicCreditor = bicCreditor;
    }
    public String getBicDebitor() {
        return bicDebitor;
    }
    public void setBicDebitor(String bicDebitor) {
        this.bicDebitor = bicDebitor;
    }
    public String getCcy() {
        return ccy;
    }
    public void setCcy(String ccy) {
        this.ccy = ccy;
    }
    public double getAmtFloor() {
        return amtFloor;
    }
    public void setAmtFloor(double amtFloor) {
        this.amtFloor = amtFloor;
    }
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Safe to share, JSON Array")
    public String[] getXml() {
        return xml;
    }
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Safe to share, JSON Array")
    public void setXml(String[] xml) {
        this.xml = xml;
    }

    @Override
    public String toString() {
        return "TransactionPayments [id=" + id + ", timestamp=" + timestamp + ", kind=" + kind + ", bicCreditor="
                + bicCreditor + ", bicDebitor=" + bicDebitor + ", ccy=" + ccy + ", amtFloor=" + amtFloor + ", xml="
                + Arrays.toString(xml) + "]";
    }

}
