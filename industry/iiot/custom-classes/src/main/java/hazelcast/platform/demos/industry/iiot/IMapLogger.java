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

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;

import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.map.IMap;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * FIXME
 */
@SuppressWarnings("checkstyle:methodcount")
public class IMapLogger implements Logger {

    private final String name;
    private final IMap<HazelcastJsonValue, HazelcastJsonValue> logMap;
    private final String memberAddress;
    private final Level level;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "threadsafe proxy")
    public IMapLogger(String arg0, IMap<HazelcastJsonValue, HazelcastJsonValue> arg1, String arg2, Level arg3) {
        this.name = arg0;
        this.logMap = arg1;
        this.memberAddress = arg2;
        this.level = arg3;
    }

    @Override
    public String getName() {
        return this.name;
    }

    /**
     * <p>Does most of the work
     * </p>
     */
    private void saveToHazelcast(String msg) {
        StringBuffer keyStringBuffer = new StringBuffer();
        keyStringBuffer.append("{");
        keyStringBuffer.append(" \"" + MyConstants.LOGGING_FIELD_MEMBER_ADDRESS + "\" : \"" + this.memberAddress + "\"");
        keyStringBuffer.append(", \"" + MyConstants.LOGGING_FIELD_TIMESTAMP + "\" : " + System.currentTimeMillis());
        keyStringBuffer.append("}");

        StringBuffer valueStringBuffer = new StringBuffer();
        valueStringBuffer.append("{");
        valueStringBuffer.append(" \"" + MyConstants.LOGGING_FIELD_LOGGER_NAME + "\" : \"" + this.name + "\"");
        valueStringBuffer.append(", \"" + MyConstants.LOGGING_FIELD_THREAD_NAME + "\" : \""
                + Thread.currentThread().getName() + "\"");
        valueStringBuffer.append(", \"" + MyConstants.LOGGING_FIELD_LEVEL + "\" : \"" + this.level + "\"");
        valueStringBuffer.append(", \"" + MyConstants.LOGGING_FIELD_MESSAGE + "\" : \"" + msg + "\"");
        valueStringBuffer.append("}");

        this.logMap.set(new HazelcastJsonValue(keyStringBuffer.toString()),
                new HazelcastJsonValue(valueStringBuffer.toString()));
    }

    @Override
    public boolean isTraceEnabled() {
        return this.level.toInt() <= Level.TRACE.toInt();
    }

    @Override
    public void trace(String msg) {
        if (this.isTraceEnabled()) {
            this.saveToHazelcast(msg);
        }
    }

    @Override
    public void trace(String format, Object arg) {
        // TODO Auto-generated method stub

    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        // TODO Auto-generated method stub

    }

    @Override
    public void trace(String format, Object... arguments) {
        // TODO Auto-generated method stub

    }

    @Override
    public void trace(String msg, Throwable t) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void trace(Marker marker, String msg) {
        // TODO Auto-generated method stub

    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        // TODO Auto-generated method stub

    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        // TODO Auto-generated method stub

    }

    @Override
    public void trace(Marker marker, String format, Object... argArray) {
        // TODO Auto-generated method stub

    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isDebugEnabled() {
        return this.level.toInt() <= Level.DEBUG.toInt();
    }

    @Override
    public void debug(String msg) {
        if (this.isDebugEnabled()) {
            this.saveToHazelcast(msg);
        }
    }

    @Override
    public void debug(String format, Object arg) {
        // TODO Auto-generated method stub

    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        // TODO Auto-generated method stub

    }

    @Override
    public void debug(String format, Object... arguments) {
        // TODO Auto-generated method stub

    }

    @Override
    public void debug(String msg, Throwable t) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void debug(Marker marker, String msg) {
        // TODO Auto-generated method stub

    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        // TODO Auto-generated method stub

    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        // TODO Auto-generated method stub

    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {
        // TODO Auto-generated method stub

    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isInfoEnabled() {
        return this.level.toInt() <= Level.INFO.toInt();
    }

    @Override
    public void info(String msg) {
        if (this.isInfoEnabled()) {
            this.saveToHazelcast(msg);
        }
    }

    @Override
    public void info(String format, Object arg) {
        // TODO Auto-generated method stub

    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        // TODO Auto-generated method stub

    }

    @Override
    public void info(String format, Object... arguments) {
        // TODO Auto-generated method stub

    }

    @Override
    public void info(String msg, Throwable t) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void info(Marker marker, String msg) {
        // TODO Auto-generated method stub

    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        // TODO Auto-generated method stub

    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        // TODO Auto-generated method stub

    }

    @Override
    public void info(Marker marker, String format, Object... arguments) {
        // TODO Auto-generated method stub

    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isWarnEnabled() {
        return this.level.toInt() <= Level.WARN.toInt();
    }

    @Override
    public void warn(String msg) {
        if (this.isWarnEnabled()) {
            this.saveToHazelcast(msg);
        }
    }

    @Override
    public void warn(String format, Object arg) {
        // TODO Auto-generated method stub
    }

    @Override
    public void warn(String format, Object... arguments) {
        // TODO Auto-generated method stub
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        // TODO Auto-generated method stub
    }

    @Override
    public void warn(String msg, Throwable t) {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void warn(Marker marker, String msg) {
        // TODO Auto-generated method stub

    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        // TODO Auto-generated method stub

    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        // TODO Auto-generated method stub

    }

    @Override
    public void warn(Marker marker, String format, Object... arguments) {
        // TODO Auto-generated method stub

    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        // TODO Auto-generated method stub

    }

    // See https://github.com/qos-ch/slf4j/blob/master/slf4j-api/src/main/java/org/slf4j/spi/LocationAwareLogger.java#L44
    @Override
    public boolean isErrorEnabled() {
        return this.level.toInt() <= Level.ERROR.toInt();
    }

    @Override
    public void error(String msg) {
        if (this.isErrorEnabled()) {
            this.saveToHazelcast(msg);
        }
    }

    @Override
    public void error(String format, Object arg) {
        // TODO Auto-generated method stub

    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        // TODO Auto-generated method stub

    }

    @Override
    public void error(String format, Object... arguments) {
        // TODO Auto-generated method stub

    }

    @Override
    public void error(String msg, Throwable t) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void error(Marker marker, String msg) {
        // TODO Auto-generated method stub

    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        // TODO Auto-generated method stub

    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        // TODO Auto-generated method stub

    }

    @Override
    public void error(Marker marker, String format, Object... arguments) {
        // TODO Auto-generated method stub

    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        // TODO Auto-generated method stub

    }

}
