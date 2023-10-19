import React from 'react';
import ReactDOM from 'react-dom';
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import App from './App';

it('renders without crashing', () => {
  const div = document.createElement('div');
  ReactDOM.render(
    <BrowserRouter>
      <Routes>
        <Route component={App}/>
      </Routes>
    </BrowserRouter>,			  
	div);
  ReactDOM.unmountComponentAtNode(div);
});
