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

package com.hazelcast.platform.demos.banking.cva;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.map.IMap;

import hazelcast.platform.demos.utils.CheckConnectIdempotentCallable;

/**
 * <p>Initialization that can be run by a client or a server.
 * Each should run this (as it's idempotent) to be sure necessary
 * objects exist before their first use.
 * <p>
 */
public class CommonIdempotentInitialization {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommonIdempotentInitialization.class);

    /**
     * <p>Full version of initialization. In the future may wish a partial
     * initialize, such as just mappings but not anything dynamically
     * configurable such as an {@link com.hazelcast.map.IMap IMap}.
     * </p>
     *
     * @param hazelcastInstance
     * @throws Exception -- from checks
     */
    public static void fullInitialize(HazelcastInstance hazelcastInstance) throws Exception {
        //@throws Exception
        CheckConnectIdempotentCallable.silentCheckCustomClasses(hazelcastInstance);

        createNeededMappingsAndViews(hazelcastInstance);
        createNeededObjects(hazelcastInstance);
    }

    /**
     * <p>Without this metadata, cannot query an empty
     * {@link IMap}.
     * </p>
     */
    public static void createNeededMappingsAndViews(HazelcastInstance hazelcastInstance) {
        String definition1 = defineIMapCpCds();
        String definition1v = defineIMapCpCdsView();
        String definition2 = defineIMapFixings();
        String definition2v = defineIMapFixingsView();
        String definition3 = defineIMapIrCurves();
        String definition3v = defineIMapIrCurvesView();
        String definition4 = defineIMapTrades();
        String definition5 = defineIMapPosition();
        String definition6 = defineIMapRisk();
        String definition7 = defineIMapStock();

        define(hazelcastInstance, definition1);
        define(hazelcastInstance, definition1v);
        define(hazelcastInstance, definition2);
        define(hazelcastInstance, definition2v);
        define(hazelcastInstance, definition3);
        define(hazelcastInstance, definition3v);
        define(hazelcastInstance, definition4);
        define(hazelcastInstance, definition5);
        define(hazelcastInstance, definition6);
        define(hazelcastInstance, definition7);
    }

    /**
     * <p>Objects such as maps are created on-demand in Hazelcast.
     * Touch all the one we'll need to be sure they exist in advance,
     * this doesn't change their behaviour but is useful for reporting.
     * </p>
     */
    public static void createNeededObjects(HazelcastInstance hazelcastInstance) {
        for (String iMapName : MyConstants.IMAP_NAMES) {
            hazelcastInstance.getMap(iMapName);
        }
        for (String iTopicName : MyConstants.ITOPIC_NAMES) {
            hazelcastInstance.getTopic(iTopicName);
        }
    }

    /**
     * <p>Describe JSON for CP CDS
     * </p>
     *
     * @return
     */
    private static String defineIMapCpCds() {
        return "CREATE OR REPLACE MAPPING "
                + MyConstants.IMAP_NAME_CP_CDS
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = '" + String.class.getCanonicalName() + "',"
                + " 'valueFormat' = 'json'"
                + " )";
    }
    private static String defineIMapCpCdsView() {
        return "CREATE OR REPLACE VIEW "
                + MyConstants.IMAP_NAME_CP_CDS + MyConstants.VIEW_SUFFIX
                + " AS SELECT "
                + "    __key"
                + "      AS \"primary_key\""
                + ",   JSON_VALUE(this, '$.date' RETURNING VARCHAR)"
                + "      AS \"date\""
                + ",   JSON_VALUE(this, '$.timezone' RETURNING VARCHAR)"
                + "      AS \"timezone\""
                + ",   JSON_VALUE(this, '$.ticker' RETURNING VARCHAR)"
                + "      AS \"ticker\""
                + ",   JSON_VALUE(this, '$.shortname' RETURNING VARCHAR)"
                + "      AS \"shortname\""
                + ",   JSON_VALUE(this, '$.redcode' RETURNING VARCHAR)"
                + "      AS \"redcode\""
                + ",   JSON_VALUE(this, '$.tier' RETURNING VARCHAR)"
                + "      AS \"tier\""
                + ",   JSON_VALUE(this, '$.ccy' RETURNING VARCHAR)"
                + "      AS \"ccy\""
                + ",   JSON_VALUE(this, '$.docclause' RETURNING VARCHAR)"
                + "      AS \"docclause\""
                + ",   JSON_VALUE(this, '$.spread_periods[0]' RETURNING DOUBLE)"
                + "      AS \"spread_periods_0\""
                + ",   JSON_VALUE(this, '$.spreads[0]' RETURNING DOUBLE)"
                + "      AS \"spreads_0\""
                + ",   JSON_QUERY(this, '$.spread_periods' WITH ARRAY WRAPPER)"
                + "      AS \"spread_periods\""
                + ",   JSON_QUERY(this, '$.spreads' WITH ARRAY WRAPPER)"
                + "      AS \"spreads\""
                + ",   JSON_VALUE(this, '$.recovery' RETURNING DOUBLE)"
                + "      AS \"recovery\""
                + ",   JSON_VALUE(this, '$.datarating' RETURNING VARCHAR)"
                + "      AS \"datarating\""
                + ",   JSON_VALUE(this, '$.sector' RETURNING VARCHAR)"
                + "      AS \"sector\""
                + ",   JSON_VALUE(this, '$.region' RETURNING VARCHAR)"
                + "      AS \"region\""
                + ",   JSON_VALUE(this, '$.country' RETURNING VARCHAR)"
                + "      AS \"country\""
                + ",   JSON_VALUE(this, '$.avrating' RETURNING VARCHAR)"
                + "      AS \"avrating\""
                + ",   JSON_VALUE(this, '$.impliedrating' RETURNING VARCHAR)"
                + "      AS \"impliedrating\""
                + " FROM " + MyConstants.IMAP_NAME_CP_CDS;
    }

    /**
     * <p>Describe JSON for Fixings
     * </p>
     *
     * @return
     */
    private static String defineIMapFixings() {
        return "CREATE OR REPLACE MAPPING "
                + MyConstants.IMAP_NAME_FIXINGS
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = '" + String.class.getCanonicalName() + "',"
                + " 'valueFormat' = 'json'"
                + " )";
    }
    private static String defineIMapFixingsView() {
        return "CREATE OR REPLACE VIEW "
                + MyConstants.IMAP_NAME_FIXINGS + MyConstants.VIEW_SUFFIX
                + " AS SELECT "
                + "    __key"
                + "      AS \"primary_key\""
                + ",   JSON_VALUE(this, '$.curvename' RETURNING VARCHAR)"
                + "      AS \"curvename\""
                + ",   JSON_VALUE(this, '$.fixing_dates[0]' RETURNING VARCHAR)"
                + "      AS \"fixing_date_0\""
                + ",   JSON_VALUE(this, '$.fixing_rates[0]' RETURNING DOUBLE)"
                + "      AS \"fixing_rate_0\""
                + ",   JSON_QUERY(this, '$.fixing_dates' WITH ARRAY WRAPPER)"
                + "      AS \"fixing_dates\""
                + ",   JSON_QUERY(this, '$.fixing_rates' WITH ARRAY WRAPPER)"
                + "      AS \"fixing_rates\""
                + " FROM " + MyConstants.IMAP_NAME_FIXINGS;
    }

    /**
     * <p>Describe JSON for Interest Rate Curves
     * </p>
     *
     * @return
     */
    private static String defineIMapIrCurves() {
        return "CREATE OR REPLACE MAPPING "
                + MyConstants.IMAP_NAME_IRCURVES
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = '" + String.class.getCanonicalName() + "',"
                + " 'valueFormat' = 'json'"
                + " )";
    }
    private static String defineIMapIrCurvesView() {
        return "CREATE OR REPLACE VIEW "
                + MyConstants.IMAP_NAME_IRCURVES + MyConstants.VIEW_SUFFIX
                + " AS SELECT "
                + "    __key"
                + "      AS \"primary_key\""
                + ",   JSON_VALUE(this, '$.curvename' RETURNING VARCHAR)"
                + "      AS \"curvename\""
                + ",   JSON_VALUE(this, '$.index' RETURNING VARCHAR)"
                + "      AS \"index\""
                + ",   JSON_VALUE(this, '$.index_frequency' RETURNING INTEGER)"
                + "      AS \"index_frequency\""
                + ",   JSON_VALUE(this, '$.index_frequency_type' RETURNING INTEGER)"
                + "      AS \"index_frequency_type\""
                + ",   JSON_VALUE(this, '$.calendar' RETURNING VARCHAR)"
                + "      AS \"calendar\""
                + ",   JSON_VALUE(this, '$.bussiness_convention' RETURNING INTEGER)"
                + "      AS \"bussiness_convention\""
                + ",   JSON_VALUE(this, '$.dcc' RETURNING VARCHAR)"
                + "      AS \"dcc\""
                + ",   JSON_VALUE(this, '$.end_of_month_flag' RETURNING BOOLEAN)"
                + "      AS \"end_of_month_flag\""
                + ",   JSON_VALUE(this, '$.settlement_days' RETURNING INTEGER)"
                + "      AS \"settlement_days\""
                + ",   JSON_VALUE(this, '$.maturity_period_value[0]' RETURNING INTEGER)"
                + "      AS \"maturity_period_value_0\""
                + ",   JSON_VALUE(this, '$.maturity_period_type[0]' RETURNING INTEGER)"
                + "      AS \"maturity_period_type_0\""
                + ",   JSON_VALUE(this, '$.rates[0]' RETURNING DOUBLE)"
                + "      AS \"rates_0\""
                + ",   JSON_QUERY(this, '$.maturity_period_value[0]' WITH ARRAY WRAPPER)"
                + "      AS \"maturity_period_value_0\""
                + ",   JSON_QUERY(this, '$.maturity_period_type[0]' WITH ARRAY WRAPPER)"
                + "      AS \"maturity_period_type_0\""
                + ",   JSON_QUERY(this, '$.rates[0]' WITH ARRAY WRAPPER)"
                + "      AS \"rates_0\""
                + " FROM " + MyConstants.IMAP_NAME_IRCURVES;
    }

    /**
     * <p>Describe JSON for Trades
     * </p>
     *
     * @return
     */
    private static String defineIMapTrades() {
        return "CREATE OR REPLACE MAPPING "
                + MyConstants.IMAP_NAME_TRADES
                + " ("
                + "  __key VARCHAR,"
                + "  tradeid VARCHAR,"
                + "  bookid VARCHAR,"
                + "  counterparty VARCHAR,"
                + "  notional INTEGER,"
                + "  payer_receiver_flag INTEGER,"
                + "  settlement_date BIGINT,"
                + "  fixed_rate DOUBLE,"
                + "  fixed_leg_dcc VARCHAR,"
                + "  float_spread INTEGER,"
                + "  float_leg_dcc VARCHAR,"
                + "  ibor_index VARCHAR,"
                + "  fixed_leg_start_date BIGINT,"
                + "  fixed_leg_end_date BIGINT,"
                + "  fixed_leg_tenor_frequency INTEGER,"
                + "  fixed_leg_tenor_period_enum INTEGER,"
                + "  fixed_leg_calendar_name VARCHAR,"
                + "  fixed_leg_biz_day_conv INTEGER,"
                + "  fixed_leg_termination_day_conv INTEGER,"
                + "  fixed_leg_date_gen_rule INTEGER,"
                + "  fixed_leg_end_of_month_flag BOOLEAN,"
                + "  float_leg_start_date BIGINT,"
                + "  float_leg_end_date BIGINT,"
                + "  float_leg_tenor_frequency INTEGER,"
                + "  float_leg_tenor_period_enum INTEGER,"
                + "  float_leg_calendar_name VARCHAR,"
                + "  float_leg_biz_day_conv INTEGER,"
                + "  float_leg_termination_day_conv INTEGER,"
                + "  float_leg_date_gen_rule INTEGER,"
                + "  float_leg_end_of_month_flag BOOLEAN"
                + ")"
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = '" + String.class.getCanonicalName() + "',"
                + " 'valueFormat' = 'json-flat',"
                + " 'valueJavaClass' = '" + HazelcastJsonValue.class.getCanonicalName() + "'"
                + " )";
    }

    private static String defineIMapPosition() {
        return "CREATE OR REPLACE MAPPING \""
                + MyConstants.IMAP_NAME_POSITION + "\""
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = '" + String.class.getCanonicalName() + "',"
                + " 'valueFormat' = 'json'"
                + " )";
    }

    private static String defineIMapRisk() {
        return "CREATE OR REPLACE MAPPING "
                + MyConstants.IMAP_NAME_RISK
                + " ("
                + "  __key VARCHAR,"
                + "  risk DOUBLE"
                + ")"
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = '" + String.class.getCanonicalName() + "',"
                + " 'valueFormat' = 'json-flat',"
                + " 'valueJavaClass' = '" + HazelcastJsonValue.class.getCanonicalName() + "'"
                + " )";
    }

    private static String defineIMapStock() {
        return "CREATE OR REPLACE MAPPING "
                + MyConstants.IMAP_NAME_STOCK
                + " ("
                + "  __key VARCHAR,"
                + "  bid DOUBLE,"
                + "  offer DOUBLE"
                + ")"
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = '" + String.class.getCanonicalName() + "',"
                + " 'valueFormat' = 'json-flat',"
                + " 'valueJavaClass' = '" + HazelcastJsonValue.class.getCanonicalName() + "'"
                + " )";
    }

    /**
     * <p>Generic handler to loading definitions
     * </p>
     *
     * @param definition
     */
    private static void define(HazelcastInstance hazelcastInstance, String definition) {
        LOGGER.info("Definition '{}'", definition);
        try {
            hazelcastInstance.getSql().execute(definition);
        } catch (Exception e) {
            LOGGER.error(definition, e);
        }
    }

}
