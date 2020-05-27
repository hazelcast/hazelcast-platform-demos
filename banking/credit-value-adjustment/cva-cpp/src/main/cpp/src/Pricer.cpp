//
// Created by Raj Subramani on 12/03/2020.
//

#include "../include/Pricer.h"


boost::shared_ptr<PiecewiseYieldCurve<Discount, LogLinear>>
Pricer::discountcurve(boost::shared_ptr<FlumaionQL::Curve> curve, Date calcDate) {
    Calendar calendar = qlutils.strtoqlcalendar(curve->calendar());
    DayCounter daycounter = qlutils.strtodaycounter(curve->dcc());

    int totalPeriodTypes = curve->maturity_period_type_size();
    int totalPeriodValues = curve->maturity_period_value_size();
    int totalRates = curve->rates_size();

    if ((totalPeriodTypes != totalPeriodValues) || (totalPeriodTypes != totalRates)) {
        cerr << "Maturity period, maturity values and rates must be of equal size" << endl;
    }
    Settings::instance().evaluationDate() = calcDate;
    /**
     * Create a set of RateHelpers
     */
    vector<boost::shared_ptr<RateHelper>> helper;
    for (int i = 0; i < totalPeriodTypes; i++) {
        float rate = curve->rates(i);
        boost::shared_ptr<SimpleQuote> quote(new SimpleQuote(rate));
        Period period((int) curve->maturity_period_value(i), (TimeUnit) curve->maturity_period_type(i));
        boost::shared_ptr<RateHelper> dh(new DepositRateHelper(
                Handle<Quote>(quote),
                period,
                (int) curve->settlement_days(),
                calendar,
                (BusinessDayConvention) curve->bussiness_convention(),
                (bool) curve->end_of_month_flag(),
                daycounter
        ));
        helper.push_back(dh);
    }
    /**
     * Construct a discount curve
     */
    boost::shared_ptr<PiecewiseYieldCurve<Discount, LogLinear>> discountcurve(
            new PiecewiseYieldCurve<Discount, LogLinear>(calcDate, helper, daycounter));

    return discountcurve;
}

