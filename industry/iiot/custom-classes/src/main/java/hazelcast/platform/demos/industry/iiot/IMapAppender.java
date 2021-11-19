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

package hazelcast.platform.demos.industry.iiot;

import com.hazelcast.cluster.Address;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.map.IMap;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

/**
 * <p>An appender that writes the logging event to an {@link IMap}.
 * </p>
 * <p>Results are written raw, can format later if required.
 * See also
 * <a href="https://logback.qos.ch/apidocs/ch/qos/logback/core/OutputStreamAppender.html">OutputStreamAppender</a>
 * which allows a
 * <a href="https://logback.qos.ch/apidocs/ch/qos/logback/classic/encoder/PatternLayoutEncoder.html">PatternLayoutEncoder</a>
 * to be attached that could do this.
 * </p>
 */
public class IMapAppender extends AppenderBase<ILoggingEvent> {

    private final IMap<HazelcastJsonValue, HazelcastJsonValue> logMap;
    private final String memberAddress;

    IMapAppender(HazelcastInstance arg0) {
        this.logMap = arg0.getMap(MyConstants.IMAP_NAME_SYS_LOGGING);
        Address address = arg0.getCluster().getLocalMember().getAddress();
        this.memberAddress = address.getHost() + ":" + address.getPort();
    }

    /**
     * <p> Capture fields from the logging event, post to a {@link IMap}.
     * </p>
     */
    @Override
    protected void append(ILoggingEvent eventObject) {
        StringBuffer keyStringBuffer = new StringBuffer();
        keyStringBuffer.append("{");
        keyStringBuffer.append(" \"" + MyConstants.LOGGING_FIELD_MEMBER_ADDRESS + "\" : \"" + this.memberAddress + "\"");
        keyStringBuffer.append(", \"" + MyConstants.LOGGING_FIELD_TIMESTAMP + "\" : " + eventObject.getTimeStamp());
        keyStringBuffer.append("}");

        StringBuffer valueStringBuffer = new StringBuffer();
        valueStringBuffer.append("{");
        valueStringBuffer.append(" \"" + MyConstants.LOGGING_FIELD_LOGGER_NAME + "\" : \"" + eventObject.getLoggerName() + "\"");
        valueStringBuffer.append(", \"" + MyConstants.LOGGING_FIELD_THREAD_NAME + "\" : \"" + eventObject.getThreadName() + "\"");
        valueStringBuffer.append(", \"" + MyConstants.LOGGING_FIELD_LEVEL + "\" : \"" + eventObject.getLevel() + "\"");
        valueStringBuffer.append(", \"" + MyConstants.LOGGING_FIELD_MESSAGE + "\" : \"" + eventObject.getMessage() + "\"");
        valueStringBuffer.append("}");

        this.logMap.set(new HazelcastJsonValue(keyStringBuffer.toString()),
                new HazelcastJsonValue(valueStringBuffer.toString()));
    }

}
