#include <ctime>
#include <iostream>
#include "../include/JsonHandler.h"
#include "../include/Pricer.h"
#include "../include/JetToCpp.grpc.pb.h"
#include <grpcpp/grpcpp.h>
using namespace std;
using namespace QuantLib;
using namespace FlumaionQL;
using namespace com_hazelcast_platform_demos_banking_cva;
using namespace grpc;

// Record the last member, should be one per C++ module.
string jetMember = "";
long jetMemberChanges = 0;

void getMTM(string jsonBundle, string* mtmjson) {
    JsonHandler jsonHandler;
    QLUtils qlutils;
    /**
     * Parse json bundle
     */
    rapidjson::Document d;
    try {
        d.Parse(jsonBundle.c_str());
    } catch(...) {
        cerr << "Error parsing json:" << jsonBundle << endl;
        boost::shared_ptr<FlumaionQL::MTM> mtm(new FlumaionQL::MTM());
        mtm->set_haserrored(true);
        mtm->set_error("error parsing json" + jsonBundle);
        return;
    }
    /**
     * Parse calcdate json and convert to QuantLib date
     */
    Value& calcdatejson = d["calcdate"];
    Document calcd;
    calcd.Parse(calcdatejson.GetString());
    Value& cdatestr = calcd["calc_date"];
    Date calcDate = qlutils.strtoqldate(cdatestr.GetString());
    /**
     * trade json to proto
     */
    Value& trade = d["trade"];
    boost::shared_ptr<SwapTrade> swapTrade = jsonHandler.jsonToSwapTrade(trade.GetString());
    /**
    * curve json to proto
    */
    Value& curve = d["curve"];
    boost::shared_ptr<FlumaionQL::Curve> ircurve = jsonHandler.jsonToCurve(curve.GetString());
    /**
     * fixing json to proto
     */
    Value& fixing = d["fixing"];
    boost::shared_ptr<Fixing> fixings = jsonHandler.jsonToFixing(fixing.GetString());
    /**
     * Track if calling member changes
     */
    Value& debug = d["debug"]; 
    if (jetMember.size() == 0) {
        jetMember = debug.GetString();
    } else {
        if (jetMember.compare(debug.GetString()) != 0) {
            jetMember = debug.GetString();
            jetMemberChanges++;
        }
    }
    /**
     * Get MtM
     */
    Pricer pricer;
    boost::shared_ptr<FlumaionQL::MTM> mtm = pricer.fetchMTMs(ircurve, fixings, swapTrade, calcDate);
    jsonHandler.MtmToJson(mtm, mtmjson);
}

class JetToCppServiceImpl final : public JetToCpp::Service {
    Status streamingCall(ServerContext* context,
                         ServerReaderWriter<OutputMessage, InputMessage>* stream) override {
        InputMessage request;
        std::time_t now;
        long count = 0;
        long threshold = 100;
        while (stream->Read(&request)) {
            OutputMessage response;
            int total = request.inputvalue_size();
            for (int i=0; i<total; i++) {
                string* mtmjson = response.add_outputvalue();
                getMTM(request.inputvalue(i), mtmjson);
            }
            stream->Write(response);
            // Includes first run as zero, then at 100, 200, 400 .. until every 12800
            if ((count % threshold) == 0) {
                now = std::time(NULL);
                std::cout << "Stream count " << count << " @ " << std::put_time(std::localtime(&now), "%FT%T")
                    << ", batch size " << total 
                    << ", member " << jetMember << " (changes: " << jetMemberChanges << ")"
                    << std::endl;
                if (threshold < 10000) {
                    threshold += threshold;
                }
            }
            count++;
        }
        return Status::OK;
    }
};
void RunServer(string server_address) {
    JetToCppServiceImpl service;
    ServerBuilder builder;
    // Listen on the given address without any authentication mechanism.
    builder.AddListeningPort(server_address, grpc::InsecureServerCredentials());
    // Register "service" as the instance through which we'll communicate with
    // clients. In this case, it corresponds to an *synchronous* service.
    builder.RegisterService(&service);
    // Finally assemble the server.
    std::unique_ptr<Server> server(builder.BuildAndStart());
    std::cout << "Server listening on " << server_address << std::endl;

    // Wait for the server to shutdown. Note that some other thread must be
    // responsible for shutting down the server for this call to ever return.
    server->Wait();
}

int main(int argc, const char * argv[]) {
    if (argc < 2) {
        cerr << "Usage: cvarisk_server <hostport (eg:0.0.0.0:50051)>>" << endl;
        return -1;
    }
    string server_address(argv[1]);
    RunServer(server_address);
    return 0;
}
