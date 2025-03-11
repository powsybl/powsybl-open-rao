/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.costevaluatorresult;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;

import java.util.List;
import java.util.Map;
import java.util.stream.DoubleStream;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class MaxCostEvaluatorResult extends AbstractCnecWiseCostEvaluatorResult {
    public MaxCostEvaluatorResult(Map<FlowCnec, Double> marginPerCnec, List<FlowCnec> costlyElements, Unit unit) {
        super(marginPerCnec, costlyElements, unit);
    }

    @Override
    protected double evaluateResultsWithSpecificStrategy(DoubleStream filteredCostsStream) {
        return filteredCostsStream.max().orElse(-Double.MAX_VALUE);
    }
}
