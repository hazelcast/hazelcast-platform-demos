/*
 * Copyright (c) 2008-2023, Hazelcast, Inc. All Rights Reserved.
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

import com.hazelcast.nio.serialization.compact.CompactReader;
import com.hazelcast.nio.serialization.compact.CompactSerializer;
import com.hazelcast.nio.serialization.compact.CompactWriter;

/**
 * <p>A class for serializing/deserializing a {@link Portfolio}.
 * </p>
 */
public class PortfolioSerializer implements CompactSerializer<Portfolio> {

    @Override
    public Portfolio read(CompactReader in) {
        Portfolio portfolio = new Portfolio();
        portfolio.setStock(in.readString("stock"));
        portfolio.setSold(in.readInt32("sold"));
        portfolio.setBought(in.readInt32("bought"));
        portfolio.setChange(in.readInt32("change"));
        return portfolio;
    }

    @Override
    public void write(CompactWriter out, Portfolio portfolio) {
        out.writeString("stock", portfolio.getStock());
        out.writeInt32("sold", portfolio.getSold());
        out.writeInt32("bought", portfolio.getBought());
        out.writeInt32("change", portfolio.getChange());
    }

    @Override
    public Class<Portfolio> getCompactClass() {
        return Portfolio.class;
    }

    @Override
    public String getTypeName() {
        return Portfolio.class.getName();
    }

}
