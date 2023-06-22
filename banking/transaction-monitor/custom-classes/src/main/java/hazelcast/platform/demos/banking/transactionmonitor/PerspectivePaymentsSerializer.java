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

package hazelcast.platform.demos.banking.transactionmonitor;

import com.hazelcast.nio.serialization.compact.CompactReader;
import com.hazelcast.nio.serialization.compact.CompactSerializer;
import com.hazelcast.nio.serialization.compact.CompactWriter;

/**
 * <p>A class for serializing/deserializing a {@link PerspectivePayments}.
 * </p>
 */
public class PerspectivePaymentsSerializer implements CompactSerializer<PerspectivePayments> {

    @Override
    public PerspectivePayments read(CompactReader in) {
        PerspectivePayments perspectivePayments = new PerspectivePayments();
        perspectivePayments.setBic(in.readString(MyConstants.PERSPECTIVE_FIELD_BIC));
        perspectivePayments.setCount(in.readInt64(MyConstants.PERSPECTIVE_FIELD_COUNT));
        perspectivePayments.setSum(in.readFloat64(MyConstants.PERSPECTIVE_FIELD_SUM));
        perspectivePayments.setAverage(in.readFloat64(MyConstants.PERSPECTIVE_FIELD_AVERAGE));
        perspectivePayments.setSeconds(in.readInt32(MyConstants.PERSPECTIVE_FIELD_SECONDS));
        perspectivePayments.setRandom(in.readInt32(MyConstants.PERSPECTIVE_FIELD_RANDOM));
        return perspectivePayments;
    }

    @Override
    public void write(CompactWriter out, PerspectivePayments perspectivePayments) {
        out.writeString(MyConstants.PERSPECTIVE_FIELD_BIC, perspectivePayments.getBic());
        out.writeInt64(MyConstants.PERSPECTIVE_FIELD_COUNT, perspectivePayments.getCount());
        out.writeFloat64(MyConstants.PERSPECTIVE_FIELD_SUM, perspectivePayments.getSum());
        out.writeFloat64(MyConstants.PERSPECTIVE_FIELD_AVERAGE, perspectivePayments.getAverage());
        out.writeInt32(MyConstants.PERSPECTIVE_FIELD_SECONDS, perspectivePayments.getSeconds());
        out.writeInt32(MyConstants.PERSPECTIVE_FIELD_RANDOM, perspectivePayments.getRandom());
    }

    @Override
    public Class<PerspectivePayments> getCompactClass() {
        return PerspectivePayments.class;
    }

    @Override
    public String getTypeName() {
        return PerspectivePayments.class.getName();
    }

}
