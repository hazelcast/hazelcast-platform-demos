import React, { Component } from 'react';
import {useTable} from 'react-table';
import styled from 'styled-components';
import update from 'immutability-helper';

var rest = require('rest');
var mime = require('rest/interceptor/mime');

//Styled-components. Could move to CSS
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
		Header: 'Select',
		accessor: 'select',
	},
	{
		Header: 'Curve Name',
		accessor: 'curvename',
	},
	{
		Header: 'Action',
		accessor: 'action',
	},
	{
		Header: 'Fixing Dates',
		accessor: 'fixing_dates',
	},
	{
		Header: 'Fixing Rates',
		accessor: 'fixing_rates',
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

class Fixings extends Component {
    constructor(props) {
        super(props);
        this.state = {
    		fixings: []
        };
    }

    componentDidMount() { 
    	var client = rest.wrap(mime);

    	client({path:'/rest/fixings'}).then(function(response) {
    		//console.log('response.entity', response.entity);
        	var fixingsResponse = response.entity.fixings;
        	for (var i = 0; i < fixingsResponse.length; i++) {
        		//var cvaUrl = "/rest/cva/fixing/?key=" + i;
        		//cva = <a href={cvaUrl}>Run</a>;
        		
        		// Submit action needs to be background launch
        		var fixing = {
        				select: i,
        				curvename: fixingsResponse[i].curvename,
        				action: "TODO",
        				fixing_dates: "TODO",
        				fixing_rates: "TODO",
        		};
            	//var fixings = this.state.fixings;
          		//this.setState({
        		//	fixings: update(this.state.fixings, {$push: [fixing]}) 
        		//	});
        	}
    	});
    }
	    
	render() {
        return (
        	<div class="minor_pane">
    		  <h2>Fixings</h2>
    	      <Styles>
    	        <Table columns={columns} data={this.state.fixings} />
    	      </Styles>
    	    </div>
        );
    }
}

export default Fixings;
