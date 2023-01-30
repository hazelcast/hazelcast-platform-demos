import React, { Component } from "react";
import Websocket from "react-websocket";
import ReactTable from "react-table";
import "react-table/react-table.css";
import detectBrowserLanguage from 'detect-browser-language';

import Page from "../Page";
import Pagination from "../Pagination";
import Querying from '../querying'
import ItemDetails from "../item-details";
import "../../Table.css";

class Home extends Component {
    constructor(props) {
        super(props);
        this.state = {
            items: [],
            expanded: {},
            browserLanguage: "en",
            keyHeader: "?",
            f1Header: "?",
            f2Header: "?"
        };

        let transactionMonitorFlavor = "@my.transaction-monitor.flavor@".toUpperCase();
        switch (transactionMonitorFlavor) {
			case "ECOMMERCE":
				this.state.keyHeader = "Item Code";
				this.state.f1Header = "Total Sales";
				this.state.f2Header = "Average Price";
			    break;
			case "PAYMENTS":
				this.state.keyHeader = "Bank Code";
				this.state.f1Header = "Total";
				this.state.f2Header = "Average";
			    break;
			case "TRADE":
				this.state.keyHeader = "Symbol";
				this.state.f1Header = "Volume";
				this.state.f2Header = "Price";
			    break;
			default:
		 	console.log("home.js", "Unexpected value transactionMonitorFlavor", transactionMonitorFlavor);
		}

        this.sendMessage = this.sendMessage.bind(this);
        this.handleData = this.handleData.bind(this);
        this.onOpen = this.onOpen.bind(this);
    }

    componentDidMount(){
        this.setState({browserLanguage: detectBrowserLanguage()})
    }

    sendMessage(message) {
        this.refWebSocket.sendMessage(message);
    }

    onOpen() {
        setInterval(() => this.sendMessage("LOAD_ITEMS"), 1000);
    }

    handleData(data) {
        let result = JSON.parse(data);
        for (let i = 0; i < result.items.length; i++) {
            let oldUpDownField;
            if (typeof this.state.items[i] === "undefined") {
                oldUpDownField = 0;
            } else {
                oldUpDownField = this.state.items[i].upDownField;
            }
            result.items[i]["oldUpDownField"] = oldUpDownField;
        }
        this.setState({ items: result.items });
    }

    render() {
        const { items } = this.state;

        const columns = [
            {
                Header: this.state.keyHeader,
                accessor: "key",
                width: 300,
                Cell: ({ value }) => (
                    <span className="Table-highlightValue">{value}</span>
                    )
                },
                {
                    Header: "Name",
                    accessor: "name"
                },
                {
                    Header: this.state.f2Header,
                    accessor: "f2",
                    width: 300,
                    Cell: ({ value, columnProps: { className } }) => (
                        <span className={`Table-highlightValue Table-price ${className}`}>
                        {(value / 1).toLocaleString(this.state.browserLanguage, {
                            style: "currency",
                            currency: "USD"
                        })}
                        </span>
                        ),
                        getProps: (state, ri, column) => {
                            if (!ri) {
                                return {};
                            }
                            // console.log(ri.row);
                            const changeUp = ri.row._original.upDownField > ri.row._original.oldUpDownField;
                            const changeDown = ri.row._original.upDownField < ri.row._original.oldUpDownField;
                            const className = changeUp
                            ? "Table-changeUp"
                            : changeDown
                            ? "Table-changeDown"
                            : "";

                            return {
                                className
                            };
                        }
                    },
                    {
                        Header: this.state.f1Header,
                        accessor: "f1",
		                Cell: ({ value }) => (
        		            <span className="Table-highlightValue Table-price">{(value / 1).toLocaleString(this.state.browserLanguage, {
                		        style: "currency",
                        		currency: "USD"
                    	})}</span>
		                )
                    }
                ];

        // Same code duplicated in: src/main/app/src/components/item-details/item-details.js
        const WS_HOST = "ws:/" + window.location.host + "/transactions";

        return (
                    <Page header="Transaction Monitor Dashboard">
                        <Querying/>
                        <ReactTable
                        className="Table-main"
                        data={items}
                        columns={columns}
                        defaultPageSize={25}
                        expanded={this.state.expanded}
                        onExpandedChange={expanded => this.setState({ expanded })}
                        PaginationComponent={Pagination}
                        getTrProps={(state, rowInfo) => ({
                            className:
                            rowInfo && state.expanded[rowInfo.viewIndex] ? "Table-expanded" : ""
                        })}
                        getTdProps={(state, rowInfo) => {
                            return {
                                onClick: (e, handleOriginal) => {
                                    const { viewIndex } = rowInfo;
                                    this.setState(prevState => ({
                                        expanded: {
                                            ...prevState.expanded,
                                            [viewIndex]: !prevState.expanded[viewIndex]
                                        }
                                    }));
                                }
                            };
                        }}
                        SubComponent={original => (
                            <ItemDetails item={original.row.key} />
                            )}
                            />

                            <Websocket
                            url={WS_HOST}
                            onOpen={this.onOpen}
                            onMessage={this.handleData}
                            reconnect={true}
                            debug={true}
                            ref={Websocket => {
                                this.refWebSocket = Websocket;
                            }}
                            />
                        </Page>
                        );
                    }
                }

                export default Home;
