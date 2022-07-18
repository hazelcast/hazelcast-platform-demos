const path = require('path');

module.exports = {
    entry: './src/index.tsx',
    mode: 'production',
    resolve: {
        extensions: [".ts", ".tsx", ".js"],
    },
    plugins: [
    ],        
    output: {
        path: __dirname,
        filename: './static/built/bundle.js'
    },
        module: {
                rules: [
                        {
                                test: path.join(__dirname, '.'),
                                exclude: /(node_modules)/,
                                use: [{
                                        loader: 'ts-loader'
                                }]
                        }
                ]
        }
};
