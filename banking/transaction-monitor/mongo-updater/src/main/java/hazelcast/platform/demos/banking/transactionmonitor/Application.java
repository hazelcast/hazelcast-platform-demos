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

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.platform.demos.utils.UtilsProperties;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

/**
 * <p>Entry point, "{@code main()}" method.
 * </p>
 */
public class Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);
    private static final long FIVE_MINUTES = 5L;

    /**
     * <p>Once every 5 minutes, update Mongo to indicate a job should be
     * started or stopped.
     * </p>
     */
    public static void main(String[] args) throws Exception {
        Properties properties = UtilsProperties.loadClasspathProperties("application.properties");

        MongoClient mongoClient = getMongoClient(properties);
        MongoDatabase mongoDatabase = getMongoDatabase(mongoClient, properties);
        MongoCollection<Document> collection = getMongoCollection(mongoDatabase);

        long count = collection.countDocuments();
        LOGGER.info("Collection size=={}", count);

        try {
            while (true) {
                LOGGER.info("Sleeping 5 minutes");
                TimeUnit.MINUTES.sleep(FIVE_MINUTES);
                count = collection.countDocuments();
                Document document = new Document();
                document.append(MyConstants.MONGO_COLLECTION_FIELD1, MyConstants.ARCHIVER_JOB_NAME);
                if (count % 2 == 0) {
                    document.append(MyConstants.MONGO_COLLECTION_FIELD2, MyConstants.ARCHIVER_JOB_STATE_OFF);
                } else {
                    document.append(MyConstants.MONGO_COLLECTION_FIELD2, MyConstants.ARCHIVER_JOB_STATE_ON);
                }
                LOGGER.info("insertOne('{}')", document);
                collection.insertOne(document);
            }
        } catch (Exception e) {
            String message = String.format(Application.class.getName() + "main(): %s", e.getMessage());
            LOGGER.error(message);
            try {
                mongoClient.close();
            } catch (Exception e2) {
                String message2 = String.format(Application.class.getName() + "main(): close(): %s", e2.getMessage());
                LOGGER.error(message2);
            }
        }
    }

    /**
     * <p>Select collection.
     * </p>
     *
     * @param mongoDatabase
     * @return
     */
    private static MongoCollection<Document> getMongoCollection(MongoDatabase mongoDatabase) {
        return mongoDatabase.getCollection(MyConstants.MONGO_COLLECTION);
    }

    /**
     * <p>Select database
     * </p>
     *
     * @param mongoClient
     * @param properties
     * @return
     */
    private static MongoDatabase getMongoDatabase(MongoClient mongoClient, Properties properties) {
        String flavor = properties.getProperty(MyConstants.TRANSACTION_MONITOR_FLAVOR, "");
        if (flavor.isBlank()) {
            throw new RuntimeException(String.format("'{}' property missing", MyConstants.TRANSACTION_MONITOR_FLAVOR));
        }

        String database = "transaction-monitor-" + flavor;
        LOGGER.info("Database: {}", database);
        return mongoClient.getDatabase(database);
    }

    /**
     * <p>Connect
     * </p>
     * @param mongoClient
     * @param properties
     * @return
     */
    private static MongoClient getMongoClient(Properties properties) throws Exception {
        String uri = MyUtils.buildMongoURI(properties);
        LOGGER.info("Using URI: {}", uri);
        return MongoClients.create(uri);
    }

}
