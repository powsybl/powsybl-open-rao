/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.rangeaction;

import com.google.common.annotations.Beta;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.ConnectedArea;
import com.powsybl.openrao.commons.CounterTradeGeneratorOrdering;
import com.powsybl.sensitivity.SensitivityVariableSet;

import java.util.List;
import java.util.Map;

/**
 * @author Gabriel Plante {@literal <gabriel.plante_externe at rte-france.com>}
 */
@Beta
public interface CounterTradeRangeAction extends StandardRangeAction<CounterTradeRangeAction> {

    /**
     * Get the area id
     */
    String getAreaId();

    /**
     * Get connected areas
     */
    List<ConnectedArea> getConnectedAreas();

    /**
     * Set the Glsk
     */
    void setGlsk(ZonalData<SensitivityVariableSet> glsk);

    void setGlsk(Network network);

    /**
     * Get the Glsk
     */
    Map<String, Map<String, Double>> getGlsk();

    CounterTradeGeneratorOrdering getGeneratorOrdering();

    void computeGeneratorOrdering(Network network, double tolerance);
}

