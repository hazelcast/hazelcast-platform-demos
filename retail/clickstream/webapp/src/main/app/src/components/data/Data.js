import React, {Component} from 'react';
import SockJsClient from 'react-stomp';
import {useTable} from 'react-table';
import styled from 'styled-components';
import update from 'immutability-helper';

// Note SockJsClient uses 'http' protocol not 'ws'
// See https://github.com/lahsivjar/react-stomp/blob/HEAD/API/
const WS_URL = 'http://' + window.location.host + '/hazelcast';
const WS_TOPICS_PREFIX = '/info';
const WS_TOPICS = [ WS_TOPICS_PREFIX + "/data" ];

// Styled-components. Could move to CSS
const Styles = styled.div `
  padding: 1rem;
  table {
    border-spacing: 0;
    width: 100%;
    text-align: center;
    th {
      color: brown;
      margin: 0;
      padding: 0.5rem;
    }
    td {
      color: purple;
      margin: 0;
      padding: 0.5rem;
    }
  }
`

// Table columns
const columns = [
        {
            Header: 'Key',
            accessor: 'left',
        },
        {
            Header: 'Value',
            accessor: 'right',
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

class Data extends Component {
    constructor(props) {
        super(props);
        this.state = {
            items: []
        };
        this.handleData = this.handleData.bind(this);
    }
    
    handleData(message) {
        console.log("Data.js", "handleData()", message);
        
        var payload = message.payload;
        var rows = [];
        

        var i = 0;
        for (; i < payload.length; i++) {
            var text_l = payload[i].split(',')[0];
            if (text_l === "TIMESTAMP") {
            	var divClass = "dataPurple";
			} else {
	            var divClass = "dataBlue";
			}
            var text_r = payload[i].split(',')[1];
            
            var text_l_styled = <div class={divClass}>{text_l}</div>;
            var text_r_styled = <div class={divClass}>{text_r}</div>;
            
            var row = {
                         left: text_l_styled,
                         right: text_r_styled
                        };
            rows.push(row);
        }
	          
        this.setState({
            items: update(this.state.items, {$set: rows})
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
                         <div class="dataOuterBox">
                             <h3>Data</h3>
                             <div class="dataOutputBox">
                                 <Styles>
                                     <Table columns={columns} data={this.state.items} />
                                 </Styles>
                             </div>
                         </div>
                    </div>
                </div>
        );
    }
}

export default Data;