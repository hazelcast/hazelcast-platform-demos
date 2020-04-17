'use strict';

const React = require('react');
const ReactDOM = require('react-dom');

class App extends React.Component {

	constructor(props) {
		super(props);
	}

	render() {
		return (
			<div>
				<p>Hello from App.js</p>
			</div>
		)
	}
}

ReactDOM.render(
	<App />,
	document.getElementById('react')
)