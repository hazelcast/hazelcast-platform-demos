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

package com.hazelcast.platform.demos.ml.ri;

import java.util.Locale;

import com.hazelcast.jet.aggregate.AggregateOperations;
import com.hazelcast.jet.pipeline.JournalInitialPosition;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sources;
import com.hazelcast.jet.python.PythonTransforms;

/**
 * <p>Version 1 of the Python job to calculate Pi.
 * </p>
 * <p>Here each Python worker independently calculates its approximation
 * for Pi, and these are averaged to produce a global approximation for
 * Pi.
 * </p>
 */
public class Pi1Job {

    // "Pi1Job" submits "pi1", for Python module "pi1.py".
    private static final String NAME = Pi1Job.class.getSimpleName().substring(0, 3).toLowerCase(Locale.ROOT);

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
     * <li><p>"{@code window()}"</p>
     * <p>Insert watermarks every 5 seconds in the stream of data, for later aggregation.</p>
     * </li>
     * <li><p>"{@code aggregate()}"</p>
     * <p>Collate the value of PI calculated by the multiple Python workers, and every watermark
     * (every 5 seconds), output the average.</p>
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
        .window(MyUtils.FIVE_SECOND_WINDOW)
        .aggregate(AggregateOperations.averagingDouble(string -> Double.parseDouble(string)))
        .writeTo(MyUtils.buildTopicSink(Pi1Job.class, "pi"));

        return pipeline;
    }

}
