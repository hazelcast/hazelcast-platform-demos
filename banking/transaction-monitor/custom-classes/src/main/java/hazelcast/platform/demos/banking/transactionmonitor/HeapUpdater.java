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
import java.util.AbstractMap.SimpleEntry;
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
 * <p>Store the heap size for analysis.
 * </p>
 */
public class HeapUpdater implements Runnable, Serializable, HazelcastInstanceAware {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(HeapUpdater.class);

    private final boolean useHzCloud;
    private transient HazelcastInstance hazelcastInstance;
    private int binaryLoggingInterval = 1;
    private int count;

    HeapUpdater(boolean arg0) {
        this.useHzCloud = arg0;
    }
    /**
     * <p>Periodically record heap size in a map with journal
     * attached, so can track heap usage from a pipeline.
     * </p>
     */
    @Override
    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "InterruptedException possible")
    public void run() {
        if (!useHzCloud) {
            LOGGER.info("START run()");
        }

        try {
            Address address = this.hazelcastInstance.getCluster().getLocalMember().getAddress();
            final var key = address.getInetAddress().getHostAddress() + ":" + address.getPort();

            final long maxMemory = Runtime.getRuntime().maxMemory();

            IMap<String, Long> heapMap =
                this.hazelcastInstance.getMap(MyConstants.IMAP_NAME_HEAP);

            // Init
            long usedHeap = maxMemory - Runtime.getRuntime().freeMemory();
            heapMap.set(key, usedHeap);
            if (!useHzCloud) {
                this.logExponentially(new SimpleEntry<>(key, usedHeap));
            }

            while (true) {
                usedHeap = maxMemory - Runtime.getRuntime().freeMemory();
                heapMap.set(key, usedHeap);
                this.logExponentially(new SimpleEntry<>(key, usedHeap));

                TimeUnit.SECONDS.sleep(1L);
            }
        } catch (HazelcastInstanceNotActiveException hnae) {
            if (!useHzCloud) {
                LOGGER.info("HazelcastInstanceNotActiveException run(): {}", hnae.getMessage());
            }
        } catch (InterruptedException ie) {
            if (!useHzCloud) {
                LOGGER.info("InterruptedException run(): {}", ie.getMessage());
            }
        } catch (Exception e) {
            if (!useHzCloud) {
                LOGGER.info("EXCEPTION run()", e);
            }
        }

        if (!useHzCloud) {
            LOGGER.info("END run()");
        }
    }

    /**
     * <p>Log less and less frequently.
     * </p>
     *
     * @param datum Some object
     */
    private void logExponentially(Object datum) {
        if (this.count % this.binaryLoggingInterval == 0) {
            LOGGER.debug("Write '{}' -> '{}'", this.count, datum);
            if (2 * this.binaryLoggingInterval <= MyConstants.MAX_LOGGING_INTERVAL) {
                this.binaryLoggingInterval = 2 * this.binaryLoggingInterval;
            }
        }
        this.count++;
    }

    @Override
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Hazelcast instance must be shared, not cloned")
    public void setHazelcastInstance(HazelcastInstance arg0) {
        this.hazelcastInstance = arg0;
    }

}
