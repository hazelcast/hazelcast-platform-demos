import React, { Component } from 'react';
import Fixings from './components/fixings'
import Jobs from './components/jobs'

class App extends Component {
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
