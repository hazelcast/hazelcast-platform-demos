import React, { useState } from "react";
import Collapsible from 'react-collapsible';

var rest = require('rest');
var mime = require('rest/interceptor/mime');

const JobSub: React.FunctionComponent = () => {
  const [params, setParams] = useState({
		kind: "Q05HotItems",
		processingGuarantee: "NONE",
		eventsPerSecondA: 12,
		eventsPerSecondB: "BILLION",
		numDistinctKeys: 10000,
		slidingStepMillis: 500,
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
	var eventsPerSecond = params.eventsPerSecondA
	if (params.eventsPerSecondB == 'BILLION') {
		eventsPerSecond = eventsPerSecond * 1000000000;
	} else {
		eventsPerSecond = eventsPerSecond * 1000000;
	}
	var restURL = '/rest/submit?kind=' + params.kind
                        + '&processing_guarantee=' + params.processingGuarantee
                        + '&events_per_second=' + eventsPerSecond
                        + '&num_distinct_keys=' + params.numDistinctKeys
                        + '&sliding_step_millis=' + params.slidingStepMillis
                        + '&window_size_millis=' + params.windowSizeMillis;

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
       <Collapsible trigger="Click for job launch"
                    className="Collapsible"
                    openedClassName="Collapsible-opened"
                    triggerClassName="Collapsible__trigger"
                    triggerOpenedClassName="Collapsible__trigger-opened"
                    >	
    	<div className="formInputBox">
      		<form onSubmit={onSubmit}>
      			<div className='formField'>
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
                	<option value="Q07HighestBid"               disabled>Q07 - Highest Bid</option>
                	<option value="Q08MonitorNewUsers"          disabled>Q08 - Monitor New Users</option>
                	<option value="Q13BoundedSideInput"         disabled>Q13 - Bounded Side Input</option>
                	<option value="SourceBenchmark"                     >Internal - Source Benchmark</option>
    			 </select>
    			</div> 
      			<div className='formField'>
      			 <label htmlFor="processingGuarantee">Processing Guarantee: &nbsp;</label>
      			 <select id="processingGuarantee" name="processingGuarantee"
      				onChange={onChange}
					>
                    <option value="NONE"                 selected>None</option>
                	<option value="AT_LEAST_ONCE"                >At Least Once</option>
                	<option value="EXACTLY_ONCE"                 >Exactly Once</option>
    			 </select> 
    			</div> 
      			<div className='formField'>
      			 <label htmlFor="eventsPerSecondA">&nbsp;Events/Second Rate: &nbsp;</label>
    	    	 <input id="eventsPerSecondA" name="eventsPerSecondA"
	        		value={params.eventsPerSecondA}
    	      		onChange={onChange}
        	  		type="number" min="1" max="999"
        		 />
      			 <label htmlFor="eventsPerSecondB">&nbsp;Events/Second Scale: &nbsp;</label>
      			 <select id="eventsPerSecondB" name="eventsPerSecondB"
      				onChange={onChange}
					>
                    <option value="BILLION"              selected>Billion</option>
                	<option value="MILLION"                      >Million</option>
    			 </select> 
    			</div> 
      			<div className='formField'>
      			 <label htmlFor="numDistinctKeys">&nbsp;Number of distinct keys: &nbsp;</label>
    	    	 <input id="numDistinctKeys" name="numDistinctKeys"
	        		value={params.numDistinctKeys}
    	      		onChange={onChange}
        	  		type="number" min="10" max="10000"
        		 />
    			</div> 
      			<div className='formField'>
      			 <label htmlFor="slidingStepMillis">&nbsp;Sliding step (ms): &nbsp;</label>
    	    	 <input id="slidingStepMillis" name="slidingStepMillis"
	        		value={params.slidingStepMillis}
    	      		onChange={onChange}
        	  		type="number" min="10" max="10000"
        		 />
    			</div> 
      			<div className='formField'>
      			 <label htmlFor="windowSizeMillis">&nbsp;Window size (ms): &nbsp;</label>
    	    	 <input id="windowSizeMillis" name="windowSizeMillis"
	        		value={params.windowSizeMillis}
    	      		onChange={onChange}
        	  		type="number" min="10" max="10000"
        		 />
    			</div> 
      			<div className='formField'>
        		 <button type="submit" className="formButton">Submit</button>
    			</div> 
      		</form>
	    	</div>
	    <div className="formOutputBox"><p className={messageClass}>{message}</p></div>
      </Collapsible>
    </div>
  );
};

export default JobSub;
