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

package com.hazelcast.platform.demos.banking.cva;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.jet.JetInstance;

/**
 * <p>Ensure the server is in a ready state, by requesting all the
 * set-up processing runs. This is idempotent. All servers will request
 * but only the <i>n</i>th will result in anything happening.
 * </p>
 */
@Configuration
public class ApplicationInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationInitializer.class);

    @Autowired
    private JetInstance jetInstance;
    //@Autowired
    //private MyProperties myProperties;

    /**
     * <p>Use a Spring "{@code @Bean}" to kick off the necessary
     * initialisation after the objects we need are ready.
     * </p>
     */
    @Bean
    public CommandLineRunner commandLineRunner() {
       return args -> {
           /*
           boolean isLocalhost =
               !System.getProperty("my.docker.enabled", "false").equalsIgnoreCase(Boolean.TRUE.toString())
               &&
               !System.getProperty("my.kubernetes.enabled", "false").equalsIgnoreCase(Boolean.TRUE.toString());
               */

           this.createNeededObjects();

           /* Not currently required, no jobs launch for housekeeping
            *
           int currentSize = this.jetInstance.getCluster().getMembers().size();
           if (this.myProperties.getInitSize() > currentSize) {
               LOGGER.info("Cluster size {}, not initializing until {}",
                       currentSize, this.myProperties.getInitSize());
           } else {
               LOGGER.info("Cluster size {}, initializing", currentSize);
               this.launchNeededJobs(isLocalhost);
           }
            */

           // For SQL against empty IMap
           this.defineIMap();
       };
    }

    /**
     * <p>Objects such as maps are created on-demand in Hazelcast.
     * Touch all the one we'll need to be sure they exist in advance,
     * this doesn't change their behaviour but is useful for reporting.
     * </p>
     */
    private void createNeededObjects() {
        for (String iMapName : MyConstants.IMAP_NAMES) {
            this.jetInstance.getHazelcastInstance().getMap(iMapName);
        }
        for (String iTopicName : MyConstants.ITOPIC_NAMES) {
            this.jetInstance.getHazelcastInstance().getTopic(iTopicName);
        }
    }

    /**
     * <p>Launch any "<i>system</i>" housekeeping jobs. Currently none.
     * <p>
     *
    private void launchNeededJobs(boolean isLocalhost) {
    }*/


    /**
     * <p>Without this metadata, cannot query an empty
     * {@link IMap}.
     * </p>
     */
    private void defineIMap() {
        String definition1 = this.defineIMapCpCds();
        String definition2 = this.defineIMapFixings();
        String definition3 = this.defineIMapIrCurves();
        String definition4 = this.defineIMapTrades();

        this.define(definition1);
        this.define(definition2);
        this.define(definition3);
        this.define(definition4);
    }

    /**
     * <p>Describe JSON for CP CDS
     * </p>
     *
     * @return
     */
    private String defineIMapCpCds() {
        return "CREATE MAPPING "
                + MyConstants.IMAP_NAME_CP_CDS
                + " ("
                + "  __key VARCHAR,"
                // "date" field is held in JSON as a String and parsed later.
                //+ "    \"date\" DATE,"
                + "    \"date\" VARCHAR,"
                + "    timezone VARCHAR,"
                + "    ticker VARCHAR,"
                + "    shortname VARCHAR,"
                + "    redcode VARCHAR,"
                + "    tier VARCHAR,"
                + "    ccy VARCHAR,"
                + "    docclause VARCHAR,"
                //TODO `JSONArray` not supported in 5.0
                //+ "    spread_periods,"
                //TODO `JSONArray` not supported in 5.0
                //+ "    spreads,"
                + "    recovery DOUBLE,"
                + "    datarating VARCHAR,"
                + "    sector VARCHAR,"
                + "    region VARCHAR,"
                + "    country VARCHAR,"
                + "    avrating VARCHAR,"
                + "    impliedrating VARCHAR"
                + ")"
                + "TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = '" + String.class.getCanonicalName() + "',"
                + " 'valueFormat' = 'json',"
                + " 'valueJavaClass' = '" + HazelcastJsonValue.class.getCanonicalName() + "'"
                + " )";
    }

    /**
     * <p>Describe JSON for Fixings
     * </p>
     *
     * @return
     */
    private String defineIMapFixings() {
        return "CREATE MAPPING "
                + MyConstants.IMAP_NAME_FIXINGS
                + " ("
                + "  __key VARCHAR,"
                + "  curvename VARCHAR"
                //TODO `JSONArray` not supported in 5.0
                //+ "    fixing_dates,"
                //TODO `JSONArray` not supported in 5.0
                //+ "    fixing_rates,"
                + ")"
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = '" + String.class.getCanonicalName() + "',"
                + " 'valueFormat' = 'json',"
                + " 'valueJavaClass' = '" + HazelcastJsonValue.class.getCanonicalName() + "'"
                + " )";
    }

    /**
     * <p>Describe JSON for Interest Rate Curves
     * </p>
     *
     * @return
     */
    private String defineIMapIrCurves() {
        return "CREATE MAPPING "
                + MyConstants.IMAP_NAME_IRCURVES
                + " ("
                + "  __key VARCHAR,"
                + "  curvename VARCHAR,"
                + "  index VARCHAR,"
                + "  index_frequency INTEGER,"
                + "  index_frequency_type INTEGER,"
                + "  calendar VARCHAR,"
                + "  bussiness_convention INTEGER,"
                + "  dcc VARCHAR,"
                + "  end_of_month_flag BOOLEAN,"
                + "  settlement_days INTEGER"
                //TODO `JSONArray` not supported in 5.0
                //+ "  maturity_period_value OBJECT,"
                //TODO `JSONArray` not supported in 5.0
                //+ "  maturity_period_type OBJECT,"
                //TODO `JSONArray` not supported in 5.0
                //+ "  rates OBJECT,"
                + ")"
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = '" + String.class.getCanonicalName() + "',"
                + " 'valueFormat' = 'json',"
                + " 'valueJavaClass' = '" + HazelcastJsonValue.class.getCanonicalName() + "'"
                + " )";
    }

    /**
     * <p>Describe JSON for Trades
     * </p>
     *
     * @return
     */
    private String defineIMapTrades() {
        return "CREATE MAPPING "
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
                + " 'valueFormat' = 'json',"
                + " 'valueJavaClass' = '" + HazelcastJsonValue.class.getCanonicalName() + "'"
                + " )";
    }

    /**
     * <p>Generic handler to loading definitions
     * </p>
     *
     * @param definition
     */
    private void define(String definition) {
        LOGGER.trace("Definition '{}'", definition);
        try {
            this.jetInstance.getSql().execute(definition);
        } catch (Exception e) {
            LOGGER.error(definition, e);
        }
    }
}
