/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.google.common.hash.Hashing;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters;
import com.powsybl.openrao.searchtreerao.commons.HvdcUtils;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import com.powsybl.openrao.searchtreerao.commons.SensitivityComputer;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.GlobalOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.TreeParameters;
import com.powsybl.openrao.searchtreerao.reports.MostLimitingElementsReports;
import com.powsybl.openrao.searchtreerao.reports.OptimizationSummaryReports;
import com.powsybl.openrao.searchtreerao.reports.SearchTreeReports;
import com.powsybl.openrao.searchtreerao.reports.VirtualCostReports;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.searchtreerao.searchtree.inputs.SearchTreeInput;
import com.powsybl.openrao.searchtreerao.searchtree.parameters.SearchTreeParameters;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import com.powsybl.openrao.util.AbstractNetworkPool;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;
import static com.powsybl.openrao.searchtreerao.commons.HvdcUtils.runLoadFlowAndUpdateHvdcActivePowerSetpoint;

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
    private static final double EPSILON = 1e-6;
    private static final int NUMBER_LOGGED_ELEMENTS_DURING_TREE = 2;
    private static final int NUMBER_LOGGED_ELEMENTS_END_TREE = 5;
    private static final String SEARCH_TREE_WORKING_VARIANT_ID = "SearchTreeWorkingVariantId";

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
    private final ReportNode reportNode;

    private Leaf rootLeaf;
    private Leaf optimalLeaf;
    private Leaf previousDepthOptimalLeaf;

    private Optional<NetworkActionCombination> combinationFulfillingStopCriterion = Optional.empty();

    public SearchTree(final SearchTreeInput input,
                      final SearchTreeParameters parameters,
                      final boolean verbose,
                      final ReportNode reportNode) {
        // inputs
        this.input = input;
        this.parameters = parameters;
        this.verbose = verbose;
        this.reportNode = reportNode;

        // build from inputs
        this.purelyVirtual = input.getOptimizationPerimeter().getOptimizedFlowCnecs().isEmpty();
        this.bloomer = new SearchTreeBloomer(input, parameters);
    }

    public CompletableFuture<OptimizationResult> run() {
        String preSearchTreeVariantId = input.getNetwork().getVariantManager().getWorkingVariantId();
        input.getNetwork().getVariantManager().cloneVariant(preSearchTreeVariantId, SEARCH_TREE_WORKING_VARIANT_ID, true);
        input.getNetwork().getVariantManager().setWorkingVariant(SEARCH_TREE_WORKING_VARIANT_ID); // the variant used for root leaf and all the child leaves
        try {
            initLeaves(input);

            TECHNICAL_LOGS.debug("Evaluating root leaf");

            // Run load flow here, update HVDC lines' active power setpoint in network that will be used
            // if we deactivate AC emulation on a HVDC line in one of the leaf.

            // Get all the range actions that are HVDC range actions and are not in AC emulation
            Set<HvdcRangeAction> hvdcRasOnHvdcLineInAcEmulation = HvdcUtils.getHvdcRangeActionsOnHvdcLineInAcEmulation(
                input.getOptimizationPerimeter()
                    .getRangeActions()
                    .stream()
                    .filter(HvdcRangeAction.class::isInstance)
                    .map(HvdcRangeAction.class::cast)
                    .collect(Collectors.toSet()),
                input.getNetwork());

            // Get Loadflow and sensitivity parameters
            LoadFlowAndSensitivityParameters loadFlowAndSensitivityParameters = parameters.getLoadFlowAndSensitivityParameters().orElse(new LoadFlowAndSensitivityParameters(reportNode));

            if (!hvdcRasOnHvdcLineInAcEmulation.isEmpty()) {
                runLoadFlowAndUpdateHvdcActivePowerSetpoint(
                    input.getNetwork(),
                    input.getOptimizationPerimeter().getMainOptimizationState(),
                    loadFlowAndSensitivityParameters.getLoadFlowProvider(),
                    loadFlowAndSensitivityParameters.getSensitivityWithLoadFlowParameters().getLoadFlowParameters(),
                    hvdcRasOnHvdcLineInAcEmulation,
                    reportNode
                );
            }

            rootLeaf.evaluate(input.getObjectiveFunction(), getSensitivityComputerForEvaluation(true, reportNode), reportNode);
            if (rootLeaf.getStatus().equals(Leaf.Status.ERROR)) {
                SearchTreeReports.reportCouldNotEvaluateLeaf(reportNode, verbose, rootLeaf);
                reportOptimizationSummary();
                rootLeaf.finalizeOptimization();

                return CompletableFuture.completedFuture(rootLeaf);
            } else if (stopCriterionReached(rootLeaf)) {
                SearchTreeReports.reportStopCriterionReachedOnLeaf(reportNode, verbose, rootLeaf);
                reportMostLimitingElementsWithVerbose(rootLeaf, NUMBER_LOGGED_ELEMENTS_END_TREE);
                reportOptimizationSummary();
                rootLeaf.finalizeOptimization();
                return CompletableFuture.completedFuture(rootLeaf);
            }

            SearchTreeReports.reportRootLeaf(reportNode, false, rootLeaf);
            reportTechnicalMostLimitingElements(rootLeaf, NUMBER_LOGGED_ELEMENTS_DURING_TREE);

            SearchTreeReports.reportLinearOptimizationOnRootLeaf(reportNode);
            optimizeLeaf(rootLeaf, reportNode);

            SearchTreeReports.reportRootLeaf(reportNode, verbose, rootLeaf);
            SearchTreeReports.reportRangeActions(reportNode, optimalLeaf, input.getOptimizationPerimeter());
            reportMostLimitingElementsWithVerbose(optimalLeaf, NUMBER_LOGGED_ELEMENTS_DURING_TREE);
            reportVirtualCostInformation(reportNode, rootLeaf, false);

            if (stopCriterionReached(rootLeaf)) {
                reportOptimizationSummary();
                rootLeaf.finalizeOptimization();
                return CompletableFuture.completedFuture(rootLeaf);
            }

            iterateOnTree();

            SearchTreeReports.reportSearchTreeRaoCompletedWithStatus(reportNode, optimalLeaf.getSensitivityStatus());

            SearchTreeReports.reportBestLeaf(reportNode, optimalLeaf);
            SearchTreeReports.reportBestLeafRangeActions(reportNode, optimalLeaf, input.getOptimizationPerimeter());
            reportTechnicalMostLimitingElements(optimalLeaf, NUMBER_LOGGED_ELEMENTS_END_TREE);

            reportOptimizationSummary();
            optimalLeaf.finalizeOptimization();
            return CompletableFuture.completedFuture(optimalLeaf);
        // Actions have been applied on root leaf, finally revert to initial network
        } finally {
            input.getNetwork().getVariantManager().setWorkingVariant(preSearchTreeVariantId);
        }
    }

    void initLeaves(SearchTreeInput input) {
        rootLeaf = makeLeaf(input.getOptimizationPerimeter(), input.getNetwork(), input.getPrePerimeterResult(), input.getPreOptimizationAppliedRemedialActions());
        optimalLeaf = rootLeaf;
        previousDepthOptimalLeaf = rootLeaf;
    }

    Leaf makeLeaf(OptimizationPerimeter optimizationPerimeter, Network network, PrePerimeterResult prePerimeterOutput, AppliedRemedialActions appliedRemedialActionsInSecondaryStates) {
        return new Leaf(optimizationPerimeter, network, prePerimeterOutput, appliedRemedialActionsInSecondaryStates);
    }

    private void iterateOnTree() {
        int depth = 0;
        boolean hasImproved = true;
        if (input.getOptimizationPerimeter().getNetworkActions().isEmpty()) {
            SearchTreeReports.reportNoNetworkActionAvailable(reportNode, verbose);
            return;
        }

        int leavesInParallel = Math.min(input.getOptimizationPerimeter().getNetworkActions().size(), parameters.getTreeParameters().leavesInParallel());
        TECHNICAL_LOGS.debug("Evaluating {} leaves in parallel", leavesInParallel);
        try (AbstractNetworkPool networkPool = makeOpenRaoNetworkPool(input.getNetwork(), leavesInParallel)) {
            while (depth < parameters.getTreeParameters().maximumSearchDepth() && hasImproved && !stopCriterionReached(optimalLeaf)) {
                final int depthForLogs = depth + 1;
                final ReportNode searchDepthReportNode = SearchTreeReports.reportSearchDepth(reportNode, depthForLogs);
                previousDepthOptimalLeaf = optimalLeaf;
                updateOptimalLeafWithNextDepthBestLeaf(networkPool, searchDepthReportNode);
                hasImproved = previousDepthOptimalLeaf != optimalLeaf; // It means this depth evaluation has improved the global cost
                if (hasImproved) {
                    SearchTreeReports.reportSearchDepthEnd(depthForLogs);

                    SearchTreeReports.reportSearchDepthBestLeaf(reportNode, verbose, depthForLogs, optimalLeaf);
                    SearchTreeReports.reportSearchDepthBestLeafRangeActions(reportNode, depthForLogs, optimalLeaf, input.getOptimizationPerimeter());
                    reportMostLimitingElementsWithVerbose(optimalLeaf, NUMBER_LOGGED_ELEMENTS_DURING_TREE);
                } else {
                    SearchTreeReports.reportNoBetterResultFoundInSearchDepth(reportNode, verbose, depthForLogs);
                }
                depth += 1;
                if (depth >= parameters.getTreeParameters().maximumSearchDepth()) {
                    SearchTreeReports.reportMaxSearchDepthReached(reportNode, verbose);
                }
            }
            networkPool.shutdownAndAwaitTermination(24, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            TECHNICAL_LOGS.warn("A computation thread was interrupted");
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Evaluate all the leaves. We use OpenRaoNetworkPool to parallelize the computation
     */
    private void updateOptimalLeafWithNextDepthBestLeaf(final AbstractNetworkPool networkPool, final ReportNode reportNode) throws InterruptedException {

        TreeSet<NetworkActionCombination> naCombinationsSorted = new TreeSet<>(this::deterministicNetworkActionCombinationComparison);
        naCombinationsSorted.addAll(bloomer.bloom(optimalLeaf, input.getOptimizationPerimeter().getNetworkActions(), reportNode));
        int numberOfCombinations = naCombinationsSorted.size();

        networkPool.initClones(numberOfCombinations);
        if (naCombinationsSorted.isEmpty()) {
            SearchTreeReports.reportNoMoreNetworkActionAvailable(reportNode);
            return;
        } else {
            SearchTreeReports.reportLeavesToEvaluate(reportNode, numberOfCombinations);
        }
        AtomicInteger remainingLeaves = new AtomicInteger(numberOfCombinations);
        List<ForkJoinTask<Object>> tasks = naCombinationsSorted.stream().map(naCombination -> {
                final ReportNode leafOptimizationReportNode = SearchTreeReports.reportLeafOptimization(reportNode, verbose, naCombination.getConcatenatedId());
                return networkPool.submit(() -> optimizeOneLeaf(networkPool, naCombination, remainingLeaves, leafOptimizationReportNode));
            }
        ).toList();
        for (ForkJoinTask<Object> task : tasks) {
            try {
                task.get();
            } catch (ExecutionException e) {
                throw new OpenRaoException(e);
            }
        }
    }

    private Object optimizeOneLeaf(final AbstractNetworkPool networkPool,
                                   final NetworkActionCombination naCombination,
                                   final AtomicInteger remainingLeaves,
                                   final ReportNode reportNode) throws InterruptedException {
        Network networkClone = networkPool.getAvailableNetwork(); //This is where the threads actually wait for available networks
        try {
            if (combinationFulfillingStopCriterion.isEmpty() || deterministicNetworkActionCombinationComparison(naCombination, combinationFulfillingStopCriterion.get()) < 0) {
                boolean shouldRangeActionBeRemoved = bloomer.shouldRangeActionsBeRemovedToApplyNa(naCombination, optimalLeaf);
                if (shouldRangeActionBeRemoved) {
                    // Remove parentLeaf range actions to respect every maxRa or maxOperator limitation
                    // If the HVDC line is in AC emulation the we won't be able to apply setpoint
                    HvdcUtils.filterOutHvdcRangeActionsOnHvdcLineInAcEmulation(input.getOptimizationPerimeter().getRangeActions(), networkClone)
                        .forEach(ra ->
                            ra.apply(networkClone, input.getPrePerimeterResult().getRangeActionSetpointResult().getSetpoint(ra))
                        );

                } else {
                    // Apply range actions that have been changed by the previous leaf on the network to start next depth leaves
                    // from previous optimal leaf starting point
                    // Network actions are not applied here. If in previous leaf AC emulation was deactivated to optimize HVDC range action
                    // we won't be able to apply the optimized setpoint because the HVDC line will still be in AC emulation
                    HvdcUtils.filterOutHvdcRangeActionsOnHvdcLineInAcEmulation(previousDepthOptimalLeaf.getRangeActions(), networkClone)
                        .forEach(ra ->
                            ra.apply(networkClone, previousDepthOptimalLeaf.getOptimizedSetpoint(ra, input.getOptimizationPerimeter().getMainOptimizationState()))
                        );
                }
                optimizeNextLeafAndUpdate(naCombination, shouldRangeActionBeRemoved, networkClone, reportNode);

            } else {
                SearchTreeReports.reportSkippingOptimization(reportNode, verbose, naCombination.getConcatenatedId());
            }
        } catch (Exception e) {
            SearchTreeReports.reportCanNotOptimizeRemedialActionCombination(reportNode, naCombination.getConcatenatedId(), e.getMessage());
        }
        SearchTreeReports.reportRemainingLeavesToEvaluate(reportNode, remainingLeaves.decrementAndGet());
        networkPool.releaseUsedNetwork(networkClone);
        return null;
    }

    int deterministicNetworkActionCombinationComparison(NetworkActionCombination ra1, NetworkActionCombination ra2) {
        // 1. First priority given to combinations detected during RAO
        // 2. Second priority given to pre-defined combinations
        // 3. Third priority given to large combinations
        // 4. Last priority is random but deterministic
        Comparator<NetworkActionCombination> networkActionCombinationComparator =
            Comparator.<NetworkActionCombination, NetworkActionCombination>comparing(ra -> ra, this::compareIsDetectedDuringRao)
                .thenComparing(ra -> ra, this::compareIsPreDefined)
                .thenComparing(ra -> ra, this::compareSize)
                .thenComparingInt(ra -> Hashing.crc32().hashString(ra.getConcatenatedId(), StandardCharsets.UTF_8).asInt());

        return networkActionCombinationComparator.compare(ra1, ra2);
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

    AbstractNetworkPool makeOpenRaoNetworkPool(Network network, int leavesInParallel) {
        return AbstractNetworkPool.create(network, network.getVariantManager().getWorkingVariantId(), leavesInParallel, false);
    }

    void optimizeNextLeafAndUpdate(final NetworkActionCombination naCombination,
                                   final boolean shouldRangeActionBeRemoved,
                                   final Network network,
                                   final ReportNode reportNode) {
        Leaf leaf;
        try {
            // We get initial range action results from the previous optimal leaf
            leaf = createChildLeaf(network, naCombination, shouldRangeActionBeRemoved);
        } catch (OpenRaoException e) {
            Set<NetworkAction> networkActions = new HashSet<>(previousDepthOptimalLeaf.getActivatedNetworkActions());
            networkActions.addAll(naCombination.getNetworkActionSet());
            SearchTreeReports.reportCouldNotEvaluateNetworkActionCombination(reportNode, verbose, networkActions, e);
            return;
        }
        // We evaluate the leaf with taking the results of the previous optimal leaf if we do not want to update some results
        leaf.evaluate(input.getObjectiveFunction(), getSensitivityComputerForEvaluation(shouldRangeActionBeRemoved, reportNode), reportNode);

        SearchTreeReports.reportEvaluatedLeaf(reportNode, verbose, leaf);
        if (!leaf.getStatus().equals(Leaf.Status.ERROR)) {
            if (!stopCriterionReached(leaf)) {
                if (combinationFulfillingStopCriterion.isPresent() && deterministicNetworkActionCombinationComparison(naCombination, combinationFulfillingStopCriterion.get()) > 0) {
                    SearchTreeReports.reportSkippingOptimization(reportNode, verbose, naCombination.getConcatenatedId());
                } else {
                    optimizeLeaf(leaf, reportNode);

                    SearchTreeReports.reportOptimizedLeaf(reportNode, verbose, leaf);
                    reportVirtualCostInformation(reportNode, leaf, true);
                }
            } else {
                SearchTreeReports.reportOptimizedLeaf(reportNode, verbose, leaf);
            }
            updateOptimalLeaf(leaf, naCombination, reportNode);
        } else {
            SearchTreeReports.reportCouldNotEvaluateLeaf(reportNode, verbose, leaf);
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

    private void optimizeLeaf(final Leaf leaf, final ReportNode reportNode) {
        if (!input.getOptimizationPerimeter().getRangeActions().isEmpty()) {
            leaf.optimize(input, parameters, reportNode);
            if (!leaf.getStatus().equals(Leaf.Status.OPTIMIZED)) {
                SearchTreeReports.reportFailedToOptimizeLeaf(reportNode, verbose, leaf);
            }
        } else {
            SearchTreeReports.reportNoRangeActionToOptimize(reportNode);
        }
    }

    private SensitivityComputer getSensitivityComputerForEvaluation(final boolean isRootLeaf, final ReportNode reportNode) {

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

        if (parameters.getLoopFlowParametersExtension() != null) {
            if (parameters.getLoopFlowParametersExtension().getPtdfApproximation().shouldUpdatePtdfWithTopologicalChange()) {
                sensitivityComputerBuilder.withCommercialFlowsResults(input.getToolProvider().getLoopFlowComputation(), input.getOptimizationPerimeter().getLoopFlowCnecs());
            } else {
                sensitivityComputerBuilder.withCommercialFlowsResults(input.getInitialFlowResult());
            }
        }

        return sensitivityComputerBuilder.build();
    }

    private synchronized void updateOptimalLeaf(final Leaf leaf,
                                                final NetworkActionCombination networkActionCombination,
                                                final ReportNode reportNode) {
        if (improvedEnough(leaf)) {
            // nominal case: stop criterion hasn't been reached yet
            if (combinationFulfillingStopCriterion.isEmpty() && leaf.getCost() < optimalLeaf.getCost()) {
                optimalLeaf = leaf;
                if (stopCriterionReached(leaf)) {
                    SearchTreeReports.reportStopCriterionReached(reportNode);
                    combinationFulfillingStopCriterion = Optional.of(networkActionCombination);
                }
            }
            // special case: stop criterion has been reached
            if (combinationFulfillingStopCriterion.isPresent()
                && stopCriterionReached(leaf)
                && deterministicNetworkActionCombinationComparison(networkActionCombination, combinationFulfillingStopCriterion.get()) < 0) {
                optimalLeaf = leaf;
                combinationFulfillingStopCriterion = Optional.of(networkActionCombination);
            }
        }
    }

    /**
     * This method evaluates stop criterion on the leaf.
     *
     * @param leaf Leaf to evaluate.
     * @return True if the stop criterion has been reached on this leaf.
     */
    private boolean stopCriterionReached(final Leaf leaf) {
        if (leaf.getVirtualCost() > EPSILON) {
            return false;
        }
        if (purelyVirtual && leaf.getVirtualCost() < EPSILON) {
            TECHNICAL_LOGS.debug("Perimeter is purely virtual and virtual cost is zero. Exiting search tree.");
            return true;
        }
        return costSatisfiesStopCriterion(leaf.getCost(), parameters);
    }

    /**
     * Returns true if a given cost value satisfies the stop criterion
     */
    public static boolean costSatisfiesStopCriterion(double cost, SearchTreeParameters parameters) {
        if (parameters.getObjectiveFunction().costOptimization()) {
            return cost < EPSILON;
        } else if (parameters.getTreeParameters().stopCriterion().equals(TreeParameters.StopCriterion.MIN_OBJECTIVE)) {
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
     * @param leaf Leaf that has to be compared with the optimal leaf.
     * @return True if the leaf cost diminution is enough compared to optimal leaf.
     */
    private boolean improvedEnough(final Leaf leaf) {
        double relativeImpact = Math.max(parameters.getNetworkActionParameters().getRelativeNetworkActionMinimumImpactThreshold(), 0);
        double absoluteImpact = Math.max(parameters.getNetworkActionParameters().getAbsoluteNetworkActionMinimumImpactThreshold(), 0);

        double previousDepthBestCost = previousDepthOptimalLeaf.getCost();
        double newCost = leaf.getCost();

        if (previousDepthBestCost > newCost && stopCriterionReached(leaf)) {
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

    private void reportVirtualCostInformation(final ReportNode reportNode, final Leaf rootLeaf, final boolean optimized) {
        VirtualCostReports.reportVirtualCostInformation(reportNode, verbose, rootLeaf, parameters.getObjectiveFunctionUnit(), previousDepthOptimalLeaf.getFunctionalCost(), parameters, optimized);
    }

    private void reportTechnicalMostLimitingElements(final Leaf rootLeaf, final int numberLoggedElementsDuringTree) {
        MostLimitingElementsReports.reportTechnicalMostLimitingElements(reportNode, rootLeaf, rootLeaf, parameters.getObjectiveFunction(), parameters.getObjectiveFunctionUnit(), numberLoggedElementsDuringTree);
    }

    private void reportOptimizationSummary() {
        OptimizationSummaryReports.reportOptimizationSummary(reportNode, rootLeaf, input, rootLeaf.getPreOptimObjectiveFunctionResult());
        VirtualCostReports.reportVirtualCostInformation(reportNode, verbose, optimalLeaf, parameters.getObjectiveFunctionUnit(), previousDepthOptimalLeaf.getFunctionalCost(), parameters, false);
    }

    private void reportMostLimitingElementsWithVerbose(final Leaf leaf, final int numberLoggedElementsDuringTree) {
        if (verbose) {
            MostLimitingElementsReports.reportBusinessMostLimitingElements(reportNode, leaf, leaf, parameters.getObjectiveFunction(), parameters.getObjectiveFunctionUnit(), numberLoggedElementsDuringTree);
        } else {
            reportTechnicalMostLimitingElements(leaf, numberLoggedElementsDuringTree);
        }
    }
}
