//
// Created by raj on 21/02/2020.
//

#include "../include/JsonHandler.h"

Date JsonHandler::jsonToCalcDate(string json_string) {
    Document document;
    document.Parse(json_string.c_str());
    return qlutils.strtoqldate(document["calc_date"].GetString());
}

boost::shared_ptr<SwapTrade> JsonHandler::jsonToSwapTrade(string json_string) {
    boost::shared_ptr<SwapTrade> trade(new SwapTrade);
    google::protobuf::util::JsonParseOptions options;
    JsonStringToMessage(json_string, &(*trade), options);
    return trade;
}

boost::shared_ptr<FlumaionQL::Curve> JsonHandler::jsonToCurve(string json_string) {
    boost::shared_ptr<FlumaionQL::Curve> curve(new FlumaionQL::Curve);
    google::protobuf::util::JsonParseOptions options;
    JsonStringToMessage(json_string, &(*curve), options);
    return curve;
}

boost::shared_ptr<FlumaionQL::Fixing> JsonHandler::jsonToFixing(string json_string) {
    boost::shared_ptr<FlumaionQL::Fixing> fixing(new FlumaionQL::Fixing);
    google::protobuf::util::JsonParseOptions options;
    JsonStringToMessage(json_string, &(*fixing), options);
    return fixing;
}

string JsonHandler::MtmToJson(boost::shared_ptr<FlumaionQL::MTM> mtm) {
    string mtm_json;
    google::protobuf::util::JsonPrintOptions options;
    options.always_print_primitive_fields = true;
    options.preserve_proto_field_names = true;
    MessageToJsonString(*mtm, &mtm_json, options);
    return mtm_json;
}

void JsonHandler::MtmToJson(boost::shared_ptr<FlumaionQL::MTM> mtm, string* mtmstr) {
    string mtm_json;
    google::protobuf::util::JsonPrintOptions options;
    options.always_print_primitive_fields = true;
    options.preserve_proto_field_names = true;
    MessageToJsonString(*mtm, mtmstr, options);
}