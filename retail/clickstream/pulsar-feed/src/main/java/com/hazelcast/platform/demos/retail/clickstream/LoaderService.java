/*
 * Copyright (c) 2008-2021, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.platform.demos.retail.clickstream;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.Schema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.datamodel.Tuple4;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Writes a CSV file to Pulsar.
 * </p>
 * <p>CSV looks like:
 * <pre>
 * UserID,basket_icon_click,basket_add_list,basket_add_detail,sort_by,image_picker,account_page_click,
 * promo_banner_click,detail_wishlist_add,list_size_dropdown,closed_minibasket_click,
 * checked_delivery_detail,checked_returns_detail,sign_in,saw_checkout,saw_sizecharts,saw_delivery,
 * saw_account_upgrade,saw_homepage,device_mobile,device_computer,device_tablet,returning_user,loc_uk,ordered
 * </pre>
 * </p>
 */
@Service
@Slf4j
public class LoaderService {

    private static final int BLOCK_SIZE = 100;
    private static final int EXPECTED_CSV_LENGTH = 25;
    private static final int CSV_KEY_POS = 0;
    private static final int CSV_DATA_START_POS = 1;
    private static final int CSV_DATA_END_POS = 23;
    private static final int CSV_BUY_POS = 24;
    private static final int INDICATE_CSV_CHECKOUT = 25;

    @Autowired
    private ApplicationContext applicationContext;
    private int errors;
    private int read;
    private int skipped;
    private int written;
    private ExponentialLogger[] exponentialLoggers = new ExponentialLogger[3];
    private Random random = new Random();

    private void init(String fileName) {
        this.errors = 0;
        this.read = 0;
        this.skipped = 0;
        this.written = 0;
        this.exponentialLoggers[0] = new ExponentialLogger(fileName, "action");
        this.exponentialLoggers[1]  = new ExponentialLogger(fileName, "checkout");
        this.exponentialLoggers[2]  = new ExponentialLogger(fileName, "buy");
    }

