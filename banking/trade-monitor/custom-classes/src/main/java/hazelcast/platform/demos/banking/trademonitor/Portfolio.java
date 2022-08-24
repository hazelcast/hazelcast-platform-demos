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

package hazelcast.platform.demos.banking.trademonitor;

/**
 * <p>A simplistic domain model for a portfolio.
 * Note this is a simple class, no serialization logic defined
 * (see {@link PortfolioSerializer}).
 * </p>
 */
public class Portfolio {

    private String stock;
    private int sold;
    private int bought;
    private int change;

    // Generated getters/setters and toString()

    public String getStock() {
        return stock;
    }
    public void setStock(String stock) {
        this.stock = stock;
    }
    public int getSold() {
        return sold;
    }
    public void setSold(int sold) {
        this.sold = sold;
    }
    public int getBought() {
        return bought;
    }
    public void setBought(int bought) {
        this.bought = bought;
    }
    public int getChange() {
        return change;
    }
    public void setChange(int change) {
        this.change = change;
    }

    @Override
    public String toString() {
        return "Portfolio [stock=" + stock + ", sold=" + sold + ", bought=" + bought + ", change=" + change + "]";
    }

}
