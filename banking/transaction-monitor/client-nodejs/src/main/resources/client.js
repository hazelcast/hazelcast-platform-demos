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

'use strict';

const { Client } = require('hazelcast-client');

const clusterName = '@my.cluster1.name@';
const instanceName = '@project.artifactId@';
const serviceDns = '@my.docker.image.prefix@-@my.cluster1.name@-hazelcast.default.svc.cluster.local';

const genericRecordMap = "__map-store.mysql_slf4j"

const kubernetes = process.env.MY_KUBERNETES_ENABLED
const host_ip = process.env.HOST_IP
const root = process.env.HOME
const user = root.substring(1)

var member = serviceDns
if (kubernetes.toLowerCase() == 'false') {
	member = host_ip
}

function myISO8601(d) {
    var dateObj = new Date(d);
    return dateObj.toISOString().replace('T',' ').split('.')[0];
}

const launchTime = myISO8601(new Date())

const clientConfig = {
	clusterName: clusterName,
	instanceName: instanceName,
	clientLabels: [ user, launchTime ],
    network: {
        clusterMembers: [
            member
        ],
    },
    properties: {
        'hazelcast.client.statistics.enabled': true,
    }
};

async function runSqlQuery(hazelcastClient, query) {
	console.log('--------------------------------------');
	console.log(query);
    try {
        const sqlService = await hazelcastClient.getSql();
        const sqlResultAll = await sqlService.execute(query, [], {returnRawResult: true});
        var count = 0;
        for await (const row of sqlResultAll) {
            console.log(row);
            count++;
        }
        console.log('[' + count + ' rows]');
    } catch (e) {
        console.log(e.message);
    }
}

async function listDistributedObjects(hazelcastClient) {
	console.log('--------------------------------------');
	console.log('Distributed Objects (excluding system objects)');
    const distributedObjects = await hazelcastClient.getDistributedObjects();
	var count = 0;
    //TODO Remove once fixed
    console.log("MAY FAIL: https://github.com/hazelcast/hazelcast-nodejs-client/issues/1474");
    for (var i = 0; i < distributedObjects.length; i++){
        const name = distributedObjects[i].getName();
        if (!(name.startsWith("__"))) {
            const serviceName = distributedObjects[i].getServiceName();
            console.log(serviceName, "=>", "'" + name + "'");
            count++;
        }
	}
    console.log('[' + count + ' rows]');
}

async function getGenericRecord(hazelcastClient) {
	console.log('--------------------------------------')
	console.log('GenericRecord, map \'' + genericRecordMap + '\'')
    const map = await hazelcastClient.getMap(genericRecordMap);
    const keySet = await map.keySet();
	var count = 0;
    for (var i = 0; i < keySet.length; i++){
        const key = keySet[i];
        const value = await map.get(key);
        //FIXME How show generic record
        console.log("XXX,", i, key, value);
        count++;
	}
    console.log('[' + count + ' rows]');
}

(async () => {
    try {
        console.log('--------------------------------------')
		console.log('MY_KUBERNETES_ENABLED ', kubernetes)
        const hazelcastClient = await Client.newHazelcastClient(clientConfig);
        console.log('--------------------------------------')

		const startTime = myISO8601(new Date())
		console.log('===================', startTime, '===================')

        console.log('Sleeping one minute, so cluster populated with data')
        await new Promise((resolve) => setTimeout(resolve, 60 * 1000));

        await runSqlQuery(hazelcastClient, "SHOW MAPPINGS")
        await runSqlQuery(hazelcastClient, "SHOW VIEWS")
    
        await listDistributedObjects(hazelcastClient)
    
        await getGenericRecord(hazelcastClient)
    
        await runSqlQuery(hazelcastClient, "SELECT * FROM \"__map-store.mysql_slf4j\"")

		const endTime = myISO8601(new Date())
		console.log('===================', endTime, '===================')
        console.log('Sleeping for a day')
        await new Promise((resolve) => setTimeout(resolve, 24 * 60 * 60 * 1000));
        console.log('Disconnecting')
        await hazelcastClient.shutdown();
    } catch (err) {
        console.error('Error:', err);
    }
})();
