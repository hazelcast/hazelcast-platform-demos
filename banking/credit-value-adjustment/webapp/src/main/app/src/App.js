'use strict';

import React, { Component } from 'react';
import Fixings from './components/fixings'
import Jobs from './components/jobs'

const ReactDOM = require('react-dom');

class App extends Component {

	constructor(props) {
		super(props);
	}

	render() {
		return (
				<div>
					<Fixings />
					<Jobs />
				</div>
		)
	}
}

export default App;

ReactDOM.render(
	<App />,
	document.getElementById('root')
)