import React, {Component} from 'react';
import SockJsClient from 'react-stomp';
import {useTable} from 'react-table';
import styled from 'styled-components';
import update from 'immutability-helper';

// Note SockJsClient uses 'http' protocol not 'ws'
// See https://github.com/lahsivjar/react-stomp/blob/HEAD/API/
const WS_URL = 'http://' + window.location.host + '/hazelcast';
const WS_TOPICS_PREFIX = '/info';
const WS_TOPICS = [ WS_TOPICS_PREFIX + "/members" ];

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
            Header: 'Members',
            accessor: 'member',
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

class Members extends Component {
    constructor(props) {
        super(props);
        this.state = {
            name: '',
            members: []
        };
        this.handleData = this.handleData.bind(this);
    }
    
    handleData(message) {
        console.log("Members.js", "handleData()", message);

        var newCluster = message.clusterName;
        var newMembers = message.members;
        var rows = [];

        var i = 0;
        for (; i < newMembers.length; i++) {
            var text = newMembers[i];
            var row = { member: text };
            rows.push(row);
        }
                
        this.setState({
            name: update(this.state.name, {$set: newCluster}) 
        })
        this.setState({
            members: update(this.state.members, {$set: rows})
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
                         <div class="membersOuterBox">
                             <h2>Cluster: {this.state.name}</h2>
                             <div class="membersOutputBox">
                                 <Styles>
                                     <Table columns={columns} data={this.state.members} />
                                 </Styles>
                             </div>
                         </div>
                    </div>
                </div>
        );
    }
}

export default Members;