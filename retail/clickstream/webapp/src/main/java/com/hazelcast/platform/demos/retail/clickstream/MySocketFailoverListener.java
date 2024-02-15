/*
 * Copyright (c) 2008-2024, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.platform.demos.retail.clickstream;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.hazelcast.core.LifecycleEvent;
import com.hazelcast.core.LifecycleEvent.LifecycleState;
import com.hazelcast.core.LifecycleListener;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Invoked for cluster events, send some to sockets
 * </p>
 */
@Slf4j
public class MySocketFailoverListener implements LifecycleListener {

    private final SimpMessagingTemplate simpMessagingTemplate;

    MySocketFailoverListener(SimpMessagingTemplate arg0) {
        this.simpMessagingTemplate = arg0;
    }

    @Override
    public void stateChanged(LifecycleEvent event) {
        log.info("LifecycleEvent {}", event);

        LifecycleState lifecycleState = event.getState();

        // Blue/green cluster changed
        if (lifecycleState == LifecycleState.CLIENT_CHANGED_CLUSTER) {
            String message =  MyLocalUtils.clusterNamePayload();
            String destination = MyLocalConstants.CLUSTER_DESTINATION;

            log.info("Sending to websocket '{}', message {}", destination, message);

            try {
                this.simpMessagingTemplate.convertAndSend(destination, message);
            } catch (Exception e) {
                log.error("stateChanged():", e.getMessage());
            }
        }
    }

}
