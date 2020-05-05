#include <iostream>
#include <fstream>
#include <memory>
#include <string>
#include <vector>
#include <sstream>
#include <string>
#include <grpcpp/grpcpp.h>
#include <ql/quantlib.hpp>
#include "../include/JetToPython.pb.h"
#include "../include/JetToPython.grpc.pb.h"
#include "../include/QLUtils.h"
#include "../include/JsonHandler.h"
#include "../include/rapidjson/document.h"

using namespace std;
using namespace rapidjson;
using namespace QuantLib;
using grpc::Channel;
using grpc::ClientContext;
using grpc::Status;
using jet_to_python::JetToPython;
using jet_to_python::InputMessage;
using jet_to_python::OutputMessage;

class CVARiskClient {
public:
    CVARiskClient(std::shared_ptr <Channel> channel) : stub_(JetToPython::NewStub(channel)) {}

    void stremingCall(std::vector <std::string> jsonBundles) {
        ClientContext context;
        ofstream output;
        output.open("mtms.json");
        auto stream = stub_->streamingCall(&context);
        for (vector<string>::iterator st = jsonBundles.begin(); st < jsonBundles.end(); st++) {
            InputMessage request;
            request.add_inputvalue(*st);
            stream->Write(request);

            OutputMessage response;
            stream->Read(&response);
            int total = response.outputvalue_size();
            for (int i = 0; i < total; i++) {
                cout << response.outputvalue(i) << endl;
                output << response.outputvalue(i) << endl;
            }
        }
        stream->WritesDone();
        Status status = stream->Finish();
        if (!status.ok()) {
            std::cout << status.error_code() << ": " << status.error_message()
                      << std::endl;
            std::cout << "RPC failed" << endl;
        }
        output.close();
    }

private:
    std::unique_ptr <JetToPython::Stub> stub_;
};

bool validateJson(string jsonstr) {
    bool haserrored = false;
    Document document;
    QLUtils qlutils;
    JsonHandler jsonHandler;
    try {
        document.Parse(jsonstr.c_str());
        Value& calcdatejson = document["calcdate"];

        Document calcd;
        calcd.Parse(calcdatejson.GetString());
        Value& cdatestr = calcd["calc_date"];
        Date calcDate = qlutils.strtoqldate(cdatestr.GetString());
        Value& trade = document["trade"];
        /**
         * trade json to proto
         */
        boost::shared_ptr<SwapTrade> swapTrade = jsonHandler.jsonToSwapTrade(trade.GetString());
        /**
        * curve json to proto
        */
        Value& curve = document["curve"];
        boost::shared_ptr<FlumaionQL::Curve> ircurve = jsonHandler.jsonToCurve(curve.GetString());
        /**
         * fixing json to proto
         */
        Value& fixing = document["fixing"];
        boost::shared_ptr<Fixing> fixings = jsonHandler.jsonToFixing(fixing.GetString());
        cout << "Received trade[" << swapTrade->tradeid() << "], curve[" << ircurve->curvename() << "]" << " for valuation on " << calcDate << endl;
    } catch(...) {
        cerr << "Invalid json string: " << jsonstr << endl;
        haserrored = true;
    }
    return haserrored;
}

int main(int argc, char **argv) {
    if (argc < 3) {
        cerr << "Usage: cvarisk_client <hostport (eg:0.0.0.0:50051)> <path to inputfile + name>> <validate (set to 0 to turn off, else on by default))>" << endl;
        return -1;
    }

    bool validate = true;
    if (argc == 4) {
        int val = stoi(argv[3]);
        if (val == 0) {
            validate = false;
        }
        else {
            validate = true;
        }
    }
    string server_address = argv[1];
    string inputfile = argv[2];

    cout << "server address: " << server_address << endl;
    cout << "inout file:" << inputfile << endl;
    grpc::ChannelArguments args;
    std::vector <std::unique_ptr<grpc::experimental::ClientInterceptorFactoryInterface>>
            interceptor_creators;
    auto channel = grpc::experimental::CreateCustomChannelWithInterceptors(
            server_address, grpc::InsecureChannelCredentials(), args,
            std::move(interceptor_creators));

    CVARiskClient client(channel);
    std::vector <std::string> bundles;

    ifstream infile;
    infile.open(inputfile);
    string line;
    bool exceptions = false;
    while(getline(infile, line)) {
        istringstream iss(line);
        if (validate) {
            exceptions = validateJson(iss.str());
        }
        bundles.push_back(iss.str());
    }
    if (exceptions) {
        cerr << "No grpc call will be made to server due to invalid json's" << endl;
        return -2;
    }
    else {
        cout << "All json's are valid - proceeding with grpc call" << endl;
    }
    infile.close();
    client.stremingCall(bundles);
    return 0;
}
