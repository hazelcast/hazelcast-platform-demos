import React, {Component} from 'react';
import {BrowserRouter as Router, Link} from 'react-router-dom';
import SockJsClient from 'react-stomp';
import {useTable} from 'react-table';
import styled from 'styled-components';
import update from 'immutability-helper';

var rest = require('rest');
var mime = require('rest/interceptor/mime');

const DOWNLOAD_URL = 'http://' + window.location.host + '/rest/download';
// Note SockJsClient uses 'http' protocol not 'ws'
// See https://github.com/lahsivjar/react-stomp/blob/HEAD/API/
const WS_URL = 'http://' + window.location.host + '/hazelcast';
const WS_TOPICS_PREFIX = '/topics';
const WS_TOPICS = [ WS_TOPICS_PREFIX + "/job_state" ];

// Styled-components. Could move to CSS
const Styles = styled.div `
  padding: 1rem;
  table {
    border-spacing: 0;
    border: 1px solid gray;
    width: 100%;
    tr {
      :last-child {
        td {
          border-bottom: 0;
        }
      }
    }
    th {
  	  color: indigo;
      margin: 0;
      padding: 0.5rem;
      border-bottom: 1px solid gray;
      border-right: 1px solid gray;
      :last-child {
        border-right: 0;
      }
    }
    td {
  	  color: var(--hazelcast-blue-light);
      margin: 0;
      padding: 0.5rem;
      border-bottom: 1px solid gray;
      border-right: 1px solid gray;
      :last-child {
        border-right: 0;
      }
    }
  }
`

// Table columns
const columns = [
	{
		Header: 'Name',
		accessor: 'name',
	},
	{
		Header: 'Size',
		accessor: 'size',
	},
]

// The '<Table/>' HTML element
function Table({ columns, data }) {
	  const {
	    getTableProps,
	    getTableBodyProps,
	    headerGroups,
	    rows,
	    prepareRow,
	  } = useTable({
	    columns,
	    data,
	  })
	  
	  return (
	    <table {...getTableProps()}>
	      <thead>
	        {headerGroups.map(headerGroup => (
	          <tr {...headerGroup.getHeaderGroupProps()}>
	            {headerGroup.headers.map(column => (
	              <th {...column.getHeaderProps()}>{column.render('Header')}</th>
	            ))}
	          </tr>
	        ))}
	      </thead>
	      <tbody {...getTableBodyProps()}>
	        {rows.map((row, i) => {
	          prepareRow(row)
	          return (
	            <tr {...row.getRowProps()}>
	              {row.cells.map(cell => {
	                return <td {...cell.getCellProps()}>{cell.render('Cell')}</td>
	              })}
	            </tr>
	          )
	        })}
	      </tbody>
	    </table>
	  )
}

class Sizes extends Component {
    constructor(props) {
        super(props);
        this.state = {
    		data: []
        };
        this.handleData = this.handleData.bind(this);
    }
    
    handleData(message){
    	// message is just a nudge to make REST call
        this.getData();
    }
    
    getData(){
        setImmediate(() => {
	    	var client = rest.wrap(mime);
	    	var self = this;
	    	
	    	client({path:'/rest/mapSizes'}).then(
	    			function(response) {
	        	var sizesResponse = response.entity.sizes;
	        	
	        	var newData = [];
	        	
	        	for (var i = 0; i < sizesResponse.length; i++) {
	        		
	        		var size_name = sizesResponse[i].name;
	        		var size_size = sizesResponse[i].size;
	        				        	
	        		var datum = {
	        				name: size_name,
	        				size: size_size,
	        		};
	        		
	        		newData.push(datum);
	        	}
	        	
	        	self.setState({
	        			data: update(self.state.data, {$set: newData}) 
	        			});
	        	
	    	});
        })
      }

    componentDidMount(){
        this.getData();
    }
    
    render() {
        return (
        	<div>
    		    <h2>Map Sizes</h2>
        		<SockJsClient 
                	url={WS_URL}
        			topics={WS_TOPICS}
        			onMessage={this.handleData}
        			debug={false} />
        	    <Styles>
        	      <Table columns={columns} data={this.state.data} />
        	    </Styles>
            </div>
        );
    }
}

export default Sizes;