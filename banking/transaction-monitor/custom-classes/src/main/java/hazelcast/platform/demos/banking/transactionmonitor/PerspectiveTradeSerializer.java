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

import com.hazelcast.nio.serialization.compact.CompactReader;
import com.hazelcast.nio.serialization.compact.CompactSerializer;
import com.hazelcast.nio.serialization.compact.CompactWriter;

/**
 * <p>A class for serializing/deserializing a {@link PerspectiveTrade}.
 * </p>
 */
public class PerspectiveTradeSerializer implements CompactSerializer<PerspectiveTrade> {

    @Override
    public PerspectiveTrade read(CompactReader in) {
        PerspectiveTrade perspectiveTrade = new PerspectiveTrade();
        perspectiveTrade.setSymbol(in.readString(MyConstants.PERSPECTIVE_FIELD_SYMBOL));
        perspectiveTrade.setCount(in.readInt64(MyConstants.PERSPECTIVE_FIELD_COUNT));
        perspectiveTrade.setSum(in.readFloat64(MyConstants.PERSPECTIVE_FIELD_SUM));
        perspectiveTrade.setLatest(in.readFloat64(MyConstants.PERSPECTIVE_FIELD_LATEST));
        perspectiveTrade.setSeconds(in.readInt32(MyConstants.PERSPECTIVE_FIELD_SECONDS));
        perspectiveTrade.setRandom(in.readInt32(MyConstants.PERSPECTIVE_FIELD_RANDOM));
        return perspectiveTrade;
    }

    @Override
    public void write(CompactWriter out, PerspectiveTrade perspectiveTrade) {
        out.writeString(MyConstants.PERSPECTIVE_FIELD_SYMBOL, perspectiveTrade.getSymbol());
        out.writeInt64(MyConstants.PERSPECTIVE_FIELD_COUNT, perspectiveTrade.getCount());
        out.writeFloat64(MyConstants.PERSPECTIVE_FIELD_SUM, perspectiveTrade.getSum());
        out.writeFloat64(MyConstants.PERSPECTIVE_FIELD_LATEST, perspectiveTrade.getLatest());
        out.writeInt32(MyConstants.PERSPECTIVE_FIELD_SECONDS, perspectiveTrade.getSeconds());
        out.writeInt32(MyConstants.PERSPECTIVE_FIELD_RANDOM, perspectiveTrade.getRandom());
    }

    @Override
    public Class<PerspectiveTrade> getCompactClass() {
        return PerspectiveTrade.class;
    }

    @Override
    public String getTypeName() {
        return PerspectiveTrade.class.getName();
    }

}
