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

'use strict';

import React from "react";
import { createRoot } from 'react-dom/client';
const { Client } = require('hazelcast-client');
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


//import { 
//    Client,
//    ClientConfig,
//    ReconnectMode
//} from 'hazelcast-client';

//import * as perspective from '@finos/perspective';
//import "@finos/perspective-viewer-datagrid";
//import "@finos/perspective-viewer-d3fc";
//import PerspectiveViewerConfig from "@finos/perspective-viewer"
//import {
//    HTMLPerspectiveViewerElement,
//    //PerspectiveViewerConfig,
//} from "@finos/perspective-viewer";

/*
declare let process : {
    env: {
        REACT_APP_MC_CLUSTER1_LIST: string,
        REACT_APP_MC_CLUSTER1_NAME: string,
        REACT_APP_MY_KUBERNETES_ENABLED: string
    }
  }
const clusterName = process.env.REACT_APP_MC_CLUSTER1_NAME
const clusterAddress = process.env.REACT_APP_MC_CLUSTER1_LIST
const kubernetes = "@my.docker.image.prefix@-@my.cluster1.name@-hazelcast.default.svc.cluster.local"
const kubernetesEnabled = process.env.REACT_APP_MY_KUBERNETES_ENABLED

function createClientConfig() : ClientConfig {
	let clusterMember = kubernetes
	if (kubernetesEnabled.toLowerCase() === 'false') {
    	clusterMember = clusterAddress
	}

	// Webapp console, on Browser
	console.log("index.tsx", "createClientConfig()", "clusterName", clusterName)
	console.log("index.tsx", "createClientConfig()", "clusterMember", clusterMember)
    console.log("index.tsx", "createClientConfig()", "Will use '" + clusterMember + "' for connection.")

    return {
    	clusterName: clusterName,
    	instanceName: '@my.docker.image.name@',
    	clientLabels: [ 'finos', 'build-@maven.build.timestamp@' ],
    	network: {
        	clusterMembers: [
            	clusterMember
        	]
    	},
    	connectionStrategy: {
        reconnectMode: ReconnectMode.OFF,
        connectionRetry: {
            clusterConnectTimeoutMillis: 5000
        	}
    	},
    	metrics: {
        	enabled: true,
    	}    
	}
}
*/

/*
const worker = perspective.default.shared_worker();

const getTable = async (): Promise<perspective.Table> => {
    const req = fetch("./superstore.arrow");
    const resp = await req;
    const buffer = await resp.arrayBuffer();
    return await worker.table(buffer as any);
};*/

/*
const getTable = async () => {
  let d: any[] = []
  d.push({ stock: 'AAAA', sold: 123, bought: 456 })
  d.push({ stock: 'BBBB', sold: 456, bought: 789 })
  d.push({ stock: 'CCCC', sold: 788, bought: 123 })
  return worker.table(d as any);
};

//const config: PerspectiveViewerConfig = {
 //XXX   group_by: ["State"],
//};

const start = async (): Promise<any> => {
    try {
        let clientConfig = createClientConfig();
        return await Client.newHazelcastClient(clientConfig);
    } catch (err) {
        console.error('Error:', err);
    }
}
//XXX start()
*/

const App = () => {
	//const viewer = React.useRef<HTMLPerspectiveViewerElement>(null);
	
	/*
  React.useEffect(() => {
    getTable().then(table => {
      if (viewer.current) {
        viewer.current.load(table);
        //viewer.current.restore(config);
      }
    });
  }, []);
  */
  
  /*
    React.useEffect(() => {
        getTable().then((table) => {
            if (viewer.current) {
                viewer.current.load(Promise.resolve(table));
                viewer.current.restore(config);
            }
        });
    }, []);*/
    	
    return (<p>FIXME</p>);
    //<perspective-viewer ref={viewer}></perspective-viewer>;
};

export default App;
