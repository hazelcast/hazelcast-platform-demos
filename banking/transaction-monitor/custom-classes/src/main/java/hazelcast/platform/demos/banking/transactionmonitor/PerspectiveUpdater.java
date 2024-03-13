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
import java.time.LocalTime;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;
import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.map.IMap;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/** Produce data updates for the FINOS Module.
 * Use an executable to show a batch style approached, still valid, although
 * this really would be a streaming use-case.
 */
public class PerspectiveUpdater implements Runnable, Serializable, HazelcastInstanceAware {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(PerspectiveUpdater.class);
    private static final Long FIFTY = 50L;
    private static final int TWENTY = 20;
    private static final int SIXTY = 60;
    private static final Random RANDOM = new Random();

    private final TransactionMonitorFlavor transactionMonitorFlavor;
    private final boolean useViridian;
    private transient HazelcastInstance hazelcastInstance;
    private int binaryLoggingInterval = 1;
    private int count;

    PerspectiveUpdater(TransactionMonitorFlavor arg0, boolean arg1) {
        this.transactionMonitorFlavor = arg0;
        this.useViridian = arg1;
    }
    /**
     * <p>Randomly update data for FINOS Perspective periodically.
     * </p>
     */
    @Override
    public void run() {
        if (!useViridian) {
            LOGGER.info("START run()");
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();

        String[] selectedKeys = this.getSelectedKeys();

        IMap<String, Tuple3<Long, Double, Double>> sourceMap =
                this.hazelcastInstance.getMap(MyConstants.IMAP_NAME_AGGREGATE_QUERY_RESULTS);
        IMap<String, Object> targetMap =
                this.hazelcastInstance.getMap(MyConstants.IMAP_NAME_PERSPECTIVE);

        // Init
        for (int i = 0; i < selectedKeys.length; i++) {
            Object object = this.etl(selectedKeys[i], sourceMap, targetMap);
            if (!useViridian) {
                this.logExponentially(object);
            }
        }

        // Randomly update
        while (true) {
            try {
                int i = random.nextInt(selectedKeys.length);

                Object object = this.etl(selectedKeys[i], sourceMap, targetMap);
                this.logExponentially(object);

                TimeUnit.MILLISECONDS.sleep(FIFTY);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                if (!useViridian) {
                    LOGGER.info("EXCEPTION run()", e);
                }
                break;
            }
        }
        if (!useViridian) {
            LOGGER.info("END run()");
        }
    }

    /**
     * <p>Find the keys of the first 20 of the data.
     * </p>
     *
     * @return
     */
    private String[] getSelectedKeys() {
        Set<Object> keySet;
        switch (transactionMonitorFlavor) {
        case ECOMMERCE:
            keySet = this.hazelcastInstance.getMap(MyConstants.IMAP_NAME_PRODUCTS).keySet();
            break;
        case PAYMENTS:
            keySet = this.hazelcastInstance.getMap(MyConstants.IMAP_NAME_BICS).keySet();
            break;
        case TRADE:
        default:
            keySet = this.hazelcastInstance.getMap(MyConstants.IMAP_NAME_SYMBOLS).keySet();
            break;
        }

        Set<String> keySetSorted = new TreeSet<>();
        Iterator<Object> iterator = keySet.iterator();
        while (iterator.hasNext()) {
            keySetSorted.add(iterator.next().toString());
        }

        String[] result = new String[TWENTY];

        Iterator<String> iterator2 = keySetSorted.iterator();
        for (int i = 0; i < result.length; i++) {
            if (iterator2.hasNext()) {
                result[i] = iterator2.next();
            } else {
                result[i] = "";
            }
        }

        return result;
    }

    /**
     * <p>Copy and reformat from one map to another. A pipeline would be
     * another way to do so, if we don't wish to control the frequency
     * of updating.
     * </p>
     *
     * @param key
     * @param sourceMap
     * @param targetMap
     * @return
     */
    private Object etl(String key, IMap<String, Tuple3<Long, Double, Double>> sourceMap,
            IMap<String, Object> targetMap) {
        Tuple3<Long, Double, Double> tuple3 = sourceMap.get(key);

        LocalTime now = LocalTime.now();
        int seconds = now.getMinute() * SIXTY + now.getSecond();
        int random = RANDOM.nextInt(TWENTY);

        Object value;
        switch (transactionMonitorFlavor) {
        case ECOMMERCE:
            PerspectiveEcommerce perspectiveEcommerce = new PerspectiveEcommerce();
            perspectiveEcommerce.setCode(key);
            perspectiveEcommerce.setSeconds(seconds);
            perspectiveEcommerce.setRandom(random);
            if (tuple3 != null) {
                perspectiveEcommerce.setCount(tuple3.f0());
                perspectiveEcommerce.setSum(tuple3.f1());
                perspectiveEcommerce.setAverage(tuple3.f2());
            }
            value = perspectiveEcommerce;
            break;
        case PAYMENTS:
            PerspectivePayments perspectivePayments = new PerspectivePayments();
            perspectivePayments.setBic(key);
            perspectivePayments.setSeconds(seconds);
            perspectivePayments.setRandom(random);
            if (tuple3 != null) {
                perspectivePayments.setCount(tuple3.f0());
                perspectivePayments.setSum(tuple3.f1());
                perspectivePayments.setAverage(tuple3.f2());
            }
            value = perspectivePayments;
            break;
        case TRADE:
        default:
            PerspectiveTrade perspectiveTrade = new PerspectiveTrade();
            perspectiveTrade.setSymbol(key);
            perspectiveTrade.setSeconds(seconds);
            perspectiveTrade.setRandom(random);
            if (tuple3 != null) {
                perspectiveTrade.setCount(tuple3.f0());
                perspectiveTrade.setSum(tuple3.f1());
                perspectiveTrade.setLatest(tuple3.f2());
            }
            value = perspectiveTrade;
            break;
        }

        targetMap.set(key, value);
        return value;
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
