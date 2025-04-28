/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.functionalcostcomputer;

import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;

import java.util.Map;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class MaximumFunctionalCostComputer extends AbstractFunctionalCostComputer {
    public MaximumFunctionalCostComputer(OptimizationResult optimizationResult, Map<State, OptimizationResult> postContingencyResults) {
        super(optimizationResult, postContingencyResults);
    }

    @Override
    public double computeFunctionalCost(Instant instant) {
        return Math.max(optimizationResult.getFunctionalCost(), streamPostContingencyResultsBeforeInstant(instant).max().orElse(-Double.MAX_VALUE));
    }
}
