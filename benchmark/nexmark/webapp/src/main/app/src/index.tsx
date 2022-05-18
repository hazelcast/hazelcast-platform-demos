import * as React from "react";
import * as ReactDOM from "react-dom";
import JobSub from "./components/jobsub/JobSub"

const jobsub = React.createElement(JobSub)

const App = (): React.ReactElement => {
    return React.createElement(
	    "div",
	    null,
	    React.createElement("div", "minor_pane", jobsub),
	    React.createElement("hr", "pane_separator", null),
	);
};

window.addEventListener("load", () => {
    ReactDOM.render(<App />, document.getElementById("root"));
});

