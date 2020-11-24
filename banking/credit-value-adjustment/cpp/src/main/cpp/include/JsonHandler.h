//
// Created by raj on 21/02/2020.
//

#ifndef HAZELCAST_JET_JSONHANDLER_H
#define HAZELCAST_JET_JSONHANDLER_H

#include <ql/quantlib.hpp>
#include <google/protobuf/util/json_util.h>
#include "rapidjson/document.h"
#include "QLUtils.h"
#include "swap.pb.h"
#include "curve.pb.h"
#include "fixing.pb.h"
#include "mtms.pb.h"

using namespace std;
using namespace rapidjson;
using namespace QuantLib;
using namespace FlumaionQL;

class JsonHandler {
public:
    Date jsonToCalcDate(string json_string);
    boost::shared_ptr<SwapTrade> jsonToSwapTrade(string json_string);
    boost::shared_ptr<FlumaionQL::Curve> jsonToCurve(string json_string);
    boost::shared_ptr<FlumaionQL::Fixing> jsonToFixing(string json_string);
    string MtmToJson(boost::shared_ptr<FlumaionQL::MTM> mtm);
    void MtmToJson(boost::shared_ptr<FlumaionQL::MTM> mtm, string* mtmstr);
private:
    QLUtils qlutils;
};

#endif //HAZELCAST_JET_JSONHANDLER_H
