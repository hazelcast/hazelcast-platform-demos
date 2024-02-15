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
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.collection.IQueue;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;
import com.hazelcast.core.HazelcastInstanceNotActiveException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * <p>Log periodically (4 * hourly), something about this member, to a queue.
 * </p>
 */
public class PeriodicQueueWriterRunnable implements HazelcastInstanceAware, Runnable, Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(PeriodicQueueWriterRunnable.class);
    private static final long SLEEP_15_MINUTES = TimeUnit.HOURS.toMillis(1L) / 4L;

    private final boolean useViridian;
    private final String executor;
    private transient HazelcastInstance hazelcastInstance;

    PeriodicQueueWriterRunnable(boolean arg0, String arg1) {
        this.useViridian = arg0;
        this.executor = arg1;
    }

    /**
     * <p>Periodically log something about this member.
     * </p>
     */
    @Override
    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "InterruptedException possible")
    public void run() {
        if (!useViridian) {
            LOGGER.info("**{}**'{}'::START run()", LocalConstants.MY_JAR_NAME, this.executor);
        }

        IQueue<String> iQueue = this.hazelcastInstance.getQueue(MyConstants.QUEUE_NAMESPACE_3);
        String member = LocalUtil.prettyPrintMember(this.hazelcastInstance.getCluster().getLocalMember());
        int count = 0;

        try {
            while (true) {
                String message = String.format(
                        "Message[%06d], sent by %s at %s",
                        ++count, member, LocalDateTime.now().toString()
                        );
                iQueue.put(message);
                LOGGER.info("**{}**'{}'::run() -> put('{}') size=={}",
                        LocalConstants.MY_JAR_NAME, this.executor, message, iQueue.size());

                Thread.sleep(SLEEP_15_MINUTES);
            }
        } catch (HazelcastInstanceNotActiveException hnae) {
            if (!useViridian) {
                LOGGER.info(
                        String.format("**%s**'%s'::HazelcastInstanceNotActiveException run(): %s",
                                LocalConstants.MY_JAR_NAME, this.executor, hnae.getMessage()));
            }
        } catch (InterruptedException ie) {
            if (!useViridian) {
                LOGGER.info(
                        String.format("**%s**'%s'::InterruptedException run(): %s",
                                LocalConstants.MY_JAR_NAME, this.executor, ie.getMessage()));
            }
        } catch (Exception e) {
            if (!useViridian) {
                LOGGER.info(
                        String.format("**%s**'%s'::EXCEPTION run()", LocalConstants.MY_JAR_NAME, this.executor),
                        e);
            }
        }

        if (!useViridian) {
            LOGGER.info("**{}**'{}'::END run()", LocalConstants.MY_JAR_NAME, this.executor);
        }
    }

    @Override
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Hazelcast instance must be shared, not cloned")
    public void setHazelcastInstance(HazelcastInstance arg0) {
        this.hazelcastInstance = arg0;
    }

}
