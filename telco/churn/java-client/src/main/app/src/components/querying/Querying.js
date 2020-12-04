import React, {Component} from 'react';
import {useTable} from 'react-table';
import styled from 'styled-components';
import update from 'immutability-helper';

var rest = require('rest');
var mime = require('rest/interceptor/mime');

// Styled-components. Could move to CSS
const Styles = styled.div `
  table {
    border-spacing: 0;
    width: 100%;
    tr {
    }
    td {
    }
  }
`
  
// Table columns
const columns = [
        {
                accessor: 'sqlRow',
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
              <tbody {...getTableBodyProps()}><pre>
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
              </pre></tbody>
            </table>
          )
}

class Querying extends Component {
    constructor(props) {
        super(props);
        this.state = {
				query: 'SELECT * FROM sentiment',
          	    message1: [],
                message1_style: {
                	color: 'yellow',
                    'font-weight': 'bold'
                },
                message2: '',
                message2_style: {
                	color: 'yellow',
                    'font-weight': 'bold'
                }
            };
        this.handleChange = this.handleChange.bind(this);
        this.handleSubmit = this.handleSubmit.bind(this);
    }

    componentDidMount(){
        var rows = [];
        for (var i = 0; i < 12; i++) {
        	var text = <div>&nbsp;</div>;
        	var row = { sqlRow: text };
        	rows.push(row);
		}
		this.setState({	
            message1: update(this.state.message1, {$push: rows}) 
        })
		this.setState({	
            message1: update(this.state.message1, {$push: rows}) 
        })
		var text2 = '.';
	    var text2_style = {visibility: 'hidden'};
		this.setState({	
            message2: update(this.state.message2, {$set: text2}) 
        })
		this.setState({	
            message2_style: update(this.state.message2_style, {$set: text2_style}) 
        })
    }

    handleChange(e) {
        if (e.target.name == 'query') {
        	console.log("Querying.js", "handleChange()", this.state.query);
            this.setState({query: event.target.value});
        }
    }
    
     handleSubmit(e) {
     	console.log("Querying.js", "handleSubmit()", this.state.query);
     	e.preventDefault();
        setTimeout(() => {
                var client = rest.wrap(mime);
                var self = this;
                
                var restURL = '/rest/sql/?query=' + this.state.query;

                client({path:restURL}).then(
                                function(response) {
                        console.log("Querying.js", "handleSubmit()", "response.entity", response.entity);
                        var payload = response.entity;
                        
                        var text1 = [];
                        var text2 = '?';
                        var text1_style = {};
                        var text2_style = {};

	                    var error_message = payload.error;
	                    var warning_message = payload.warning;
	                    var rows_message = payload.rows;

                        var error_style = {
                        	color: 'red',
                        	'font-weight': 'bold'
	                	}
    					var warning_style = {
                            color: 'yellow',
                        	'font-weight': 'lighter'
                    	}
    					var rows_style = {
                            color: 'var(--hazelcast-blue-light)',
                        	'font-weight': 'lighter'
                    	}

        				var text1 = [];
                        if (payload.error.length > 0) {
                        		var divClass = "sqlRed";
                        		var divMessage = payload.error;
                        		var sqlRow = <div class={divClass}>{divMessage}</div>;
                        		var errorRow = { sqlRow: sqlRow };
                        		text1.push(errorRow);
                                for (var i = 1; i < 12; i++) {
        							var text = <div>&nbsp;</div>;
        							var row = { sqlRow: text };
        							text1.push(row);
								}
                                text1_style = error_style;
                        } else {
                        		var i = 0;
                                for (; i < rows_message.length; i++) {
        							var text = rows_message[i];
        							var row = { sqlRow: text };
        							text1.push(row);
								}
                                for (; i < 12; i++) {
        							var text = <div>&nbsp;</div>;
        							var row = { sqlRow: text };
        							text1.push(row);
								}
                                text1_style = rows_style;
                        }
                        if (warning_message.length > 0) {
	                        text2 = warning_message
                        	text2_style = warning_style;
						} else {
	                        text2 = '.';
                        	text2_style = {visibility: 'hidden'};
                        }
  
                        self.setState({
                                message1: update(self.state.message1, {$set: text1})  
                        });
                        self.setState({
                                message2: update(self.state.message2, {$set: text2}) 
                        });
                        self.setState({
                                message1_style: update(self.state.message1_style, {$set: text1_style}) 
                        });
                        self.setState({
                                message2_style: update(self.state.message2_style, {$set: text2_style}) 
                        });
                });
        }, 250)
        setTimeout(() => {
                var self = this;

        		var text1 = [];
        		for (var i=0; i < 12; i++) {
		        	var text = <div>&nbsp;</div>;
        			var row = { sqlRow: text };
        			text1.push(row);
				}
                
				var text2 = '.';
	            var text2_style = {visibility: 'hidden'};
				
                self.setState({
                	message1: update(self.state.message1, {$set: text1})  
                });
                self.setState({
                	message2: update(self.state.message2, {$set: text2}) 
                });
                self.setState({
                	message2_style: update(self.state.message2_style, {$set: text2_style}) 
                });
        }, 15000)
        window.location = "#";
    }
    
    render() {
        return (
                <div>
        			<h2>Querying</h2>
					<div class="greenOuterBox">
                        <div class="greenInputBox" id="sqlIn">
                        	<form>
                                <label for="query">Query:</label>
                                <input type="text" 
                                 id="query" name="query" defaultValue={this.state.query}
                                 onChange={this.handleChange}/>
                                <button onClick={this.handleSubmit}>Submit</button>
                          	</form>
                    	</div>
                    </div>
                    <div class="greenOuterBox">     
                        <div class="greenOutputBox" id="sqlOut">
                        	<code>
	                            <Styles>
    	                			<Table columns={columns} data={this.state.message1} />
        	            		</Styles>
                        	</code>
                        	<p style={this.state.message2_style}>{this.state.message2}</p>
                    	</div>
                    </div>
	            </div>
        );
    }
}

export default Querying;
