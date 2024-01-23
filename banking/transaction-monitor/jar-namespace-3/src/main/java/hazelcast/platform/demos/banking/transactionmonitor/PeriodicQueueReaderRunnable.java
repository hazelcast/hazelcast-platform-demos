/*
 * Copyright (c) 2008-2024, Hazelcast, Inc. All Rights Reserved.
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
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.collection.IQueue;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;
import com.hazelcast.core.HazelcastInstanceNotActiveException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * <p>Try every 5 minutes to read from a queue.
 * </p>
 */
public class PeriodicQueueReaderRunnable implements HazelcastInstanceAware, Runnable, Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(PeriodicQueueReaderRunnable.class);
    private static final long SLEEP_5_MINUTES = TimeUnit.MINUTES.toMillis(5L);

    private final boolean useViridian;
    private final String prefix;
    private transient HazelcastInstance hazelcastInstance;

    PeriodicQueueReaderRunnable(boolean arg0, String arg1) {
        this.useViridian = arg0;
        this.prefix = arg1;
    }

    /**
     * <p>Periodically try to read from queue.
     * </p>
     */
    @Override
    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "InterruptedException possible")
    public void run() {
        if (!useViridian) {
            LOGGER.info("**{}**'{}'::START run()", LocalConstants.MY_JAR_NAME, this.prefix);
        }

        IQueue<String> iQueue = this.hazelcastInstance.getQueue(MyConstants.QUEUE_NAMESPACE_3);

        try {
            while (true) {
                String message = iQueue.poll();
                if (message != null) {
                    LOGGER.info("**{}**'{}'::run() -> poll -> '{}'", LocalConstants.MY_JAR_NAME, this.prefix, message);
                    // Retry immediately in case of fast writer
                } else {
                    LOGGER.info("**{}**'{}'::run() -> poll got null", LocalConstants.MY_JAR_NAME, this.prefix);
                    Thread.sleep(SLEEP_5_MINUTES);
                }
            }
        } catch (HazelcastInstanceNotActiveException hnae) {
            if (!useViridian) {
                LOGGER.info(
                        String.format("**%s**'%s'::HazelcastInstanceNotActiveException run(): %s",
                                LocalConstants.MY_JAR_NAME, this.prefix, hnae.getMessage()));
            }
        } catch (InterruptedException ie) {
            if (!useViridian) {
                LOGGER.info(
                        String.format("**%s**'%s'::InterruptedException run(): %s",
                                LocalConstants.MY_JAR_NAME, this.prefix, ie.getMessage()));
            }
        } catch (Exception e) {
            if (!useViridian) {
                LOGGER.info(String.format("**%s**'%s'::EXCEPTION run()", LocalConstants.MY_JAR_NAME, this.prefix), e);
            }
        }

        if (!useViridian) {
            LOGGER.info("**{}**'{}'::END run()", LocalConstants.MY_JAR_NAME, this.prefix);
        }
    }

    @Override
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Hazelcast instance must be shared, not cloned")
    public void setHazelcastInstance(HazelcastInstance arg0) {
        this.hazelcastInstance = arg0;
    }

}
