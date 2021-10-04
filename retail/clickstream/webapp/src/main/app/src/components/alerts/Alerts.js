import React, {Component} from 'react';
import SockJsClient from 'react-stomp';
import {useTable} from 'react-table';
import styled from 'styled-components';
import update from 'immutability-helper';

// Note SockJsClient uses 'http' protocol not 'ws'
// See https://github.com/lahsivjar/react-stomp/blob/HEAD/API/
const WS_URL = 'http://' + window.location.host + '/hazelcast';
const WS_TOPICS_PREFIX = '/info';
const WS_TOPICS = [ WS_TOPICS_PREFIX + "/alerts" ];

// Styled-components. Could move to CSS
const Styles = styled.div `
  padding: 1rem;
  table {
    border-spacing: 0;
    width: 100%;
    text-align: center;
    th {
      color: aquamarine;
      margin: 0;
      padding: 0.5rem;
    }
    td {
      color: yellow;
      margin: 0;
      padding: 0.5rem;
    }
  }
`

// Table columns
const columns = [
        {
            Header: 'Timestamp',
            accessor: 'timestamp',
        },
        {
            Header: 'Message',
            accessor: 'payload',
        }
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

class Alerts extends Component {
    constructor(props) {
        super(props);
        this.state = {
            info: []
        };
        this.handleData = this.handleData.bind(this);
    }
    
    handleData(message) {
        console.log("Alerts.js", "handleData()", message);

        var infet = {
             timestamp: message.timestamp,
             payload: message.payload
        };

        // Prepend
        const MAX_SHOW = 5;
        var info = this.state.info;
        if (info.length == 0) {
                        // Set
            this.setState({
                info: update(this.state.info, {$push: [infet]}) 
            })
        } else {
            if (info.length >= MAX_SHOW) {
                this.state.info.splice(MAX_SHOW- 1);
                    this.setState({
                    info: update(this.state.info, {$push: [this.state.info]}) 
                    })
            }
            // Prepend
            this.setState({
                        info: [infet, ...this.state.info] 
            })
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
                     <div>
                         <div class="alertsOuterBox">
                             <h2>Alerts</h2>
                             <div class="alertsOutputBox">
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

export default Alerts;