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
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;
import com.hazelcast.core.HazelcastInstanceNotActiveException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * <p>Determines if the server is enterprise or open source.
 * </p>
 */
public class EnterpriseChecker implements Callable<Boolean>, Serializable, HazelcastInstanceAware {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(EnterpriseChecker.class);

    private final boolean useViridian;
    private transient HazelcastInstance hazelcastInstance;

    EnterpriseChecker(boolean arg0) {
        this.useViridian = arg0;
    }
    /**
     * <p>Determine if the server supports enterprise features.
     * </p>
     *
     * @return Null on error, {@code true} for enterprise, {@code false} for open source.
     */
    @Override
    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "InterruptedException possible")
    public Boolean call() {
        Boolean result = null;
        if (!useViridian) {
            LOGGER.info("START call()");
        }

        try {
            String licenseKeyConfig = this.hazelcastInstance.getConfig().getLicenseKey();
            if (licenseKeyConfig == null || licenseKeyConfig.isBlank()) {
                result = false;
            } else {
                result = true;
            }
        } catch (HazelcastInstanceNotActiveException hnae) {
            if (!useViridian) {
                LOGGER.info("HazelcastInstanceNotActiveException run(): {}", hnae.getMessage());
            }
        } catch (Exception e) {
            if (!useViridian) {
                LOGGER.info("EXCEPTION call()", e);
            }
        }

        if (!useViridian) {
            LOGGER.info("END call() -> {}", result);
        }
        return result;
    }

    @Override
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Hazelcast instance must be shared, not cloned")
    public void setHazelcastInstance(HazelcastInstance arg0) {
        this.hazelcastInstance = arg0;
    }

}
