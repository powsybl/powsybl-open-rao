/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;

import java.util.List;
import java.util.Map;
import java.util.stream.DoubleStream;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class SumCostEvaluatorResult extends AbstractCostEvaluatorResult {
    public SumCostEvaluatorResult(Map<State, Double> costPerState, List<FlowCnec> costlyElements) {
        super(costPerState, costlyElements, 0);
    }

    @Override
    protected double evaluateResultsWithSpecificStrategy(double preventiveCost, DoubleStream postContingencyCosts) {
        return preventiveCost + postContingencyCosts.sum();
    }
}
