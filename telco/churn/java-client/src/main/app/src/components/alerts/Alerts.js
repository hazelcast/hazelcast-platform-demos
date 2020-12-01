import React, {Component} from 'react';
import SockJsClient from 'react-stomp';
import {useTable} from 'react-table';
import styled from 'styled-components';
import update from 'immutability-helper';

// Note SockJsClient uses 'http' protocol not 'ws'
// See https://github.com/lahsivjar/react-stomp/blob/HEAD/API/
const WS_URL = 'http://' + window.location.host + '/hazelcast';
const WS_TOPICS_PREFIX = '/topics';
const WS_TOPICS = [ WS_TOPICS_PREFIX + "/slack" ];

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
	    	Header: 'Timestamp',
            accessor: 'now',
        },
    	{
	    	Header: 'Alert Message',
            accessor: 'payload',
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

// Format Java timestamp in milliseconds
function myISO8601(longStr) {
        var dateObj = new Date(Number(longStr));
        return dateObj.toISOString().replace('T',' ').split('.')[0];
}

class Alerts extends Component {
    constructor(props) {
        super(props);
        this.state = {
                alerts: []
        };
        this.handleData = this.handleData.bind(this);
    }
    
    handleData(message) {
        console.log("Alerts.js", "handleData()", message);

        var nowStr = myISO8601(message.now);

        var alert = {
                        now: nowStr,
                        payload: message.payload,
        };

        // Prepend
        var alerts = this.state.alerts;
		if (alerts.length == 0) {
            this.setState({
            	alerts: update(this.state.alerts, {$push: [alert]}) 
            })
        } else {
            this.setState({
            	alerts: [alert, ...this.state.alerts] 
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
        			<h2>Alerts</h2>
                    <Styles>
                      <Table columns={columns} data={this.state.alerts} />
                    </Styles>
            </div>
        );
    }
}

export default Alerts;
