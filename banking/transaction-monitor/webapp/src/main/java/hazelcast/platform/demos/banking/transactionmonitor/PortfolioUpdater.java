/*
 * Copyright (c) 2008-2022, Hazelcast, Inc. All Rights Reserved.
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

import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

/**TODO Move to {@code transaction-producer} module?
 *FIXME Add SKIN based change
 */
public class PortfolioUpdater implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(PortfolioUpdater.class);
    private static final Long FIFTY = 50L;
    private static final int MINUS_TEN = -10;
    private static final int TEN = 10;

    private final IMap<String, Portfolio> portfolioMap;
    private final String[] stocks;
    private int binaryLoggingInterval = 1;
    private int count;

    public PortfolioUpdater(HazelcastInstance hazelcastInstance) {
        this.portfolioMap = hazelcastInstance.getMap(MyConstants.IMAP_NAME_PORTFOLIOS);
        this.stocks = new TreeSet<>(MyConstants.SAMPLE_STOCKS).toArray(new String[0]);
    }

    /**
     * <p>Randomly update a portfolio periodically.
     * </p>
     */
    @Override
    public void run() {
        LOGGER.info("START run()");
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Portfolio portfolio = new Portfolio();

        // Init
        for (int i = 0; i < this.stocks.length; i++) {
            portfolio.setStock(this.stocks[i]);
            this.portfolioMap.set(portfolio.getStock(), portfolio);
            this.logExponentially(portfolio);
        }

        // Randomly update
        while (true) {
            try {
                int i = random.nextInt(this.stocks.length);
                int sold = random.nextInt(MINUS_TEN, 0);
                int bought = random.nextInt(0, TEN);
                int change = bought - sold;

                portfolio.setStock(this.stocks[i]);
                portfolio.setSold(sold);
                portfolio.setBought(bought);
                portfolio.setChange(change);

                this.portfolioMap.set(portfolio.getStock(), portfolio);
                this.logExponentially(portfolio);

                TimeUnit.MILLISECONDS.sleep(FIFTY);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                LOGGER.info("EXCEPTION run()", e);
                break;
            }
        }
        LOGGER.info("END run()");
    }

    /**
     * <p>Log less and less frequently.
     * </p>
     *
     * @param portfolio
     */
    private void logExponentially(Portfolio portfolio) {
        if (this.count % this.binaryLoggingInterval == 0) {
            LOGGER.debug("Write '{}' -> key '{}', value '{}'", this.count, portfolio.getStock(), portfolio);
            if (2 * this.binaryLoggingInterval <= MyConstants.MAX_LOGGING_INTERVAL) {
                this.binaryLoggingInterval = 2 * this.binaryLoggingInterval;
            }
        }
        this.count++;
    }

}
