'use strict';

import React, {Component} from 'react';
import {useTable} from 'react-table';
import SockJsClient from 'react-stomp';

// Note SockJsClient uses 'http' protocol not 'ws'
// See https://github.com/lahsivjar/react-stomp/blob/HEAD/API/
const WS_URL = 'http://' + window.location.host + '/hazelcast';
const WS_TOPICS_PREFIX = '/topics';
const WS_TOPICS = [ WS_TOPICS_PREFIX + "/job_state" ];

class Jobs extends Component {
    constructor(props) {
        super(props);
        this.state = {
        		jobs: []
        };
        this.handleData = this.handleData.bind(this);
    }

    handleData(message) {
        console.log(message);
    }
    
    render() {
    	const { jobs } = this.state;
    	
        return (
        	<div class="minor_pane">
        		<SockJsClient 
                	url={WS_URL}
        			topics={WS_TOPICS}
        			onMessage={this.handleData}
        			debug={false} />
        		<h2>Jet Jobs</h2>
        	</div>
        );
    }
}

export default Jobs;