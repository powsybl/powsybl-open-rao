/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.rangeaction;

import com.powsybl.iidm.network.Country;

/**
 * @author Gabriel Plante {@literal <gabriel.plante_externe at rte-france.com>}
 */
public interface CounterTradeRangeActionAdder extends StandardRangeActionAdder<CounterTradeRangeActionAdder> {

    CounterTradeRangeActionAdder withExportingCountry(Country exportingCountry);

    CounterTradeRangeActionAdder withImportingCountry(Country importingCountry);

    CounterTradeRangeAction add();
}
