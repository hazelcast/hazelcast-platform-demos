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
 * <p>A class for serializing/deserializing a {@link PerspectiveEcommerce}.
 * </p>
 */
public class PerspectiveEcommerceSerializer implements CompactSerializer<PerspectiveEcommerce> {

    @Override
    public PerspectiveEcommerce read(CompactReader in) {
        PerspectiveEcommerce perspectiveEcommerce = new PerspectiveEcommerce();
        perspectiveEcommerce.setCode(in.readString("code"));
        perspectiveEcommerce.setCount(in.readInt64("count"));
        perspectiveEcommerce.setSum(in.readFloat64("sum"));
        perspectiveEcommerce.setAverage(in.readFloat64("average"));
        perspectiveEcommerce.setSeconds(in.readInt32("seconds"));
        perspectiveEcommerce.setRandom(in.readInt32("random"));
        return perspectiveEcommerce;
    }

    @Override
    public void write(CompactWriter out, PerspectiveEcommerce perspectiveEcommerce) {
        out.writeString("code", perspectiveEcommerce.getCode());
        out.writeInt64("count", perspectiveEcommerce.getCount());
        out.writeFloat64("sum", perspectiveEcommerce.getSum());
        out.writeFloat64("average", perspectiveEcommerce.getAverage());
        out.writeInt32("seconds", perspectiveEcommerce.getSeconds());
        out.writeInt32("random", perspectiveEcommerce.getRandom());
    }

    @Override
    public Class<PerspectiveEcommerce> getCompactClass() {
        return PerspectiveEcommerce.class;
    }

    @Override
    public String getTypeName() {
        return PerspectiveEcommerce.class.getName();
    }

}
