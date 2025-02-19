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

package hazelcast.platform.demos.industry.iiot;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import com.hazelcast.cluster.Address;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.map.IMap;

/**
 * <p>Returns IMap logger instances.
 * </p>
 */
public class IMapLoggerFactory implements ILoggerFactory {

    private static IMap<HazelcastJsonValue, HazelcastJsonValue> logMap;
    private static String memberAddress;
    private static Level level = Level.INFO;

    public static synchronized Logger getLogger(Class<?> klass) {
        if (logMap == null) {
            HazelcastInstance hazelcastInstance = Hazelcast.getAllHazelcastInstances().iterator().next();
            logMap = hazelcastInstance.getMap(MyConstants.IMAP_NAME_SYS_LOGGING);
            Address address = hazelcastInstance.getCluster().getLocalMember().getAddress();
            memberAddress = address.getHost() + ":" + address.getPort();
        }
        return new IMapLogger(klass.getName(), logMap, memberAddress, level);
    }

    public static void setLevel(Level arg0) {
        level = arg0;
    }

    /**
     * <p>Use default, from {@link LoggerFactory} for String name argument.
     * </p>
     */
    @Override
    public Logger getLogger(String name) {
        return LoggerFactory.getLogger(name);
    }


}
