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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Properties;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * <p>Test the enumeration lookup.
 * </p>
 */
public class MyUtilsTransactionMonitorFlavorTest {

    @Test
    public void testNullProperty(TestInfo testInfo) {
        Properties properties = null;

        assertThrows(RuntimeException.class, () -> {
            MyUtils.getTransactionMonitorFlavor(properties);
            });
    }

    @Test
    public void testNoProperty(TestInfo testInfo) {
        Properties properties = new Properties();

        assertThrows(RuntimeException.class, () -> {
            MyUtils.getTransactionMonitorFlavor(properties);
            });
    }

    @Test
    public void testBlankProperty(TestInfo testInfo) {
        Properties properties = new Properties();
        properties.put(MyConstants.TRANSACTION_MONITOR_FLAVOR, "");

        assertThrows(RuntimeException.class, () -> {
            MyUtils.getTransactionMonitorFlavor(properties);
            });
    }

    // Defend against someone adding "unknown" as a type.
    @Test
    public void testUnknownProperty(TestInfo testInfo) {
        Properties properties = new Properties();
        properties.put(MyConstants.TRANSACTION_MONITOR_FLAVOR, "unknown-" + UUID.randomUUID().toString());

        assertThrows(RuntimeException.class, () -> {
            MyUtils.getTransactionMonitorFlavor(properties);
            });
    }

    @Test
    public void testCorrectProperty(TestInfo testInfo) throws Exception {
        Properties properties = new Properties();
        properties.put(MyConstants.TRANSACTION_MONITOR_FLAVOR, "ecommerce");
        TransactionMonitorFlavor output = MyUtils.getTransactionMonitorFlavor(properties);
        TransactionMonitorFlavor expected = TransactionMonitorFlavor.ECOMMERCE;

        assertEquals(expected, output);
    }

}
