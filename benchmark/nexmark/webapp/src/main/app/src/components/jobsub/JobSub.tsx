import React, { useState } from "react";

var rest = require('rest');
var mime = require('rest/interceptor/mime');

const JobSub: React.FunctionComponent = () => {
  const [params, setParams] = useState({
		kind: "Q05HotItems",
		eventsPerSecond: 10,
		windowSizeMillis: 10000
	});
  const [message, setMessage] = useState('');
  const [messageClass, setMessageClass] = useState('');
  
  const onChange = (event) => {
    setParams({ ...params, [event.target.name]: event.target.value });
  };

  const onSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
	console.log('JobSub', 'onSubmit', params);
	var restURL = '/rest/submit/?kind=' + params.kind
                        + '&eventsPerSecond=' + params.eventsPerSecond
                        + '&windowSizeMillis=' + params.windowSizeMillis;
	setTimeout(() => {
		var client = rest.wrap(mime);
		
		client({path:restURL}).then(
                                function(response) {
			console.log('JobSub', 'onSubmit', response.entity);
			var payload = response.entity;
            var text = '';
                        
			if (payload.error == true) {
            	text = payload.error_message;
            	setMessageClass("jobNotOk");
            } else {
                text = 'Job \"' + payload.name + '\" launched with Job ID '+  payload.id;
            	setMessageClass("jobOk");
            }
			setMessage(text);
            });
        }, 250);
    setTimeout(() => {
	    setMessage("");
        }, 10000);
  	}

  return (
	<div className="formOuterBox">
    	<div>
      		<form onSubmit={onSubmit}>
      			<label htmlFor="kind">Test: &nbsp;</label>
      			<select id="kind" name="kind"
      				onChange={onChange}
					>
                	<option value="Q01CurrencyConversion"       disabled>Q01 - Currency Conversion</option>
                	<option value="Q02Selection"                disabled>Q02 - Selection</option>
                	<option value="Q03LocalItemSuggestion"      disabled>Q03 - Local Item Suggestion</option>
                	<option value="Q04AveragePriceForCategory"  disabled>Q04 - Average Price For Category</option>
                    <option value="Q05HotItems"                 selected>Q05 - Hot Item</option>
                	<option value="Q06AvgSellingPrice"          disabled>Q06 - Average Selling Price</option>
                	<option value="Q07HighestBid"                       >Q07 - Highest Bid</option>
                	<option value="Q08MonitorNewUsers"          disabled>Q08 - Monitor New Users</option>
                	<option value="Q13BoundedSideInput"         disabled>Q13 - Bounded Side Input</option>
    			</select> 
      			<label htmlFor="eventsPerSecond">&nbsp;Events/Second: &nbsp;</label>
    	    	<input id="eventsPerSecond" name="eventsPerSecond"
	        		value={params.eventsPerSecond}
    	      		onChange={onChange}
        	  		type="number" min="1" max="10000000000"
        		/>
      			<label htmlFor="windowSizeMillis">&nbsp;Window Size (ms): &nbsp;</label>
    	    	<input id="windowSizeMillis" name="windowSizeMillis"
	        		value={params.windowSizeMillis}
    	      		onChange={onChange}
        	  		type="number" min="10" max="10000"
        		/>
        		<button type="submit" className="formButton">Submit</button>
      		</form>
	    	</div>
	    <div className="formOutputBox"><p className={messageClass}>{message}</p></div>
    </div>
  );
};

export default JobSub;
