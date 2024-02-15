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

package hazelcast.platform.demos.industry.iiot;

import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hazelcast.core.HazelcastInstance;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>For Kubernetes liveness/readiness probes.</p>
 */
@RestController
@RequestMapping("/rest")
@Slf4j
public class MyRestController {

    @Autowired
    private HazelcastInstance hazelcastInstance;
    @Autowired
    private MyProperties myProperties;

    /**
     * <p>Provide a URL for Kubernetes to test the client is alive.
     * </p>
     *
     * @return Any String, doesn't matter, so why not the build timestamp.
     */
    @GetMapping(value = "/k8s")
    public String k8s() {
        log.trace("k8s() -> {}", this.myProperties.getBuildTimestamp());
        return this.myProperties.getBuildTimestamp();
    }

    /**
     * <p>REST endpoint to initialize the cluster.
     * </p>
     *
     * @return
     */
    @GetMapping(value = "/initialize", produces = MediaType.APPLICATION_JSON_VALUE)
    public String initialize() {
        log.debug("initialize()");

        String[][] config = {};
        String failingCallable = "";

        InitializerAllNodes initializerAllNodes = new InitializerAllNodes(Level.DEBUG);
        boolean ok = Utils.runTuple4Callable(log, initializerAllNodes, this.hazelcastInstance, false);
        if (!ok) {
            failingCallable = initializerAllNodes.getClass().getSimpleName();
        } else {
            InitializerAnyNode initializerAnyNode = new InitializerAnyNode(config);
            ok = Utils.runTuple4Callable(log, initializerAnyNode, this.hazelcastInstance, true);
            if (!ok) {
                failingCallable = initializerAnyNode.getClass().getSimpleName();
            }
        }
        if (!ok) {
            log.error("initialize(): FAILED: " + failingCallable);
        }

        StringBuilder stringBuilder = new StringBuilder("{ ");
        stringBuilder.append(" \"failing_callable\" : \"" + failingCallable + "\"");
        stringBuilder.append(", \"error\" : " + !ok);
        stringBuilder.append(" }");

        return stringBuilder.toString();
    }

}
