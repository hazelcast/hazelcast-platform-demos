const path = require('path');

module.exports = {
    entry: './src/index.js',
    mode: 'production',
    resolve: {
        extensions: [".ts", ".tsx", ".js"],
    },
    output: {
        path: __dirname,
        filename: './static/built/bundle.js'
    },
        module: {
                rules: [
                        {
                            test: path.join(__dirname, '.'),
                            exclude: [ /(node_modules)/, /\.css$/ ],
                            use: [{
                                    loader: 'babel-loader',
                                    options: {
										presets: ["@babel/preset-env", "@babel/preset-react"]
									}
                            }]
                        },
                        {
                            test: /\.css$/,
                            exclude: /node_modules/,
                            use: [{ 
                                    loader: "style-loader" 
                                   }, { 
                                    loader: "css-loader"
                                 }],
                        }                        
                ]
        }
};
