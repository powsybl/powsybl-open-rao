/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.reports;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;
import com.powsybl.openrao.commons.logs.OpenRaoLogger;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.GlobalOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.searchtree.algorithms.Leaf;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.powsybl.commons.report.TypedValue.INFO_SEVERITY;
import static com.powsybl.commons.report.TypedValue.TRACE_SEVERITY;
import static com.powsybl.commons.report.TypedValue.WARN_SEVERITY;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_LOGS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;
import static java.lang.String.format;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
public final class SearchTreeReports {
    private SearchTreeReports() {
        // Utility class should not be instantiated
    }

    private static OpenRaoLogger getLogger(final boolean verbose) {
        return verbose ? BUSINESS_LOGS : TECHNICAL_LOGS;
    }

    private static TypedValue getSeverity(final boolean verbose) {
        return verbose ? INFO_SEVERITY : TRACE_SEVERITY;
    }

    public static void reportLeafAlreadyEvaluated(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportLeafAlreadyEvaluated")
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.debug("Leaf has already been evaluated");
    }

    public static void reportEvaluatingLeaf(final ReportNode parentNode, final Leaf leaf) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportEvaluatingLeaf")
            .withUntypedValue("leaf", Objects.toString(leaf))
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.debug("Evaluating {}", leaf);
    }

    public static void reportEvaluatedLeaf(final ReportNode parentNode, final boolean verbose, final Leaf leaf) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportEvaluatedLeaf")
            .withUntypedValue("leaf", Objects.toString(leaf))
            .withSeverity(getSeverity(verbose))
            .add();

        getLogger(verbose).info("Evaluated {}", leaf);
    }

    public static void reportEvaluatingRootLeaf(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportEvaluatingRootLeaf")
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.debug("Evaluating root leaf");
    }

    public static void reportCouldNotEvaluateLeaf(final ReportNode parentNode, final boolean verbose, final Leaf leaf) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportCouldNotEvaluateLeaf")
            .withUntypedValue("leaf", Objects.toString(leaf))
            .withSeverity(getSeverity(verbose))
            .add();

        getLogger(verbose).info("Could not evaluate leaf: {}", leaf);
    }

    public static void reportCouldNotEvaluateNetworkActionCombination(final ReportNode parentNode,
                                                                      final boolean verbose,
                                                                      final Set<NetworkAction> networkActions,
                                                                      final Exception exception) {
        final String concatenatedNetworkActions = networkActions.stream()
            .map(NetworkAction::getId)
            .collect(Collectors.joining(" + "));

        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportCouldNotEvaluateNetworkActionCombination")
            .withUntypedValue("networkActions", concatenatedNetworkActions)
            .withUntypedValue("exceptionMessage", exception.getMessage())
            .withSeverity(getSeverity(verbose))
            .add();

        getLogger(verbose).info("Could not evaluate network action combination \"{}\": {}", concatenatedNetworkActions, exception.getMessage());
    }

    public static void reportRootLeaf(final ReportNode parentNode, final boolean verbose, final Leaf rootLeaf) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportRootLeaf")
            .withUntypedValue("rootLeaf", Objects.toString(rootLeaf))
            .withSeverity(getSeverity(verbose))
            .add();

        getLogger(verbose).info("{}", rootLeaf);
    }

    public static void reportOptimizedLeaf(final ReportNode parentNode, final boolean verbose, final Leaf leaf) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportOptimizedLeaf")
            .withUntypedValue("leaf", Objects.toString(leaf))
            .withSeverity(getSeverity(verbose))
            .add();

        getLogger(verbose).info("Optimized {}", leaf);
    }

    public static void reportStopCriterionReachedOnLeaf(final ReportNode parentNode, final boolean verbose, final Leaf leaf) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportStopCriterionReachedOnLeaf")
            .withUntypedValue("leaf", Objects.toString(leaf))
            .withSeverity(getSeverity(verbose))
            .add();

        getLogger(verbose).info("Stop criterion reached on {}", leaf);
    }

    public static void reportLinearOptimizationOnRootLeaf(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportLinearOptimizationOnRootLeaf")
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("Linear optimization on root leaf");
    }

    public static void reportBestLeaf(final ReportNode parentNode, final Leaf leaf) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportBestLeaf")
            .withUntypedValue("leaf", Objects.toString(leaf))
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("Best leaf: {}", leaf);
    }

    public static void reportSearchTreeRaoCompletedWithStatus(final ReportNode parentNode, final ComputationStatus status) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportSearchTreeRaoCompletedWithStatus")
            .withUntypedValue("status", Objects.toString(status))
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("Search-tree RAO completed with status {}", status);
    }

    public static void reportFailedToEvaluateLeafSensiFailed(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportFailedToEvaluateLeafSensiFailed")
            .withSeverity(WARN_SEVERITY)
            .add();

        BUSINESS_WARNS.warn("Failed to evaluate leaf: sensitivity analysis failed");
    }

    public static void reportResetRangeActionSetpoints(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportResetRangeActionSetpoints")
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.debug("Resetting range action setpoints to their pre-optim values");
    }

    public static void reportOptimizingLeaf(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportOptimizingLeaf")
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.debug("Optimizing leaf...");
    }

    public static void reportImpossibleToOptimizeLeafBecauseEvaluationFailed(final ReportNode parentNode, final Leaf leaf) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportImpossibleToOptimizeLeafBecauseEvaluationFailed")
            .withUntypedValue("leaf", Objects.toString(leaf))
            .withSeverity(WARN_SEVERITY)
            .add();

        BUSINESS_WARNS.warn("Impossible to optimize leaf: {} because evaluation failed", leaf);
    }

    public static void reportImpossibleToOptimizeLeafBecauseEvaluationNotPerformed(final ReportNode parentNode, final Leaf leaf) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportImpossibleToOptimizeLeafBecauseEvaluationNotPerformed")
            .withUntypedValue("leaf", Objects.toString(leaf))
            .withSeverity(WARN_SEVERITY)
            .add();

        BUSINESS_WARNS.warn("Impossible to optimize leaf: {} because evaluation has not been performed", leaf);
    }

    public static void reportNetworkActionCombinationsFilteredOutTooFar(final ReportNode parentNode, final int nbOfCombinations) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportNetworkActionCombinationsFilteredOutTooFar")
            .withUntypedValue("nbOfCombinations", nbOfCombinations)
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("{} network action combinations have been filtered out because they are too far from the most limiting element", nbOfCombinations);
    }

    public static void reportNetworkActionCombinationsFilteredOutMaxElementaryActionsExceeded(final ReportNode parentNode, final int nbOfCombinations) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportNetworkActionCombinationsFilteredOutMaxElementaryActionsExceeded")
            .withUntypedValue("nbOfCombinations", nbOfCombinations)
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("{} network action combinations have been filtered out because the maximum number of elementary actions has been exceeded for one of its operators", nbOfCombinations);
    }

    public static void reportNetworkActionCombinationsFilteredOutMaxNetworkActionsReached(final ReportNode parentNode, final int nbOfCombinations) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportNetworkActionCombinationsFilteredOutMaxNetworkActionsReached")
            .withUntypedValue("nbOfCombinations", nbOfCombinations)
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("{} network action combinations have been filtered out because the maximum number of network actions for their TSO has been reached", nbOfCombinations);
    }

    public static void reportNetworkActionCombinationsFilteredOutMaxUsableRasReached(final ReportNode parentNode, final int nbOfCombinations) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportNetworkActionCombinationsFilteredOutMaxUsableRasReached")
            .withUntypedValue("nbOfCombinations", nbOfCombinations)
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("{} network action combinations have been filtered out because the max number of usable RAs has been reached", nbOfCombinations);
    }

    public static void reportNetworkActionCombinationsFilteredOutMaxUsableTsosReached(final ReportNode parentNode, final int nbOfCombinations) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportNetworkActionCombinationsFilteredOutMaxUsableTsosReached")
            .withUntypedValue("nbOfCombinations", nbOfCombinations)
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("{} network action combinations have been filtered out because the max number of usable TSOs has been reached", nbOfCombinations);
    }

    public static void reportNoNetworkActionAvailable(final ReportNode parentNode, final boolean verbose) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportNoNetworkActionAvailable")
            .withSeverity(getSeverity(verbose))
            .add();

        getLogger(verbose).info("No network action available");
    }

    public static void reportEvaluatingNbLeavesInParallel(final ReportNode parentNode, final int nbLeavesInParallel) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportEvaluatingNbLeavesInParallel")
            .withUntypedValue("nbLeaves", nbLeavesInParallel)
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.debug("Evaluating {} leaves in parallel", nbLeavesInParallel);
    }

    public static ReportNode reportSearchDepth(final ReportNode parentNode, final int depth) {
        return parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportSearchDepth")
            .withUntypedValue("depth", depth)
            .withSeverity(TRACE_SEVERITY)
            .add();
    }

    public static void reportSearchDepthStart(final ReportNode parentNode, final int depth) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportSearchDepthStart")
            .withUntypedValue("depth", depth)
            .withSeverity(INFO_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("Search depth {} [start]", depth);
    }

    public static void reportSearchDepthEnd(final ReportNode parentNode, final int depth) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportSearchDepthEnd")
            .withUntypedValue("depth", depth)
            .withSeverity(INFO_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("Search depth {} [end]", depth);
    }

    public static void reportSearchDepthBestLeaf(final ReportNode parentNode, final boolean verbose, final int depth, final Leaf leaf) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportSearchDepthBestLeaf")
            .withUntypedValue("depth", depth)
            .withUntypedValue("leaf", Objects.toString(leaf))
            .withSeverity(getSeverity(verbose))
            .add();

        getLogger(verbose).info("Search depth {} best leaf: {}", depth, leaf);
    }

    public static void reportNoBetterResultFoundInSearchDepth(final ReportNode parentNode, final boolean verbose, final int depth) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportNoBetterResultFoundInSearchDepth")
            .withUntypedValue("depth", depth)
            .withSeverity(getSeverity(verbose))
            .add();

        getLogger(verbose).info("No better result found in search depth {}, exiting search tree", depth);
    }

    public static void reportMaxSearchDepthReached(final ReportNode parentNode, final boolean verbose) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportMaxSearchDepthReached")
            .withSeverity(getSeverity(verbose))
            .add();

        getLogger(verbose).info("Maximum search depth has been reached, exiting search tree");
    }

    public static void reportNoMoreNetworkActionAvailable(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportNoMoreNetworkActionAvailable")
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("No more network action available");
    }

    public static void reportLeavesToEvaluate(final ReportNode parentNode, final int nbLeaves) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportLeavesToEvaluate")
            .withUntypedValue("nbLeaves", nbLeaves)
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("Leaves to evaluate: {}", nbLeaves);
    }

    public static void reportRemainingLeavesToEvaluate(final ReportNode parentNode, final int nbLeaves) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportRemainingLeavesToEvaluate")
            .withUntypedValue("nbLeaves", nbLeaves)
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("Leaves to evaluate: {}", nbLeaves);
    }

    public static void reportSkippingOptimization(final ReportNode parentNode, final boolean verbose, final String id) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportSkippingOptimization")
            .withUntypedValue("id", id)
            .withSeverity(getSeverity(verbose))
            .add();

        getLogger(verbose).info("Skipping {} optimization because earlier combination fulfills stop criterion.", id);
    }

    public static void reportCanNotOptimizeRemedialActionCombination(final ReportNode parentNode, final String id, final String errorMessage) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportCanNotOptimizeRemedialActionCombination")
            .withUntypedValue("id", id)
            .withUntypedValue("errorMessage", errorMessage)
            .withSeverity(WARN_SEVERITY)
            .add();

        BUSINESS_WARNS.warn("Cannot optimize remedial action combination {}: {}", id, errorMessage);
    }

    public static void reportFailedToOptimizeLeaf(final ReportNode parentNode, final boolean verbose, final Leaf leaf) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportFailedToOptimizeLeaf")
            .withUntypedValue("leaf", Objects.toString(leaf))
            .withSeverity(getSeverity(verbose))
            .add();

        getLogger(verbose).info("Failed to optimize leaf: {}", leaf);
    }

    public static void reportNoRangeActionToOptimize(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportNoRangeActionToOptimize")
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("No range actions to optimize");
    }

    public static void reportNoRangeActionToOptimizeAfterFilteringHvdcRangeActions(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportNoRangeActionToOptimizeAfterFilteringHvdcRangeActions")
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("No range actions to optimize after filtering HVDC range actions");
    }

    public static void reportStopCriterionReached(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportStopCriterionReached")
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("Stop criterion reached, other threads may skip optimization.");
    }

    public static void reportPerimeterPurelyVirtual(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportPerimeterPurelyVirtual")
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.debug("Perimeter is purely virtual and virtual cost is zero. Exiting search tree.");
    }

    private static List<String> getRangeActionSetpoints(final Leaf leaf, final OptimizationPerimeter optimizationContext) {
        final boolean globalPstOptimization = optimizationContext instanceof GlobalOptimizationPerimeter;

        return optimizationContext.getRangeActionOptimizationStates().stream()
            .flatMap(state -> leaf.getActivatedRangeActions(state).stream()
                .map(rangeAction -> getIndividualStringForRangeActionAndState(leaf, state, rangeAction, globalPstOptimization))
            )
            .toList();
    }

    private static String getIndividualStringForRangeActionAndState(Leaf leaf, State state, RangeAction<?> rangeAction, boolean globalPstOptimization) {
        final double valueVariation = rangeAction instanceof PstRangeAction pstRangeAction
            ? leaf.getTapVariation(pstRangeAction, state)
            : leaf.getSetPointVariation(rangeAction, state);
        final double postOptimValue = rangeAction instanceof PstRangeAction pstRangeAction
            ? leaf.getOptimizedTap(pstRangeAction, state)
            : leaf.getOptimizedSetpoint(rangeAction, state);
        final double cost = rangeAction.getTotalCostForVariation(valueVariation);
        return globalPstOptimization
            ? format("%s@%s: %.0f (var: %.0f)", rangeAction.getName(), state.getId(), postOptimValue, valueVariation)
            : format("%s: %.0f (var: %.0f, cost %.0f)", rangeAction.getName(), postOptimValue, valueVariation, cost);
    }

    public static void reportRangeActions(final ReportNode parentNode,
                                          final Leaf leaf,
                                          final OptimizationPerimeter optimizationContext) {
        final List<String> rangeActionSetpoints = getRangeActionSetpoints(leaf, optimizationContext);

        if (rangeActionSetpoints.isEmpty()) {
            reportNoRangeActionActivated(parentNode);
        } else {
            reportRangeActionsActivated(parentNode, rangeActionSetpoints);
        }
    }

    private static void reportNoRangeActionActivated(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportNoRangeActionActivated")
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("No range actions activated");
    }

    private static void reportRangeActionsActivated(final ReportNode parentNode, final List<String> rangeActionSetpoints) {
        final String joinedRaSetpoints = String.join(", ", rangeActionSetpoints);

        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportRangeActionsActivated")
            .withUntypedValue("rangeActions", joinedRaSetpoints)
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("Range action(s): {}", joinedRaSetpoints);
    }

    public static void reportBestLeafRangeActions(final ReportNode parentNode,
                                                  final Leaf leaf,
                                                  final OptimizationPerimeter optimizationContext) {
        final List<String> rangeActionSetpoints = getRangeActionSetpoints(leaf, optimizationContext);

        if (rangeActionSetpoints.isEmpty()) {
            reportBestLeafNoRangeActionActivated(parentNode);
        } else {
            reportBestLeafRangeActionsActivated(parentNode, rangeActionSetpoints);
        }
    }

    private static void reportBestLeafNoRangeActionActivated(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportBestLeafNoRangeActionActivated")
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("Best leaf: No range actions activated");
    }

    private static void reportBestLeafRangeActionsActivated(final ReportNode parentNode, final List<String> rangeActionSetpoints) {
        final String joinedRaSetpoints = String.join(", ", rangeActionSetpoints);

        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportBestLeafRangeActionsActivated")
            .withUntypedValue("rangeActions", joinedRaSetpoints)
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("Best leaf: range action(s): {}", joinedRaSetpoints);
    }

    public static void reportSearchDepthBestLeafRangeActions(final ReportNode parentNode,
                                                             final int depth,
                                                             final Leaf leaf,
                                                             final OptimizationPerimeter optimizationContext) {
        final List<String> rangeActionSetpoints = getRangeActionSetpoints(leaf, optimizationContext);

        if (rangeActionSetpoints.isEmpty()) {
            reportSearchDepthBestLeafNoRangeActionActivated(parentNode, depth);
        } else {
            reportSearchDepthBestLeafRangeActionsActivated(parentNode, depth, rangeActionSetpoints);
        }
    }

    private static void reportSearchDepthBestLeafNoRangeActionActivated(final ReportNode parentNode, final int depth) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportSearchDepthBestLeafNoRangeActionActivated")
            .withUntypedValue("depth", depth)
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("Search depth {} best leaf: No range actions activated", depth);
    }

    private static void reportSearchDepthBestLeafRangeActionsActivated(final ReportNode parentNode, final int depth, final List<String> rangeActionSetpoints) {
        final String joinedRaSetpoints = String.join(", ", rangeActionSetpoints);

        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportSearchDepthBestLeafRangeActionsActivated")
            .withUntypedValue("depth", depth)
            .withUntypedValue("rangeActions", joinedRaSetpoints)
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("Search depth {} best leaf: Range action(s): {}", depth, joinedRaSetpoints);
    }
}
