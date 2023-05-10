const PerspectivePlugin = require("@finos/perspective-webpack-plugin");
const HtmlWebPackPlugin = require("html-webpack-plugin");
const path = require('path');

module.exports = {
    entry: './src/index.js',
    mode: 'production',
    plugins: [
        new HtmlWebPackPlugin({
            title: "@project.artifactId@",
        }),		
        new PerspectivePlugin(),
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
                                        loader: 'babel-loader',
                                        options: {
                                                presets: ["@babel/preset-env", "@babel/preset-react"]
                                        }
                        }]
                }
        ]
    },
    ignoreWarnings: [/Failed to parse source map/],
    devServer: {
        contentBase: [
            path.join(__dirname, "dist"),
        ],
    },
    target: 'node',
node: {
},
};
