/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import java.util.List;
import java.util.Map;
import java.util.stream.DoubleStream;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class ConstantCostEvaluatorResult extends AbstractCostEvaluatorResult {
    public ConstantCostEvaluatorResult(double defaultCost) {
        super(Map.of(), List.of(), defaultCost);
    }

    @Override
    protected double evaluateResultsWithSpecificStrategy(double preventiveCost, DoubleStream postContingencyCosts) {
        return defaultCost;
    }
}
