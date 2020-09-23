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

package com.hazelcast.platform.demos.telco.churn;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.configurationprocessor.json.JSONObject;

/**
 * <p>Make a REST call to sink the object to the designated Slack destination.
 * </p>
 * <p>Kudos to Gokhan Oner, <a href="https://github.com/gokhanoner/hazelcast-jet-connectors">hazelcast-jet-connectors</a>
 * for the basis of this.
 * </p>
 */
public class MySlackSink {
    private static final Logger LOGGER = LoggerFactory.getLogger(MySlackSink.class);

    /*
    return SinkBuilder.sinkBuilder(
            "slackSink-" + channelName,
            context -> null
             SimpleHttpClient
                    .create(URL))
            .<String>receiveFn(
                    ((httpClient, message) ->
                    httpClient
                            .withHeader("Authorization", String.format("Bearer %s", accessToken))
                            .withParam("channel", channel)
                            .withParam("text", message)
                            .postWithParams())
                    )
            .build();*/

    public MySlackSink(Properties properties) {
        // TODO Auto-generated constructor stub
    }

    /**
     * XXX
     *
     * @param o
     */
    public Object receiveFn(JSONObject item) {
        LOGGER.error("XXX receive " + item);
        return this;
    }

    /**
     * XXX
     */
    public Object destroyFn() {
        LOGGER.error("XXX destroy");
        return this;
    }
}
