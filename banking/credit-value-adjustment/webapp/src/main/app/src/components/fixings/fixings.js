import React, { Component } from 'react';
import {Collapse} from 'react-collapse';
import {Scroll} from 'react-scroll';
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
		Header: 'Curve Name',
		accessor: 'curvename',
	},
	{
		Header: 'Fixing Dates And Rates',
		accessor: 'fixing_dates_and_rates',
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
        		batch_size: '100',
        		calc_date: '2016-01-07',
        		debug: false,
        		fixings: [],
        		message: '...',
            	message_style: {
            		color: 'grey',
            		fontWeight: 'bold'
            	},
        		parallelism: '1'
            };
        this.getFixings = this.getFixings.bind(this);
        this.handleChange = this.handleChange.bind(this);
        this.handleSubmit = this.handleSubmit.bind(this);
    }

    handleChange(e) {
    	// Not quite worth switch...
    	if (e.target.name == 'batch_size') {
    	    this.setState({batch_size: event.target.value});
    	}
    	if (e.target.name == 'debug') {
    	    this.setState({debug: e.target.checked});
    	}
    	if (e.target.name == 'parallelism') {
    	    this.setState({parallelism: event.target.value});
    	}
    }
    
    handleSubmit(e) {
		e.preventDefault();
        setTimeout(() => {
	    	var client = rest.wrap(mime);
	    	var self = this;
	    	
	    	var restURL = '/rest/cva/run/?batch_size=' + this.state.batch_size
	    		+ '&calc_date=' + this.state.calc_date
	    		+ '&debug=' + this.state.debug
	    		+ '&parallelism=' + this.state.parallelism;

	    	client({path:restURL}).then(
	    			function(response) {
	    		console.log('response.entity', response.entity);
	        	var payload = response.entity;
	        	var text = '';
	        	var text_style = {};

	            var failure_message = payload.error_message;
	        	var success_message = 'Job \"' + payload.name + '\" launched at '+  payload.date;
	        	var failure_style = {
            		color: 'red',
            		fontWeight: 'bold'
            	}
	        	var success_style = {
	        		color: 'var(--hazelcast-blue)',
	            	fontWeight: 'lighter'
	            }
	            
	        	if (payload.error == true) {
	        		text = failure_message
	        		text_style = failure_style
	        	} else {
	        		text = success_message
	        		text_style = success_style
	        	}
	        	
	        	self.setState({
	        		message: update(self.state.message, {$set: text}) 
	        	});
	        	self.setState({
	        		message_style: update(self.state.message_style, {$set: text_style}) 
	        	});
	    	});
        }, 250)
        setTimeout(() => {
        	var self = this;
        	var cleared = '...';
        	var cleared_style = {
            		color: 'grey',
            		fontWeight: 'normal'
            	}
        	self.setState({
        		message: update(self.state.message, {$set: cleared})
        	});
        	self.setState({
        		message_style: update(self.state.message_style, {$set: cleared_style})
        	});
        }, 10000)
		window.location = "#";
    }
    
    getFixings(){
        setTimeout(() => {
	    	var client = rest.wrap(mime);
	    	var self = this;
	    	
	    	client({path:'/rest/fixings'}).then(
	    			function(response) {
	        	var fixingsResponse = response.entity.fixings;
	        	for (var i = 0; i < fixingsResponse.length; i++) {
	        		
	        		var fixing_dates = fixingsResponse[i].fixing_dates_ccyymmdd;
	        		var fixing_rates = fixingsResponse[i].fixing_rates;
	        		
	        		const dates_and_rates_p = [];
	        		
	        		for (var j = 0; j < fixing_dates.length; j++) {
	        			var pair = fixing_dates[j] + ' - ' + fixing_rates[j];
	        			dates_and_rates_p.push(<p>{pair}</p>);
	        		}
	        		
		        	var fixing_dates_and_rates_div = <div class="three_row_scrollbar">{dates_and_rates_p}</div>;
		        	
	        		var fixing = {
	        				curvename: fixingsResponse[i].curvename,
	        				fixing_dates_and_rates: fixing_dates_and_rates_div,
	        		};

	        		self.setState({
	        			fixings: update(self.state.fixings, {$push: [fixing]}) 
	        			});
	        	}
	    	});
        }, 0)
      }

    componentDidMount(){
        this.getFixings();
    }

	render() {
        return (
        	<div>
    		  <h2>Fixings</h2>
    	      <Styles>
    	        <Table columns={columns} data={this.state.fixings} />
    	      </Styles>
			  <form>
			  	<label for="calc_date">Calc Date:</label>
			  	<input type="text"     id="calc_date"   name="calc_date"   value={this.state.calc_date}   readonly/>
			  	<label for="debug">Debug:</label>
			  	<input type="checkbox" id="debug"       name="debug"
			  		checked={this.state.debug} onChange={this.handleChange}/>
			  	<label for="batch_size">Batch Size:</label>
			  	<input type="number"   id="batch_size"  name="batch_size"  value={this.state.batch_size}  min="1"
			  		onChange={this.handleChange}/>
				<label for="parallelism">Parallelism:</label>
			  	<input type="number"   id="parallelism" name="parallelism" value={this.state.parallelism} min="1"
			  		onChange={this.handleChange}/>
				<button onClick={this.handleSubmit}>Submit</button>
			  </form>
    	      <p style={this.state.message_style}>{this.state.message}</p>
    	    </div>
        );
    }
}

export default Fixings;
