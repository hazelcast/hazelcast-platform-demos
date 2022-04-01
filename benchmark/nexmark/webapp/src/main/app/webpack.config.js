const PerspectivePlugin = require("@finos/perspective-webpack-plugin");
const path = require('path');

module.exports = {
    entry: './src/index.tsx',
    mode: 'production',
    resolve: {
        extensions: [".ts", ".tsx", ".js"],
    },
    plugins: [
        new PerspectivePlugin()
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
