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

const { Client, LocalDateTime } = require('hazelcast-client');
const fs = require('fs');
const path = require('path');
const readline = require('readline');

const clusterName = '@my.cluster1.name@';
const instanceName = '@project.artifactId@';
const serviceDns = '@my.docker.image.prefix@-@my.cluster1.name@-hazelcast.default.svc.cluster.local';

const viridianId = "@my.viridian.cluster1.id@";
const viridianDiscoveryToken = "@my.viridian.cluster1.discovery.token@";
const viridianKeyPassword = "@my.viridian.cluster1.key.password@";

const controlFile = "/tmp/control.file";
const cloudServerUrl = "https://api.viridian.hazelcast.com";
const genericRecordMapPrefix = "__map-store."
const genericRecordMap = "mysql_slf4j"
const useViridianKey = "use.viridian"
const viridianCaFile = "/tmp/ca.pem"
const viridianCertFile = "/tmp/cert.pem"
const viridianKeyFile = "/tmp/key.pem"

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
const clientConfigViridian = {
	clusterName: viridianId,
	instanceName: instanceName,
	clientLabels: [ user, launchTime ],
    network: {
        hazelcastCloud: {
            discoveryToken: viridianDiscoveryToken
        },
        ssl: {
            enabled: true,
            sslOptions: {
                ca: [fs.readFileSync(path.resolve(viridianCaFile))],
                cert: [fs.readFileSync(path.resolve(viridianCertFile))],
                key: [fs.readFileSync(path.resolve(viridianKeyFile))],
                passphrase: viridianKeyPassword,
                checkServerIdentity: () => null
            }
        }
    },
    properties: {
        'hazelcast.client.cloud.url': cloudServerUrl,
        'hazelcast.client.statistics.enabled': true,
    }
};

async function runSqlQuery(hazelcastClient, query) {
	console.log('--------------------------------------');
	console.log(query);
    try {
        const sqlService = await hazelcastClient.getSql();
        const sqlResult = await sqlService.execute(query, [], {returnRawResult: true});
        const rowMetadata = await sqlResult.rowMetadata;
        const columns = await rowMetadata.getColumns();
        var count = 0;
        let header = "";
        for (let j = 0; j < columns.length; j++) {
            if (j > 0) {
                header = header + ", ";
            }
            header = header + columns[j].name.toUpperCase()
        }
        console.log(header);
        for await (const row of sqlResult) {
            const values = await row.values;
            let line = "";
            for (let i = 0; i < values.length; i++) {
                if (i > 0) {
                    line = line + ", ";
                }
                const kind = columns[i].type;
                if (kind == 0) {
                    // Varchar
                    line = line + values[i];
                } else {
                    if (kind == 5) {
                        // Bigint
                        line = line + values[i];
                    } else {
                        if (kind == 11) {
                            // Timestamp
                            try {
                                line = line + values[i].toString();
                            } catch (e) {
                                console.log("runSqlQuery()", kind, columns[i].name, e);
                            }
                        } else {
                            console.log("Unhandled column type" + kind + " for + '" + columns[i].name + "'");
                        }
                    }
                }
            }
            console.log(line);
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
    //const distributedObjects = await hazelcastClient.getDistributedObjects();
	var count = 0;
    //TODO Remove once fixed
    console.log("TODO: https://github.com/hazelcast/hazelcast-nodejs-client/issues/1474");
    console.log("TODO: https://github.com/hazelcast/hazelcast-nodejs-client/issues/1474");
    console.log("TODO: https://github.com/hazelcast/hazelcast-nodejs-client/issues/1474");
    /*for (var i = 0; i < distributedObjects.length; i++){
        const name = distributedObjects[i].getName();
        if (!(name.startsWith("__"))) {
            const serviceName = distributedObjects[i].getServiceName();
            console.log(serviceName, "=>", "'" + name + "'");
            count++;
        }
	}*/
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
        var viridian = false;
        var readerI = readline.createInterface({
            input: fs.createReadStream(controlFile)
        });
        const matchStr = useViridianKey + "=true";
        const checkViridian = async () =>{
            for await (const line of readerI) {
                if (line.toLowerCase() == matchStr) {
                    viridian = true;
                }
            }
        }
        await checkViridian()
        console.log('VIRIDIAN \'', viridian, '\'')
        var hazelcastClient;
        if (viridian) {
            hazelcastClient = await Client.newHazelcastClient(clientConfigViridian);
        } else {
            hazelcastClient = await Client.newHazelcastClient(clientConfig);
        }
        console.log('--------------------------------------')

		const startTime = myISO8601(new Date())
		console.log('===================', startTime, '===================')

        console.log('Sleeping one minute, so cluster populated with data')
        await new Promise((resolve) => setTimeout(resolve, 60 * 1000));

        await runSqlQuery(hazelcastClient, "SHOW MAPPINGS")
        await runSqlQuery(hazelcastClient, "SHOW VIEWS")
    
        await listDistributedObjects(hazelcastClient)
    
        await getGenericRecord(hazelcastClient)
    
        await runSqlQuery(hazelcastClient, "SELECT * FROM \"" + genericRecordMapPrefix + genericRecordMap + "\"")

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
