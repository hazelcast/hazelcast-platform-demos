//
// Created by raj on 05/07/19.
//

#include "../include/ProtoHandler.h"

boost::shared_ptr<FlumaionQL::DataExchange> ProtoHandler::dataExProto(string dataExProtoFile) {
    boost::shared_ptr<FlumaionQL::DataExchange> dataEx(new FlumaionQL::DataExchange());
    fstream input(dataExProtoFile, ios::in | ios::binary);
    if (!(*dataEx).ParseFromIstream(&input)) {
        throw ("Failed to parse " + dataExProtoFile);
    }
    input.close();
    return dataEx;
}

boost::shared_ptr<FlumaionQL::Curve> ProtoHandler::curveProto(string curveProtoFile) {
    boost::shared_ptr<FlumaionQL::Curve> curve(new FlumaionQL::Curve());
    fstream input(curveProtoFile, ios::in | ios::binary);
    if (!(*curve).ParseFromIstream(&input)) {
        throw ("Failed to parse " + curveProtoFile);
    }
    input.close();
    return curve;
}

boost::shared_ptr<FlumaionQL::Fixing> ProtoHandler::fixingProto(string fixingProtoFile) {
    boost::shared_ptr<FlumaionQL::Fixing> fixing(new FlumaionQL::Fixing());
    fstream input(fixingProtoFile, ios::in | ios::binary);
    if (!(*fixing).ParseFromIstream(&input)) {
        throw ("Failed to parse " + fixingProtoFile);
    }
    input.close();
    return fixing;
}

boost::shared_ptr<FlumaionQL::SwapTrade> ProtoHandler::tradeProto(string swapProtoFile) {
    boost::shared_ptr<FlumaionQL::SwapTrade> trade(new FlumaionQL::SwapTrade());
    fstream input(swapProtoFile, ios::in | ios::binary);
    if (!(*trade).ParseFromIstream(&input)) {
        throw ("Failed to parse " + swapProtoFile);
    }
    input.close();
    return trade;
}

void ProtoHandler::writeResultsProto(FlumaionQL::MTM& mtmproto, string destination) {
    fstream output(destination, ios::out | ios::trunc | ios::binary);
    if (!mtmproto.SerializeToOstream(&output)) {
        throw ("Failed to write MTM results protobuf to " + destination);
    }
    output.close();
}