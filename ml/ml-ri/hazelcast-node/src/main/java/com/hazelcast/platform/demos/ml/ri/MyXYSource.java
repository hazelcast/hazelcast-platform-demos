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

package com.hazelcast.platform.demos.ml.ri;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.pipeline.SourceBuilder;

/**
 * <p>TODO Documentation, stripe across nodes
 * </p>
 */
public class MyXYSource {

    private static Random random = new Random();

    /**
     * <p>TODO Documentation
     * </p>
     *
     * @param buffer TODO Documentation
     */
    void fillBufferFn(SourceBuilder.SourceBuffer<Map.Entry<Double, Double>> buffer) {
        //TODO TMP implementation
        Tuple2<Double, Double> tuple2 = Tuple2.tuple2(random.nextDouble(), random.nextDouble());
        buffer.add(tuple2);
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
    }

}
