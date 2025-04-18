/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;

import java.util.List;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class AutomatonSimulationUtils {
    private AutomatonSimulationUtils() {
    }

    public static boolean isSecure(OptimizationResult optimizationResult) {
        List<FlowCnec> mostLimitingElements = optimizationResult.getMostLimitingElements(1);
        return mostLimitingElements.isEmpty() || optimizationResult.getMargin(mostLimitingElements.get(0), Unit.MEGAWATT) >= 0;
    }
}
