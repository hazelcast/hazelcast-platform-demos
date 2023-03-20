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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import com.hazelcast.jet.datamodel.Tuple2;

/**
 * <p>Test the currency conversion for obvious scenarios.
 * </p>
 */
public class MyUtilsCurrencyTest {

    @Test
    public void testNullCountry(TestInfo testInfo) {
        String country = null;
        assertThrows(RuntimeException.class, () -> {
            MyUtils.getCurrency(country);
            });
    }

    @Test
    public void testEmptyCountry(TestInfo testInfo) {
        String country = "";
        assertThrows(RuntimeException.class, () -> {
            MyUtils.getCurrency(country);
            });
    }

    @Test
    public void testKnownCountry(TestInfo testInfo) throws Exception {
        // Swtizerland
        String country = "CH";
        // Swiss Franc
        String expected = "CHF";
        Tuple2<String, Double> actual = MyUtils.getCurrency(country);
        assertEquals(expected, actual.f0());
    }

    @Test
    public void testUnknownCountry(TestInfo testInfo) {
        String country = "Narnia";
        assertThrows(RuntimeException.class, () -> {
            MyUtils.getCurrency(country);
            });
    }

}
