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

package com.hazelcast.platform.demos.retail.clickstream.job;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.Map.Entry;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.platform.demos.retail.clickstream.MyConstants;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Compare the given assessment against the others, possibly
 * update the best copy to use.
 * </p>
 */
@Slf4j
public class RetrainingAssessmentListenerRunnable implements Runnable {

    private static final DecimalFormat TWO_DP = new DecimalFormat("#.##");

    private final HazelcastInstance hazelcastInstance;
    private final EntryEvent<String, Double> entryEvent;

    public RetrainingAssessmentListenerRunnable(EntryEvent<String, Double> arg0) {
        this.hazelcastInstance = Hazelcast.getAllHazelcastInstances().iterator().next();
        this.entryEvent = arg0;
    }

    @Override
    public void run() {
        try {
            log.trace("run(): Entry<'{}',{}>", this.entryEvent.getKey(), this.entryEvent.getValue());
            String newModel = this.entryEvent.getKey();
            double newScore = this.entryEvent.getValue();
            String algorithm = newModel.split("-")[0];

            // Find best
            String bestModel = newModel;
            double bestScore = newScore;
            Map<String, Double> retrainingAssessmentMap =
                    this.hazelcastInstance.getMap(MyConstants.IMAP_NAME_RETRAINING_ASSESSMENT);
            for (Entry<String, Double> entry : retrainingAssessmentMap.entrySet()) {
                if (entry.getValue() > bestScore) {
                    bestScore = entry.getValue();
                    bestModel = entry.getKey();
                }
            }

            if (bestScore > newScore) {
                log.info("run(): Ignore model '{}' with score {}%s, best for {} is model '{}' with score {}%",
                       newModel, TWO_DP.format(newScore),
                       algorithm,
                       bestModel, TWO_DP.format(bestScore));
                return;
            }

            // Find current recommendation
            Map<String, String> modelSelectionMap = this.hazelcastInstance.getMap(MyConstants.IMAP_NAME_MODEL_SELECTION);
            String currentModel = modelSelectionMap.get(algorithm);
            if (currentModel == null) {
                log.info("run(): First model for {}, '{}' with score {}%",
                        algorithm, newModel, TWO_DP.format(newScore));
                modelSelectionMap.put(algorithm, newModel);
            } else {
                double currentScore = retrainingAssessmentMap.get(currentModel);
                if (currentScore >= newScore) {
                    log.info("run(): Ignore model '{}' with score {}%s, current for {} is model '{}' with score {}%",
                            newModel, TWO_DP.format(newScore),
                            algorithm,
                            currentModel, TWO_DP.format(currentScore));
                } else {
                    log.info("run(): Insert model '{}' with score {}%s, current for {} is model '{}' with score {}%",
                            newModel, TWO_DP.format(newScore),
                            algorithm,
                            currentModel, TWO_DP.format(currentScore));
                    modelSelectionMap.put(algorithm, newModel);
                }
            }
        } catch (Exception e) {
            log.error("run()", e);
        }
    }
}