boost::shared_ptr<FlumaionQL::MTM> Pricer::fetchMTMs(boost::shared_ptr<FlumaionQL::Curve> curve,
                                                     boost::shared_ptr<FlumaionQL::Fixing> fixing,
                                                     boost::shared_ptr<FlumaionQL::SwapTrade> trade,
                                                     Date calcDate) {
    /**
     * Swap Schedule construction
     */
    Calendar calendar = qlutils.strtoqlcalendar(curve->calendar());
    Period fixedLegTenor((int) trade->fixed_leg_tenor_frequency(), (TimeUnit) trade->fixed_leg_tenor_period_enum());
    Period floatingLegTenor((int) trade->float_leg_tenor_frequency(), (TimeUnit) trade->float_leg_tenor_period_enum());

    Schedule fixedSchedule(
            qlutils.timestampToQLDate(trade->fixed_leg_start_date()),
            qlutils.timestampToQLDate(trade->fixed_leg_end_date()),
            fixedLegTenor,
            calendar,
            qlutils.bizDayConv(trade->fixed_leg_biz_day_conv()),
            qlutils.bizDayConv(trade->fixed_leg_termination_day_conv()),
            qlutils.dateGenerationRule(trade->fixed_leg_date_gen_rule()),
            trade->fixed_leg_end_of_month_flag()
    );
    Schedule floatSchedule(
            qlutils.timestampToQLDate(trade->float_leg_start_date()),
            qlutils.timestampToQLDate(trade->float_leg_end_date()),
            floatingLegTenor,
            calendar,
            qlutils.bizDayConv(trade->float_leg_biz_day_conv()),
            qlutils.bizDayConv(trade->float_leg_termination_day_conv()),
            qlutils.dateGenerationRule(trade->float_leg_date_gen_rule()),
            trade->float_leg_end_of_month_flag()
    );
    /**
     * Swap index curve construction
     */
    Period indexPeriod((int) curve->index_frequency(), (TimeUnit) curve->index_frequency_type());
    boost::shared_ptr<PiecewiseYieldCurve<Discount, LogLinear>> discountcurve =
            this->discountcurve(curve, calcDate);
    RelinkableHandle<YieldTermStructure> discountingTermStructure;
    RelinkableHandle<YieldTermStructure> forecastingTermStructure;
    boost::shared_ptr<IborIndex> iborcurve = qlutils.iborcurve(
            trade->ibor_index(), discountingTermStructure, indexPeriod
    );
    /**
     * Add the fixings to the ibor curve
     */
    int totalFixings = fixing->fixing_dates_size();
//    //--------------------------------------------------------------------------------
//    cout << "Add fixings to " << iborcurve->name() << ":" << endl;// CHECK HERE
//    //--------------------------------------------------------------------------------
    for (int i = 0; i < totalFixings; i++) {
        Date fixingDate = qlutils.timestampToQLDate(fixing->fixing_dates(i));
//        int day, month, year;
//        sscanf(fixing->fixing_dates(i).c_str(), "%2d/%2d/%4d", &day, &month, &year);
//        Date fixingDate((Day) day, (Month) month, (Year) year);
//        //--------------------------------------------------------------------------------
//        cout << "\tTimestamp:" << fixing->fixing_dates(i) << " | QL Date:" << fixingDate << endl;
//        //--------------------------------------------------------------------------------
        float fixingRate = fixing->fixing_rates(i);
//        //--------------------------------------------------------------------------------
//        cout << "\t" << fixingDate << " is business day for " << iborcurve->fixingCalendar().name() << ": " << iborcurve->isValidFixingDate(fixingDate) << endl;
//        //--------------------------------------------------------------------------------
        if (iborcurve->isValidFixingDate(fixingDate)) {
//            //--------------------------------------------------------------------------------
//            cout << "\t" << fixingDate << " | " << fixingRate << endl;
//            //--------------------------------------------------------------------------------
            iborcurve->addFixing(fixingDate, fixingRate, true);
        }
    }
    /**
     * Construct the vanilla swap
     */
    VanillaSwap vanSwap(
            qlutils.swaptype(trade->payer_receiver_flag()),
            trade->notional(),
            fixedSchedule,
            trade->fixed_rate(),
            qlutils.strtodaycounter(trade->fixed_leg_dcc()),
            floatSchedule,
            iborcurve,
            trade->float_spread(),
            qlutils.strtodaycounter(trade->float_leg_dcc())
    );
    /**
     * Set pricing engine
     */
    boost::shared_ptr<PricingEngine> swapEngine(new DiscountingSwapEngine(discountingTermStructure));
    forecastingTermStructure.linkTo(discountcurve);
    discountingTermStructure.linkTo(discountcurve);
    /**
     * Calculate MTM's for fixed and float legs
     * Here we assume tenor parity between fixed and float (for this demo only)
     */
    vanSwap.setPricingEngine(swapEngine);
    /**
     * Save MTM's into Protobuf along with discount value for the leg dates
     * Also calculate leg fractions based on leg dates
     */
    boost::shared_ptr<FlumaionQL::MTM> mtm(new FlumaionQL::MTM());
    mtm->set_curvename(curve->curvename());
    mtm->set_tradeid(trade->tradeid());

    try {
        vanSwap.NPV();
    } catch(std::exception &e) {
        //--------------------------------------------------------------------------------
        cout << "ERROR:" << e.what() << endl;
        //--------------------------------------------------------------------------------
        mtm->set_haserrored(true);
        mtm->set_error(e.what());
        return mtm;
    }

    DayCounter dayCounter = qlutils.strtodaycounter(curve->dcc());
    Date oneyearfromcalc = calendar.advance(calcDate, Period(1,(TimeUnit)3));
    float daysinYear = (float)dayCounter.dayCount(calcDate, oneyearfromcalc);
    float totalDays = (float)dayCounter.dayCount(vanSwap.startDate(), vanSwap.maturityDate());

    //--------------------------------------------------------------------------------
//    cout << "Fixed leg:" << endl;
    //--------------------------------------------------------------------------------
    for (auto const &cashflow: vanSwap.fixedLeg()) {
        if (cashflow->date() > calcDate) {
            long legTimestamp = qlutils.qldatetotimestamp(cashflow->date());
            //float daysToLeg = (float) dayCounter.dayCount(vanSwap.startDate(), cashflow->date());
            float daysToLeg = (float) dayCounter.dayCount(calcDate, cashflow->date());
            //--------------------------------------------------------------------------------
//            cout << "\t" <<  legTimestamp << "," << cashflow->amount() << endl;
            //--------------------------------------------------------------------------------
            mtm->add_fixlegdates(legTimestamp);
            mtm->add_fixlegamount(cashflow->amount());
            mtm->add_discountvalues(discountcurve->discount(cashflow->date()));
            //mtm->add_legfractions(daysToLeg / totalDays);
            mtm->add_legfractions(daysToLeg / daysinYear);
        }
    }

    //--------------------------------------------------------------------------------
//    cout << "Floating leg:" << endl;
    //--------------------------------------------------------------------------------
    for (auto const &cashflow: vanSwap.floatingLeg()) {
        if (cashflow->date() > calcDate) {
            long legTimestamp = qlutils.qldatetotimestamp(cashflow->date());
            //--------------------------------------------------------------------------------
//            cout << "\t" <<  legTimestamp << "," << cashflow->amount() << endl;
            //--------------------------------------------------------------------------------
            mtm->add_fltlegdates(legTimestamp);
            mtm->add_fltlegamount(cashflow->amount());
        }
    }

    return mtm;
}