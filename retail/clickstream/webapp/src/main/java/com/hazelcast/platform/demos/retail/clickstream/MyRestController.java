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

package com.hazelcast.platform.demos.retail.clickstream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>For Kubernetes liveness/readiness probes.</p>
 */
@RestController
@RequestMapping("/rest")
@Slf4j
public class MyRestController {

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

}
