import React, { Component } from 'react';
import Downloads from './components/downloads'
import Fixings from './components/fixings'
import Jobs from './components/jobs'
import Sizes from './components/sizes'

class App extends Component {
	render() {
		return (
				<div>
					<div class="minor_pane">
					  <table>
						<tr>
							<td> <Fixings /> </td>
							<td> <Sizes /> </td>
							<td> <Downloads /> </td>
						</tr>
					  </table>
					</div>
					<div class="minor_pane">
						<Jobs />
				    </div>
				</div>
		)
	}
}

export default App;
