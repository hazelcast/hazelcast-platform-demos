import React, {Component} from 'react';
import SockJsClient from 'react-stomp';
import {useTable} from 'react-table';
import styled from 'styled-components';
import update from 'immutability-helper';

// Note SockJsClient uses 'http' protocol not 'ws'
// See https://github.com/lahsivjar/react-stomp/blob/HEAD/API/
const WS_URL = 'http://' + window.location.host + '/hazelcast';
const WS_TOPICS_PREFIX = '/data';
const WS_TOPICS = [ WS_TOPICS_PREFIX + "/logging" ];

// Styled-components. Could move to CSS
const Styles = styled.div `
  padding: 1rem;
  table {
    border-spacing: 0;
    width: 100%;
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
            Header: 'Timestamp',
            accessor: 'timestamp',
        },
        {
            Header: 'Member',
            accessor: 'member',
        },
        {
            Header: 'Level',
            accessor: 'level',
        },
        {
            Header: 'Message',
            accessor: 'message',
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

class Slf4j extends Component {
    constructor(props) {
        super(props);
        this.state = {
            info: []
        };
        this.handleData = this.handleData.bind(this);
    }
    
    handleData(message) {
        console.log("Slf4j.js", "handleData()", message);

        var infet = {
             timestamp: message.timestamp,
             member: message.memberAddress,
             level: message.level,
             message: message.message
        };

        // Prepend
        const MAX_SHOW = 50;
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
                         <div class="loggingOuterBox">
                             <h2>Logging</h2>
                             <div class="loggingOutputBox">
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

export default Slf4j;