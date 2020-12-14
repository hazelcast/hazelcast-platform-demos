//
// Created by raj on 7/7/19.
//

#include "../include/QLUtils.h"

Calendar QLUtils::strtoqlcalendar(string calstr) {
    if (calstr == "UnitedStates")  {
        return UnitedStates();
    }
    else if (calstr == "UnitedKingdom") {
        return UnitedKingdom();
    }
}

DayCounter QLUtils::strtodaycounter(string daycounterstr) {
    if (daycounterstr == "Actual360") return Actual360();
    else if (daycounterstr == "Actual365Fixed") return Actual365Fixed();
}

BusinessDayConvention QLUtils::strtobizdayconv(string bizdatconvstr) {
    if (bizdatconvstr == "ModifiedFollowing") return ModifiedFollowing;
}

QuantLib::Date QLUtils::timestampToQLDate(long timeinseconds) {
    std::chrono::system_clock::time_point tp{std::chrono::seconds{timeinseconds}};
    time_t time_after_duration = std::chrono::system_clock::to_time_t(tp);
    tm* t = std::localtime(&time_after_duration);
    return QuantLib::Date((Day)t->tm_mday, (Month)(t->tm_mon+1), (Year)(t->tm_year+1900));
}

BusinessDayConvention QLUtils::bizDayConv(int value) {
    switch(value) {
        case Following:
            return Following;
        case ModifiedFollowing:
            return ModifiedFollowing;
        case Preceding:
            return Preceding;
        case ModifiedPreceding:
            return ModifiedPreceding;
        case Unadjusted:
            return Unadjusted;
        case HalfMonthModifiedFollowing:
            return HalfMonthModifiedFollowing;
        case Nearest:
            return Nearest;
    }
}

DateGeneration::Rule QLUtils::dateGenerationRule(int value) {
    switch(value) {
        case DateGeneration::Backward:
            return DateGeneration::Backward;
        case DateGeneration::Forward:
            return DateGeneration::Forward;
        case DateGeneration::Zero:
            return DateGeneration::Zero;
        case DateGeneration::ThirdWednesday:
            return DateGeneration::ThirdWednesday;
        case DateGeneration::Twentieth:
            return DateGeneration::Twentieth;
        case DateGeneration::TwentiethIMM:
            return DateGeneration::TwentiethIMM;
        case DateGeneration::OldCDS:
            return DateGeneration::OldCDS;
        case DateGeneration::CDS:
            return DateGeneration::CDS;
        case DateGeneration::CDS2015:
            return DateGeneration::CDS2015;
    }
}

boost::shared_ptr<IborIndex> QLUtils::iborcurve(
        string iborcurvename,
        RelinkableHandle<YieldTermStructure> discountingTermStructure,
        Period period) {
    if (iborcurvename == "USDLibor") {
        boost::shared_ptr<IborIndex> iborCurve(new USDLibor(period, discountingTermStructure));
        return iborCurve;
    }
}

VanillaSwap::Type QLUtils::swaptype(int value) {
    switch(value) {
        case VanillaSwap::Type::Payer:
            return VanillaSwap::Type::Payer;
        case VanillaSwap::Type::Receiver:
            return VanillaSwap::Type::Receiver;
    }
}

long QLUtils::qldatetotimestamp(QuantLib::Date qldate) {
    struct tm t;
    time_t t_of_day;

    t.tm_year = qldate.year()-1900;  // Year - 1900
    t.tm_mon = qldate.month()-1;           // Month, where 0 = jan
    t.tm_mday = qldate.dayOfMonth();          // Day of the month
    t.tm_hour = 0;
    t.tm_min = 0;
    t.tm_sec = 0;
    t.tm_isdst = -1;        // Is DST on? 1 = yes, 0 = no, -1 = unknown
    t_of_day = mktime(&t);

    return (long) t_of_day;
}

Date QLUtils::strtoqldate(string datestr) {
    int day, month, year;
    sscanf(datestr.c_str(), "%4d-%2d-%2d", &year, &month, &day);
    Date qlDate((Day) day, (Month) month, (Year) year);
    return qlDate;
}