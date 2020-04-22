import React, {Component} from 'react';
import {useTable} from 'react-table';
import SockJsClient from 'react-stomp';
import styled from 'styled-components';

// Note SockJsClient uses 'http' protocol not 'ws'
// See https://github.com/lahsivjar/react-stomp/blob/HEAD/API/
const WS_URL = 'http://' + window.location.host + '/hazelcast';
const WS_TOPICS_PREFIX = '/topics';
const WS_TOPICS = [ WS_TOPICS_PREFIX + "/job_state" ];

// Styled-components
const Styles = styled.div `
  padding: 1rem;
  table {
    border-spacing: 0;
    border: 1px solid black;
    width: 100%;
    tr {
      :last-child {
        td {
          border-bottom: 0;
        }
      }
    }
    th,
    td {
      margin: 0;
      padding: 0.5rem;
      border-bottom: 1px solid black;
      border-right: 1px solid black;
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
				accessor: 'x1',
			},
			{
				Header: 'Name',
				accessor: 'x2',
			},
			{
				Header: 'Submitted',
				accessor: 'x3',
			},
		],
	},
	{
		Header: 'Status',
		columns: [
			{
				Header: 'Previous',
				accessor: 'x4',
			},
			{
				Header: 'Current',
				accessor: 'x5',
			},
		],
	},
]

//Table data
const data = []

// The '<Table/>' HTML element
function Table({ columns, data }) {
	  // Use the state and functions returned from useTable to build your UI
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

	  // Render the UI for your table
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
        	    <Styles>
        	      <Table columns={columns} data={data} />
        	    </Styles>
            </div>
        );
    }
}

export default Jobs;