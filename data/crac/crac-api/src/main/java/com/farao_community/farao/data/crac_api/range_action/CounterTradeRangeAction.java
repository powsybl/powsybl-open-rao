package com.farao_community.farao.data.crac_api.range_action;

import com.powsybl.iidm.network.Country;

/**
 * @author Gabriel Plante {@literal <gabriel.plante_externe at rte-france.com>}
 */
public interface CounterTradeRangeAction extends StandardRangeAction<CounterTradeRangeAction> {

    /**
     * Get the exporting country
     */
    Country getExportingCountry();

    /**
     * Get the importing country
     */
    Country getImportingCountry();
}
