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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.jet.datamodel.KeyedWindowResult;

/**
 * <p>Test {@link VectorCollectionMomemtsSink}
 * </p>
 */
public class VectorCollectionMomentsSinkTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(VectorCollectionMomentsSinkTest.class);

    private static final long NINETY_SECONDS = 90 * 1000L;
    private static final long ONE_HOUR = 60 * 60 * 1000L;
    private static final long THIRTY_MINUTES = 30 * 60 * 1000L;
    private static final String KEY = "k";
    private static final Long RESULT = 0L;
    private static long baseTime;

    @BeforeAll
    public static void beforeAll() {
        LocalDate today = LocalDate.now();
        String midday = today + "T12:00:00.000000";
        LocalDateTime noon = LocalDateTime.parse(midday);
        baseTime = noon.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        LOGGER.info("baseTime={} ({})", baseTime, noon);
    }

    @Test
    public void testNoon(TestInfo testInfo) throws Exception {
        long start = baseTime;
        long end = start + NINETY_SECONDS;
        KeyedWindowResult<String, Long> keyedWindowResult
            = new KeyedWindowResult<>(start, end, KEY, RESULT);
        int actual = VectorCollectionMomentsSink.findMoment(keyedWindowResult);
        int expected = 0;
        assertEquals(expected, actual);
        LOGGER.info("{}({}): in moment: {}", start, new Date(start), actual);
    }

    @Test
    public void testOnePM(TestInfo testInfo) throws Exception {
        long start = baseTime + ONE_HOUR;
        long end = start + NINETY_SECONDS;
        KeyedWindowResult<String, Long> keyedWindowResult
            = new KeyedWindowResult<>(start, end, KEY, RESULT);
        int actual = VectorCollectionMomentsSink.findMoment(keyedWindowResult);
        int expected = 0;
        assertEquals(expected, actual);
        LOGGER.info("{}({}): in moment: {}", start, new Date(start), actual);
    }

    @Test
    public void testOne30PM(TestInfo testInfo) throws Exception {
        long start = baseTime + ONE_HOUR + THIRTY_MINUTES;
        long end = start + NINETY_SECONDS;
        KeyedWindowResult<String, Long> keyedWindowResult
            = new KeyedWindowResult<>(start, end, KEY, RESULT);
        int actual = VectorCollectionMomentsSink.findMoment(keyedWindowResult);
        int expected = 20;
        assertEquals(expected, actual);
        LOGGER.info("{}({}): in moment: {}", start, new Date(start), actual);
    }

}
