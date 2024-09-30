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
  	  color: var(--color-blueberry);
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
		Header: 'Key',
		accessor: 'key',
	},
	{
		Header: 'Score',
		accessor: 'score',
	},
	{
	Header: 'Vector',
	accessor: 'vector',
	}
]
const ROWS = 5;
const COLS = 8;

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

class Search extends Component {
    constructor(props) {
        super(props);
        this.state = {
				ints: [ 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,],
				result: [],
        		message: '...',
            	message_style: {
            		color: 'red',
            		fontWeight: 'bold',
					visibility: 'hidden'
            	}
            };
		this.getInputTable = this.getInputTable.bind(this);
        this.handleChange = this.handleChange.bind(this);
        this.handleSubmit = this.handleSubmit.bind(this);
    }

    handleChange(e) {
		var i = parseInt(e.target.name.substring(1));
		var value = parseInt(e.target.value);
		var newInts = [];
		for (var j = 0;  j < this.state.ints.length; j++) {
			if (i == j) {
				newInts.push(value)
			} else {
				newInts.push(this.state.ints[j])
			}
		}
		this.setState({ints: newInts});
	}
    
    handleSubmit(e) {
		e.preventDefault();
        setTimeout(() => {
	    	var client = rest.wrap(mime);
	    	var self = this;
	    	
	    	var restURL = '/rest/vectorsearch?ints=' + this.state.ints;
			console.log("Search.js", "handleSubmit()", restURL);

	    	client({path:restURL}).then(
	    			function(response) {
	    		console.log("Search.js", 'response.entity', response.entity);
	        	var payload = response.entity;
	        	var text = '';
	        	var text_style = {};

	            var failure_message = payload.error_message;
	        	var success_message = "Search completed";
	        	var failure_style = {
            		color: 'red',
            		fontWeight: 'bold'
            	}
	        	var success_style = {
	        		color: 'var(--color-sky)',
	            	fontWeight: 'lighter'
	            }
	            
				var vectorsearch = [];
				
	        	if (payload.error == true) {
	        		text = failure_message
	        		text_style = failure_style
	        	} else {
	        		text = success_message
	        		text_style = success_style
					
					var resultResponse = response.entity.vectorsearch;
					for (var i = 0; i < resultResponse.length; i++) {
						var vector_values = resultResponse[i].vector_values;
						var vv = [];
						if (vector_values.length == ROWS * COLS) {
							for (var j = 0; j < ROWS; j++) {
								var remainder = "";
								for (var k = 0; k < COLS; k++) {
									var val = vector_values[k + j * COLS];
									if (k == 0) {
										remainder = val;
									} else {
										remainder += "," + val;
									}
								}
								var start = j * COLS;
								var end = j * (COLS + 1) - 1;
								var line = String(start).padStart(2, "0") + '-' + String(end).padStart(2, "0") + ":";
								vv.push(<p><b>{line}</b>{remainder}</p>);
							}
						} else {
							console.log("Search.js", "vector_values.length", vector_values.length);
							text = "Got " + vector_values.length;
							text_style = failure_style;
						}
						
						var line = {
							key : resultResponse[i].key,
							score : resultResponse[i].score,
							vector: vv
						};

						self.setState({
							result: update(self.state.result, {$push: [line]}) 
							});
					}
	        	}
				
				self.setState({
					message: update(self.state.result, {$set: vectorsearch}) 
				});
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
					color: 'red',
					fontWeight: 'bold',
					visibility: 'hidden'
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

	getInputTable() {
		const table = document.getElementById("inputTable");
		const tbody = table.getElementsByTagName("tbody")[0];
		for (var r = 0; r < ROWS; r++) {
			var tr = document.createElement("tr");
			for (var c = 0; c < COLS ; c++) {
				var td = document.createElement("td");
				var i = COLS * r + c;
				var padded = String(i).padStart(2, "0");
				var label = document.createElement("label");
				label.setAttribute("for", "i" + padded);
				label.appendChild(document.createTextNode(padded));
				var input = document.createElement("input");
				input.setAttribute("type", "number");
				input.setAttribute("value", this.state.ints[i]);
				input.setAttribute("min", "0");
				input.setAttribute("max", "500");
				input.setAttribute("id", "i" + padded);
				input.setAttribute("name", "i" + padded);
				input.onchange = this.handleChange; 
				td.appendChild(label);
				td.appendChild(input);
				tr.appendChild(td);
			}
			tbody.appendChild(tr);
		}
	}

	componentDidMount(){
	    this.getInputTable();
	}
	  
	render() {
        return (
        	<div>
			  <div class="vectorInput">
    		  <h2>Vector Search</h2>
			  <form>
			  	<table id="inputTable" class="vectorInputTable">
					<tbody/>
				</table>
			  </form>
			  </div>
			  <hr/>
			  <div class="vectorButton">
					<button onClick={this.handleSubmit}>Search</button>
					<p style={this.state.message_style}>{this.state.message}</p>
			  </div>
			  <hr/>
			  <div class="vectorOutput">
			  	<Styles>
			    	<Table columns={columns} data={this.state.result} />
			  	</Styles>
			  </div>
    	    </div>
        );
    }
}

export default Search;