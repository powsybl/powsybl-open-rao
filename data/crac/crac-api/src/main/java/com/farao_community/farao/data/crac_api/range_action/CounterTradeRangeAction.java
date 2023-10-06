package com.farao_community.farao.data.crac_api.range_action;

import com.powsybl.iidm.network.Country;

public interface CounterTradeRangeAction extends StandardRangeAction<CounterTradeRangeAction> {

    Country getExportingCountry();
}
