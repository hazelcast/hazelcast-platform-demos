//
// Created by raj on 05/07/19.
//

#ifndef CVARISK_PROTOHANDLER_H
#define CVARISK_PROTOHANDLER_H

#include "exchange.pb.h"
#include "curve.pb.h"
#include "fixing.pb.h"
#include "swap.pb.h"
#include "mtms.pb.h"
#include <boost/shared_ptr.hpp>
#include <fstream>
#include <string>
#include <ql/quantlib.hpp>

using namespace std;
using namespace QuantLib;

class ProtoHandler {
public:
    boost::shared_ptr<FlumaionQL::DataExchange> dataExProto(string dataExProtoFile);
    boost::shared_ptr<FlumaionQL::Curve> curveProto(string curveProtoFile);
    boost::shared_ptr<FlumaionQL::Fixing> fixingProto(string fixingProtoFile);
    boost::shared_ptr<FlumaionQL::SwapTrade> tradeProto(string tradeProtoFile);
    void writeResultsProto(FlumaionQL::MTM& mtmproto, string destination);
};


#endif //CVARISK_PROTOHANDLER_H
