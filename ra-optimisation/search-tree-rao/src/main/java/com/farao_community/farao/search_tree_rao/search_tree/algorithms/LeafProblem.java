/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.search_tree.algorithms;

import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.rao_api.parameters.LinearOptimizerParameters;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.fillers.*;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionResult;
import com.farao_community.farao.search_tree_rao.result.api.SensitivityResult;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.LinearProblem;
import com.farao_community.farao.search_tree_rao.search_tree.parameters.TreeParameters;
import com.powsybl.iidm.network.Network;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class LeafProblem {
    private final FlowResult initialFlowResult;
    private final FlowResult prePerimeterFlowResult;
    private final RangeActionResult prePerimeterSetPoints;
    private final Set<FlowCnec> flowCnecs;
    private final Set<FlowCnec> loopFlowCnecs;
    private final LinearOptimizerParameters linearOptimizerParameters;
    private final TreeParameters treeParameters;
    private final Set<RangeAction<?>> rangeActions;
    private Integer maxRa;
    private Integer maxTso;
    private Set<String> maxTsoExclusions; // TSOs already used in network actions should be considered "free" for range actions
    private Map<String, Integer> maxPstPerTso;
    private Map<String, Integer> maxRaPerTso;

    public LeafProblem(FlowResult initialFlowResult,
                       FlowResult prePerimeterFlowResult,
                       RangeActionResult prePerimeterSetPoints,
                       Set<FlowCnec> flowCnecs,
                       Set<FlowCnec> loopFlowCnecs,
                       LinearOptimizerParameters linearOptimizerParameters,
                       TreeParameters treeParameters,
                       Set<RangeAction<?>> rangeActions,
                       Set<NetworkAction> activatedNetworkActions) {
        this.initialFlowResult = initialFlowResult;
        this.prePerimeterFlowResult = prePerimeterFlowResult;
        this.prePerimeterSetPoints = prePerimeterSetPoints;
        this.flowCnecs = flowCnecs;
        this.loopFlowCnecs = loopFlowCnecs;
        this.linearOptimizerParameters = linearOptimizerParameters;
        this.treeParameters = treeParameters;
        this.rangeActions = rangeActions;
        computeRaUsageLimits(activatedNetworkActions);
    }

    // For testing
    Integer getMaxRa() {
        return maxRa;
    }

    // For testing
    Integer getMaxTso() {
        return maxTso;
    }

    // For testing
    Set<String> getMaxTsoExclusions() {
        return Collections.unmodifiableSet(maxTsoExclusions);
    }

    // For testing
    Map<String, Integer> getMaxPstPerTso() {
        return Collections.unmodifiableMap(maxPstPerTso);
    }

    // For testing
    Map<String, Integer> getMaxRaPerTso() {
        return Collections.unmodifiableMap(maxRaPerTso);
    }

    private void computeRaUsageLimits(Set<NetworkAction> activatedNetworkActions) {
        this.maxRa = treeParameters.getMaxRa() - activatedNetworkActions.size();
        this.maxPstPerTso = treeParameters.getMaxPstPerTso();
        this.maxTsoExclusions = activatedNetworkActions.stream().map(RemedialAction::getOperator).collect(Collectors.toSet());
        this.maxTso = treeParameters.getMaxTso() - this.maxTsoExclusions.size();
        this.maxRaPerTso = new HashMap<>(treeParameters.getMaxRaPerTso());
        this.maxRaPerTso.entrySet().forEach(entry -> {
            int activatedNetworkActionsForTso = activatedNetworkActions.stream().filter(na -> entry.getKey().equals(na.getOperator())).collect(Collectors.toSet()).size();
            entry.setValue(entry.getValue() - activatedNetworkActionsForTso);
        });
    }

    public LinearProblem getLinearProblem(Network network, FlowResult preOptimFlowResult, SensitivityResult preOptimSensitivityResult) {
        LinearProblem.LinearProblemBuilder linearProblemBuilder =  LinearProblem.create()
                .withProblemFiller(createCoreProblemFiller(network, flowCnecs, rangeActions));

        if (linearOptimizerParameters.getObjectiveFunction().relativePositiveMargins()) {
            linearProblemBuilder.withProblemFiller(createMaxMinRelativeMarginFiller(flowCnecs, rangeActions, preOptimFlowResult));
        } else {
            linearProblemBuilder.withProblemFiller(createMaxMinMarginFiller(flowCnecs, rangeActions));
        }

        if (linearOptimizerParameters.isRaoWithMnecLimitation()) {
            linearProblemBuilder.withProblemFiller(createMnecFiller(flowCnecs));
        }

        if (linearOptimizerParameters.isRaoWithLoopFlowLimitation()) {
            linearProblemBuilder.withProblemFiller(createLoopFlowFiller(loopFlowCnecs));
        }
        if (!Objects.isNull(linearOptimizerParameters.getUnoptimizedCnecParameters())) {
            linearProblemBuilder.withProblemFiller(createUnoptimizedCnecFiller(flowCnecs));
        }

        if (linearOptimizerParameters.getPstOptimizationApproximation().equals(RaoParameters.PstOptimizationApproximation.APPROXIMATED_INTEGERS)) {
            linearProblemBuilder.withProblemFiller(createIntegerPstTapFiller(network, rangeActions));
            linearProblemBuilder.withProblemFiller(createDiscretePstGroupFiller(network, rangeActions.stream().filter(PstRangeAction.class::isInstance).map(PstRangeAction.class::cast).collect(Collectors.toSet())));
            linearProblemBuilder.withProblemFiller(createContinuousRangeActionGroupFiller(rangeActions.stream().filter(ra -> !(ra instanceof PstRangeAction)).collect(Collectors.toSet())));
        } else {
            linearProblemBuilder.withProblemFiller(createContinuousRangeActionGroupFiller(rangeActions));
        }

        if ((maxRa != null && maxRa < rangeActions.size())
            || (maxTso != null && maxTso < rangeActions.stream().map(RemedialAction::getOperator).count() - maxTsoExclusions.size())
            || (maxPstPerTso != null && !maxPstPerTso.isEmpty())
            || (maxRaPerTso != null && !maxRaPerTso.isEmpty())) {
            linearProblemBuilder.withProblemFiller(createRaUageLimitsFiller(rangeActions));
        }

        linearProblemBuilder.withBranchResult(preOptimFlowResult)
                .withSensitivityResult(preOptimSensitivityResult)
                .withSolver(linearOptimizerParameters.getSolver())
                .withRelativeMipGap(linearOptimizerParameters.getRelativeMipGap())
                .withSolverSpecificParameters(linearOptimizerParameters.getSolverSpecificParameters());

        return linearProblemBuilder.build();
    }

    private ProblemFiller createRaUageLimitsFiller(Set<RangeAction<?>> rangeActions) {
        return new RaUsageLimitsFiller(
            rangeActions,
            prePerimeterSetPoints,
            maxRa,
            maxTso,
            maxTsoExclusions,
            maxPstPerTso,
            maxRaPerTso,
            linearOptimizerParameters.getPstOptimizationApproximation() == RaoParameters.PstOptimizationApproximation.APPROXIMATED_INTEGERS);
    }

    private ProblemFiller createCoreProblemFiller(Network network, Set<FlowCnec> flowCnecs, Set<RangeAction<?>> rangeActions) {
        return new CoreProblemFiller(
            network,
            flowCnecs,
            rangeActions,
            prePerimeterSetPoints,
            linearOptimizerParameters.getPstSensitivityThreshold(),
            linearOptimizerParameters.getHvdcSensitivityThreshold(),
            linearOptimizerParameters.getInjectionSensitivityThreshold(),
            linearOptimizerParameters.getObjectiveFunction().relativePositiveMargins()
        );
    }

    private ProblemFiller createMaxMinRelativeMarginFiller(Set<FlowCnec> flowCnecs, Set<RangeAction<?>> rangeActions, FlowResult preOptimFlowResult) {
        return new MaxMinRelativeMarginFiller(
            flowCnecs.stream().filter(Cnec::isOptimized).collect(Collectors.toSet()),
            preOptimFlowResult,
            rangeActions,
            linearOptimizerParameters.getObjectiveFunction().getUnit(),
            linearOptimizerParameters.getMaxMinRelativeMarginParameters()
        );
    }

    private ProblemFiller createMaxMinMarginFiller(Set<FlowCnec> flowCnecs, Set<RangeAction<?>> rangeActions) {
        return new MaxMinMarginFiller(
            flowCnecs.stream().filter(Cnec::isOptimized).collect(Collectors.toSet()),
            rangeActions,
            linearOptimizerParameters.getUnit(),
            linearOptimizerParameters.getMaxMinMarginParameters()
        );
    }

    private ProblemFiller createMnecFiller(Set<FlowCnec> flowCnecs) {
        return new MnecFiller(
            initialFlowResult,
            flowCnecs.stream().filter(Cnec::isMonitored).collect(Collectors.toSet()),
            linearOptimizerParameters.getUnit(),
            linearOptimizerParameters.getMnecParameters()
        );
    }

    private ProblemFiller createLoopFlowFiller(Set<FlowCnec> loopFlowCnecs) {
        return new MaxLoopFlowFiller(
            loopFlowCnecs,
            initialFlowResult,
            linearOptimizerParameters.getLoopFlowParameters()
        );
    }

    private ProblemFiller createUnoptimizedCnecFiller(Set<FlowCnec> flowCnecs) {
        return new UnoptimizedCnecFiller(
            flowCnecs,
            prePerimeterFlowResult,
            linearOptimizerParameters.getUnoptimizedCnecParameters()
        );
    }

    private ProblemFiller createContinuousRangeActionGroupFiller(Set<RangeAction<?>> rangeActions) {
        return new ContinuousRangeActionGroupFiller(
            rangeActions
        );
    }

    private ProblemFiller createIntegerPstTapFiller(Network network, Set<RangeAction<?>> rangeActions) {
        return new DiscretePstTapFiller(
            network,
            rangeActions,
            prePerimeterSetPoints
        );
    }

    private ProblemFiller createDiscretePstGroupFiller(Network network, Set<PstRangeAction> pstRangeActions) {
        return new DiscretePstGroupFiller(
            network,
            pstRangeActions
        );
    }
}
