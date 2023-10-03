import React from 'react'

const Page = ({header, children}) =>
    <div className="App">
        <header className="App-header">
            <h1 className="App-title">Transaction Processing</h1>
            <span className="App-subtitle"><span className="sr-only">hazelcast</span>&nbsp;<img className="hazelcastLogo" src="/images/hazelcast-logo-reverse.png" srcSet="" alt=""/></span>
        </header>
        <main>
            {children}
        </main>
    </div>

export default Page
