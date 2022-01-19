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

package com.hazelcast.platform.demos.ml.ri;

import java.util.Locale;

import com.hazelcast.function.BiFunctionEx;
import com.hazelcast.function.ToDoubleFunctionEx;
import com.hazelcast.jet.accumulator.MutableReference;
import com.hazelcast.jet.aggregate.AggregateOperation;
import com.hazelcast.jet.aggregate.AggregateOperation1;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.pipeline.JournalInitialPosition;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sources;
import com.hazelcast.jet.python.PythonTransforms;

/**
 * <p>Version 2 of the Python job to calculate Pi.
 * </p>
 * <p>For each input point, the Python routine determines if it is
 * inside ("{@code true}") or outside ("{@code false}", unsurprisingly)
 * the circle. The Jet job then calculates Pi from this ratio.
 * </p>
 */
public class Pi2Job {

    // "Pi2Job" submits "pi2", for Python module "pi2.py".
    private static final String NAME = Pi2Job.class.getSimpleName().substring(0, 3).toLowerCase(Locale.ROOT);
    private static final double QUARTER_CIRCLE = 4d;

    /**
     * <p>Calculate Pi from a stream of "{@code true}" &amp; "{@code false}" values.
     * Pi is the ratio of true in the stream.
     * </p>
     */
    private static final BiFunctionEx<MutableReference<Tuple2<Long, Long>>, String, Double> PI_CALCULATOR =
            (MutableReference<Tuple2<Long, Long>> reference, String string) -> {
                long trueCount = 0;
                long falseCount = 0;

                Tuple2<Long, Long> previous = reference.get();
                if (previous != null) {
                    trueCount = previous.f0();
                    falseCount = previous.f1();
                }

                if (string.toLowerCase(Locale.ROOT).equals("true")) {
                    trueCount++;
                } else {
                    falseCount++;
                }

                Tuple2<Long, Long> next = Tuple2.tuple2(trueCount, falseCount);
                reference.set(next);

                return QUARTER_CIRCLE * trueCount / (trueCount + falseCount);
            };

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
     * <li><p>"{@code mapStateful()}"</p>
     * <p>Pass each "{@code true}" or "{@code false}" from each Python instance running, into
     * a mapping function that maintains state. The state here being the rolling count of "{@code true}"
     * and "{@code false}" from which we output the current global estimate for Pi.
     * </li>
     * <li><p>"{@code window()}"</p>
     * <p>Insert watermarks every 5 seconds in the stream of data, for later aggregation.</p>
     * </li>
     * <li><p>"{@code aggregate()}"</p>
     * <p>For multiple refinements of Pi in the window, output the last as this is best.</p>
     * </li>
     * <li><p>"{@code writeTo()}"</p>
     * <p>Publish the average as a String to a Hazelcast {@link com.hazelcast.topic.ITopic}</p>
     * </li>
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
        .mapStateful(MutableReference::new, PI_CALCULATOR)
        .window(MyUtils.FIVE_SECOND_WINDOW)
        .aggregate(lastInWindow(Double::doubleValue))
        .writeTo(MyUtils.buildTopicSink(Pi2Job.class, "pi"));

        return pipeline;
    }

    /**
     * <p>Output the "<i>last</i>" item in the window. As we are refining Pi
     * with every calculation, the last in the window will be the most accurate.
     * </p>
     * <p>Note because the job executes in parallel we can't guarantee "<i>last</i>",
     * as the combine operation has no access to a timestamp, but nearly last is a
     * sufficient approximation for the purposes of this job.
     * </p>
     *
     * @param getDoubleValueFn How to get the double from the input
     * @return An aggregate operation on a single stream
     */
    public static AggregateOperation1<Double, MutableReference<Double>, String> lastInWindow(
            ToDoubleFunctionEx<Double> getDoubleValueFn
    ) {
        return AggregateOperation
                .withCreate(() -> new MutableReference<Double>())
                .andAccumulate((MutableReference<Double> reference, Double item) -> {
                    reference.set(item);
                })
                .andCombine((reference1, reference2) -> {
                    if (reference1.get() == null) {
                        reference1.set(reference2.get());
                    }
                })
                .andDeduct((reference1, reference2) -> {
                })
                .andExportFinish(reference -> reference.get() == null ? "<no value>" : reference.get().toString());
    }

}
