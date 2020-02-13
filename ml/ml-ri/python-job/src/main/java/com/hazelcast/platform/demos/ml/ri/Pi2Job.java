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

import com.hazelcast.jet.accumulator.MutableReference;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.pipeline.JournalInitialPosition;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.Sources;
import com.hazelcast.jet.python.PythonTransforms;

/**
 * <p>Version 2 of the Python job to calculate Pi.
 * </p>
 * <p>For each input point, the Python routine determines if it is
 * inside ("{@code true}") or outside ("{@code false}", unsurprisingly)
 * the circle. The Jet job then calculates Pi from this ratio.
 * </p>
 * TODO Python coding.
 */
public class Pi2Job {

    // "Pi2Job" submits "pi2", for Python module "pi2.py".
    private static final String NAME = Pi2Job.class.getSimpleName().substring(0, 3).toLowerCase();
    private static final int QUARTER_CIRCLE = 4;

    /**
     * <p>A Jet pipeline to calculate Pi. The essence of this version of the processing is that
     * multiple Python processes independently calculate their approximation to Pi, and Jet
     * averages these to give a global approximation.
     * </p>
     * <ul>
     * <li><p>"{@code readFrom()}"</p>
     * <p>Read in parallel across all nodes an input stream of X &amp; Y co-ordinates stored
     * in the {@link com.hazelcast.map.IMap} named "{@code points}".</p>
     * </li>
     * <li><p>"{@code map()}"</p>
     * <p>The X &amp; Y co-ordinates from the previous stage are held as a {@link java.util.Map.Entry}.
     * Turn these into CSV format.</p>
     * </li>
     * <li><p>"{@code apply()}"</p>
     * <p>Pass the points from the previous stage on this node to one or more Python workers.
     * Jet will start one for each CPU by default, so using the full capacity of this node's CPUs.</p>
     * </li>
     *XXX Document later stages of job
     * </ul>
     *
     * @return A Jet pipeline to calculate PI
     */
    public static Pipeline buildPipeline() throws Exception {

        Pipeline pipeline = Pipeline.create();

        pipeline
        .readFrom(Sources.mapJournal("points", JournalInitialPosition.START_FROM_CURRENT)).withIngestionTimestamps()
        .map(entry -> entry.getKey() + "," + entry.getValue())
        .apply(PythonTransforms.mapUsingPython(MyUtils.getPythonServiceConfig(NAME)))
        .mapStateful(MutableReference::new,
                (MutableReference<Tuple2<Long, Long>> reference, String string) -> {
                    long trueCount = 0;
                    long falseCount = 0;

                    Tuple2<Long, Long> previous = reference.get();
                    if (previous != null) {
                        trueCount = previous.f0();
                        falseCount = previous.f1();
                    }

                    if (string.toLowerCase().equals("true")) {
                        trueCount++;
                    } else {
                        falseCount++;
                    }

                    Tuple2<Long, Long> next = Tuple2.tuple2(trueCount, falseCount);
                    reference.set(next);

                    double pi = QUARTER_CIRCLE * trueCount / (trueCount + falseCount);
                    return Double.toString(pi);
                })
        .writeTo(Sinks.logger());

        return pipeline;
    }

}
