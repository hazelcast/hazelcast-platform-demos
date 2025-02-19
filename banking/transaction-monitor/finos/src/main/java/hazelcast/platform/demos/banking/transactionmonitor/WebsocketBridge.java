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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

/**
 * <p>Pass {@link IMap} events to the websocket sender.
 * Javascript is websocket receiver.
 * </p>
 */
@Configuration
public class WebsocketBridge {

    @Autowired
    private MyWebSocketBridgeListener myWebSocketBridgeListener;
    @Autowired
    private HazelcastInstance hazelcastInstance;

    @Bean
    CommandLineRunner commandLineRunner() {
        return args -> {
            this.hazelcastInstance
                .getMap(MyConstants.IMAP_NAME_PERSPECTIVE)
                .addEntryListener(this.myWebSocketBridgeListener, true);
        };
    }

}
