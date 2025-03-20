/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.searchtreerao.commons.FlowCnecSorting;
import com.powsybl.openrao.searchtreerao.commons.costevaluatorresult.CostEvaluatorResult;
import com.powsybl.openrao.searchtreerao.commons.costevaluatorresult.SumMaxPerTimestampCostEvaluatorResult;
import com.powsybl.openrao.searchtreerao.commons.marginevaluator.MarginEvaluator;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.CostEvaluatorUtils.groupFlowCnecsPerState;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class MinMarginViolationEvaluator extends MinMarginEvaluator implements CostEvaluator {
    private static final double OVERLOAD_PENALTY = 1000d; // TODO : set this in RAO parameters

    public MinMarginViolationEvaluator(Set<FlowCnec> flowCnecs, Unit unit, MarginEvaluator marginEvaluator) {
        super(flowCnecs, unit, marginEvaluator);
    }

    @Override
    public String getName() {
        return "min-margin-violation-evaluator";
    }

    @Override
    protected double computeCostForState(FlowResult flowResult, Set<FlowCnec> flowCnecsOfState) {
        return Math.max(0, super.computeCostForState(flowResult, flowCnecsOfState)) * OVERLOAD_PENALTY;
    }

    @Override
    public CostEvaluatorResult evaluate(FlowResult flowResult, RemedialActionActivationResult remedialActionActivationResult) {
        Map<State, Set<FlowCnec>> flowCnecsPerState = groupFlowCnecsPerState(flowCnecs);
        Map<State, Double> costPerState = flowCnecsPerState.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> computeCostForState(flowResult, entry.getValue())));
        return new SumMaxPerTimestampCostEvaluatorResult(costPerState, FlowCnecSorting.sortByNegativeMargin(flowCnecs, unit, marginEvaluator, flowResult));
    }
}
