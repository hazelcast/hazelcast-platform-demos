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

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.cluster.Address;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;
import com.hazelcast.core.HazelcastInstanceNotActiveException;
import com.hazelcast.map.IMap;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * <p>Log periodically (hourly), something about this member.
 * Roll to a new {@link com.hazelcast.map.IMap IMap} at midnight.
 * </p>
 */
public class TodayLoggingRunnable implements HazelcastInstanceAware, Runnable, Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(TodayLoggingRunnable.class);

    private final boolean useHzCloud;
    private final String executor;
    private final String mapPrefix;
    private transient HazelcastInstance hazelcastInstance;

    TodayLoggingRunnable(boolean arg0, String arg1, String arg2) {
        this.useHzCloud = arg0;
        this.executor = arg1;
        this.mapPrefix = arg2;
    }

    /**
     * <p>Periodically log something about this member.
     * </p>
     */
    @Override
    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "InterruptedException possible")
    public void run() {
        String previous = "";
        if (!useHzCloud) {
            LOGGER.info("**{}**'{}'::START run()", LocalConstants.MY_JAR_NAME, this.executor);
        }

        try {
            Address address = this.hazelcastInstance.getCluster().getLocalMember().getAddress();
            IMap<String, String> iMap = null;
            while (true) {
                String today = LocalDate.now().toString();
                if (!today.equals(previous)) {
                    iMap = this.dateRollover(today);
                    previous = today;
                }
                if (iMap != null) {
                    final var key = address.getInetAddress().getHostAddress() + ":" + address.getPort();
                    String value = LocalTime.now().atOffset(ZoneOffset.UTC).toString();
                    iMap.set(key, value);
                    LOGGER.info("**{}**'{}'::run() -> Map '{}' set('{}', '{}')",
                            LocalConstants.MY_JAR_NAME, this.executor, iMap.getName(), key, value);
                }

                TimeUnit.HOURS.sleep(1L);
            }
        } catch (HazelcastInstanceNotActiveException hnae) {
            if (!useHzCloud) {
                LOGGER.info(
                        String.format("**%s**'%s'::HazelcastInstanceNotActiveException run(): %s",
                                LocalConstants.MY_JAR_NAME, this.executor, hnae.getMessage()));
            }
        } catch (InterruptedException ie) {
            if (!useHzCloud) {
                LOGGER.info(
                        String.format("**%s**'%s'::InterruptedException run(): %s",
                                LocalConstants.MY_JAR_NAME, this.executor, ie.getMessage()));
            }
        } catch (Exception e) {
            if (!useHzCloud) {
                LOGGER.info(String.format("**%s**'%s'::EXCEPTION run()", LocalConstants.MY_JAR_NAME, this.executor), e);
            }
        }

        if (!useHzCloud) {
            LOGGER.info("**{}**'{}'::END run()", LocalConstants.MY_JAR_NAME, this.executor);
        }
    }

    /**
     * <p>Invoked when date changes, switch logging to new map.
     * @param today
     * @return
     */
    private IMap<String, String> dateRollover(String today) {
        String mapName = this.mapPrefix + "_" + today.replaceAll("-", "_");
        String definition = "CREATE MAPPING IF NOT EXISTS \"" + mapName + "\""
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'varchar',"
                + " 'valueFormat' = 'varchar'"
                + " )";

        try {
            hazelcastInstance.getSql().execute(definition);
            LOGGER.debug("**{}**'{}'::Definition '{}'", LocalConstants.MY_JAR_NAME, this.executor, definition);
        } catch (Exception e) {
            LOGGER.error(
                    String.format("**%s**'%s'::Definition '%s'", LocalConstants.MY_JAR_NAME, this.executor, definition),
                    e);
            return null;
        }
        return this.hazelcastInstance.getMap(mapName);
    }

    @Override
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Hazelcast instance must be shared, not cloned")
    public void setHazelcastInstance(HazelcastInstance arg0) {
        this.hazelcastInstance = arg0;
    }

}
