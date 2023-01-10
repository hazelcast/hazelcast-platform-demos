import React, {Component, Fragment} from 'react'

import Websocket from 'react-websocket';
import ReactTable from 'react-table';
import 'react-table/react-table.css';
import Pagination from '../Pagination';
import detectBrowserLanguage from 'detect-browser-language';

class ItemDetails extends Component {

    constructor(props) {
        super(props);
        this.state = {
            item: [],
            loading: true,
            browserLanguage: "en"
        };

        //let transactionMonitorFlavor = "@my.transaction-monitor.flavor@".toUpperCase();
        //switch (transactionMonitorFlavor) {
		//	case "ECOMMERCE":
		//	    break;
		//  case "PAYMENTS_ISO20022":
		//		break;
		//	case "TRADE":
		//	    break;
		//	default:
		// 	console.log("item-details.js", "Unexpected value transactionMonitorFlavor", transactionMonitorFlavor);
		//}

        this.sendMessage = this.sendMessage.bind(this);
        this.handleData = this.handleData.bind(this);
        this.onOpen = this.onOpen.bind(this);
        this.renderPagination = this.renderPagination.bind(this);
    }

    componentDidMount(){
        this.setState({browserLanguage: detectBrowserLanguage()})
    }

    sendMessage(message) {
        this.refWebSocket.sendMessage(message);
    }

    onOpen() {
        this.sendMessage('DRILL_ITEM ' + this.props.item);
    }

    handleData(data) {
        let result = JSON.parse(data);
        this.setState(prevState => ({
            item: prevState.item.concat(result.data),
            loading: false,
        }));
    }

    renderPagination(paginationProps) {
        return <Pagination {...paginationProps} extraData={<span>{this.props.item} has {this.state.item.length} records</span>}/>
    }

    render() {
        const {item} = this.state;

        // Same code duplicated in: src/main/app/src/components/home/home.js
        const WS_HOST = "ws:/" + window.location.host + "/transactions";

        const columns = [
            {
                Header: 'ID',
                accessor: 'id'
            },
            {
                Header: 'Time',
                accessor: 'timestamp',
                Cell: ({ value }) => (
                    <span className="Table-highlightValue">{new Date(value).toLocaleString(this.state.browserLanguage)}</span>
                )
            },
            {
                Header: 'Quantity',
                accessor: 'quantity',
                Cell: ({ value }) => (
                    <span className="Table-highlightValue">{value.toLocaleString()}</span>
                )

            },
            {
                Header: 'Price',
                accessor: 'price',
                Cell: ({ value }) => (
                    <span className="Table-highlightValue Table-price">{(value / 1).toLocaleString(this.state.browserLanguage, {
                        style: "currency",
                        currency: "USD"
                    })}</span>
                )
            }
        ];

        return <Fragment>
        <Websocket url={WS_HOST} onOpen={this.onOpen}
        onMessage={this.handleData}
        reconnect={true} debug={true}
        ref={Websocket => {
            this.refWebSocket = Websocket;
        }}/>
        <ReactTable
        data={item}
        columns={columns}
        defaultSorted={[
            {
                id: "timestamp",
                desc: true
            }
        ]}
        defaultPageSize={10}
        noDataText={this.state.loading ? 'Loading data...' : 'No rows found'}
        expanded={this.state.expanded}
        onExpandedChange={expanded => this.setState({expanded})}
        className="Table-subtable"
        PaginationComponent={this.renderPagination}
        />
        </Fragment>
    }
}

export default ItemDetails;
