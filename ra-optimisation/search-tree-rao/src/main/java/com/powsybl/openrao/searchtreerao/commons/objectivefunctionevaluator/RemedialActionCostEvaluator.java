/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.commons.costevaluatorresult.AbsoluteCostEvaluatorResult;
import com.powsybl.openrao.searchtreerao.commons.costevaluatorresult.CostEvaluatorResult;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Set;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class RemedialActionCostEvaluator implements CostEvaluator {
    private final Set<State> optimizedStates;

    public RemedialActionCostEvaluator(Set<State> optimizedStates) {
        this.optimizedStates = optimizedStates;
    }

    @Override
    public String getName() {
        return "remedial-action-cost-evaluator";
    }

    @Override
    public CostEvaluatorResult evaluate(FlowResult flowResult, RemedialActionActivationResult remedialActionActivationResult) {
        return new AbsoluteCostEvaluatorResult(getTotalNetworkActionsCost(remedialActionActivationResult) + getTotalRangeActionsCost(remedialActionActivationResult));
    }

    private double getTotalNetworkActionsCost(RemedialActionActivationResult remedialActionActivationResult) {
        double totalNetworkActionsCost = 0;
        Map<State, Set<NetworkAction>> networkActionsPerState = remedialActionActivationResult.getActivatedNetworkActionsPerState();
        for (State state : networkActionsPerState.keySet()) {
            totalNetworkActionsCost += networkActionsPerState.get(state).stream().mapToDouble(networkAction -> networkAction.getActivationCost().orElse(0.0)).sum();
        }
        return totalNetworkActionsCost;
    }

    private double getTotalRangeActionsCost(RemedialActionActivationResult remedialActionActivationResult) {
        // TODO: shall we filter out states with contingencies in 'contingenciesToExclude' from evaluate?
        return optimizedStates.stream().mapToDouble(state -> remedialActionActivationResult.getActivatedRangeActions(state).stream().mapToDouble(rangeAction -> computeRangeActionCost(rangeAction, state, remedialActionActivationResult)).sum()).sum();
    }

    private double computeRangeActionCost(RangeAction<?> rangeAction, State state, RemedialActionActivationResult remedialActionActivationResult) {
        double variation = rangeAction instanceof PstRangeAction pstRangeAction ? (double) remedialActionActivationResult.getTapVariation(pstRangeAction, state) : remedialActionActivationResult.getSetPointVariation(rangeAction, state);
        double after = rangeAction instanceof PstRangeAction pstRangeAction ? (double) remedialActionActivationResult.getOptimizedTap(pstRangeAction, state) : remedialActionActivationResult.getOptimizedSetpoint(rangeAction, state);
        if (Math.abs(variation) < 1e-6) {
            return 0.0;
        }
        if (!(rangeAction instanceof PstRangeAction)) {
            TECHNICAL_LOGS.debug("{} variation of {} MW at state {} ({} -> {})", rangeAction.getId(),
                BigDecimal.valueOf(variation).setScale(2, RoundingMode.HALF_UP),
                state,
                BigDecimal.valueOf(after - variation).setScale(2, RoundingMode.HALF_UP),
                BigDecimal.valueOf(after).setScale(2, RoundingMode.HALF_UP));
        } else {
            TECHNICAL_LOGS.debug("{} variation of {} taps at state {} ({} -> {})", rangeAction.getId(),
                BigDecimal.valueOf(variation).setScale(2, RoundingMode.HALF_UP),
                state,
                BigDecimal.valueOf(after - variation).setScale(2, RoundingMode.HALF_UP),
                BigDecimal.valueOf(after).setScale(2, RoundingMode.HALF_UP));
        }
        return rangeAction.getTotalCostForVariation(variation);
    }
}
