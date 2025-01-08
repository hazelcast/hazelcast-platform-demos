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

package com.hazelcast.platform.demos.retail.clickstream;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>CSV input.
 * </p>
 * <p>
 * <pre>
 * UserID,basket_icon_click,basket_add_list,basket_add_detail,sort_by,image_picker,account_page_click,
 * promo_banner_click,detail_wishlist_add,list_size_dropdown,closed_minibasket_click,
 * checked_delivery_detail,checked_returns_detail,sign_in,saw_checkout,saw_sizecharts,saw_delivery,
 * saw_account_upgrade,saw_homepage,device_mobile,device_computer,device_tablet,returning_user,loc_uk,ordered
 * </pre>
 * </p>
 * <p><b>NOTE</b>Field 25 "{@code checkout}" does not exist in the input data. We have the
 * user id (field 0), then fields (1-23) for actions they may do, and finally field 24 indicates
 * if they bought.
 * </p>
 * <p>We stream these fields, and although "{@code ordered}" is last, it may not be sent if the
 * customer abandons. So we use "{@code checkout}" as a marker that all true values for fields
 * 1-23 have been sent, and can use this to trigger buying prediction, as we need to do this
 * before the "{@code ordered}" action happens (or doesn't happen) in order to generate the
 * custom discount.
 * </p>
 */
public enum CsvField {

    UserID(0),
    basket_icon_click(1),
    basket_add_list(2),
    basket_add_detail(3),
    sort_by(4),
    image_picker(5),
    account_page_click(6),
    promo_banner_click(7),
    detail_wishlist_add(8),
    list_size_dropdown(9),
    closed_minibasket_click(10),
    checked_delivery_detail(11),
    checked_returns_detail(12),
    sign_in(13),
    saw_checkout(14),
    saw_sizecharts(15),
    saw_delivery(16),
    saw_account_upgrade(17),
    saw_homepage(18),
    device_mobile(19),
    device_computer(20),
    device_tablet(21),
    returning_user(22),
    loc_uk(23),
    ordered(24),
    // See comment on class for explanation. Field 25 does not occur in input data.
    checkout(25);

    private static Map<Integer, CsvField> lookup;
    static {
        lookup = Arrays.stream(CsvField.values())
        .collect(Collectors.<CsvField, Integer, CsvField>toUnmodifiableMap(
                csvField -> csvField.i,
                csvField -> csvField));
    }

    private int i;

    CsvField(int arg0) {
        this.i = arg0;
    }

    static CsvField valueOfCsvField(int arg0) {
        return lookup.get(arg0);
    }

    String getCsvField() {
        return String.valueOf(this.i);
    }

}

