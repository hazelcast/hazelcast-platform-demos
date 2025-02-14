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
 * <p>Describes a NASDAQ stock symbol.
 * </p>
 * <p>{@link java.io.Serializable} is not a good choice for speed or
 * compactness, used here for simplicity.
 * </p>
 */
public class SymbolInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    String securityName;
    NasdaqFinancialStatus financialStatus;
    NasdaqMarketCategory marketCategory;

    public String getSecurityName() {
        return securityName;
    }
    public void setSecurityName(String securityName) {
        this.securityName = securityName;
    }
    public NasdaqFinancialStatus getFinancialStatus() {
        return financialStatus;
    }
    public void setFinancialStatus(NasdaqFinancialStatus financialStatus) {
        this.financialStatus = financialStatus;
    }
    public NasdaqMarketCategory getMarketCategory() {
        return marketCategory;
    }
    public void setMarketCategory(NasdaqMarketCategory marketCategory) {
        this.marketCategory = marketCategory;
    }

    @Override
    public String toString() {
        return "SymbolInfo [securityName=" + securityName + ", financialStatus=" + financialStatus + ", marketCategory="
                + marketCategory + "]";
    }

}
