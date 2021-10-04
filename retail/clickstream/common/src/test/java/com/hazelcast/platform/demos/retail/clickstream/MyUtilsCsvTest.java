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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Junit 5
 * </p>
 */
@Slf4j
public class MyUtilsCsvTest {

    private static final String KEY = MyUtilsCsvTest.class.getSimpleName();

    @Test
    public void testCsvToBinaryNoActionsWoKeyWithOrdered(TestInfo testInfo) throws Exception {
        String input = "";
        String[] arr = MyUtils.digitalTwinCsvToBinary(null, input, true);
        String output = Arrays.toString(arr);
        String expected = "[0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]";
        log.info("{} :: input=='{}', output=='{}' ({})", testInfo.getDisplayName(), input, output, arr.length);

        assertNotNull(output);
        int commas = output.split(",").length;
        assertEquals(24, commas);
        assertEquals(expected, output);
    }
    @Test
    public void testCsvToBinaryNoActionsWoKeyWoOrdered(TestInfo testInfo) throws Exception {
        String input = "";
        String[] arr = MyUtils.digitalTwinCsvToBinary(null, input, false);
        String output = Arrays.toString(arr);
        String expected = "[0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]";
        log.info("{} :: input=='{}', output=='{}' ({})", testInfo.getDisplayName(), input, output, arr.length);

        assertNotNull(output);
        int commas = output.split(",").length;
        assertEquals(23, commas);
        assertEquals(expected, output);
    }
    @Test
    public void testCsvToBinaryNoActionsWithKeyWithOrdered(TestInfo testInfo) throws Exception {
        String input = "";
        String[] arr = MyUtils.digitalTwinCsvToBinary(KEY, input, true);
        String output = Arrays.toString(arr);
        String expected = "[" + KEY + ", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]";
        log.info("{} :: input=='{}', output=='{}' ({})", testInfo.getDisplayName(), input, output, arr.length);

        assertNotNull(output);
        int commas = output.split(",").length;
        assertEquals(25, commas);
        assertEquals(expected, output);
    }
    @Test
    public void testCsvToBinaryNoActionsWithKeyWoOrdered(TestInfo testInfo) throws Exception {
        String input = "";
        String[] arr = MyUtils.digitalTwinCsvToBinary(KEY, input, false);
        String output = Arrays.toString(arr);
        String expected = "[" + KEY + ", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]";
        log.info("{} :: input=='{}', output=='{}' ({})", testInfo.getDisplayName(), input, output, arr.length);

        assertNotNull(output);
        int commas = output.split(",").length;
        assertEquals(24, commas);
        assertEquals(expected, output);
    }

    @Test
    public void testCsvToBinarySomeActionsWoKeyWithOrdered(TestInfo testInfo) throws Exception {
        String input = "basket_add_list,"
                + "sort_by,"
                + "account_page_click,"
                + "detail_wishlist_add,"
                + "closed_minibasket_click,"
                + "checked_returns_detail,"
                + "saw_checkout,"
                + "saw_delivery,"
                + "saw_homepage,"
                + "device_computer,"
                + "returning_user,"
                + "ordered";
        String[] arr = MyUtils.digitalTwinCsvToBinary(null, input, true);
        String output = Arrays.toString(arr);
        String expected = "[0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1]";
        log.info("{} :: input=='{}', output=='{}' ({})", testInfo.getDisplayName(), input, output, arr.length);

        assertNotNull(output);
        int commas = output.split(",").length;
        assertEquals(24, commas);
        assertEquals(expected, output);
    }
    @Test
    public void testCsvToBinarySomeActionsWoKeyWoOrdered(TestInfo testInfo) throws Exception {
        String input = "basket_add_list,"
                + "sort_by,"
                + "account_page_click,"
                + "detail_wishlist_add,"
                + "closed_minibasket_click,"
                + "checked_returns_detail,"
                + "saw_checkout,"
                + "saw_delivery,"
                + "saw_homepage,"
                + "device_computer,"
                + "returning_user,"
                + "ordered";
        String[] arr = MyUtils.digitalTwinCsvToBinary(null, input, false);
        String output = Arrays.toString(arr);
        String expected = "[0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0]";
        log.info("{} :: input=='{}', output=='{}' ({})", testInfo.getDisplayName(), input, output, arr.length);

        assertNotNull(output);
        int commas = output.split(",").length;
        assertEquals(23, commas);
        assertEquals(expected, output);
    }
    @Test
    public void testCsvToBinarySomeActionsWithKeyWithOrdered(TestInfo testInfo) throws Exception {
        String input = "basket_add_list,"
                + "sort_by,"
                + "account_page_click,"
                + "detail_wishlist_add,"
                + "closed_minibasket_click,"
                + "checked_returns_detail,"
                + "saw_checkout,"
                + "saw_delivery,"
                + "saw_homepage,"
                + "device_computer,"
                + "returning_user";
        String[] arr = MyUtils.digitalTwinCsvToBinary(KEY, input, true);
        String output = Arrays.toString(arr);
        String expected = "[" + KEY + ", 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0]";
        log.info("{} :: input=='{}', output=='{}' ({})", testInfo.getDisplayName(), input, output, arr.length);

        assertNotNull(output);
        int commas = output.split(",").length;
        assertEquals(25, commas);
        assertEquals(expected, output);
    }
    @Test
    public void testCsvToBinarySomeActionsWithKeyWoOrdered(TestInfo testInfo) throws Exception {
        String input = "basket_add_list,"
                + "sort_by,"
                + "account_page_click,"
                + "detail_wishlist_add,"
                + "closed_minibasket_click,"
                + "checked_returns_detail,"
                + "saw_checkout,"
                + "saw_delivery,"
                + "saw_homepage,"
                + "device_computer,"
                + "returning_user,"
                + "ordered";
        String[] arr = MyUtils.digitalTwinCsvToBinary(KEY, input, false);
        String output = Arrays.toString(arr);
        String expected = "[" + KEY + ", 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0]";
        log.info("{} :: input=='{}', output=='{}' ({})", testInfo.getDisplayName(), input, output, arr.length);

        assertNotNull(output);
        int commas = output.split(",").length;
        assertEquals(24, commas);
        assertEquals(expected, output);
    }

