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

package hazelcast.platform.demos.banking.transactionmonitor;

import java.time.LocalDateTime;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.nio.serialization.genericrecord.GenericRecord;
import com.hazelcast.nio.serialization.genericrecord.GenericRecordBuilder;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * <p>An implementation of logging that saves to an {@link com.hazelcast.map.IMap IMap}
 * using {@link com.hazelcast.nio.serialization.genericrecord.GenericRecord GenericRecord}.
 * Most of the work is done by {@link #saveToHazelcast(Level, String)}.
 * </p>
 * <p>There is no formatting applied to the message.
 * </p>
 */
@SuppressWarnings("checkstyle:MethodCount")
public class IMapLogger implements Logger {
    // Sizes should match MySql column limits
    private static final int EIGHT = 8;
    private static final int FORTY_EIGHT = 48;
    private static final int TWO_HUNDRED_AND_FIFTY_SIX = 256;

    private final String name;
    private final HazelcastInstance hazelcastInstance;
    private final String socketAddress;
    private final Level level;

    private IMap<Long, GenericRecord> mySqlSlf4jMap;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "HazelcastInstance is thread-safe")
    public IMapLogger(String arg0, HazelcastInstance arg1, Level arg2) {
        this.name = arg0;
        this.hazelcastInstance = arg1;
        this.socketAddress = hazelcastInstance.getLocalEndpoint().getSocketAddress().toString().substring(1);
        this.level = arg2;
    }

    private static String nullSafeTruncate(Object input, int max) {
        String output = Objects.toString(input);
        return output.length() > max ? output.substring(0, max) : output;
    }

    private void saveToHazelcast(Level arg0, String arg1) {
        // To give map loader a chance to attach, defer map access until needed
        if (this.mySqlSlf4jMap == null) {
            this.mySqlSlf4jMap = hazelcastInstance.getMap(MyConstants.IMAP_NAME_MYSQL_SLF4J);
        }

        String hashStr = nullSafeTruncate(this.socketAddress, FORTY_EIGHT) + LocalDateTime.now();
        long hash = hashStr.hashCode();
        /*FIXME Once MySql compound key supported by Data Link
        GenericRecord key = GenericRecordBuilder.compact(MyConstants.IMAP_NAME_MYSQL_SLF4J + ".key")
                .setString(MyConstants.MYSQL_DATASTORE_TABLE_COLUMN0, nullSafeTruncate(this.socketAddress, FORTY_EIGHT))
                .setTimestamp(MyConstants.MYSQL_DATASTORE_TABLE_COLUMN1, LocalDateTime.now())
                .build();

        GenericRecord value = GenericRecordBuilder.compact(MyConstants.IMAP_NAME_MYSQL_SLF4J + ".value")
                .setString(MyConstants.MYSQL_DATASTORE_TABLE_COLUMN2,
                        nullSafeTruncate(arg0, EIGHT))
                .setString(MyConstants.MYSQL_DATASTORE_TABLE_COLUMN3,
                        nullSafeTruncate(arg1, TWO_HUNDRED_AND_FIFTY_SIX))
                .setString(MyConstants.MYSQL_DATASTORE_TABLE_COLUMN4,
                        nullSafeTruncate(Thread.currentThread().getName(), FORTY_EIGHT))
                .setString(MyConstants.MYSQL_DATASTORE_TABLE_COLUMN5,
                        nullSafeTruncate(this.name, FORTY_EIGHT))
                .build();
                */
        GenericRecord value = GenericRecordBuilder.compact(MyConstants.IMAP_NAME_MYSQL_SLF4J + ".value")
                .setString(MyConstants.MYSQL_DATACONNECTION_TABLE_COLUMN0,
                        nullSafeTruncate(this.socketAddress, FORTY_EIGHT))
                .setTimestamp(MyConstants.MYSQL_DATACONNECTION_TABLE_COLUMN1,
                        LocalDateTime.now())
                .setString(MyConstants.MYSQL_DATACONNECTION_TABLE_COLUMN2,
                        nullSafeTruncate(arg0, EIGHT))
                .setString(MyConstants.MYSQL_DATACONNECTION_TABLE_COLUMN3,
                        nullSafeTruncate(arg1, TWO_HUNDRED_AND_FIFTY_SIX))
                .setString(MyConstants.MYSQL_DATACONNECTION_TABLE_COLUMN4,
                        nullSafeTruncate(Thread.currentThread().getName(), FORTY_EIGHT))
                .setString(MyConstants.MYSQL_DATACONNECTION_TABLE_COLUMN5,
                        nullSafeTruncate(this.name, FORTY_EIGHT))
                .build();

        //FIXME Once GenericRecord as key is supported
        this.mySqlSlf4jMap.set(hash, value);
    }

    /**
     * <p>The following are standard methods from {@link org.slf4j.Logger}.
     * </p>
     */

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public boolean isTraceEnabled() {
        return this.level.toInt() <= Level.TRACE.toInt();
    }

    @Override
    public void trace(String msg) {
        if (this.isTraceEnabled()) {
            this.saveToHazelcast(Level.TRACE, msg);
        }
    }

    @Override
    public void trace(String format, Object arg) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
    }

    @Override
    public void trace(String format, Object... arguments) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
    }

    @Override
    public void trace(String msg, Throwable t) {
        if (this.isTraceEnabled()) {
            this.saveToHazelcast(Level.TRACE, msg + ":" + t.getMessage());
        }
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
        return false;
    }

    @Override
    public void trace(Marker marker, String msg) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
    }

    @Override
    public void trace(Marker marker, String format, Object... argArray) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
    }

    @Override
    public boolean isDebugEnabled() {
        return this.level.toInt() <= Level.DEBUG.toInt();
    }

    @Override
    public void debug(String msg) {
        if (this.isDebugEnabled()) {
            this.saveToHazelcast(Level.DEBUG, msg);
        }
    }

    @Override
    public void debug(String format, Object arg) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
    }

    @Override
    public void debug(String format, Object... arguments) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
    }

    @Override
    public void debug(String msg, Throwable t) {
        if (this.isDebugEnabled()) {
            this.saveToHazelcast(Level.DEBUG, msg + ":" + t.getMessage());
        }
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
        return false;
    }

    @Override
    public void debug(Marker marker, String msg) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
    }

    @Override
    public boolean isInfoEnabled() {
        return this.level.toInt() <= Level.INFO.toInt();
    }

    @Override
    public void info(String msg) {
        if (this.isInfoEnabled()) {
            this.saveToHazelcast(Level.INFO, msg);
        }
    }

    @Override
    public void info(String format, Object arg) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
    }

    @Override
    public void info(String format, Object... arguments) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
    }

    @Override
    public void info(String msg, Throwable t) {
        if (this.isInfoEnabled()) {
            this.saveToHazelcast(Level.INFO, msg + ":" + t.getMessage());
        }
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
        return false;
    }

    @Override
    public void info(Marker marker, String msg) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
    }

    @Override
    public void info(Marker marker, String format, Object... arguments) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
    }

    @Override
    public boolean isWarnEnabled() {
        return this.level.toInt() <= Level.WARN.toInt();
    }

    @Override
    public void warn(String msg) {
        if (this.isWarnEnabled()) {
            this.saveToHazelcast(Level.WARN, msg);
        }
    }

    @Override
    public void warn(String format, Object arg) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
    }

    @Override
    public void warn(String format, Object... arguments) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
    }

    @Override
    public void warn(String msg, Throwable t) {
        if (this.isWarnEnabled()) {
            this.saveToHazelcast(Level.WARN, msg + ":" + t.getMessage());
        }
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
        return false;
    }

    @Override
    public void warn(Marker marker, String msg) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
    }

    @Override
    public void warn(Marker marker, String format, Object... arguments) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
    }

    @Override
    public boolean isErrorEnabled() {
        return this.level.toInt() <= Level.ERROR.toInt();
    }

    @Override
    public void error(String msg) {
        if (this.isErrorEnabled()) {
            this.saveToHazelcast(Level.ERROR, msg);
        }
    }

    @Override
    public void error(String format, Object arg) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
    }

    @Override
    public void error(String format, Object... arguments) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
    }

    @Override
    public void error(String msg, Throwable t) {
        if (this.isErrorEnabled()) {
            this.saveToHazelcast(Level.ERROR, msg + ":" + t.getMessage());
        }
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
        return false;
    }

    @Override
    public void error(Marker marker, String msg) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
    }

    @Override
    public void error(Marker marker, String format, Object... arguments) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        System.out.println(this.getClass().getSimpleName() + ": Unimplemented method invoked");
    }

}
