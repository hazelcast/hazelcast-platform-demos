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

package hazelcast.platform.demos.industry.iiot.mapstore;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.map.MapLoader;
import com.hazelcast.map.MapLoaderLifecycleSupport;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;

import hazelcast.platform.demos.industry.iiot.MyConstants;
import hazelcast.platform.demos.industry.iiot.Utils;

/**
 * <p>Load a value from Mongo. This version does not store back to Mongo.
 * </p>
 * <p>Based on <a href="https://github.com/hazelcast/hazelcast-cloud-code-samples
/tree/master/mapstore/mapstore-sample-hazelcast4-mongodb">this example</a>
 * although it is used here against Mongo 5 not Mongo 4.
 * </p>
 */
public class ServiceHistoryMapLoader implements MapLoader<String, HazelcastJsonValue>,
    MapLoaderLifecycleSupport, Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceHistoryMapLoader.class);

    private transient MongoClient mongoClient;
    private transient MongoDatabase mongoDatabase;
    private String collection;
    private String database;
    private String host;
    private String mapName;
    private String password;
    private String username;

    public ServiceHistoryMapLoader(Map<String, String> mongoProperties, String mapName) {
        this.collection = mongoProperties.getOrDefault(MyConstants.MONGO_COLLECTION1, "");
        this.database = mongoProperties.getOrDefault(MyConstants.MONGO_DATABASE, "");
        this.host = mongoProperties.getOrDefault(MyConstants.MONGO_HOST, "");
        this.password = mongoProperties.getOrDefault(MyConstants.MONGO_PASSWORD, "");
        this.username = mongoProperties.getOrDefault(MyConstants.MONGO_USERNAME, "");
        String[][] config = new String[][] {
            { MyConstants.MONGO_COLLECTION1, this.collection },
            { MyConstants.MONGO_DATABASE, this.database },
            { MyConstants.MONGO_PASSWORD, this.password },
            { MyConstants.MONGO_USERNAME, this.username },
            { MyConstants.MONGO_HOST, this.host},
        };
        for (String[] pair : config) {
            if (pair[1].length() == 0) {
                String message = String.format("Bad Mongo property: '%s' == '%s'", pair[0], pair[1]);
                throw new RuntimeException(message);
            }
        }
        this.mapName = mapName;
    }

    /**
     * <p>Attempt to connect, using the provided properties.
     * </p>
     */
    @Override
    public void init(HazelcastInstance hazelcastInstance, Properties properties, String mapName) {
        LOGGER.trace(this.mapName + ":init(), START");
        MongoClient tmpClient = null;

        try {
            // Default port 27017, credentials for user in admin database
            MongoClientURI uri = new MongoClientURI("mongodb://" + this.username + ":"
                        + this.password + "@" + this.host + "/?authSource=admin");

            tmpClient = new MongoClient(uri);

            this.mongoDatabase = tmpClient.getDatabase(this.database);
        } catch (Exception e) {
            Utils.addExceptionToLogger(LOGGER, "init()", e);
        }

        // Only use if fully initialised and connected
        if (tmpClient != null) {
            this.mongoClient = tmpClient;
        }
        LOGGER.trace(this.mapName + ":init(), END OK==" + Boolean.valueOf(this.mongoClient != null));
    }

    /**
     * <p>Disconnect, in the unlikely case this class is destroyed.
     * </p>
     */
    @Override
    public void destroy() {
        if (this.mongoClient != null) {
            this.mongoClient.close();
        }
        LOGGER.trace(this.mapName + ":destroy()");
    }

    /**
     * <p>Load and reformat Mongo collection to the JSON we want.
     * </p>
     * <p>As this example does not save back to Mongo, we don't need to upload
     * all fields.
     * </p>
     */
    @Override
    public HazelcastJsonValue load(String key) {
        LOGGER.trace(this.mapName + ":load(" + key + ") START");

        HazelcastJsonValue result = null;

        try {
            MongoCollection<Document> collection = this.mongoDatabase.getCollection(this.collection);
            MongoCursor<Document> cursor = collection.find(Filters.in("_id", key)).iterator();
            Document document = null;
            while (cursor.hasNext()) {
                document = cursor.next();
            }
            cursor.close();

            if (document != null) {
                result = this.formValue(document);
            }
        } catch (Exception e) {
            Utils.addExceptionToLogger(LOGGER, "load('" + key + "')", e);
        }

        LOGGER.trace("load(" + key + ") END, '" + Utils.escapeJson(result) + "'");
        return result;
    }

    /**
     * <p>Retrieve a partition's worth of data records.
     * </p>
     * <p>Do this reusing the {@link load} method rather than an optimization.
     * </p>
     */
    @Override
    public Map<String, HazelcastJsonValue> loadAll(Collection<String> keys) {
        LOGGER.trace(this.mapName + ":loadAll(" + new ArrayList<>(keys) + ")");

        Map<String, HazelcastJsonValue> result = new HashMap<>();
        for (String key : keys) {
            HazelcastJsonValue json = null;
            try {
                json = this.load(key);
            } catch (Exception exception) {
                String diagnostic = String.format("loadAll() for key '%s'", key);
                Utils.addExceptionToLogger(LOGGER, diagnostic, exception);
            }

            if (json != null) {
                result.put(key, json);
            }
        }

        if (result.size() != keys.size()) {
            LOGGER.warn(this.mapName + ":loadAll(" + new ArrayList<>(keys) + ") got only " + result.size());
        }
        return result;
    }

    /**
     * <p>Retrieve primary keys (field "{@code _id}") from a Mongo collection. As there
     * are only 12, retrieve them all.
     * </p>
     */
    @Override
    public Iterable<String> loadAllKeys() {
        LOGGER.debug(this.mapName + ":loadAllKeys(), START");
        List<String> ids = new ArrayList<>();
        try {
            MongoCollection<Document> collection = this.mongoDatabase.getCollection(this.collection);
            MongoCursor<Document> cursor = collection.find().projection(Projections.include("_id")).iterator();
            while (cursor.hasNext()) {
                Document document = cursor.next();
                ids.add(document.get("_id", String.class));
            }
            cursor.close();
        } catch (Exception e) {
            Utils.addExceptionToLogger(LOGGER, "loadAllKeys()", e);
        }
        LOGGER.debug(this.mapName + ":loadAllKeys(), END, " + ids.size());
        return ids;
    }

    /**
     * <p>Convert Mongo document to Hazelcast.
     * </p>
     * <p>To keep the demo up to date, in Mongo we store relative offsets for
     * the machine service history. As loaded we turn these into absolute
     * dates.
     * </p>
     *
     * @param document
     * @return
     */
    private HazelcastJsonValue formValue(Document document) {
        try {
            Integer lastServicedOffset = document.get("lastServicedOffset", Integer.class);
            LocalDate today = LocalDate.now();
            LocalDate lastServiced = today.minusDays(lastServicedOffset);

            StringBuffer valueStringBuffer = new StringBuffer();
            valueStringBuffer.append("{");
            valueStringBuffer.append(" \"lastServiced\" : \"" + lastServiced + "\"");
            valueStringBuffer.append("}");

            return new HazelcastJsonValue(valueStringBuffer.toString());
        } catch (Exception e) {
            Utils.addExceptionToLogger(LOGGER, "formValue() " + document, e);
            return null;
        }
    }
}
