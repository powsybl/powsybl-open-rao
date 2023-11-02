package com.farao_community.farao.data.crac_api.range_action;

import com.powsybl.iidm.network.Country;

/**
 * @author Gabriel Plante {@literal <gabriel.plante_externe at rte-france.com>}
 */
public interface CounterTradeRangeActionAdder extends StandardRangeActionAdder<com.farao_community.farao.data.crac_api.range_action.CounterTradeRangeActionAdder> {

    CounterTradeRangeActionAdder withExportingCountry(Country exportingCountry);

    CounterTradeRangeActionAdder withImportingCountry(Country importingCountry);
}
