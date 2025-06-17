/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.searchtreerao.commons.FlowCnecSorting;
import com.powsybl.openrao.searchtreerao.commons.costevaluatorresult.CostEvaluatorResult;
import com.powsybl.openrao.searchtreerao.commons.costevaluatorresult.SumMaxPerTimestampCostEvaluatorResult;
import com.powsybl.openrao.searchtreerao.commons.marginevaluator.MarginEvaluator;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class MinMarginViolationEvaluator extends MinMarginEvaluator implements CostEvaluator {
    private static final double OVERLOAD_PENALTY = 10000d;

    public MinMarginViolationEvaluator(Set<FlowCnec> flowCnecs, Unit unit, MarginEvaluator marginEvaluator) {
        super(flowCnecs, unit, marginEvaluator);
    }

    @Override
    public String getName() {
        return "min-margin-violation-evaluator";
    }

    @Override
    public Map<FlowCnec, Double> getMarginPerCnec(Set<FlowCnec> flowCnecs, FlowResult flowResult, Unit unit) {
        Map<FlowCnec, Double> costPerCnec = new HashMap<>();
        flowCnecs.forEach(cnec -> costPerCnec.put(cnec, Math.min(0, marginEvaluator.getMargin(flowResult, cnec, unit)) * OVERLOAD_PENALTY));
        return costPerCnec;
    }

    @Override
    public CostEvaluatorResult evaluate(FlowResult flowResult, RemedialActionActivationResult remedialActionActivationResult) {
        Map<FlowCnec, Double> costPerCnec = getMarginPerCnec(flowCnecs, flowResult, unit);
        return new SumMaxPerTimestampCostEvaluatorResult(costPerCnec, FlowCnecSorting.sortByMargin(flowCnecs, unit, marginEvaluator, flowResult), unit);
    }
}
