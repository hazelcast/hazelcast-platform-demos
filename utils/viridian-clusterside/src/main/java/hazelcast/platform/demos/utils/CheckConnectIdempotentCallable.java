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

package hazelcast.platform.demos.utils;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.cluster.Member;
import com.hazelcast.core.HazelcastInstance;

/**
 * <p>If this module is a dependency, it provides an easy check to confirm
 * if the user has uploaded classes to Viridian.
 * </p>
 */
public class CheckConnectIdempotentCallable {
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckConnectIdempotentCallable.class);

    /**
     * <p>Test serverside. Only really needed if using Viridian
     * to confirm upload of custom classes hasn't been forgotten.
     * </p>
     * @return
     */
    public static boolean performCheck(HazelcastInstance hazelcastInstance) {
        boolean ok = true;
        String message;

        ConnectIdempotentCallable connectIdempotentCallable = new ConnectIdempotentCallable();

        // Oldest
        Member member = hazelcastInstance.getCluster().getMembers().iterator().next();

        LOGGER.debug("Send {} to {}", connectIdempotentCallable.getClass().getSimpleName(), member.getAddress());
        Future<List<String>> future =
                hazelcastInstance.getExecutorService("default").submitToMember(connectIdempotentCallable, member);

        try {
            List<String> list = future.get();
            if (list == null || list.isEmpty()) {
                message = String.format("connectIdempotentCallable :: => :: '%s'", Objects.toString(list));
                LOGGER.error(message);
                ok = false;
            } else {
                for (String item : list) {
                    message = String.format("connectIdempotentCallable :: => :: '%s'", item);
                    LOGGER.info(message);
                }
            }
        } catch (Exception e) {
            LOGGER.error("connectIdempotentCallable", e);
            ok = false;
        }
        return ok;
    }

    /**
     * <p>Check the custom classes. If fail, use client/server to determine a possible
     * cause.
     * </p>
     * <p>For live running each JVM will contain 1 client or 1 server. If the client
     * list is empty we are a server, else we are a client.
     * </p>
     *
     * @param hazelcastInstance
     * @throws Exception -- includes suggestion
     */
    public static void silentCheckCustomClasses(HazelcastInstance hazelcastInstance) throws Exception {
        boolean ok = CheckConnectIdempotentCallable.performCheck(hazelcastInstance);
        if (!ok) {
            String message = ConnectIdempotentCallable.class.getSimpleName() + " failed:";

            // All clients in current JVM
            Collection<HazelcastInstance> clients = HazelcastClient.getAllHazelcastClients();
            if (clients.isEmpty()) {
                message += "bad Maven dependency if classes not found?";
            } else {
                message += "custom classes not uploaded to Viridian?";
            }

            throw new RuntimeException(message);
        }
    }
}
