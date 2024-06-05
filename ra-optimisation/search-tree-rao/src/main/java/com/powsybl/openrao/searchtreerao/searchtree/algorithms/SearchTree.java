/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import com.powsybl.openrao.searchtreerao.commons.RaoLogger;
import com.powsybl.openrao.searchtreerao.commons.SensitivityComputer;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.GlobalOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.TreeParameters;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.searchtreerao.searchtree.SearchTreeReports;
import com.powsybl.openrao.searchtreerao.searchtree.inputs.SearchTreeInput;
import com.powsybl.openrao.searchtreerao.searchtree.parameters.SearchTreeParameters;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import com.powsybl.openrao.util.AbstractNetworkPool;
import com.google.common.hash.Hashing;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.NotImplementedException;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.powsybl.openrao.searchtreerao.castor.algorithm.AutomatonSimulator.getRangeActionsAndTheirTapsAppliedOnState;

/**
 * The "tree" is one of the core object of the search-tree algorithm.
 * It aims at finding a good combination of Network Actions.
 * <p>
 * The tree is composed of leaves which evaluate the impact of Network Actions,
 * one by one. The tree is orchestrating the leaves : it looks for a smart
 * routing among the leaves in order to converge as quickly as possible to a local
 * minimum of the objective function.
 * <p>
 * The leaves of a same depth can be evaluated simultaneously.
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class SearchTree {
    private static final int NUMBER_LOGGED_ELEMENTS_DURING_TREE = 2;
    private static final int NUMBER_LOGGED_ELEMENTS_END_TREE = 5;
    private static final int NUMBER_LOGGED_VIRTUAL_COSTLY_ELEMENTS = 10;

    /**
     * attribute defined in constructor of the search tree class
     */

    private final SearchTreeInput input;
    private final SearchTreeParameters parameters;
    private final boolean verbose;

    /**
     * attribute defined and used within the class
     */

    private final boolean purelyVirtual;
    private final SearchTreeBloomer bloomer;

    private Leaf rootLeaf;
    private Leaf optimalLeaf;
    private Leaf previousDepthOptimalLeaf;

    private Optional<NetworkActionCombination> combinationFulfillingStopCriterion = Optional.empty();

    public SearchTree(SearchTreeInput input,
                      SearchTreeParameters parameters,
                      boolean verbose) {
        // inputs
        this.input = input;
        this.parameters = parameters;
        this.verbose = verbose;

        // build from inputs
        this.purelyVirtual = input.getOptimizationPerimeter().getOptimizedFlowCnecs().isEmpty();
        this.bloomer = new SearchTreeBloomer(input, parameters);
    }

    private TypedValue reportSeverity() {
        return verbose ? TypedValue.INFO_SEVERITY : TypedValue.DEBUG_SEVERITY;
    }

    public CompletableFuture<OptimizationResult> run(ReportNode reportNode) {

        initLeaves(input);
        ReportNode rootLeafReportNode = SearchTreeReports.reportRootLeafEvaluation(reportNode);
        rootLeaf.evaluate(input.getObjectiveFunction(), getSensitivityComputerForEvaluation(true, reportNode), reportNode);
        if (rootLeaf.getStatus().equals(Leaf.Status.ERROR)) {
            SearchTreeReports.reportLeafEvaluationError(rootLeafReportNode, rootLeaf.toString(), reportSeverity());
            logOptimizationSummary(rootLeaf, rootLeafReportNode);
            rootLeaf.finalizeOptimization();
            return CompletableFuture.completedFuture(rootLeaf);
        } else if (stopCriterionReached(rootLeaf, rootLeafReportNode)) {
            SearchTreeReports.reportStopCriterionReached(rootLeafReportNode, rootLeaf.toString(), reportSeverity());
            RaoLogger.logMostLimitingElementsResults(rootLeaf, parameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_END_TREE, rootLeafReportNode, reportSeverity());
            logOptimizationSummary(rootLeaf, rootLeafReportNode);
            rootLeaf.finalizeOptimization();
            return CompletableFuture.completedFuture(rootLeaf);
        }

        SearchTreeReports.reportLeaf(rootLeafReportNode, rootLeaf.toString(), reportSeverity());
        RaoLogger.logMostLimitingElementsResults(rootLeaf, parameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_DURING_TREE, rootLeafReportNode, TypedValue.DEBUG_SEVERITY);

        SearchTreeReports.reportLinearOptimization(rootLeafReportNode);
        optimizeLeaf(rootLeaf, rootLeafReportNode);

        SearchTreeReports.reportLeaf(rootLeafReportNode, rootLeaf.toString(), reportSeverity());
        RaoLogger.logRangeActions(optimalLeaf, input.getOptimizationPerimeter(), "", rootLeafReportNode);
        RaoLogger.logMostLimitingElementsResults(optimalLeaf, parameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_DURING_TREE, rootLeafReportNode, reportSeverity());
        logVirtualCostInformation(rootLeaf, "", rootLeafReportNode);

        if (stopCriterionReached(rootLeaf, rootLeafReportNode)) {
            logOptimizationSummary(rootLeaf, rootLeafReportNode);
            rootLeaf.finalizeOptimization();
            return CompletableFuture.completedFuture(rootLeaf);
        }

        iterateOnTree(rootLeafReportNode);

        SearchTreeReports.reportRaoCompleted(rootLeafReportNode, optimalLeaf.getSensitivityStatus());

        SearchTreeReports.reportBestLeaf(rootLeafReportNode, optimalLeaf.toString());
        RaoLogger.logRangeActions(optimalLeaf, input.getOptimizationPerimeter(), "Best leaf: ", rootLeafReportNode);
        RaoLogger.logMostLimitingElementsResults(optimalLeaf, parameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_END_TREE, reportNode, TypedValue.DEBUG_SEVERITY);

        logOptimizationSummary(optimalLeaf, reportNode);
        optimalLeaf.finalizeOptimization();
        return CompletableFuture.completedFuture(optimalLeaf);
    }

    void initLeaves(SearchTreeInput input) {
        rootLeaf = makeLeaf(input.getOptimizationPerimeter(), input.getNetwork(), input.getPrePerimeterResult(), input.getPreOptimizationAppliedRemedialActions());
        optimalLeaf = rootLeaf;
        previousDepthOptimalLeaf = rootLeaf;
    }

    Leaf makeLeaf(OptimizationPerimeter optimizationPerimeter, Network network, PrePerimeterResult prePerimeterOutput, AppliedRemedialActions appliedRemedialActionsInSecondaryStates) {
        return new Leaf(optimizationPerimeter, network, prePerimeterOutput, appliedRemedialActionsInSecondaryStates);
    }

    private void logOptimizationSummary(Leaf optimalLeaf, ReportNode reportNode) {
        State state = input.getOptimizationPerimeter().getMainOptimizationState();
        RaoLogger.logOptimizationSummary(state, optimalLeaf.getActivatedNetworkActions(), getRangeActionsAndTheirTapsAppliedOnState(optimalLeaf, state), rootLeaf.getPreOptimObjectiveFunctionResult(), optimalLeaf, reportNode);
        logVirtualCostInformation(optimalLeaf, "", reportNode);
    }

    private void iterateOnTree(ReportNode rootLeafReportNode) {
        int depth = 0;
        boolean hasImproved = true;
        if (input.getOptimizationPerimeter().getNetworkActions().isEmpty()) {
            SearchTreeReports.reportNoNetworkActionAvailable(rootLeafReportNode, reportSeverity());
            return;
        }

        int leavesInParallel = Math.min(input.getOptimizationPerimeter().getNetworkActions().size(), parameters.getTreeParameters().leavesInParallel());
        SearchTreeReports.reportLeavesInParallel(rootLeafReportNode, leavesInParallel);
        try (AbstractNetworkPool networkPool = makeOpenRaoNetworkPool(input.getNetwork(), leavesInParallel)) {
            while (depth < parameters.getTreeParameters().maximumSearchDepth() && hasImproved && !stopCriterionReached(optimalLeaf, rootLeafReportNode)) {
                SearchTreeReports.reportDepthStart(rootLeafReportNode, depth + 1);
                previousDepthOptimalLeaf = optimalLeaf;
                updateOptimalLeafWithNextDepthBestLeaf(networkPool, rootLeafReportNode);
                hasImproved = previousDepthOptimalLeaf != optimalLeaf; // It means this depth evaluation has improved the global cost
                if (hasImproved) {
                    SearchTreeReports.reportDepthEnd(rootLeafReportNode, depth + 1);

                    SearchTreeReports.reportBestLeafAtDepth(rootLeafReportNode, depth + 1, optimalLeaf.toString(), reportSeverity());
                    RaoLogger.logRangeActions(optimalLeaf, input.getOptimizationPerimeter(), String.format("Search depth %s best leaf: ", depth + 1), rootLeafReportNode);
                    RaoLogger.logMostLimitingElementsResults(optimalLeaf, parameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_DURING_TREE, rootLeafReportNode, reportSeverity());
                } else {
                    SearchTreeReports.reportDepthNoBetterResult(rootLeafReportNode, depth + 1, reportSeverity());
                }
                depth += 1;
                if (depth >= parameters.getTreeParameters().maximumSearchDepth()) {
                    SearchTreeReports.reportMaxDepth(rootLeafReportNode, reportSeverity());
                }
            }
            networkPool.shutdownAndAwaitTermination(24, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            SearchTreeReports.reportInterrupted(rootLeafReportNode);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Evaluate all the leaves. We use OpenRaoNetworkPool to parallelize the computation
     */
    private void updateOptimalLeafWithNextDepthBestLeaf(AbstractNetworkPool networkPool, ReportNode rootLeafReportNode) throws InterruptedException {

        TreeSet<NetworkActionCombination> naCombinationsSorted = new TreeSet<>(this::deterministicNetworkActionCombinationComparison);
        naCombinationsSorted.addAll(bloomer.bloom(optimalLeaf, input.getOptimizationPerimeter().getNetworkActions()));
        int numberOfCombinations = naCombinationsSorted.size();

        networkPool.initClones(numberOfCombinations);
        if (naCombinationsSorted.isEmpty()) {
            SearchTreeReports.reportNoMoreNetworkAction(rootLeafReportNode);
            return;
        } else {
            SearchTreeReports.reportLeavesToEvaluate(rootLeafReportNode, numberOfCombinations);
        }
        AtomicInteger remainingLeaves = new AtomicInteger(numberOfCombinations);
        List<ForkJoinTask<ReportNode>> tasks = naCombinationsSorted.stream().map(naCombination ->
            networkPool.submit(() -> optimizeOneLeaf(networkPool, naCombination, remainingLeaves))
        ).toList();
        for (ForkJoinTask<ReportNode> task : tasks) {
            try {
                rootLeafReportNode.include(task.get());
            } catch (ExecutionException e) {
                throw new OpenRaoException(e);
            }
        }
    }

    private ReportNode optimizeOneLeaf(AbstractNetworkPool networkPool, NetworkActionCombination naCombination, AtomicInteger remainingLeaves) throws InterruptedException {
        ReportNode rootReportNode = SearchTreeReports.generateOneLeafRootReportNode();
        ReportNode leafReportNode = SearchTreeReports.reportOneLeaf(rootReportNode, naCombination.getConcatenatedId(), remainingLeaves.get());
        Network networkClone = networkPool.getAvailableNetwork(); //This is where the threads actually wait for available networks
        try {
            if (combinationFulfillingStopCriterion.isEmpty() || deterministicNetworkActionCombinationComparison(naCombination, combinationFulfillingStopCriterion.get()) < 0) {
                boolean shouldRangeActionBeRemoved = bloomer.shouldRangeActionsBeRemovedToApplyNa(naCombination, optimalLeaf);
                if (shouldRangeActionBeRemoved) {
                    // Remove parentLeaf range actions to respect every maxRa or maxOperator limitation
                    input.getOptimizationPerimeter().getRangeActions().forEach(ra ->
                        ra.apply(networkClone, input.getPrePerimeterResult().getRangeActionSetpointResult().getSetpoint(ra))
                    );
                } else {
                    // Apply range actions that have been changed by the previous leaf on the network to start next depth leaves
                    // from previous optimal leaf starting point
                    // todo : Not sure previousDepthOptimalLeaf.getRangeActions() returns what we expect, this needs to be investigated
                    previousDepthOptimalLeaf.getRangeActions()
                        .forEach(ra ->
                            ra.apply(networkClone, previousDepthOptimalLeaf.getOptimizedSetpoint(ra, input.getOptimizationPerimeter().getMainOptimizationState()))
                        );
                }
                optimizeNextLeafAndUpdate(naCombination, shouldRangeActionBeRemoved, networkClone, leafReportNode);

            } else {
                SearchTreeReports.reportOneLeafSkipped(leafReportNode, naCombination.getConcatenatedId(), reportSeverity());
            }
        } catch (Exception e) {
            SearchTreeReports.reportOneLeafCannotOptimize(leafReportNode, naCombination.getConcatenatedId(), e.getMessage());
        }
        SearchTreeReports.reportOneLeafRemainingLeaves(leafReportNode, remainingLeaves.decrementAndGet());
        networkPool.releaseUsedNetwork(networkClone);
        return rootReportNode;
    }

    int deterministicNetworkActionCombinationComparison(NetworkActionCombination ra1, NetworkActionCombination ra2) {
        // 1. First priority given to combinations detected during RAO
        int comp1 = compareIsDetectedDuringRao(ra1, ra2);
        if (comp1 != 0) {
            return comp1;
        }
        // 2. Second priority given to pre-defined combinations
        int comp2 = compareIsPreDefined(ra1, ra2);
        if (comp2 != 0) {
            return comp2;
        }
        // 3. Third priority given to large combinations
        int comp3 = compareSize(ra1, ra2);
        if (comp3 != 0) {
            return comp3;
        }
        // 4. Last priority is random but deterministic
        return Integer.compare(Hashing.crc32().hashString(ra1.getConcatenatedId(), StandardCharsets.UTF_8).hashCode(),
                Hashing.crc32().hashString(ra2.getConcatenatedId(), StandardCharsets.UTF_8).hashCode());
    }

    /**
     * Prioritizes the better network action combination that was detected by the RAO
     */
    private int compareIsDetectedDuringRao(NetworkActionCombination ra1, NetworkActionCombination ra2) {
        return -Boolean.compare(ra1.isDetectedDuringRao(), ra2.isDetectedDuringRao());
    }

    /**
     * Prioritizes the network action combination that pre-defined by the user
     */
    private int compareIsPreDefined(NetworkActionCombination ra1, NetworkActionCombination ra2) {
        return -Boolean.compare(this.bloomer.hasPreDefinedNetworkActionCombination(ra1), this.bloomer.hasPreDefinedNetworkActionCombination(ra2));
    }

    /**
     * Prioritizes the bigger network action combination
     */
    private int compareSize(NetworkActionCombination ra1, NetworkActionCombination ra2) {
        return -Integer.compare(ra1.getNetworkActionSet().size(), ra2.getNetworkActionSet().size());
    }

    private String printNetworkActions(Set<NetworkAction> networkActions) { // TODO refactoring with NetworkActionCombination ?
        return networkActions.stream().map(NetworkAction::getId).collect(Collectors.joining(" + "));
    }

    AbstractNetworkPool makeOpenRaoNetworkPool(Network network, int leavesInParallel) {
        return AbstractNetworkPool.create(network, network.getVariantManager().getWorkingVariantId(), leavesInParallel, false);
    }

    void optimizeNextLeafAndUpdate(NetworkActionCombination naCombination, boolean shouldRangeActionBeRemoved, Network network, ReportNode leafReportNode) {
        Leaf leaf;
        try {
            // We get initial range action results from the previous optimal leaf
            leaf = createChildLeaf(network, naCombination, shouldRangeActionBeRemoved);
        } catch (OpenRaoException e) {
            Set<NetworkAction> networkActions = new HashSet<>(previousDepthOptimalLeaf.getActivatedNetworkActions());
            networkActions.addAll(naCombination.getNetworkActionSet());
            SearchTreeReports.reportOneLeafCouldNotEvaluateCombination(leafReportNode, printNetworkActions(networkActions), e.getMessage(), reportSeverity());
            return;
        } catch (NotImplementedException e) {
            throw e;
        }
        // We evaluate the leaf with taking the results of the previous optimal leaf if we do not want to update some results
        leaf.evaluate(input.getObjectiveFunction(), getSensitivityComputerForEvaluation(shouldRangeActionBeRemoved, leafReportNode), leafReportNode);

        SearchTreeReports.reportOneLeafEvaluated(leafReportNode, leaf.toString(), reportSeverity());
        if (!leaf.getStatus().equals(Leaf.Status.ERROR)) {
            if (!stopCriterionReached(leaf, leafReportNode)) {
                if (combinationFulfillingStopCriterion.isPresent() && deterministicNetworkActionCombinationComparison(naCombination, combinationFulfillingStopCriterion.get()) > 0) {
                    SearchTreeReports.reportOneLeafSkipped(leafReportNode, naCombination.getConcatenatedId(), reportSeverity());
                } else {
                    optimizeLeaf(leaf, leafReportNode);

                    SearchTreeReports.reportOneLeafOptimized(leafReportNode, leaf.toString(), reportSeverity());
                    logVirtualCostInformation(leaf, "Optimized ", leafReportNode);
                }
            } else {
                SearchTreeReports.reportOneLeafOptimized(leafReportNode, leaf.toString(), reportSeverity());
            }
            updateOptimalLeaf(leaf, naCombination, leafReportNode);
        } else {
            SearchTreeReports.reportLeafEvaluationError(leafReportNode, leaf.toString(), reportSeverity());
        }
    }

    Leaf createChildLeaf(Network network, NetworkActionCombination naCombination, boolean shouldRangeActionBeRemoved) {
        return new Leaf(
            input.getOptimizationPerimeter(),
            network,
            previousDepthOptimalLeaf.getActivatedNetworkActions(),
            naCombination,
            shouldRangeActionBeRemoved ? new RangeActionActivationResultImpl(input.getPrePerimeterResult()) : previousDepthOptimalLeaf.getRangeActionActivationResult(),
            input.getPrePerimeterResult(),
            shouldRangeActionBeRemoved ? input.getPreOptimizationAppliedRemedialActions() : getPreviousDepthAppliedRemedialActionsBeforeNewLeafEvaluation(previousDepthOptimalLeaf));
    }

    private void optimizeLeaf(Leaf leaf, ReportNode reportNode) {
        if (!input.getOptimizationPerimeter().getRangeActions().isEmpty()) {
            leaf.optimize(input, parameters, reportNode);
            if (!leaf.getStatus().equals(Leaf.Status.OPTIMIZED)) {
                SearchTreeReports.reportOneLeafFailedToOptimize(reportNode, leaf.toString(), reportSeverity());
            }
        } else {
            SearchTreeReports.reportOneLeafNoRangeActionToOptimize(reportNode);
        }
    }

    private SensitivityComputer getSensitivityComputerForEvaluation(boolean isRootLeaf, ReportNode reportNode) {

        SensitivityComputer.SensitivityComputerBuilder sensitivityComputerBuilder = SensitivityComputer.create(reportNode)
            .withToolProvider(input.getToolProvider())
            .withCnecs(input.getOptimizationPerimeter().getFlowCnecs())
            .withRangeActions(input.getOptimizationPerimeter().getRangeActions())
            .withOutageInstant(input.getOutageInstant());

        if (isRootLeaf) {
            sensitivityComputerBuilder.withAppliedRemedialActions(input.getPreOptimizationAppliedRemedialActions());
        } else {
            sensitivityComputerBuilder.withAppliedRemedialActions(getPreviousDepthAppliedRemedialActionsBeforeNewLeafEvaluation(previousDepthOptimalLeaf));
        }

        if (parameters.getObjectiveFunction().relativePositiveMargins()) {
            if (parameters.getMaxMinRelativeMarginParameters().getPtdfApproximation().shouldUpdatePtdfWithTopologicalChange()) {
                sensitivityComputerBuilder.withPtdfsResults(input.getToolProvider().getAbsolutePtdfSumsComputation(), input.getOptimizationPerimeter().getFlowCnecs());
            } else {
                sensitivityComputerBuilder.withPtdfsResults(input.getInitialFlowResult());
            }
        }

        if (parameters.getLoopFlowParameters() != null && parameters.getLoopFlowParameters().getPtdfApproximation().shouldUpdatePtdfWithTopologicalChange()) {
            sensitivityComputerBuilder.withCommercialFlowsResults(input.getToolProvider().getLoopFlowComputation(), input.getOptimizationPerimeter().getLoopFlowCnecs());
        } else if (parameters.getLoopFlowParameters() != null) {
            sensitivityComputerBuilder.withCommercialFlowsResults(input.getInitialFlowResult());
        }

        return sensitivityComputerBuilder.build();
    }

    private synchronized void updateOptimalLeaf(Leaf leaf, NetworkActionCombination networkActionCombination, ReportNode reportNode) {
        if (improvedEnough(leaf, reportNode)) {
            // nominal case: stop criterion hasn't been reached yet
            if (combinationFulfillingStopCriterion.isEmpty() && leaf.getCost() < optimalLeaf.getCost()) {
                optimalLeaf = leaf;
                if (stopCriterionReached(leaf, reportNode)) {
                    SearchTreeReports.reportOneLeafStopCriterionReached(reportNode);
                    combinationFulfillingStopCriterion = Optional.of(networkActionCombination);
                }
            }
            // special case: stop criterion has been reached
            if (combinationFulfillingStopCriterion.isPresent()
                && stopCriterionReached(leaf, reportNode)
                && deterministicNetworkActionCombinationComparison(networkActionCombination, combinationFulfillingStopCriterion.get()) < 0) {
                optimalLeaf = leaf;
                combinationFulfillingStopCriterion = Optional.of(networkActionCombination);
            }
        }
    }

    /**
     * This method evaluates stop criterion on the leaf.
     *
     * @param leaf: Leaf to evaluate.
     * @param reportNode
     * @return True if the stop criterion has been reached on this leaf.
     */
    private boolean stopCriterionReached(Leaf leaf, ReportNode reportNode) {
        if (leaf.getVirtualCost() > 1e-6) {
            return false;
        }
        if (purelyVirtual && leaf.getVirtualCost() < 1e-6) {
            SearchTreeReports.reportOneLeafPerimeterPurelyVirtual(reportNode);
            return true;
        }
        return costSatisfiesStopCriterion(leaf.getCost());
    }

    /**
     * Returns true if a given cost value satisfies the stop criterion
     */
    boolean costSatisfiesStopCriterion(double cost) {
        if (parameters.getTreeParameters().stopCriterion().equals(TreeParameters.StopCriterion.MIN_OBJECTIVE)) {
            return false;
        } else if (parameters.getTreeParameters().stopCriterion().equals(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE)) {
            return cost < parameters.getTreeParameters().targetObjectiveValue();
        } else {
            throw new OpenRaoException("Unexpected stop criterion: " + parameters.getTreeParameters().stopCriterion());
        }
    }

    /**
     * This method checks if the leaf's cost respects the minimum impact thresholds
     * (absolute and relative) compared to the previous depth's optimal leaf.
     *
     * @param leaf: Leaf that has to be compared with the optimal leaf.
     * @param reportNode
     * @return True if the leaf cost diminution is enough compared to optimal leaf.
     */
    private boolean improvedEnough(Leaf leaf, ReportNode reportNode) {
        double relativeImpact = Math.max(parameters.getNetworkActionParameters().getRelativeNetworkActionMinimumImpactThreshold(), 0);
        double absoluteImpact = Math.max(parameters.getNetworkActionParameters().getAbsoluteNetworkActionMinimumImpactThreshold(), 0);

        double previousDepthBestCost = previousDepthOptimalLeaf.getCost();
        double newCost = leaf.getCost();

        if (previousDepthBestCost > newCost && stopCriterionReached(leaf, reportNode)) {
            return true;
        }

        return previousDepthBestCost - absoluteImpact > newCost // enough absolute impact
            && (1 - Math.signum(previousDepthBestCost) * relativeImpact) * previousDepthBestCost > newCost; // enough relative impact
    }

    private AppliedRemedialActions getPreviousDepthAppliedRemedialActionsBeforeNewLeafEvaluation(RangeActionActivationResult previousDepthRangeActionActivations) {
        AppliedRemedialActions alreadyAppliedRa = input.getPreOptimizationAppliedRemedialActions().copy();
        if (input.getOptimizationPerimeter() instanceof GlobalOptimizationPerimeter) {
            input.getOptimizationPerimeter().getRangeActionsPerState().entrySet().stream()
                    .filter(e -> !e.getKey().equals(input.getOptimizationPerimeter().getMainOptimizationState())) // remove preventive state
                    .forEach(e -> e.getValue().forEach(ra -> alreadyAppliedRa.addAppliedRangeAction(e.getKey(), ra, previousDepthRangeActionActivations.getOptimizedSetpoint(ra, e.getKey()))));
        }
        return alreadyAppliedRa;
    }

    /**
     * This method logs information about positive virtual costs
     */
    private void logVirtualCostInformation(Leaf leaf, String prefix, ReportNode reportNode) {
        leaf.getVirtualCostNames().stream()
                .filter(virtualCostName -> leaf.getVirtualCost(virtualCostName) > 1e-6)
                .forEach(virtualCostName -> logVirtualCostDetails(leaf, virtualCostName, prefix, reportNode));
    }

    /**
     * If stop criterion could have been reached without the given virtual cost, this method logs a message, in order
     * to inform the user that the given network action was rejected because of a virtual cost
     * (message is not logged if it has already been logged at previous depth)
     * In all cases, this method also logs most costly elements for given virtual cost
     */
    void logVirtualCostDetails(Leaf leaf, String virtualCostName, String prefix, ReportNode reportNode) {
        TypedValue severity = reportSeverity();
        if (!costSatisfiesStopCriterion(leaf.getCost())
                && costSatisfiesStopCriterion(leaf.getCost() - leaf.getVirtualCost(virtualCostName))
                && (leaf.isRoot() || !costSatisfiesStopCriterion(previousDepthOptimalLeaf.getFunctionalCost()))) {
            // Stop criterion would have been reached without virtual cost, for the first time at this depth
            // and for the given leaf
            SearchTreeReports.reportVirtualCostDetail(reportNode, prefix, leaf.getIdentifier(), virtualCostName, TypedValue.INFO_SEVERITY);
            // Promote detailed logs about costly elements to BUSINESS_LOGS
            severity = TypedValue.INFO_SEVERITY;
        }
        logVirtualCostlyElementsLogs(reportNode, leaf, virtualCostName, prefix, severity);
    }

    void logVirtualCostlyElementsLogs(ReportNode reportNode, Leaf leaf, String virtualCostName, String prefix, TypedValue severity) {
        Unit unit = parameters.getObjectiveFunction().getUnit();
        int i = 1;
        for (FlowCnec flowCnec : leaf.getCostlyElements(virtualCostName, NUMBER_LOGGED_VIRTUAL_COSTLY_ELEMENTS)) {
            Side limitingSide = leaf.getMargin(flowCnec, Side.LEFT, unit) < leaf.getMargin(flowCnec, Side.RIGHT, unit) ? Side.LEFT : Side.RIGHT;
            double flow = leaf.getFlow(flowCnec, limitingSide, unit);
            Double limitingThreshold = flow >= 0 ? flowCnec.getUpperBound(limitingSide, unit).orElse(flowCnec.getLowerBound(limitingSide, unit).orElse(Double.NaN))
                    : flowCnec.getLowerBound(limitingSide, unit).orElse(flowCnec.getUpperBound(limitingSide, unit).orElse(Double.NaN));
            SearchTreeReports.reportVirtualCostlyElementsLog(reportNode, prefix, leaf.getIdentifier(), virtualCostName, i++, flow, unit, limitingThreshold, leaf.getMargin(flowCnec, limitingSide, unit), flowCnec.getNetworkElement().getId(), flowCnec.getState().getId(), flowCnec.getId(), flowCnec.getName(), severity);
        }
    }
}
