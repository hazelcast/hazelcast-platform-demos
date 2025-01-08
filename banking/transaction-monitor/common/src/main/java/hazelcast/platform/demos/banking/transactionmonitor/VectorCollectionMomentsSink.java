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

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.datamodel.KeyedWindowResult;
import com.hazelcast.jet.pipeline.Sink;
import com.hazelcast.jet.pipeline.SinkBuilder;
import com.hazelcast.vector.VectorCollection;
import com.hazelcast.vector.VectorDocument;
import com.hazelcast.vector.VectorValues;
import com.hazelcast.vector.impl.MultiIndexVectorValues;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * <p>Use a {@link com.hazelcast.vector.VectorCollection VectorCollection} as a sink,
 * but with merging data into existing value.
 * </p>
 */
public class VectorCollectionMomentsSink {
    private final VectorCollection<String, String> transactions;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Object is thread-safe")
    public VectorCollectionMomentsSink(HazelcastInstance hazelcastInstance) {
        this.transactions = VectorCollection.getCollection(hazelcastInstance, MyConstants.VECTOR_COLLECTION_TRANSACTIONS);
    }

    /**
     * <p>Build the sink for {@link #receiveFn(KeyedWindowResult<String, Long>)}.
     * </p>
     *
     * @return
     */
    public static Sink<KeyedWindowResult<String, Long>> vectorCollectionMomentsSink() {
        return SinkBuilder.sinkBuilder(
                    "vectorCollectionMomentsSink-",
                    context -> new VectorCollectionMomentsSink(context.hazelcastInstance())
                )
                .receiveFn(
                        (VectorCollectionMomentsSink vectorCollectionMomentsSink,
                                KeyedWindowResult<String, Long> keyedWindowResult) ->
                            vectorCollectionMomentsSink.receiveFn(keyedWindowResult)
                        )
                .build();
    }

    /**
     * <p>Update/Insert the Vector Collection for the count per moment.
     * </p>
     *
     * @param key/value pair
     * @return
     */
    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Future get()")
    public Object receiveFn(KeyedWindowResult<String, Long> keyedWindowResult) {
        try {
            int moment = findMoment(keyedWindowResult);
            String key = keyedWindowResult.getKey();

            VectorDocument<String> vectorDocument =
                    this.transactions.getAsync(key).toCompletableFuture().get();

            float[] moments;
            if (vectorDocument == null) {
                moments = new float[MyConstants.MOMENTS_IN_HOUR];
            } else {
                VectorValues vectors = vectorDocument.getVectors();
                if (vectors instanceof MultiIndexVectorValues) {
                    MultiIndexVectorValues multiIndexVectorValues = (MultiIndexVectorValues) vectors;
                    moments = multiIndexVectorValues.indexNameToVector().get(MyConstants.VECTOR_DOCUMENT_MOMENTS);
                } else {
                    System.out.println(VectorCollectionMomentsSink.class.getName()
                            + ":Class expected '" + vectors.getClass().getCanonicalName() + "'");
                    moments = new float[MyConstants.MOMENTS_IN_HOUR];
                }
            }
            moments[moment] = keyedWindowResult.getValue();
            System.out.println(VectorCollectionMomentsSink.class.getName()
                    + ":Save '" + key + "': " + Arrays.toString(moments));

            VectorValues vectorValues = VectorValues.of(MyConstants.VECTOR_DOCUMENT_MOMENTS, moments);

            transactions.putAsync(key, VectorDocument.of(key, vectorValues)).toCompletableFuture().get();
        } catch (Exception e) {
            System.out.println(VectorCollectionMomentsSink.class.getName()
                    + ":Exception for '" + keyedWindowResult + "': " + e.getMessage());
        }
        return this;
    }

    /**
     * <p>Extract the "moment" (fortieth of an hour) from the window</p>
     * </p>
     *
     * @param keyedWindowResult
     * @return
     */
    @SuppressWarnings("checkstyle:magicnumber")
    protected static int findMoment(KeyedWindowResult<String, Long> keyedWindowResult) {
        Instant start = Instant.ofEpochMilli(keyedWindowResult.start());
        int minute = start.atZone(ZoneOffset.UTC).getMinute();
        int secondsInMinute = start.atZone(ZoneOffset.UTC).getSecond();
        int secondsInHour = 60 * minute + secondsInMinute;
        return secondsInHour / 90;
    }

}
