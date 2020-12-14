//
// Created by raj on 7/7/19.
//

#ifndef CVARISK_QLUTILS_H
#define CVARISK_QLUTILS_H

#include <ql/quantlib.hpp>
#include <chrono>

using namespace QuantLib;
using namespace std;

class QLUtils {
public:
    Calendar strtoqlcalendar(std::string calstr);
    DayCounter strtodaycounter(std::string daycounterstr);
    BusinessDayConvention strtobizdayconv(std::string bizdatconvstr);
    QuantLib::Date timestampToQLDate(long timeinseconds);
    BusinessDayConvention bizDayConv(int value);
    DateGeneration::Rule dateGenerationRule(int value);
    boost::shared_ptr<IborIndex> iborcurve(
            string iborcurvename,
            RelinkableHandle<YieldTermStructure> discountingTermStructure,
            Period period);
    VanillaSwap::Type swaptype(int value);
    long qldatetotimestamp(QuantLib::Date qldate);
    Date strtoqldate(string datestr); //fixed format of YYYY-mm-dd (json compatible format)
};


#endif //CVARISK_QLUTILS_H
