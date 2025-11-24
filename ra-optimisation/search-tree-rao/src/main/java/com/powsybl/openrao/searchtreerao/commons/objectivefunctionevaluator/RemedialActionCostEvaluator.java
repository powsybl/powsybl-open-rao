/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.commons.costevaluatorresult.AbsoluteCostEvaluatorResult;
import com.powsybl.openrao.searchtreerao.commons.costevaluatorresult.CostEvaluatorResult;
import com.powsybl.openrao.searchtreerao.reports.CommonReports;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;

import java.util.Map;
import java.util.Set;

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
    public CostEvaluatorResult evaluate(final FlowResult flowResult,
                                        final RemedialActionActivationResult remedialActionActivationResult,
                                        final ReportNode reportNode) {
        return new AbsoluteCostEvaluatorResult(getTotalNetworkActionsCost(remedialActionActivationResult) + getTotalRangeActionsCost(remedialActionActivationResult, reportNode));
    }

    private double getTotalNetworkActionsCost(RemedialActionActivationResult remedialActionActivationResult) {
        Map<State, Set<NetworkAction>> networkActionsPerState = remedialActionActivationResult.getActivatedNetworkActionsPerState();
        return networkActionsPerState.values().stream()
                .flatMap(Set::stream)
                .mapToDouble(networkAction -> networkAction.getActivationCost().orElse(0.0))
                .sum();
    }

    private double getTotalRangeActionsCost(final RemedialActionActivationResult remedialActionActivationResult, final ReportNode reportNode) {
        // TODO: shall we filter out states with contingencies in 'contingenciesToExclude' from evaluate?
        return optimizedStates.stream().mapToDouble(state -> remedialActionActivationResult.getActivatedRangeActions(state).stream().mapToDouble(rangeAction -> computeRangeActionCost(rangeAction, state, remedialActionActivationResult, reportNode)).sum()).sum();
    }

    private double computeRangeActionCost(final RangeAction<?> rangeAction,
                                          final State state,
                                          final RemedialActionActivationResult remedialActionActivationResult,
                                          final ReportNode reportNode) {
        double variation = rangeAction instanceof PstRangeAction pstRangeAction ? (double) remedialActionActivationResult.getTapVariation(pstRangeAction, state) : remedialActionActivationResult.getSetPointVariation(rangeAction, state);
        double after = rangeAction instanceof PstRangeAction pstRangeAction ? (double) remedialActionActivationResult.getOptimizedTap(pstRangeAction, state) : remedialActionActivationResult.getOptimizedSetpoint(rangeAction, state);
        if (Math.abs(variation) < 1e-6) {
            return 0.0;
        }
        CommonReports.reportRangeActionVariation(reportNode, rangeAction, variation, state, after);
        return rangeAction.getTotalCostForVariation(variation);
    }
}
