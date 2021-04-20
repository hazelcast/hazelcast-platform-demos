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

package com.hazelcast.platform.demos.ml.ri;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.pipeline.SourceBuilder;

/**
 * <p>A source of random X &amp; Y co-ordinates which fall between
 * "{@code (0,0)}" inclusive to "{@code (1,1)}" exclusive.
 * </p>
 * <p>As this is invoked with "{@code .distributed(1)" Jet will create
 * one instance of this source per node, so generating random numbers
 * in a scalable way.
 * </p>
 */
public class MyXYSource {

    private static Random random = new Random();

    /**
     * <p>Generate a random X & Y coordinate, and add it to Jet's input
     * buffer to pass to later stream stages. Only generate and add one
     * point in this function, though we could add multiple at once if
     * needed.
     * </p>
     *
     * @param buffer Jet's unbounded input buffer.
     */
    void fillBufferFn(SourceBuilder.SourceBuffer<Map.Entry<Double, Double>> buffer) {
        Tuple2<Double, Double> tuple2 = Tuple2.tuple2(random.nextDouble(), random.nextDouble());
        buffer.add(tuple2);

        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
    }

}
