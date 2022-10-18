import React, {Component} from 'react';
import {BrowserRouter as Router, Link} from 'react-router-dom';
import SockJsClient from 'react-stomp';
import {useTable} from 'react-table';
import styled from 'styled-components';
import update from 'immutability-helper';

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
		Header: 'Job Info',
		columns: [
			{
				Header: 'Id',
				accessor: 'id',
			},
			{
				Header: 'Name',
				accessor: 'name',
			},
		],
	},
	{
		Header: 'Status',
		columns: [
			{
				Header: 'Local Start Time',
				accessor: 'submission_time',
			},
			{
				Header: 'Last Change',
				accessor: 'now',
			},
			{
				Header: 'Previous',
				accessor: 'previous_status',
			},
			{
				Header: 'Current',
				accessor: 'status',
			},
		],
	},
	{
		Header: 'Output',
		columns: [
			{
				Header: 'CSV',
				accessor: 'csv',
			},
			{
				Header: 'XLS',
				accessor: 'xls',
			},
		],
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

// Format Java timestamp in milliseconds
function myISO8601(longStr) {
	var dateObj = new Date(Number(longStr));
	return dateObj.toISOString().replace('T',' ').split('.')[0];
}

class Jobs extends Component {
    constructor(props) {
        super(props);
        this.state = {
    		jobs: []
        };
        this.handleData = this.handleData.bind(this);
    }
    
    handleData(message) {
    	//console.log(message);

		if (!message.job.dummy) {
	    	let notAvailable = <div><i>"N/a"</i></div>;
    		var nowStr = myISO8601(message.now);
    		var jobKey = message.job.name.split('$')[0];
    		var outputKey = message.job.name.split('$')[1];
    		var submissionTimeStr = myISO8601(message.job.submission_time);
    		var csv = notAvailable;
    		var xls = notAvailable;
    		if (jobKey == 'CvaStpJob' && message.job.status == 'COMPLETED') {
    			var csvUrl = "/rest/download/cva_csv?key=" + outputKey;
    			var xlsUrl = "/rest/download/cva_xlsx?key=" + outputKey;
    			csv = <a href={csvUrl} download>Download</a>;
    			xls = <a href={xlsUrl} download>Download</a>;
    		}

        	var job = {
				dummy: message.job.dummy,
        		id: message.job.id,
        		name: message.job.name,
        		submission_time: submissionTimeStr,
        		now: nowStr,
        		previous_status: message.previous_status,
        		status: message.job.status,
        		csv: csv,
        		xls: xls,
        		output_key: outputKey,
	        };

        	// Append or replace
    		var jobs = this.state.jobs;
    		var row = -1;
    		for (var i = 0; i < jobs.length; i++) { 
    	    	if (jobs[i].id == job.id) {
    		    	row = i;
    		    }
	    	}

			if (row < 0) {
				this.setState({
					jobs: update(this.state.jobs, {$push: [job]}) 
				})
			} else {
				if (this.state.jobs[row].status != job.status) {
					this.setState({
						jobs: update(this.state.jobs, {[row] : {$set: job}}) 
					})
				}
			}
		}
    }
    
    render() {
        return (
        	<div>
        		<SockJsClient 
                	url={WS_URL}
        			topics={WS_TOPICS}
        			onMessage={this.handleData}
        			debug={false} />
        		<h2>Jet Jobs</h2>
        	    <Styles>
        	      <Table columns={columns} data={this.state.jobs} />
        	    </Styles>
            </div>
        );
    }
}

export default Jobs;