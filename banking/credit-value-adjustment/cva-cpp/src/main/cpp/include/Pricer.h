//
// Created by Raj Subramani on 12/03/2020.
//

#ifndef CVARISK_PRICER_H
#define CVARISK_PRICER_H

#include "../include/ProtoHandler.h"
#include "../include/QLUtils.h"
#include <ql/quantlib.hpp>
#include <ql/time/period.hpp>
#include <chrono>
#include <iostream>

using namespace QuantLib;

class Pricer {
public:
    boost::shared_ptr<PiecewiseYieldCurve<Discount, LogLinear>>
    discountcurve(boost::shared_ptr<FlumaionQL::Curve> curve,
                  Date calcDate);

    boost::shared_ptr<FlumaionQL::MTM> fetchMTMs(boost::shared_ptr<FlumaionQL::Curve> curve,
                                                 boost::shared_ptr<FlumaionQL::Fixing> fixing,
                                                 boost::shared_ptr<FlumaionQL::SwapTrade> trade,
                                                 Date calcDate);

private:
    QLUtils qlutils;
};


#endif //CVARISK_PRICER_H