    /**
     * <p>Write scan for input file, with periodic logging.
     * </p>
     *
     * @param fileName In "src/main/resources"
     * @param producer An existing connection to Pulsar
     */
    public Tuple2<Integer, String> readCsvWritePulsar(String fileName, Producer<String> producer) {
        // Reset for multiple invocations.
        this.init(fileName);

        Resource resource = this.applicationContext.getResource("classpath:" + fileName);
        try (BufferedReader bufferedReader =
                new BufferedReader(
                        new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

            List<Tuple4<String, List<String>, List<String>, List<String>>> data =
                    this.readDataBlock(bufferedReader);
            while (!data.isEmpty()) {
                this.processDataBlock(data, producer);
                data = this.readDataBlock(bufferedReader);
            }
        } catch (Exception e) {
            this.errors++;
            log.error("Processing {}, read {} ... {}", fileName, this.read, e.getMessage());
            log.error(fileName, e);
        }

        String message;
        if (this.errors == 0) {
            message = String.format("File '%s', read %d lines, skipped %d, wrote %d entries",
                    fileName, this.read, this.skipped, this.written);
        } else {
            message = String.format("File '%s', read %d lines, skipped %d, wrote %d entries *** with %d errors ***",
                    fileName, this.read, this.skipped, this.written, this.errors);
        }
        return Tuple2.tuple2(this.written, message);
    }

    /**
     * <p>Read a bundle of CSV lines, parse into actions present.
     * </p>
     *
     * @param bufferedReader
     * @return
     */
    private List<Tuple4<String, List<String>, List<String>, List<String>>>
        readDataBlock(BufferedReader bufferedReader) throws IOException {
        List<Tuple4<String, List<String>, List<String>, List<String>>> data = new ArrayList<>();

        int count = 0;
        while (count < BLOCK_SIZE) {
            String line = bufferedReader.readLine();
            if (line != null) {
                this.read++;
                if (line.startsWith("#") || line.startsWith(CsvField.valueOfCsvField(0).toString())
                        || line.startsWith("-")) {
                    this.skipped++;
                    log.trace("Skip due to prefix: {}", line);
                } else {
                    Tuple4<String, List<String>, List<String>, List<String>> datum = parse(line);
                    if (datum == null) {
                        this.skipped++;
                        log.trace("Skip due to content: {}", line);
                    } else {
                        count++;
                        data.add(datum);
                    }
                }
            } else {
                break;
            }
        }
        return data;
    }

    /**
     * <p>Turn the clickstream summary into three lists of actions, to be viewed
     * as one happening after the other. The first list is the actions a user
     * may do. The second is going to checkout, which is make explicit instead
     * of applied. The third is buying, which not all users may do.
     * </p>
     * <p>Three lists, although the last two are singleton lists, make processing
     * simpler.
     * </p>
     *
     * @param line
     * @return
     */
    private Tuple4<String, List<String>, List<String>, List<String>> parse(String line) {
        String[] tokens = line.split(",");
        if (tokens.length != EXPECTED_CSV_LENGTH) {
            return null;
        }

        String key = tokens[CSV_KEY_POS];

        List<String> actions = new ArrayList<>();
        for (int i = CSV_DATA_START_POS; i <= CSV_DATA_END_POS; i++) {
            if (tokens[i].equals("1")) {
                actions.add(CsvField.valueOfCsvField(i).toString());
            }
        }

        // Can't use List.of, want mutable list
        List<String> checkout = new ArrayList<>();
        checkout.add(CsvField.valueOfCsvField(INDICATE_CSV_CHECKOUT).toString());

        List<String> buy = new ArrayList<>();
        if (tokens[CSV_BUY_POS].equals("1")) {
            buy.add(CsvField.valueOfCsvField(CSV_BUY_POS).toString());
        }

        return Tuple4.tuple4(key, actions, checkout, buy);
    }

    /**
     * <p>Randomly select one line in the block, output an item from it. If that line is
     * exhausted, remove it from the block. Repeat until the entire block is output.
     * </p>
     *
     * @param block
     * @param producer
     */
    @SuppressFBWarnings(value = "DMI_RANDOM_USED_ONLY_ONCE",
            justification = "Wish single random sequence, each time fillBuffernFn() is called")
    private void processDataBlock(List<Tuple4<String, List<String>, List<String>, List<String>>> block,
            Producer<String> producer) throws Exception {
        int size = block.size();
        while (size > 0) {
            int i = random.nextInt(size);
            Tuple4<String, List<String>, List<String>, List<String>> tuple4 =
                    block.get(i);
            processDataRow(tuple4, producer);
            // Remove when all rows written
            if (tuple4.f1().size() == 0 && tuple4.f2().size() == 0 && tuple4.f3().size() == 0) {
                block.remove(i);
            }
            size = block.size();
        }
    }

    /**
     * <p>Process an item from first list until empty, second list then third list of actions.
     * </p>
     *
     * @param tuple4
     * @param producer
     */
    private void processDataRow(Tuple4<String, List<String>, List<String>, List<String>> tuple4,
            Producer<String> producer) throws Exception {
        if (tuple4.f1().size() > 0) {
            processDataItem(tuple4.f0(), tuple4.f1(), this.exponentialLoggers[0], producer);
        } else {
            if (tuple4.f2().size() > 0) {
                processDataItem(tuple4.f0(), tuple4.f2(), this.exponentialLoggers[1], producer);
            } else {
                if (tuple4.f3().size() > 0) {
                    processDataItem(tuple4.f0(), tuple4.f3(), this.exponentialLoggers[2], producer);
                }
            }
        }
    }


    /**
     * <p>Write any one value from the list of actions, randomly if more than one available
     * </p>
     *
     * @param key
     * @param actions
     * @param exponentialLogger
     * @param producer
     */
    @SuppressFBWarnings(value = "DMI_RANDOM_USED_ONLY_ONCE",
            justification = "Wish single random sequence, each time fillBuffernFn() is called")
    private void processDataItem(String key, List<String> actions, ExponentialLogger exponentialLogger,
            Producer<String> producer) throws Exception {
        int size = actions.size();
        String value;
        if (size == 1) {
            value = actions.get(0);
            actions.remove(0);
        } else {
            int i = random.nextInt(size);
            value = actions.get(i);
            actions.remove(i);
        }

        producer
        .newMessage(Schema.STRING)
        .key(key)
        .value(value)
        .send();

        this.written++;
        exponentialLogger.log(this.read, this.written, key, value);
    }
}