    @Test
    public void testCsvToBinaryAllActionsWoKeyWithOrdered(TestInfo testInfo) throws Exception {
        String input = "basket_icon_click,"
                + "basket_add_list,"
                + "basket_add_detail,"
                + "sort_by,"
                + "image_picker,"
                + "account_page_click,"
                + "promo_banner_click,"
                + "detail_wishlist_add,"
                + "list_size_dropdown,"
                + "closed_minibasket_click,"
                + "checked_delivery_detail,"
                + "checked_returns_detail,"
                + "sign_in,"
                + "saw_checkout,"
                + "saw_sizecharts,"
                + "saw_delivery,"
                + "saw_account_upgrade,"
                + "saw_homepage,"
                + "device_mobile,"
                + "device_computer,"
                + "device_tablet,"
                + "returning_user,"
                + "loc_uk,"
                + "ordered";
        String[] arr = MyUtils.digitalTwinCsvToBinary(null, input, true);
        String output = Arrays.toString(arr);
        String expected = "[1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1]";
        log.info("{} :: input=='{}', output=='{}' ({})", testInfo.getDisplayName(), input, output, arr.length);

        assertNotNull(output);
        int commas = output.split(",").length;
        assertEquals(24, commas);
        assertEquals(expected, output);
    }
    @Test
    public void testCsvToBinaryAllActionsWoKeyWoOrdered(TestInfo testInfo) throws Exception {
        String input = "basket_icon_click,"
                + "basket_add_list,"
                + "basket_add_detail,"
                + "sort_by,"
                + "image_picker,"
                + "account_page_click,"
                + "promo_banner_click,"
                + "detail_wishlist_add,"
                + "list_size_dropdown,"
                + "closed_minibasket_click,"
                + "checked_delivery_detail,"
                + "checked_returns_detail,"
                + "sign_in,"
                + "saw_checkout,"
                + "saw_sizecharts,"
                + "saw_delivery,"
                + "saw_account_upgrade,"
                + "saw_homepage,"
                + "device_mobile,"
                + "device_computer,"
                + "device_tablet,"
                + "returning_user,"
                + "loc_uk,"
                + "ordered";
        String[] arr = MyUtils.digitalTwinCsvToBinary(null, input, false);
        String output = Arrays.toString(arr);
        String expected = "[1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1]";
        log.info("{} :: input=='{}', output=='{}' ({})", testInfo.getDisplayName(), input, output, arr.length);

        assertNotNull(output);
        int commas = output.split(",").length;
        assertEquals(23, commas);
        assertEquals(expected, output);
    }
    @Test
    public void testCsvToBinaryAllActionsWithKeyWithOrdered(TestInfo testInfo) throws Exception {
        String input = "basket_icon_click,"
                + "basket_add_list,"
                + "basket_add_detail,"
                + "sort_by,"
                + "image_picker,"
                + "account_page_click,"
                + "promo_banner_click,"
                + "detail_wishlist_add,"
                + "list_size_dropdown,"
                + "closed_minibasket_click,"
                + "checked_delivery_detail,"
                + "checked_returns_detail,"
                + "sign_in,"
                + "saw_checkout,"
                + "saw_sizecharts,"
                + "saw_delivery,"
                + "saw_account_upgrade,"
                + "saw_homepage,"
                + "device_mobile,"
                + "device_computer,"
                + "device_tablet,"
                + "returning_user,"
                + "loc_uk,"
                + "ordered";
        String[] arr = MyUtils.digitalTwinCsvToBinary(KEY, input, true);
        String output = Arrays.toString(arr);
        String expected = "[" + KEY + ", 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1]";
        log.info("{} :: input=='{}', output=='{}' ({})", testInfo.getDisplayName(), input, output, arr.length);

        assertNotNull(output);
        int commas = output.split(",").length;
        assertEquals(25, commas);
        assertEquals(expected, output);
    }
    @Test
    public void testCsvToBinaryAllActionsWithKeyWoOrdered(TestInfo testInfo) throws Exception {
        String input = "basket_icon_click,"
                + "basket_add_list,"
                + "basket_add_detail,"
                + "sort_by,"
                + "image_picker,"
                + "account_page_click,"
                + "promo_banner_click,"
                + "detail_wishlist_add,"
                + "list_size_dropdown,"
                + "closed_minibasket_click,"
                + "checked_delivery_detail,"
                + "checked_returns_detail,"
                + "sign_in,"
                + "saw_checkout,"
                + "saw_sizecharts,"
                + "saw_delivery,"
                + "saw_account_upgrade,"
                + "saw_homepage,"
                + "device_mobile,"
                + "device_computer,"
                + "device_tablet,"
                + "returning_user,"
                + "loc_uk,"
                + "ordered";
        String[] arr = MyUtils.digitalTwinCsvToBinary(KEY, input, false);
        String output = Arrays.toString(arr);
        String expected = "[" + KEY + ", 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1]";
        log.info("{} :: input=='{}', output=='{}' ({})", testInfo.getDisplayName(), input, output, arr.length);

        assertNotNull(output);
        int commas = output.split(",").length;
        assertEquals(24, commas);
        assertEquals(expected, output);
    }

}
