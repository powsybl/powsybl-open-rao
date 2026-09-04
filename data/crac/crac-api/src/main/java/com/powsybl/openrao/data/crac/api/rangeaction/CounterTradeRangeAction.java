/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.rangeaction;

import java.util.List;

/**
 * @author Gabriel Plante {@literal <gabriel.plante_externe at rte-france.com>}
 */
public interface CounterTradeRangeAction extends StandardRangeAction<CounterTradeRangeAction> {

    /**
     * Get the area on which the counter-trade is operated
     */
    String getArea();

    /**
     * Get the net position of the area before the counter-trade is applied
     */
    Double getInitialNetPosition();

    /**
     * Get the areas connected
     */
    List<ConnectedArea> getConnectedAreas();
}
