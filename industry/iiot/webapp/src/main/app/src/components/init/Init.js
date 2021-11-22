import React, {Component} from 'react';
import {useTable} from 'react-table';
import styled from 'styled-components';
import update from 'immutability-helper';

var rest = require('rest');
var mime = require('rest/interceptor/mime');

class Init extends Component {
    constructor(props) {
        super(props);
        this.state = {
            message: '_',
            message_style: {
            	color: 'grey',
                fontWeight: 'normal'
            }
        };
        this.handleSubmit = this.handleSubmit.bind(this);
    }
    
    // Rest call to MyRestController.java	
    handleSubmit(e) {
        console.log("Init.js", "handleSubmit()");
    	e.preventDefault();
        setTimeout(() => {
            var client = rest.wrap(mime);
            var self = this;
                
            var restURL = '/rest/initialize';

            client({path:restURL}).then(
                                function(response) {
                var payload = response.entity;

                var text = '';
                var text_style = {};

                var failure_message = 'Failed at \'' + payload.failing_callable + '\'';
                var success_message = 'Done';
                var failure_style = {
	                color: 'red',
    	            fontWeight: 'bold'
                }
                var success_style = {
	                color: 'navy',
    	            fontWeight: 'bold'
                }
                    
                if (payload.error == true) {
                    text = failure_message
                    text_style = failure_style
                } else {
                    text = success_message
                    text_style = success_style
                }

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
        
    render() {
        return (
                <div>
                     <div>
                         <form>
                                <button onClick={this.handleSubmit}>Initialize Cluster</button>
                          </form>
              			  <p style={this.state.message_style}>{this.state.message}</p>
                    </div>
                </div>
        );
    }
}

export default Init;