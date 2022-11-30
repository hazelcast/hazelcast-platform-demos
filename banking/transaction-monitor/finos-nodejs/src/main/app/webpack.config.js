const PerspectivePlugin = require("@finos/perspective-webpack-plugin");
const path = require('path');

module.exports = {
    entry: './src/index.tsx',
    mode: 'production',
    externals: [
        "dns",
    ],
    resolve: {
        extensions: [".ts", ".tsx", ".js"],
        fallback: {
            //"https": require.resolve("https-browserify"),
            //"https": false,
            //"os": require.resolve("os-browserify/browser")
            "os": false
            //"zlib": require.resolve("browserify-zlib")
        }
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
