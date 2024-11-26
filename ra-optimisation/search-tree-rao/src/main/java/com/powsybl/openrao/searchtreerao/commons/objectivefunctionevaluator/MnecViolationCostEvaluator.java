/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.raoapi.parameters.extensions.MnecParametersExtension;
import com.powsybl.openrao.searchtreerao.commons.costevaluatorresult.CostEvaluatorResult;
import com.powsybl.openrao.searchtreerao.commons.costevaluatorresult.SumCostEvaluatorResult;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.CostEvaluatorUtils.groupFlowCnecsPerState;
import static com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.CostEvaluatorUtils.sortFlowCnecsByDecreasingCost;

/**
 * An evaluator that computes the virtual cost resulting from the violation of
 * the MNEC minimum margin soft constraint
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class MnecViolationCostEvaluator implements CostEvaluator {
    private final Set<FlowCnec> flowCnecs;
    private final Unit unit;
    private final FlowResult initialFlowResult;
    private final double mnecAcceptableMarginDecrease;
    private final double mnecViolationCost;

    public MnecViolationCostEvaluator(Set<FlowCnec> flowCnecs, Unit unit, FlowResult initialFlowResult, MnecParametersExtension mnecParametersExtension) {
        this.flowCnecs = flowCnecs;
        this.unit = unit;
        this.initialFlowResult = initialFlowResult;
        this.mnecAcceptableMarginDecrease = mnecParametersExtension.getAcceptableMarginDecrease();
        this.mnecViolationCost = mnecParametersExtension.getViolationCost();
    }

    @Override
    public String getName() {
        return "mnec-cost";
    }

    @Override
    public CostEvaluatorResult evaluate(FlowResult flowResult, RemedialActionActivationResult remedialActionActivationResult) {
        Map<FlowCnec, Double> costPerMnec = flowCnecs.stream().filter(Cnec::isMonitored).collect(Collectors.toMap(Function.identity(), mnec -> computeMnecCost(flowResult, mnec)));
        Map<State, Set<FlowCnec>> mnecsPerState = groupFlowCnecsPerState(costPerMnec.keySet());
        Map<State, Double> costPerState = mnecsPerState.keySet().stream().collect(Collectors.toMap(Function.identity(), state -> Math.abs(mnecViolationCost) < 1e-10 ? 0.0 : mnecViolationCost * mnecsPerState.get(state).stream().mapToDouble(costPerMnec::get).sum()));
        List<FlowCnec> sortedMnecs = sortFlowCnecsByDecreasingCost(costPerMnec);
        return new SumCostEvaluatorResult(costPerState, sortedMnecs);
    }

    private double computeMnecCost(FlowResult flowResult, FlowCnec mnec) {
        double initialMargin = initialFlowResult.getMargin(mnec, unit);
        double currentMargin = flowResult.getMargin(mnec, unit);
        return Math.max(0, Math.min(0, initialMargin - mnecAcceptableMarginDecrease) - currentMargin);
    }
}
