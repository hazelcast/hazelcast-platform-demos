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

package com.hazelcast.platform.demos.telco.churn.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.hazelcast.platform.demos.telco.churn.MyConstants;

/**
 * <p>Java representation of a table in MySql,
 * created by the <i>mysql</i> module,
 * from it's "{@code src/main/resources/init.sql}"
 * and populated by the <i>preload-legacy</i> module.
 * </p>
 */
@Table(name = MyConstants.MYSQL_TABLE_NAME)
@Entity
public class Tariff {

    @Id
    @Column
    private String id;
    @Column
    private int year;
    @Column
    private String name;
    @Column
    private boolean international;
    @Column(name = "rate")
    private double ratePerMinute;

    // Generated code below

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public int getYear() {
        return year;
    }
    public void setYear(int year) {
        this.year = year;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public boolean isInternational() {
        return international;
    }
    public void setInternational(boolean international) {
        this.international = international;
    }
    public double getRatePerMinute() {
        return ratePerMinute;
    }
    public void setRatePerMinute(double ratePerMinute) {
        this.ratePerMinute = ratePerMinute;
    }

    @Override
    public String toString() {
        return "Tariff [id=" + id + ", year=" + year + ", name=" + name + ", international=" + international
                + ", ratePerMinute=" + ratePerMinute + "]";
    }

}
