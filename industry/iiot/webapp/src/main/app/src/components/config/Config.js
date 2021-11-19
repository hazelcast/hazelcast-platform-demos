import React, {Component} from 'react';
import SockJsClient from 'react-stomp';
import {useTable} from 'react-table';
import styled from 'styled-components';
import update from 'immutability-helper';

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
            info: []
        };
        this.handleData = this.handleData.bind(this);
    }
    
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
                    </div>
                </div>
        );
    }
}

export default Config;