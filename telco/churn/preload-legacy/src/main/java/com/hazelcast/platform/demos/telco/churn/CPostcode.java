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

package com.hazelcast.platform.demos.telco.churn;

import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

//XXX import java.io.Serializable;

/**
 * XXX
datastax-java-driver.basic.load-balancing-policy {
  local-datacenter = datacenter1
}
 */
@Table(value = "cpostcode")
public class CPostcode {

    /**
     * XXX
     */
    //private static final long serialVersionUID = 1L;
    @PrimaryKeyColumn(value = "couter", type = PrimaryKeyType.PARTITIONED)
    private String outer;
    @Column(value = "cinner")
    private String inner;

    /**
     * XXX
     */
    public String getInner() {
        return inner;
    }

    /**
     * XXX
     */
    public void setInner(String inner) {
        this.inner = inner;
    }

    /**
     * XXX
     */
    public String getOuter() {
        return outer;
    }

    /**
     * XXX
     */
    public void setOuter(String outer) {
        this.outer = outer;
    }

    /**
     * XXX
     */
    @Override
    public String toString() {
        return "Postcode [outer=" + outer + ", inner=" + inner + "]";
    }

}