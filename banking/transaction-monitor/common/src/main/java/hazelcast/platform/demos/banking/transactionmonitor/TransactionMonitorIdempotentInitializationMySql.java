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

import java.util.Locale;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.config.DataConnectionConfig;
import com.hazelcast.core.HazelcastInstance;

/**
 * <p>MySql specific initialization
 * </p>
 * <p>Invoked by the overlarge {@link TransactionMonitorIdempotentInitialization}
 * </p>
 */
public class TransactionMonitorIdempotentInitializationMySql {
    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionMonitorIdempotentInitializationMySql.class);

    /**
     * <p>Define MySql connection via SQL
     * </p>
     */
    static boolean defineMySql(HazelcastInstance hazelcastInstance, Properties properties,
            TransactionMonitorFlavor transactionMonitorFlavor) {
        try {
            String address = System.getProperty(MyConstants.MYSQL_ADDRESS);
            String database = "transaction-monitor-" + transactionMonitorFlavor.toString().toLowerCase(Locale.ROOT);
            String username = properties.getProperty("my.mysql.user");
            String password = properties.getProperty("my.mysql.password");
            String jdbcUrl = "jdbc:mysql://" + address + "/" + database;

            LOGGER.info("MySql connection: Url: '{}'", jdbcUrl);
            LOGGER.trace("MySql connection: User: '{}'", username);
            LOGGER.trace("MySql connection: Pass: '{}'", password);

            DataConnectionConfig dataConnectionConfig =
                    new DataConnectionConfig(MyConstants.MYSQL_DATACONNECTION_CONFIG_NAME)
                    .setType("jdbc")
                    .setProperty("jdbcUrl", jdbcUrl)
                    .setProperty("username", username)
                    .setProperty("password", password)
                    .setShared(true);

            hazelcastInstance.getConfig().addDataConnectionConfig(dataConnectionConfig);
            return true;
        } catch (Exception e) {
            LOGGER.error("defineMySql()", e);
            return false;
        }
    }

    /**
     * <p>For using MySql datalink.
     * </p>
     *
     * @return
     */
    public static Properties getMySqlProperties() {
        Properties mySqlProperties = new Properties();
        mySqlProperties.setProperty("data-connection-ref", MyConstants.MYSQL_DATACONNECTION_CONFIG_NAME);
        mySqlProperties.setProperty("mapping-type", "JDBC");
        mySqlProperties.setProperty("table-name", MyConstants.MYSQL_DATACONNECTION_TABLE_NAME);
        //TODO Once MySql compound key supported by Data Link
        //MyConstants.MYSQL_DATACONNECTION_TABLE_COLUMN0 + "," + MyConstants.MYSQL_DATACONNECTION_TABLE_COLUMN1);
        mySqlProperties.setProperty("id-column", "hash");
        mySqlProperties.setProperty("column", MyConstants.MYSQL_DATACONNECTION_TABLE_COLUMN0
                + "," + MyConstants.MYSQL_DATACONNECTION_TABLE_COLUMN1
                + "," + MyConstants.MYSQL_DATACONNECTION_TABLE_COLUMN2
                + "," + MyConstants.MYSQL_DATACONNECTION_TABLE_COLUMN3
                + "," + MyConstants.MYSQL_DATACONNECTION_TABLE_COLUMN4
                + "," + MyConstants.MYSQL_DATACONNECTION_TABLE_COLUMN5);
        return mySqlProperties;
    }

}
