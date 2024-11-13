/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class RemedialActionCostEvaluator implements FunctionalCostEvaluator {
    private final Set<State> optimizedStates;
    private final Set<FlowCnec> flowCnecs;
    private final Unit unit;
    private final MarginEvaluator marginEvaluator;

    public RemedialActionCostEvaluator(Set<State> optimizedStates, Set<FlowCnec> flowCnecs, Unit unit, MarginEvaluator marginEvaluator) {
        this.optimizedStates = optimizedStates;
        this.flowCnecs = flowCnecs;
        this.unit = unit;
        this.marginEvaluator = marginEvaluator;
    }

    @Override
    public String getName() {
        return "remedial-action-cost-evaluator";
    }

    @Override
    public Pair<Double, List<FlowCnec>> computeCostAndLimitingElements(FlowResult flowResult, RemedialActionActivationResult remedialActionActivationResult, Set<String> contingenciesToExclude) {
        return Pair.of(getTotalNetworkActionsCost(remedialActionActivationResult) + getTotalRangeActionsCost(remedialActionActivationResult), EvaluatorsUtils.getCostlyElements(flowCnecs, marginEvaluator, unit, flowResult, contingenciesToExclude));
    }

    private double getTotalNetworkActionsCost(RemedialActionActivationResult remedialActionActivationResult) {
        return remedialActionActivationResult.getActivatedNetworkActions().stream().mapToDouble(networkAction -> networkAction.getActivationCost().orElse(0.0)).sum();
    }

    private double getTotalRangeActionsCost(RemedialActionActivationResult remedialActionActivationResult) {
        return optimizedStates.stream().mapToDouble(state -> remedialActionActivationResult.getActivatedRangeActions(state).stream().mapToDouble(rangeAction -> computeRangeActionCost(rangeAction, state, remedialActionActivationResult)).sum()).sum();
    }

    private double computeRangeActionCost(RangeAction<?> rangeAction, State state, RemedialActionActivationResult remedialActionActivationResult) {
        double variation = rangeAction instanceof PstRangeAction pstRangeAction ? (double) remedialActionActivationResult.getTapVariation(pstRangeAction, state) : remedialActionActivationResult.getSetPointVariation(rangeAction, state);
        if (variation == 0.0) {
            return 0.0;
        }
        double activationCost = rangeAction.getActivationCost().orElse(0.0);
        RangeAction.VariationDirection variationDirection = variation > 0 ? RangeAction.VariationDirection.UP : RangeAction.VariationDirection.DOWN;
        return activationCost + Math.abs(variation) * rangeAction.getVariationCost(variationDirection).orElse(0.0);
    }

    @Override
    public Unit getUnit() {
        return unit;
    }

    @Override
    public Set<FlowCnec> getFlowCnecs() {
        return flowCnecs;
    }
}
