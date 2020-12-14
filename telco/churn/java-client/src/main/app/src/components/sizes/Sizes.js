import React, {Component} from 'react';
import SockJsClient from 'react-stomp';
import {useTable} from 'react-table';
import styled from 'styled-components';
import update from 'immutability-helper';

var rest = require('rest');
var mime = require('rest/interceptor/mime');

// Note SockJsClient uses 'http' protocol not 'ws'
// See https://github.com/lahsivjar/react-stomp/blob/HEAD/API/
const WS_URL = 'http://' + window.location.host + '/hazelcast';
const WS_TOPICS_PREFIX = '/topics';
const WS_TOPICS = [ WS_TOPICS_PREFIX + "/sizes" ];

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

function myISO8601(longStr) {
        var dateObj = new Date(Number(longStr));
        return dateObj.toISOString().replace('T',' ').split('.')[0];
}

class Sizes extends Component {
    constructor(props) {
        super(props);
        this.state = {
                data: [],
				message: '.',
                message_style: {
                	color: 'yellow',
                    'font-weight': 'lighter',
                    visibility: 'hidden'
                }
        };
        this.handleData = this.handleData.bind(this);
    }
    
    getData(){
    	console.log("Sizes.js", "getData()");
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
                    var size_error = sizesResponse[i].error;

					if (size_error.length > 0) {
						var divClass = "sqlRed";
                        var divMessage = size_error;
                        var errorRow = <div class={divClass}>{divMessage}</div>;
                    	var datum = {
                    		name: size_name,
                        	size: errorRow
	                	};
					} else {
						var divClass = "sqlBlueLight";
                        var divMessage = size_size;
                        var sizeRow = <div class={divClass}>{divMessage}</div>;
                    	var datum = {
                    		name: size_name,
                        	size: sizeRow
	                	};
                    }                                                                
                                
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

    handleData(message) {
    	console.log("Sizes.js", "handleData()", message);

        setTimeout(() => {
			var self = this;
                        
			var sizesResponse = message.sizes;
			var oldData = self.state.data;
			var newData = [];
			
			for (var i = 0; i < oldData.length; i++) {
			    // Copy old
			    var size_name = oldData[i].name;
				var divClass = oldData[i].size.props.class;
                var divMessage = oldData[i].size.props.children;
                var sizeRow = <div class={divClass}>{divMessage}</div>;

                // Possible override
			    for (var j = 0; j < sizesResponse.length; j++) {
			    	if (size_name === sizesResponse[j].name) {
						divClass = String("sqlYellow");
        		        divMessage = sizesResponse[j].size;
                		sizeRow = <div class={divClass}>{divMessage}</div>;
			    	}
			    } 
			    			    
                var datum = {
                	name: size_name,
                    size: sizeRow
	            };
				newData.push(datum);
			}	

			self.setState({
            	data: update(self.state.data, {$set: newData}) 
            });
    	
	    	// Note when last updated
	   	 	var nowStr = myISO8601(message.now);
	    	var text = 'Sizes changed: ' + nowStr;
   		 	var text_style = {
        	    color: 'var(--hazelcast-orange)',
            	'font-weight': 'lighter',
	            'font-size': 'smaller',
    	        visibility: 'visible'
	        }
			self.setState({
 				message: update(self.state.message, {$set: text}) 
	        });
			self.setState({
        		message_style: update(self.state.message_style, {$set: text_style}) 
  	      });
        }, 100)
		// After 10 seconds reset any highlit row back to normal
        setTimeout(() => {
 			var self = this;
                        
			var oldData = self.state.data;
			var newData = [];
			
			for (var i = 0; i < oldData.length; i++) {
			    // Copy old
			    var size_name = oldData[i].name;
				var divClass = oldData[i].size.props.class;
                var divMessage = oldData[i].size.props.children;
                var sizeRow = <div class={divClass}>{divMessage}</div>;

                // Possible override
 			    if (oldData[i].size.props.class === 'sqlYellow') {
					divClass = String("sqlBlueLight");
                	sizeRow = <div class={divClass}>{divMessage}</div>;
			    }
			    			    
                var datum = {
                	name: size_name,
                    size: sizeRow
	            };
				newData.push(datum);
			}	

			self.setState({
            	data: update(self.state.data, {$set: newData}) 
            });
            
        }, 10000)
        window.location = "#";
    	
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
        			<p style={this.state.message_style}>{this.state.message}</p>
	            </div>
        );
    }
}

export default Sizes;
