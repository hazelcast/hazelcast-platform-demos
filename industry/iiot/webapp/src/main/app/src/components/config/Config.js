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
const WS_TOPICS_PREFIX = '/data';
const WS_TOPICS = [ WS_TOPICS_PREFIX + "/config" ];

// Styled-components. Could move to CSS
const Styles = styled.div `
  padding: 1rem;
  table {
    border-spacing: 0;
    width: 100%;
    text-align: center;
    th {
      color: blueviolet;
      margin: 0;
      padding: 0.5rem;
	  text-align: left;
    }
    td {
      color: darkblue;
      margin: 0;
      padding: 0.5rem;
	  text-align: left;
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
            Header: 'Value',
            accessor: 'value',
        }
        /* TODO
        ,{
            Header: 'Updated',
            accessor: 'since',
        }
        */
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

class Config extends Component {
    constructor(props) {
        super(props);
        this.state = {
            info: [],
            message: '_',
            message_style: {
            	color: 'grey',
                fontWeight: 'normal'
            },
            newKey: '',
            newValue: ''
        };
        this.getData = this.getData.bind(this);
        this.handleData = this.handleData.bind(this);
        this.handleChange = this.handleChange.bind(this);
        this.handleSubmit = this.handleSubmit.bind(this);
    }

    // REST call to MyConfigHandler.java	
    getData(){
        console.log("Config.js", "getData()");
    	setTimeout(() => {
        	var client = rest.wrap(mime);
            var self = this;
                
            client({path:'/rest/config/get'}).then(
                                function(response) {
		        var rows = [];
				var payload = response.entity;

				for (var i = 0; i < payload.payload.length; i++) {
					var tuple = payload.payload[i];
            		var row = { key: tuple.key, value: tuple.value, since: tuple.since };
            		rows.push(row);
        		}

        		self.setState({
            		info: update(self.state.info, {$set: rows})
        		})         
            });
        }, 0);
	}

	// Activated by WebSocket receipt of data    
    handleData(message) {
        console.log("Config.js", "handleData()", message);

        var rows = [];

		for (var i = 0; i < message.payload.length; i++) {
			var tuple = message.payload[i];
            var row = { key: tuple.key, value: tuple.value, since: tuple.since };
            rows.push(row);
        }
                
        this.setState({
            info: update(this.state.info, {$set: rows})
        })         
    }
    
    // Stash form value into memory
    handleChange(e) {
        if (e.target.name == 'newKey') {
            this.setState({newKey: event.target.value});
        }
        if (e.target.name == 'newValue') {
            this.setState({newValue: event.target.value});
        }
    }
    
    // Rest call to MyConfigHandler.java	
    handleSubmit(e) {
        console.log("Config.js", "handleSubmit()", this.state.newKey, this.state.newValue);
    	e.preventDefault();
        setTimeout(() => {
            var client = rest.wrap(mime);
            var self = this;
                
            var restURL = '/rest/config/set'
                        + '?the_key=' + this.state.newKey
                        + '&the_value=' + this.state.newValue;

            client({path:restURL}).then(
                                function(response) {
                var payload = response.entity;

                var text = 'map.put(\'' + payload.the_key
                	+ '\',\'' + payload.the_value
                	+ '\') returned previous value: \'' + payload.the_old_value + '\'';
                var text_style = {
	                color: 'navy',
    	            fontWeight: 'bold'
				};

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

            var cleared = '_';
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

    componentDidMount(){
        this.getData();
    }
        
    render() {
        return (
                <div>
                    <SockJsClient 
                        url={WS_URL}
                                topics={WS_TOPICS}
                                onMessage={this.handleData}
                                debug={false} />
                     <div>
                         <div class="configOuterBox">
                             <h2>Config</h2>
                             <div class="configOutputBox">
                                 <Styles>
                                     <Table columns={columns} data={this.state.info} />
                                 </Styles>
                             </div>
                         </div>
                         <form>
                                <label for="newKey">Key:</label>
                                <input type="text" id="newKey" name="newKey" value={this.state.newKey} onChange={this.handleChange}/>
                                <label for="newValue">Value:</label>
                                <input type="text" id="newValue" name="newValue" value={this.state.newValue} onChange={this.handleChange}/>
                                <button onClick={this.handleSubmit}>Add/Update</button>
                          </form>
              			  <p style={this.state.message_style}>{this.state.message}</p>
                    </div>
                </div>
        );
    }
}

export default Config;