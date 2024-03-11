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

import java.io.Serializable;

/**
 * <p>Describes a Bank Interchange Code item.
 * </p>
 * <p>{@link java.io.Serializable} is not a good choice for speed or
 * compactness, used here for simplicity.
 * </p>
 */
public class BicInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    String bankCode;
    String country;
    String currency;
    String locationCode;
    String location;
    String name;
    Double exchangeRate;

    // Generated getters, setters and toString()

    public String getBankCode() {
        return bankCode;
    }
    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }
    public String getCountry() {
        return country;
    }
    public void setCountry(String country) {
        this.country = country;
    }
    public String getCurrency() {
        return currency;
    }
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    public String getLocationCode() {
        return locationCode;
    }
    public void setLocationCode(String locationCode) {
        this.locationCode = locationCode;
    }
    public String getLocation() {
        return location;
    }
    public void setLocation(String location) {
        this.location = location;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Double getExchangeRate() {
        return exchangeRate;
    }
    public void setExchangeRate(Double exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    @Override
    public String toString() {
        return "BicInfo [bankCode=" + bankCode + ", country=" + country + ", currency=" + currency + ", locationCode="
                + locationCode + ", location=" + location + ", name=" + name + ", exchangeRate=" + exchangeRate + "]";
    }

}
