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

package com.hazelcast.platform.demos.retail.clickstream.job;

import java.text.SimpleDateFormat;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import com.hazelcast.jet.core.AbstractProcessor;
import com.hazelcast.jet.datamodel.Tuple5;
import com.hazelcast.platform.demos.retail.clickstream.PredictionKey;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Turns Python output into a map entry.
 * <p>
 * <p>Expected input looks like:
 * <pre>
 * abc,123,456,RandomForest-123,0,
 * </pre>
 * Key, publish timestamp, ingestion timestamp, model version, prediction (0==false, 1==true), any
 * error message.
 * </p>
 */
@Slf4j
public class RandomForestPredictionProcessor extends AbstractProcessor {
    private static final int EXPECTED_LENGTH_OF_5 = 5;
    private static final int FIRST_KEY = 0;
    private static final int SECOND_PUBLISH_TIMESTAMP = 1;
    private static final int THIRD_INGEST_TIMESTAMP = 2;
    private static final int FOURTH_MODEL_VERSION = 3;
    private static final int FIFTH_PREDICTION = 4;

    private final String algorithm;
    private final SimpleDateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    public RandomForestPredictionProcessor(String arg0) {
        this.algorithm = arg0;
    }

    @Override
    protected boolean tryProcess(int ordinal, Object item) {
        String[] tokens = item.toString().split(",");

        if (tokens.length != EXPECTED_LENGTH_OF_5) {
            log.error("tryProcess({}, '{}')", ordinal, item);
            return true;
        }

        List<String> tokenList = Arrays.asList(tokens);
        String key = tokenList.get(FIRST_KEY);
        Long publishTimestamp = Long.parseLong(tokenList.get(SECOND_PUBLISH_TIMESTAMP));
        Long ingestTimestamp = Long.parseLong(tokenList.get(THIRD_INGEST_TIMESTAMP));
        Long predictionTimestamp = System.currentTimeMillis();
        String version = "?";
        try {
            long timestamp = Long.parseLong(tokenList.get(FOURTH_MODEL_VERSION));
            Date date = new Date(timestamp);
            version = iso8601.format(date);
        } catch (NumberFormatException nfe) {
            // Was a String already
            version = tokenList.get(FOURTH_MODEL_VERSION);
        }
        Integer prediction = Integer.parseInt(tokenList.get(FIFTH_PREDICTION));

        PredictionKey predictionKey = new PredictionKey();
        predictionKey.setAlgorithm(this.algorithm);
        predictionKey.setKey(key);

        Tuple5<String, Long, Long, Long, Integer> predictionValue
            = Tuple5.<String, Long, Long, Long, Integer>tuple5(
                    version, publishTimestamp, ingestTimestamp, predictionTimestamp, prediction);

        Entry<PredictionKey, Tuple5<String, Long, Long, Long, Integer>> entry
            = new SimpleImmutableEntry<>(predictionKey, predictionValue);

        return super.tryEmit(entry);
    }

}
