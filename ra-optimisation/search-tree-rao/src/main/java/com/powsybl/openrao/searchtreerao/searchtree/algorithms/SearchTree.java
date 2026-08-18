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
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters;
import com.powsybl.openrao.searchtreerao.commons.HvdcUtils;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import com.powsybl.openrao.searchtreerao.commons.SensitivityComputer;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.GlobalOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.TreeParameters;
import com.powsybl.openrao.searchtreerao.networkpool.AbstractNetworkPool;
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

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;
import static com.powsybl.openrao.searchtreerao.commons.HvdcUtils.runLoadFlowAndUpdateHvdcActivePowerSetpoint;
import static com.powsybl.openrao.searchtreerao.commons.RaoUtil.getNumberOfConnectedComponent;

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
    private Integer initialNumberOfConnectedComponent;

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
        // the optimization is purely virtual only if no timestamp's perimeter has any optimizable flow CNEC
        this.purelyVirtual = input.getAllOptimizationPerimeters().getDataPerTimestamp().values().stream().allMatch(optimizationPerimeter -> optimizationPerimeter.getOptimizedFlowCnecs().isEmpty());
        this.bloomer = new SearchTreeBloomer(input, parameters);
        this.initialNumberOfConnectedComponent = null;
        if (!parameters.getNetworkActionParameters().isAllowElectricalIslandCreation()) {
            this.initialNumberOfConnectedComponent = getNumberOfConnectedComponent(input.getNetwork());
        }
    }

    public CompletableFuture<OptimizationResult> run() {
        // one pre-search-tree variant per timestamp, restored at the end
        TemporalData<String> preSearchTreeVariantIds = getPreSearchTreeVariantIds(input);
        try {
            initLeaves(input);

            TECHNICAL_LOGS.debug("Evaluating root leaf");

            // Run load flow here, update HVDC lines' active power setpoint in network that will be used
            // if we deactivate AC emulation on a HVDC line in one of the leaf.

            // Get Loadflow and sensitivity parameters
            LoadFlowAndSensitivityParameters loadFlowAndSensitivityParameters = parameters.getLoadFlowAndSensitivityParameters().orElse(new LoadFlowAndSensitivityParameters(reportNode));
            input.getAllNetworks().getDataPerTimestamp().forEach(
                    (timestamp, network) -> {
                        OptimizationPerimeter perimeter = input.getAllOptimizationPerimeters().getData(timestamp).orElseThrow();
                        // Get all the range actions that are HVDC range actions and are not in AC emulation
                        Set<HvdcRangeAction> hvdcRasOnHvdcLineInAcEmulation = HvdcUtils.getHvdcRangeActionsOnHvdcLineInAcEmulation(
                                perimeter.getRangeActions().stream().filter(HvdcRangeAction.class::isInstance).map(HvdcRangeAction.class::cast).collect(Collectors.toSet()), network
                        );
                        if (!hvdcRasOnHvdcLineInAcEmulation.isEmpty()) {
                            runLoadFlowAndUpdateHvdcActivePowerSetpoint(
                                network,
                                perimeter.getMainOptimizationState(),
                                loadFlowAndSensitivityParameters.getLoadFlowProvider(),
                                loadFlowAndSensitivityParameters.getSensitivityWithLoadFlowParameters().getLoadFlowParameters(),
                                hvdcRasOnHvdcLineInAcEmulation,
                                reportNode
                            );
                        }
                    });
            rootLeaf.evaluate(input.getObjectiveFunction(), getSensitivityComputersForEvaluation(true, reportNode), reportNode);
            if (rootLeaf.getStatus().equals(Leaf.Status.ERROR)) {
                SearchTreeReports.reportCouldNotEvaluateLeaf(reportNode, verbose, rootLeaf);
                reportOptimizationSummary(rootLeaf);
                rootLeaf.finalizeOptimization();

                return CompletableFuture.completedFuture(rootLeaf);
            } else if (stopCriterionReached(rootLeaf)) {
                SearchTreeReports.reportStopCriterionReachedOnLeaf(reportNode, verbose, rootLeaf);
                reportMostLimitingElementsWithVerbose(rootLeaf, NUMBER_LOGGED_ELEMENTS_END_TREE);
                reportOptimizationSummary(rootLeaf);
                rootLeaf.finalizeOptimization();
                return CompletableFuture.completedFuture(rootLeaf);
            }

            SearchTreeReports.reportRootLeaf(reportNode, false, rootLeaf);
            reportTechnicalMostLimitingElements(rootLeaf, NUMBER_LOGGED_ELEMENTS_DURING_TREE);

            SearchTreeReports.reportLinearOptimizationOnRootLeaf(reportNode);
            optimizeLeaf(rootLeaf, reportNode);

            SearchTreeReports.reportRootLeaf(reportNode, verbose, rootLeaf);
            SearchTreeReports.reportRangeActions(reportNode, optimalLeaf, input.getAllOptimizationPerimeters());
            reportMostLimitingElementsWithVerbose(optimalLeaf, NUMBER_LOGGED_ELEMENTS_DURING_TREE);
            reportVirtualCostInformation(reportNode, rootLeaf, false);

            if (stopCriterionReached(rootLeaf)) {
                reportOptimizationSummary(rootLeaf);
                rootLeaf.finalizeOptimization();
                return CompletableFuture.completedFuture(rootLeaf);
            }

            iterateOnTree();

            SearchTreeReports.reportSearchTreeRaoCompletedWithStatus(reportNode, optimalLeaf.getSensitivityStatus());

            SearchTreeReports.reportBestLeaf(reportNode, optimalLeaf);
            SearchTreeReports.reportBestLeafRangeActions(reportNode, optimalLeaf, input.getAllOptimizationPerimeters());
            reportTechnicalMostLimitingElements(optimalLeaf, NUMBER_LOGGED_ELEMENTS_END_TREE);

            reportOptimizationSummary(optimalLeaf);
            optimalLeaf.finalizeOptimization();
            return CompletableFuture.completedFuture(optimalLeaf);
            // Actions have been applied on root leaf, revert every timestamp's network to its initial variant
        } finally {
            preSearchTreeVariantIds.getDataPerTimestamp().forEach((timestamp, variantId) ->
                    input.getAllNetworks().getData(timestamp).orElseThrow().getVariantManager().setWorkingVariant(variantId));
        }
    }

    void initLeaves(SearchTreeInput input) {
        rootLeaf = makeLeaf(input.getAllOptimizationPerimeters(), input.getAllNetworks(), input.getAllPrePerimeterResults(), input.getAllPreOptimizationAppliedRemedialActions());
        optimalLeaf = rootLeaf;
        previousDepthOptimalLeaf = rootLeaf;
    }

    Leaf makeLeaf(TemporalData<OptimizationPerimeter> optimizationPerimeters,
                  TemporalData<Network> networks,
                  TemporalData<PrePerimeterResult> prePerimeterOutputs,
                  TemporalData<AppliedRemedialActions> appliedRemedialActionsInSecondaryStates) {
        return new Leaf(optimizationPerimeters, networks, prePerimeterOutputs, appliedRemedialActionsInSecondaryStates);
    }

    private void iterateOnTree() {
        int depth = 0;
        boolean hasImproved = true;
        // the candidates are the network actions available on every timestamp's perimeter, matched by their ID
        Set<NetworkAction> availableNetworkActions = getAvailableNetworkActionsAcrossTimestamps();
        if (availableNetworkActions.isEmpty()) {
            SearchTreeReports.reportNoNetworkActionAvailable(reportNode, verbose);
            return;
        }

        int leavesInParallel = Math.min(availableNetworkActions.size(), parameters.getTreeParameters().leavesInParallel());
        TECHNICAL_LOGS.debug("Evaluating {} leaves in parallel", leavesInParallel);
        // one network pool per timestamp
        TemporalData<AbstractNetworkPool> networkPools = makeOpenRaoNetworkPools(input.getAllNetworks(), leavesInParallel);
        try {
            while (depth < parameters.getTreeParameters().maximumSearchDepth() && hasImproved && !stopCriterionReached(optimalLeaf)) {
                final int depthForLogs = depth + 1;
                final ReportNode searchDepthReportNode = SearchTreeReports.reportSearchDepth(reportNode, depthForLogs);
                previousDepthOptimalLeaf = optimalLeaf;
                updateOptimalLeafWithNextDepthBestLeaf(networkPools, availableNetworkActions, searchDepthReportNode);
                hasImproved = previousDepthOptimalLeaf != optimalLeaf; // It means this depth evaluation has improved the global cost
                if (hasImproved) {
                    SearchTreeReports.reportSearchDepthEnd(depthForLogs);

                    SearchTreeReports.reportSearchDepthBestLeaf(reportNode, verbose, depthForLogs, optimalLeaf);
                    SearchTreeReports.reportSearchDepthBestLeafRangeActions(reportNode, depthForLogs, optimalLeaf, input.getAllOptimizationPerimeters());
                    reportMostLimitingElementsWithVerbose(optimalLeaf, NUMBER_LOGGED_ELEMENTS_DURING_TREE);
                } else {
                    SearchTreeReports.reportNoBetterResultFoundInSearchDepth(reportNode, verbose, depthForLogs);
                }
                depth += 1;
                if (depth >= parameters.getTreeParameters().maximumSearchDepth()) {
                    SearchTreeReports.reportMaxSearchDepthReached(reportNode, verbose);
                }
            }
            shutdownNetworkPools(networkPools);
        } catch (InterruptedException e) {
            TECHNICAL_LOGS.warn("A computation thread was interrupted");
            Thread.currentThread().interrupt();
        } finally {
            closeNetworkPools(networkPools);
        }
    }

    /**
     * Evaluate all the leaves. We use OpenRaoNetworkPool to parallelize the computation.
     * <p>
     * In time coupled, tasks are submitted to the first timestamp's pool, which provides the execution threads. each
     * Then, every task borrows one network clone from every timestamp's pool.
     */
    private void updateOptimalLeafWithNextDepthBestLeaf(final TemporalData<AbstractNetworkPool> networkPools,
                                                        final Set<NetworkAction> availableNetworkActions,
                                                        final ReportNode reportNode) throws InterruptedException {

        TreeSet<NetworkActionCombination> naCombinationsSorted = new TreeSet<>(this::deterministicNetworkActionCombinationComparison);
        input.getNetwork().getVariantManager().setWorkingVariant(SEARCH_TREE_WORKING_VARIANT_ID);
        naCombinationsSorted.addAll(bloomer.bloom(optimalLeaf, availableNetworkActions, reportNode));
        int numberOfCombinations = naCombinationsSorted.size();

        for (AbstractNetworkPool networkPool : networkPools.getDataPerTimestamp().values()) {
            networkPool.initClones(numberOfCombinations);
        }
        if (naCombinationsSorted.isEmpty()) {
            SearchTreeReports.reportNoMoreNetworkActionAvailable(reportNode);
            return;
        } else {
            SearchTreeReports.reportLeavesToEvaluate(reportNode, numberOfCombinations);
        }
        AtomicInteger remainingLeaves = new AtomicInteger(numberOfCombinations);
        // first timestamp's pool -> execution threads, every pool -> network clones
        AbstractNetworkPool networkPool = networkPools.getData(networkPools.getTimestamps().getFirst()).orElseThrow();
        List<ForkJoinTask<Object>> tasks = naCombinationsSorted.stream().map(naCombination -> {
            final ReportNode leafOptimizationReportNode = SearchTreeReports.reportLeafOptimization(reportNode, verbose, naCombination.getConcatenatedId());
            return networkPool.submit(() -> optimizeOneLeaf(networkPools, naCombination, remainingLeaves, leafOptimizationReportNode));
        }).toList();
        for (ForkJoinTask<Object> task : tasks) {
            try {
                task.get();
            } catch (ExecutionException e) {
                throw new OpenRaoException(e);
            }
        }
    }

    private Object optimizeOneLeaf(final TemporalData<AbstractNetworkPool> networkPools,
                                   final NetworkActionCombination naCombination,
                                   final AtomicInteger remainingLeaves,
                                   final ReportNode reportNode) throws InterruptedException {
        Map<OffsetDateTime, Network> clones = new HashMap<>();
        try {
            // one clone from every timestamp's pool, this is where the threads actually wait for available networks
            for (Map.Entry<OffsetDateTime, AbstractNetworkPool> entry : networkPools.getDataPerTimestamp().entrySet()) {
                clones.put(entry.getKey(), entry.getValue().getAvailableNetwork());
            }
            TemporalData<Network> networkClones = new TemporalDataImpl<>(clones);

            if (combinationFulfillingStopCriterion.isEmpty() || deterministicNetworkActionCombinationComparison(naCombination, combinationFulfillingStopCriterion.get()) < 0) {
                boolean shouldRangeActionBeRemoved = bloomer.shouldRangeActionsBeRemovedToApplyNa(naCombination, optimalLeaf);
                if (shouldRangeActionBeRemoved) {
                    // Remove parentLeaf range actions to respect every maxRa or maxOperator limitation
                    // If the HVDC line is in AC emulation then we won't be able to apply setpoint
                    resetRangeActionsToSetpoints(networkClones,
                            input.getAllOptimizationPerimeters().map(OptimizationPerimeter::getRangeActions),
                            (timestamp, rangeAction) ->
                                    input.getAllPrePerimeterResults().getData(timestamp).orElseThrow().getRangeActionSetpointResult().getSetpoint(rangeAction));
                } else {
                    // Apply range actions that have been changed by the previous leaf on the network to start next depth leaves
                    // from previous optimal leaf starting point
                    // Network actions are not applied here. If in previous leaf AC emulation was deactivated to optimize HVDC range action
                    // we won't be able to apply the optimized setpoint because the HVDC line will still be in AC emulation
                    resetRangeActionsToSetpoints(networkClones,
                            input.getAllOptimizationPerimeters().map(OptimizationPerimeter::getRangeActions),
                            (timestamp, rangeAction) ->
                                previousDepthOptimalLeaf.getAllRangeActionActivationResults().getData(timestamp).orElseThrow()
                                        .getOptimizedSetpoint(rangeAction, input.getAllOptimizationPerimeters().getData(timestamp).orElseThrow().getMainOptimizationState()));
                }
                optimizeNextLeafAndUpdate(naCombination, shouldRangeActionBeRemoved, networkClones, reportNode);

            } else {
                SearchTreeReports.reportSkippingOptimization(reportNode, verbose, naCombination.getConcatenatedId());
            }
        } catch (OpenRaoException e) {
            SearchTreeReports.reportCanNotOptimizeRemedialActionCombination(reportNode, naCombination.getConcatenatedId(), e.getMessage());
        } finally {
            SearchTreeReports.reportRemainingLeavesToEvaluate(reportNode, remainingLeaves.decrementAndGet());
            // release every clone that was effectively borrowed (robust to partial acquisition)
            for (Map.Entry<OffsetDateTime, Network> entry : clones.entrySet()) {
                networkPools.getData(entry.getKey()).orElseThrow().releaseUsedNetwork(entry.getValue());
            }
        }
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

    void optimizeNextLeafAndUpdate(final NetworkActionCombination naCombination,
                                   final boolean shouldRangeActionBeRemoved,
                                   final TemporalData<Network> networks,
                                   final ReportNode reportNode) {
        Leaf leaf;
        try {
            // We get initial range action results from the previous optimal leaf
            leaf = createChildLeaf(networks, naCombination, shouldRangeActionBeRemoved);
        } catch (OpenRaoException e) {
            // the leaf creation fails if the combination cannot be applied on every timestamp's network
            Set<NetworkAction> networkActions = new HashSet<>(previousDepthOptimalLeaf.getActivatedNetworkActions());
            networkActions.addAll(naCombination.getNetworkActionSet());
            SearchTreeReports.reportCouldNotEvaluateNetworkActionCombination(reportNode, verbose, networkActions, e);
            return;
        }
        // We evaluate the leaf with taking the results of the previous optimal leaf if we do not want to update some results
        leaf.evaluate(input.getObjectiveFunction(), getSensitivityComputersForEvaluation(shouldRangeActionBeRemoved, reportNode), reportNode);

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

    Leaf createChildLeaf(TemporalData<Network> networks, NetworkActionCombination naCombination, boolean shouldRangeActionBeRemoved) throws OpenRaoException {
        return new Leaf(
            input.getAllOptimizationPerimeters(),
            networks,
            previousDepthOptimalLeaf.getActivatedNetworkActions(),
            naCombination,
            shouldRangeActionBeRemoved ? input.getAllPrePerimeterResults().map(RangeActionActivationResultImpl::new) : previousDepthOptimalLeaf.getAllRangeActionActivationResults(),
            input.getAllPrePerimeterResults().map(prePerimeterResult -> prePerimeterResult),
            shouldRangeActionBeRemoved ? input.getAllPreOptimizationAppliedRemedialActions() : getPreviousDepthAppliedRemedialActionsBeforeNewLeafEvaluation(previousDepthOptimalLeaf),
            parameters.getNetworkActionParameters().isAllowElectricalIslandCreation(),
            initialNumberOfConnectedComponent);
    }

    private void optimizeLeaf(final Leaf leaf, final ReportNode reportNode) {
        // the linear optimization is run if at least one timestamp still has range actions to optimize
        boolean anyRangeActions = input.getAllOptimizationPerimeters().getDataPerTimestamp().values().stream().anyMatch(perimeter -> !perimeter.getRangeActions().isEmpty());
        if (anyRangeActions) {
            leaf.optimize(input, parameters, getMipParallelism(), reportNode);
            if (!leaf.getStatus().equals(Leaf.Status.OPTIMIZED)) {
                SearchTreeReports.reportFailedToOptimizeLeaf(reportNode, verbose, leaf);
            }
        } else {
            SearchTreeReports.reportNoRangeActionToOptimize(reportNode);
        }
    }

    private TemporalData<SensitivityComputer> getSensitivityComputersForEvaluation(final boolean isRootLeaf, final ReportNode reportNode) {
        // build one sensitivity computer per timestamp
        Map<OffsetDateTime, SensitivityComputer> sensitivityComputers = new HashMap<>();
        TemporalData<AppliedRemedialActions> appliedRaForSensi;
        if (isRootLeaf) {
            appliedRaForSensi = input.getAllPreOptimizationAppliedRemedialActions();
        } else {
            appliedRaForSensi = getPreviousDepthAppliedRemedialActionsBeforeNewLeafEvaluation(previousDepthOptimalLeaf);
        }
        input.getAllOptimizationPerimeters().getDataPerTimestamp().forEach((timestamp, perimeter) -> {
            SensitivityComputer.SensitivityComputerBuilder sensitivityComputerBuilder = SensitivityComputer.create(reportNode)
                    .withToolProvider(input.getAllToolProviders().getData(timestamp).orElseThrow()).withCnecs(perimeter.getFlowCnecs()).withRangeActions(perimeter.getRangeActions())
                    .withOutageInstant(input.getAllOutageInstants().getData(timestamp).orElseThrow()).withAppliedRemedialActions(appliedRaForSensi.getData(timestamp).orElseThrow());
            if (parameters.getObjectiveFunction().relativePositiveMargins()) {
                if (parameters.getMaxMinRelativeMarginParameters().getPtdfApproximation().shouldUpdatePtdfWithTopologicalChange()) {
                    sensitivityComputerBuilder.withPtdfsResults(input.getAllToolProviders().getData(timestamp).orElseThrow().getAbsolutePtdfSumsComputation(), perimeter.getFlowCnecs());
                } else {
                    sensitivityComputerBuilder.withPtdfsResults(input.getAllInitialFlowResults().getData(timestamp).orElseThrow());
                }
            }
            if (parameters.getLoopFlowParametersExtension() != null) {
                if (parameters.getLoopFlowParametersExtension().getPtdfApproximation().shouldUpdatePtdfWithTopologicalChange()) {
                    sensitivityComputerBuilder.withCommercialFlowsResults(input.getAllToolProviders().getData(timestamp).orElseThrow().getLoopFlowComputation(), perimeter.getLoopFlowCnecs());
                } else {
                    sensitivityComputerBuilder.withCommercialFlowsResults(input.getAllInitialFlowResults().getData(timestamp).orElseThrow());
                }
            }
            sensitivityComputers.put(timestamp, sensitivityComputerBuilder.build());
        });
        return new TemporalDataImpl<>(sensitivityComputers);
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
        // TODO: a satisfied global cost does not mean that every timestamp is secure,
        //  should the stop criterion also require the worst margin of all the timestamps to be positive?
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

    private TemporalData<AppliedRemedialActions> getPreviousDepthAppliedRemedialActionsBeforeNewLeafEvaluation(Leaf previousDepthLeaf) {
        Map<OffsetDateTime, AppliedRemedialActions> appliedRemedialActions = new HashMap<>();
        input.getAllOptimizationPerimeters().getDataPerTimestamp().forEach((timestamp, optimizationPerimeter) -> {
            AppliedRemedialActions alreadyAppliedRa = input.getAllPreOptimizationAppliedRemedialActions().getData(timestamp).orElseThrow().copy();
            if (optimizationPerimeter instanceof GlobalOptimizationPerimeter) {
                RangeActionActivationResult previousDepthActivation = previousDepthLeaf.getAllRangeActionActivationResults().getData(timestamp).orElseThrow();
                optimizationPerimeter.getRangeActionsPerState().keySet().stream()
                        .filter(state -> !state.equals(optimizationPerimeter.getMainOptimizationState()))
                        .forEach(state -> previousDepthActivation.getActivatedRangeActions(state)
                                .forEach(ra -> alreadyAppliedRa.addAppliedRangeAction(state, ra, previousDepthActivation.getOptimizedSetpoint(ra, state))));
            }
            appliedRemedialActions.put(timestamp, alreadyAppliedRa);
        });
        return new TemporalDataImpl<>(appliedRemedialActions);
    }

    private TemporalData<String> getPreSearchTreeVariantIds(SearchTreeInput input) {
        return input.getAllNetworks().map(network -> {
            String preSearchTreeVariantId = network.getVariantManager().getWorkingVariantId();
            network.getVariantManager().cloneVariant(preSearchTreeVariantId, SEARCH_TREE_WORKING_VARIANT_ID, true);
            network.getVariantManager().setWorkingVariant(SEARCH_TREE_WORKING_VARIANT_ID); // the variant used for root leaf and all the child leaves
            return preSearchTreeVariantId;
        });
    }

    TemporalData<AbstractNetworkPool> makeOpenRaoNetworkPools(TemporalData<Network> networks, int leavesInParallel) {
        return networks.map(network -> AbstractNetworkPool.create(network, network.getVariantManager().getWorkingVariantId(), leavesInParallel, false));
    }

    /** Waits for the shutdown of every timestamp's pool. */
    private static void shutdownNetworkPools(TemporalData<AbstractNetworkPool> networkPools) throws InterruptedException {
        for (AbstractNetworkPool networkPool : networkPools.getDataPerTimestamp().values()) {
            networkPool.shutdownAndAwaitTermination(24, TimeUnit.HOURS);
        }
    }

    /** Closes every timestamp's pool and releases its network clones. */
    private static void closeNetworkPools(TemporalData<AbstractNetworkPool> networkPools) {
        networkPools.getDataPerTimestamp().values().forEach(AbstractNetworkPool::close);
    }

    /** Applies on every timestamp's network clone the given setpoint of the range actions, skipping the the HVDC range
     * actions whose line is in AC emulation as their setpoint cannot be applied. */
    private void resetRangeActionsToSetpoints(TemporalData<Network> networkClones,
                                              TemporalData<Set<RangeAction<?>>> rangeActions,
                                              BiFunction<OffsetDateTime, RangeAction<?>, Double> setpointProvider) {
        networkClones.getDataPerTimestamp().forEach((timestamp, networkClone) ->
                HvdcUtils.filterOutHvdcRangeActionsOnHvdcLineInAcEmulation(rangeActions.getData(timestamp).orElseThrow(), networkClone)
                        .forEach(rangeAction -> rangeAction.apply(networkClone, setpointProvider.apply(timestamp, rangeAction)))
        );
    }

    /** Gathers the network actions available on every timestamp's perimeter.*/
    private Set<NetworkAction> getAvailableNetworkActionsAcrossTimestamps() {
        Map<String, NetworkAction> networkActionsById = new HashMap<>();
        input.getAllOptimizationPerimeters().getDataPerTimestamp().values().forEach(
                optimizationPerimeter -> optimizationPerimeter.getNetworkActions().forEach(
                        networkAction -> networkActionsById.putIfAbsent(networkAction.getId(), networkAction)
                )
        );
        // keep only the network actions whose ID is present in every timestamp's perimeter
        input.getAllOptimizationPerimeters().getDataPerTimestamp().values().forEach(
                optimizationPerimeter -> networkActionsById.keySet().retainAll(
                        optimizationPerimeter.getNetworkActions().stream().map(NetworkAction::getId).collect(Collectors.toSet())
                )
        );
        return new HashSet<>(networkActionsById.values());
    }

    private int getMipParallelism() {
        return Math.min(parameters.getTreeParameters().leavesInParallel(), input.getAllNetworks().getTimestamps().size());
    }

    private void reportVirtualCostInformation(final ReportNode reportNode, final Leaf rootLeaf, final boolean optimized) {
        VirtualCostReports.reportVirtualCostInformation(reportNode, verbose, rootLeaf, parameters.getFlowUnit(), previousDepthOptimalLeaf.getFunctionalCost(), parameters, optimized);
    }

    private void reportTechnicalMostLimitingElements(final Leaf rootLeaf, final int numberLoggedElementsDuringTree) {
        MostLimitingElementsReports.reportTechnicalMostLimitingElements(reportNode, rootLeaf, rootLeaf, parameters.getObjectiveFunction(), parameters.getFlowUnit(), numberLoggedElementsDuringTree);
    }

    private void reportOptimizationSummary(final Leaf leaf) {
        OptimizationSummaryReports.reportOptimizationSummary(reportNode, leaf, input, rootLeaf.getPreOptimObjectiveFunctionResult());
        VirtualCostReports.reportVirtualCostInformation(reportNode, verbose, leaf, parameters.getFlowUnit(), previousDepthOptimalLeaf.getFunctionalCost(), parameters, false);
    }

    private void reportMostLimitingElementsWithVerbose(final Leaf leaf, final int numberLoggedElementsDuringTree) {
        if (verbose) {
            MostLimitingElementsReports.reportBusinessMostLimitingElements(reportNode, leaf, leaf, parameters.getObjectiveFunction(), parameters.getFlowUnit(), numberLoggedElementsDuringTree);
        } else {
            reportTechnicalMostLimitingElements(leaf, numberLoggedElementsDuringTree);
        }
    }
}
