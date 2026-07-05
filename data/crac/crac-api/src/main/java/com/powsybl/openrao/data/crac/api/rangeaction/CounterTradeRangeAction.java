/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.rangeaction;

/**
 * @author Gabriel Plante {@literal <gabriel.plante_externe at rte-france.com>}
 */
public interface CounterTradeRangeAction extends StandardRangeAction<CounterTradeRangeAction> {

    /**
     * Get the exporting area
     */
    String getExportingArea();

    /**
     * Get the importing area
     */
    String getImportingArea();
}
