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
import com.powsybl.openrao.data.cracapi.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class RemedialActionCostEvaluator implements CostEvaluator {
    private final Set<State> optimizedStates;
    private final Set<FlowCnec> flowCnecs;
    private final Unit unit;
    private final MarginEvaluator marginEvaluator;
    private final RangeActionsOptimizationParameters rangeActionsOptimizationParameters;

    public RemedialActionCostEvaluator(Set<State> optimizedStates, Set<FlowCnec> flowCnecs, Unit unit, MarginEvaluator marginEvaluator, RangeActionsOptimizationParameters rangeActionsOptimizationParameters) {
        this.optimizedStates = optimizedStates;
        this.flowCnecs = flowCnecs;
        this.unit = unit;
        this.marginEvaluator = marginEvaluator;
        this.rangeActionsOptimizationParameters = rangeActionsOptimizationParameters;
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
        double totalRangeActionsCost = 0d;
        // TODO: this should maybe only be used for MILP equations but not in the evaluator
        for (State state : optimizedStates) {
            for (RangeAction<?> rangeAction : remedialActionActivationResult.getActivatedRangeActions(state)) {
                totalRangeActionsCost += rangeAction.getActivationCost().orElse(0d);
                if (rangeAction instanceof PstRangeAction) {
                    totalRangeActionsCost += computeVariationCost(rangeAction, rangeActionsOptimizationParameters.getPstPenaltyCost(), state, remedialActionActivationResult);
                } else if (rangeAction instanceof InjectionRangeAction) {
                    totalRangeActionsCost += computeVariationCost(rangeAction, rangeActionsOptimizationParameters.getInjectionRaPenaltyCost(), state, remedialActionActivationResult);
                } else if (rangeAction instanceof HvdcRangeAction) {
                    totalRangeActionsCost += computeVariationCost(rangeAction, rangeActionsOptimizationParameters.getHvdcPenaltyCost(), state, remedialActionActivationResult);
                } else {
                    // TODO: add penalty for CT
                    totalRangeActionsCost += computeVariationCost(rangeAction, 0d, state, remedialActionActivationResult);
                }
            }
        }
        return totalRangeActionsCost;
    }

    private double computeVariationCost(RangeAction<?> rangeAction, double defaultCost, State state, RemedialActionActivationResult remedialActionActivationResult) {
        double variation = rangeAction instanceof PstRangeAction pstRangeAction ? (double) remedialActionActivationResult.getTapVariation(pstRangeAction, state) : remedialActionActivationResult.getSetPointVariation(rangeAction, state);
        RangeAction.VariationDirection variationDirection = variation > 0 ? RangeAction.VariationDirection.UP : RangeAction.VariationDirection.DOWN;
        return Math.abs(variation) * rangeAction.getVariationCost(variationDirection).orElse(defaultCost);
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
