/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.timecoupledsearchtreerao.searchtree.algorithms;

import com.google.common.hash.Hashing;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.crac.api.RaUsageLimits;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import com.powsybl.openrao.timecoupledsearchtreerao.commons.HvdcUtils;
import com.powsybl.openrao.timecoupledsearchtreerao.commons.NetworkActionCombination;
import com.powsybl.openrao.timecoupledsearchtreerao.commons.SensitivityComputer;
import com.powsybl.openrao.timecoupledsearchtreerao.commons.optimizationperimeters.GlobalOptimizationPerimeter;
import com.powsybl.openrao.timecoupledsearchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.timecoupledsearchtreerao.commons.parameters.TreeParameters;
import com.powsybl.openrao.timecoupledsearchtreerao.reports.MostLimitingElementsReports;
import com.powsybl.openrao.timecoupledsearchtreerao.reports.OptimizationSummaryReports;
import com.powsybl.openrao.timecoupledsearchtreerao.reports.SearchTreeReports;
import com.powsybl.openrao.timecoupledsearchtreerao.reports.VirtualCostReports;
import com.powsybl.openrao.timecoupledsearchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.timecoupledsearchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.timecoupledsearchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.timecoupledsearchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.timecoupledsearchtreerao.searchtree.inputs.TimeCoupledSearchTreeInput;
import com.powsybl.openrao.timecoupledsearchtreerao.searchtree.parameters.SearchTreeParameters;
import com.powsybl.openrao.util.AbstractNetworkPool;

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
import static com.powsybl.openrao.timecoupledsearchtreerao.commons.HvdcUtils.runLoadFlowAndUpdateHvdcActivePowerSetpoint;

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
 * <p>
 *     Time coupled :
 *     <li> all inputs except parameters are temporal data and one leaf applies one shared network action combination on every timestamp at once.
 *     <li> the objective function is now global (identical to marmot)
 *     <li> one network pool is created per timestamp and the tasks are submitted on the first timestamp's
 *     pool, so a leaf can be evaluated on all timestamps simultaneously.
 *     <li> RaUsageLimits are common
 * </p>
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class TimeCoupledSearchTree {
    private static final double EPSILON = 1e-6;
    private static final int NUMBER_LOGGED_ELEMENTS_DURING_TREE = 2;
    private static final int NUMBER_LOGGED_ELEMENTS_END_TREE = 5;
    private static final String SEARCH_TREE_WORKING_VARIANT_ID = "TimeCoupledSearchTreeWorkingVariantId";

    /**
     * Attributes defined in the constructor of the search tree class.
     */
    private final TimeCoupledSearchTreeInput input; // temporal data
    private final SearchTreeParameters parameters;
    private final boolean verbose;

    /**
     * Attributes defined and used within the class.
     */
    private final boolean purelyVirtual;
    private final TimeCoupledSearchTreeBloomer bloomer; // temporal data
    private final ReportNode reportNode;

    // temporal data
    private TimeCoupledLeaf rootLeaf;
    private TimeCoupledLeaf optimalLeaf;
    private TimeCoupledLeaf previousDepthOptimalLeaf;

    private Optional<NetworkActionCombination> combinationFulfillingStopCriterion = Optional.empty();

    public TimeCoupledSearchTree(final TimeCoupledSearchTreeInput input,
                                 final SearchTreeParameters parameters,
                                 final boolean verbose,
                                 final ReportNode reportNode) {
        // inputs
        this.input = input;
        this.parameters = parameters;
        this.verbose = verbose;
        this.reportNode = reportNode;
        // build from inputs
        // the whole optimization is purely virtual only if every timestamp's perimeter has no optimizable flow CNEC at all, one timestamp with flow CNECs is enough to trigger global functional cost
        this.purelyVirtual = input.getOptimizationPerimeters().getDataPerTimestamp().values().stream().allMatch(optimizationPerimeter -> optimizationPerimeter.getOptimizedFlowCnecs().isEmpty());
        // RaUsageLimits are shared between all the timestamps
        this.bloomer = new TimeCoupledSearchTreeBloomer(input, parameters, getRaUsageLimits(input, parameters));
    }

    /**
     * evaluates + optimizes the root leaf then iterates depth by depth on the network action combinations till -> stop criterion (max depth or no combination improves the cost)
     */
    public CompletableFuture<OptimizationResult> run() {
        // one pre-search-tree variant per timestamp restored later
        TemporalData<String> preSearchTreeVariantIds = getPreSearchTreeVariantIds(input);
        try {
            initLeaves(input);

            TECHNICAL_LOGS.debug("Evaluating root leaf");

            // Run load flow here, update HVDC lines' active power setpoint in network that will be used
            // if we deactivate AC emulation on a HVDC line in one of the leaf.

            // Get LoadFlow and sensitivity parameters
            LoadFlowAndSensitivityParameters loadFlowAndSensitivityParameters = parameters.getLoadFlowAndSensitivityParameters().orElse(new LoadFlowAndSensitivityParameters(reportNode));
            input.getNetworks().getDataPerTimestamp().forEach((timestamp, network) -> {            // Get all the range actions that are HVDC range actions and are not in AC emulation
                OptimizationPerimeter perimeter = input.getOptimizationPerimeters().getData(timestamp).orElseThrow();
                // Get all the range actions that are HVDC range actions and are not in AC emulation
                Set<HvdcRangeAction> hvdcRasOnHvdcLineInAcEmulation = HvdcUtils.getHvdcRangeActionsOnHvdcLineInAcEmulation(
                        perimeter
                            .getRangeActions()
                            .stream()
                            .filter(HvdcRangeAction.class::isInstance)
                            .map(HvdcRangeAction.class::cast)
                            .collect(Collectors.toSet()),
                        network);
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

            rootLeaf.evaluate(input.getGlobalObjectiveFunction(), getSensitivityComputerForEvaluation(true, reportNode), reportNode);
            if (rootLeaf.getStatus().equals(TimeCoupledLeaf.Status.ERROR)) {
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
            SearchTreeReports.reportRangeActions(reportNode, optimalLeaf, input.getOptimizationPerimeters());
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
            SearchTreeReports.reportBestLeafRangeActions(reportNode, optimalLeaf, input.getOptimizationPerimeters());
            reportTechnicalMostLimitingElements(optimalLeaf, NUMBER_LOGGED_ELEMENTS_END_TREE);

            reportOptimizationSummary(optimalLeaf);
            optimalLeaf.finalizeOptimization();
            return CompletableFuture.completedFuture(optimalLeaf);
        } finally {
            // restore every timestamp's network to its initial variant
            preSearchTreeVariantIds.getDataPerTimestamp().forEach((timestamp, variantId) -> input.getNetworks().getData(timestamp).orElseThrow().getVariantManager().setWorkingVariant(variantId));
        }
    }

    void initLeaves(TimeCoupledSearchTreeInput input) {
        rootLeaf = makeLeaf(input.getOptimizationPerimeters(), input.getNetworks(), input.getPrePerimeterResults(), input.getPreOptimizationAppliedRemedialActions());
        optimalLeaf = rootLeaf;
        previousDepthOptimalLeaf = rootLeaf;
    }

    TimeCoupledLeaf makeLeaf(TemporalData<OptimizationPerimeter> optimizationPerimeters,
                             TemporalData<Network> networks,
                             TemporalData<PrePerimeterResult> prePerimeterOutputs,
                             TemporalData<AppliedRemedialActions> appliedRaInSecondaryStates) {
        return new TimeCoupledLeaf(optimizationPerimeters, networks, prePerimeterOutputs, appliedRaInSecondaryStates);
    }

    /**
     * at every depth computes the network actions available across all timestamps,
     * creates one network pool per timestamp and evaluates the depth's leaves.
     * Stops when the maximum depth is reached, no improvement was found, or the stop criterion is met.
     */
    private void iterateOnTree() {
        int depth = 0;
        boolean hasImproved = true;
        // merge all available network actions across all the timestamps
        Set<NetworkAction> allAvailableNetworkActions = getAvailableNetworkActionsAcrossTimestamps();
        if (allAvailableNetworkActions.isEmpty()) {
            SearchTreeReports.reportNoNetworkActionAvailable(reportNode, verbose);
            return;
        }

        int leavesInParallel = Math.min(allAvailableNetworkActions.size(), parameters.getTreeParameters().leavesInParallel());
        TECHNICAL_LOGS.debug("Evaluating {} leaves in parallel", leavesInParallel);
        // one network pool per timestamp
        TemporalData<AbstractNetworkPool> networkPools = makeTimeCoupledNetworkPool(input.getNetworks(), leavesInParallel);
        try {
            while (depth < parameters.getTreeParameters().maximumSearchDepth() && hasImproved && !stopCriterionReached(optimalLeaf)) {
                final int depthForLogs = depth + 1;
                final ReportNode searchDepthReportNode = SearchTreeReports.reportSearchDepth(reportNode, depthForLogs);
                previousDepthOptimalLeaf = optimalLeaf;
                updateOptimalLeafWithNextDepthBestLeaf(networkPools, allAvailableNetworkActions, searchDepthReportNode);
                hasImproved = previousDepthOptimalLeaf != optimalLeaf; // It means this depth's evaluation improved the global cost
                if (hasImproved) {
                    SearchTreeReports.reportSearchDepthEnd(depthForLogs);

                    SearchTreeReports.reportSearchDepthBestLeaf(reportNode, verbose, depthForLogs, optimalLeaf);
                    SearchTreeReports.reportSearchDepthBestLeafRangeActions(reportNode, depthForLogs, optimalLeaf, input.getOptimizationPerimeters());
                    reportMostLimitingElementsWithVerbose(optimalLeaf, NUMBER_LOGGED_ELEMENTS_DURING_TREE);
                } else {
                    SearchTreeReports.reportNoBetterResultFoundInSearchDepth(reportNode, verbose, depthForLogs);
                }
                depth += 1;
                if (depth >= parameters.getTreeParameters().maximumSearchDepth()) {
                    SearchTreeReports.reportMaxSearchDepthReached(reportNode, verbose);
                }
            }
            // shut down all the pools
            shutdownNetworkPools(networkPools);
        } catch (InterruptedException e) {
            TECHNICAL_LOGS.warn("A computation thread was interrupted");
            Thread.currentThread().interrupt();
        } finally {
            // close all the pools
            closeNetworkPools(networkPools);
        }
    }

    /**
     * Evaluates all the leaves of the current depth.
     *
     * In time coupled :
     * <p>tasks are submitted to the first timestamp's pool, which provides the execution
     * threads, each task then borrows one network clone from every timestamp's pool.</p>
     */
    private void updateOptimalLeafWithNextDepthBestLeaf(final TemporalData<AbstractNetworkPool> networkPools,
                                                        final Set<NetworkAction> allAvailableNetworkActions,
                                                        final ReportNode reportNode) throws InterruptedException {

        TreeSet<NetworkActionCombination> naCombinationsSorted = new TreeSet<>(this::deterministicNetworkActionCombinationComparison);
        naCombinationsSorted.addAll(bloomer.bloom(optimalLeaf, allAvailableNetworkActions, reportNode));
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
        // first timestamp's pool -> threads, other pools -> clones
        AbstractNetworkPool networkPool = networkPools.getDataPerTimestamp().values().iterator().next();
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

    /**
     * for one network action combination: borrows a network clone from every timestamp's pool,
     * resets the range action setpoints, then evaluates/optimizes the candidate leaf on all
     * timestamps at once.
     */
    private Object optimizeOneLeaf(final TemporalData<AbstractNetworkPool> networkPools,
                                   final NetworkActionCombination naCombination,
                                   final AtomicInteger remainingLeaves,
                                   final ReportNode reportNode) throws InterruptedException {
        Map<OffsetDateTime, Network> clones = new HashMap<>();
        try {
            // pick one clone in every timestamp's pool
            for (Map.Entry<OffsetDateTime, AbstractNetworkPool> entry : networkPools.getDataPerTimestamp().entrySet()) {
                clones.put(entry.getKey(), entry.getValue().getAvailableNetwork());
            }
            TemporalData<Network> networkClones = new TemporalDataImpl<>(clones);

            if (combinationFulfillingStopCriterion.isEmpty() || deterministicNetworkActionCombinationComparison(naCombination, combinationFulfillingStopCriterion.get()) < 0) {
                boolean shouldRangeActionBeRemoved = bloomer.shouldRangeActionsBeRemovedToApplyNa(naCombination, optimalLeaf);
                // reset every timestamp's range actions to a known setpoint before testing the candidate combination on top of them
                if (shouldRangeActionBeRemoved) {
                    // Remove parentLeaf range actions to respect every maxRa or maxOperator limitation
                    // If the HVDC line is in AC emulation then we won't be able to apply setpoint
                    resetRangeActionsToSetpoints(networkClones, (timestamp, rangeAction) -> input.getPrePerimeterResults().getData(timestamp).orElseThrow().getRangeActionSetpointResult().getSetpoint(rangeAction));
                } else {
                    // Apply range actions that have been changed by the previous leaf on the network to start next depth leaves
                    // from previous optimal leaf starting point
                    // Network actions are not applied here. If in previous leaf AC emulation was deactivated to optimize HVDC range action
                    // we won't be able to apply the optimized setpoint because the HVDC line will still be in AC emulation
                    resetRangeActionsToSetpoints(networkClones, (timestamp, rangeAction) -> previousDepthOptimalLeaf.getRangeActionActivationResults().getData(timestamp).orElseThrow().getOptimizedSetpoint(rangeAction, input.getOptimizationPerimeters().getData(timestamp).orElseThrow().getMainOptimizationState()));
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
        TimeCoupledLeaf leaf;
        try {
            // We get initial range action results from the previous optimal leaf
            leaf = createChildLeaf(networks, naCombination, shouldRangeActionBeRemoved);
        } catch (OpenRaoException e) {
            // Leaf creation fails if the combination cannot be applied on every timestamp's network
            Set<NetworkAction> networkActions = new HashSet<>(previousDepthOptimalLeaf.getActivatedNetworkActions());
            networkActions.addAll(naCombination.getNetworkActionSet());
            SearchTreeReports.reportCouldNotEvaluateNetworkActionCombination(reportNode, verbose, networkActions, e);
            return;
        }
        // Evaluate the leaf, reusing the previous optimal leaf's results when they must not be updated.
        leaf.evaluate(input.getGlobalObjectiveFunction(), getSensitivityComputerForEvaluation(shouldRangeActionBeRemoved, reportNode), reportNode);

        SearchTreeReports.reportEvaluatedLeaf(reportNode, verbose, leaf);
        if (!leaf.getStatus().equals(TimeCoupledLeaf.Status.ERROR)) {
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

    TimeCoupledLeaf createChildLeaf(TemporalData<Network> networks, NetworkActionCombination naCombination, boolean shouldRangeActionBeRemoved) {
        return new TimeCoupledLeaf(
            input.getOptimizationPerimeters(),
            networks,
            previousDepthOptimalLeaf.getActivatedNetworkActions(),
            naCombination,
            shouldRangeActionBeRemoved ? input.getPrePerimeterResults().map(RangeActionActivationResultImpl::new) : previousDepthOptimalLeaf.getRangeActionActivationResults(),
            input.getPrePerimeterResults().map(perimeterResult -> perimeterResult),
            shouldRangeActionBeRemoved ? input.getPreOptimizationAppliedRemedialActions() : getPreviousDepthAppliedRemedialActionsBeforeNewLeafEvaluation(previousDepthOptimalLeaf));
    }

    private void optimizeLeaf(final TimeCoupledLeaf leaf, final ReportNode reportNode) {
        // the mip is run if at least one timestamp has range actions to optimize
        boolean anyRangeActions = input.getOptimizationPerimeters().getDataPerTimestamp().values().stream().anyMatch(p -> !p.getRangeActions().isEmpty());
        if (anyRangeActions) {
            leaf.optimize(input, parameters, getMipParallelism(), reportNode);
            if (!leaf.getStatus().equals(TimeCoupledLeaf.Status.OPTIMIZED)) {
                SearchTreeReports.reportFailedToOptimizeLeaf(reportNode, verbose, leaf);
            }
        } else {
            SearchTreeReports.reportNoRangeActionToOptimize(reportNode);
        }
    }

    private int getMipParallelism() {
        return Math.min(parameters.getTreeParameters().leavesInParallel(), input.getNetworks().getTimestamps().size());
    }

    /**
     * Builds one sensitivity computer per timestamp, each configured with its own timestamp's tool provider, CNECs, range actions, outage instant and applied remedial actions.
     */
    private TemporalData<SensitivityComputer> getSensitivityComputerForEvaluation(final boolean isRootLeaf, final ReportNode reportNode) {

        Map<OffsetDateTime, SensitivityComputer> sensitivityComputers = new HashMap<>();
        TemporalData<AppliedRemedialActions> appliedRaForSensi;
        if (isRootLeaf) {
            appliedRaForSensi = input.getPreOptimizationAppliedRemedialActions();
        } else {
            appliedRaForSensi = getPreviousDepthAppliedRemedialActionsBeforeNewLeafEvaluation(previousDepthOptimalLeaf);
        }
        input.getOptimizationPerimeters().getDataPerTimestamp().forEach((timestamp, perimeter) -> {
            SensitivityComputer.SensitivityComputerBuilder sensitivityComputerBuilder = SensitivityComputer.create(reportNode)
                .withToolProvider(input.getToolProviders().getData(timestamp).orElseThrow())
                .withCnecs(perimeter.getFlowCnecs())
                .withRangeActions(perimeter.getRangeActions())
                .withOutageInstant(input.getOutageInstants().getData(timestamp).orElseThrow())
                .withAppliedRemedialActions(appliedRaForSensi.getData(timestamp).orElseThrow());

            if (parameters.getObjectiveFunction().relativePositiveMargins()) {
                if (parameters.getMaxMinRelativeMarginParameters().getPtdfApproximation().shouldUpdatePtdfWithTopologicalChange()) {
                    sensitivityComputerBuilder.withPtdfsResults(input.getToolProviders().getData(timestamp).orElseThrow().getAbsolutePtdfSumsComputation(), perimeter.getFlowCnecs());
                } else {
                    sensitivityComputerBuilder.withPtdfsResults(input.getInitialFlowResults().getData(timestamp).orElseThrow());
                }
            }
            if (parameters.getLoopFlowParametersExtension() != null) {
                if (parameters.getLoopFlowParametersExtension().getPtdfApproximation().shouldUpdatePtdfWithTopologicalChange()) {
                    sensitivityComputerBuilder.withCommercialFlowsResults(input.getToolProviders().getData(timestamp).orElseThrow().getLoopFlowComputation(), perimeter.getLoopFlowCnecs());
                } else {
                    sensitivityComputerBuilder.withCommercialFlowsResults(input.getInitialFlowResults().getData(timestamp).orElseThrow());
                }
            }
            sensitivityComputers.put(timestamp, sensitivityComputerBuilder.build());
        });

        return new TemporalDataImpl<>(sensitivityComputers);
    }

    /**
     * Compares a freshly evaluated leaf with the current optimal leaf and keeps the best one.
     */
    private synchronized void updateOptimalLeaf(final TimeCoupledLeaf leaf,
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
    private boolean stopCriterionReached(final TimeCoupledLeaf leaf) {
        if (leaf.getVirtualCost() > EPSILON) {
            return false;
        }
        if (purelyVirtual && leaf.getVirtualCost() < EPSILON) {
            TECHNICAL_LOGS.debug("Perimeter is purely virtual and virtual cost is zero. Exiting search tree.");
            return true;
        }
        return costSatisfiesStopCriterion(leaf.getCost(), parameters) && isSecureAtEveryTimestamp(leaf);
    }

    private boolean isSecureAtEveryTimestamp(final TimeCoupledLeaf leaf) {
        List<FlowCnec> mostLimitingElements = leaf.getMostLimitingElements(1);
        if (mostLimitingElements.isEmpty()) {
            // no optimized CNEC at all : nothing can be overloaded
            return true;
        }
        // the worst margin across all timestamps must be positive for the whole time-coupled layer to be secure
        return leaf.getMargin(mostLimitingElements.getFirst(), parameters.getFlowUnit()) >= 0;
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
    private boolean improvedEnough(final TimeCoupledLeaf leaf) {
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

    private TemporalData<AppliedRemedialActions> getPreviousDepthAppliedRemedialActionsBeforeNewLeafEvaluation(TimeCoupledLeaf previousDepthLeaf) {
        Map<OffsetDateTime, AppliedRemedialActions> appliedRemedialActions = new HashMap<>();
        input.getOptimizationPerimeters().getDataPerTimestamp().forEach((timestamp, optimizationPerimeter) -> {
            AppliedRemedialActions alreadyAppliedRa = input.getPreOptimizationAppliedRemedialActions().getData(timestamp).orElseThrow().copy();
            if (optimizationPerimeter instanceof GlobalOptimizationPerimeter) {
                RangeActionActivationResult previousDepthActivation = previousDepthLeaf.getRangeActionActivationResults().getData(timestamp).orElseThrow();
                optimizationPerimeter.getRangeActionsPerState().entrySet().stream()
                        .filter(e -> !e.getKey().equals(optimizationPerimeter.getMainOptimizationState())) // remove preventive state
                        .forEach(e -> e.getValue().forEach(ra -> alreadyAppliedRa.addAppliedRangeAction(e.getKey(), ra, previousDepthActivation.getOptimizedSetpoint(ra, e.getKey()))));
            }
            appliedRemedialActions.put(timestamp, alreadyAppliedRa);
        });
        return new TemporalDataImpl<>(appliedRemedialActions);
    }

    private void reportVirtualCostInformation(final ReportNode reportNode, final TimeCoupledLeaf leaf, final boolean optimized) {
        VirtualCostReports.reportVirtualCostInformation(reportNode, verbose, leaf, parameters.getFlowUnit(), previousDepthOptimalLeaf.getFunctionalCost(), parameters, optimized);
    }

    private void reportTechnicalMostLimitingElements(final TimeCoupledLeaf leaf, final int numberLoggedElementsDuringTree) {
        MostLimitingElementsReports.reportTechnicalMostLimitingElements(reportNode, leaf, leaf, parameters.getObjectiveFunction(), parameters.getFlowUnit(), numberLoggedElementsDuringTree);
    }

    private void reportOptimizationSummary(final TimeCoupledLeaf leaf) {
        OptimizationSummaryReports.reportOptimizationSummary(reportNode, leaf, input, rootLeaf.getPreOptimObjectiveFunctionResult());
        VirtualCostReports.reportVirtualCostInformation(reportNode, verbose, leaf, parameters.getFlowUnit(), previousDepthOptimalLeaf.getFunctionalCost(), parameters, false);
    }

    private void reportMostLimitingElementsWithVerbose(final TimeCoupledLeaf leaf, final int numberLoggedElementsDuringTree) {
        if (verbose) {
            MostLimitingElementsReports.reportBusinessMostLimitingElements(reportNode, leaf, leaf, parameters.getObjectiveFunction(), parameters.getFlowUnit(), numberLoggedElementsDuringTree);
        } else {
            reportTechnicalMostLimitingElements(leaf, numberLoggedElementsDuringTree);
        }
    }

    // helpers
    /**
     * gets the single ra usage limits instance shared by all timestamps.
     * since one shared combination is applied on every timestamp, the usage limits must be the same for all of them.
     */
    private static RaUsageLimits getRaUsageLimits(TimeCoupledSearchTreeInput input, SearchTreeParameters parameters) {
        // collect every timestamp's main optimization state id (should be identical)
        Set<String> mainOptimizationStates = input.getOptimizationPerimeters().getDataPerTimestamp().values().stream()
                .map(perimeter ->
                        perimeter.getMainOptimizationState().getInstant().getId()).collect(Collectors.toSet()
                );
        // get the ra usage limits declared for this state
        return parameters.getRaLimitationParameters().entrySet().stream()
                .filter(entry -> entry.getKey().getId().equals(mainOptimizationStates.iterator().next()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseGet(RaUsageLimits::new);
    }

    private TemporalData<String> getPreSearchTreeVariantIds(TimeCoupledSearchTreeInput input) {
        return input.getNetworks().map(network -> {
            String preSearchTreeVariantId = network.getVariantManager().getWorkingVariantId();
            network.getVariantManager().cloneVariant(preSearchTreeVariantId, SEARCH_TREE_WORKING_VARIANT_ID, true);
            network.getVariantManager().setWorkingVariant(SEARCH_TREE_WORKING_VARIANT_ID);
            return preSearchTreeVariantId;
        });
    }

    private TemporalData<AbstractNetworkPool> makeTimeCoupledNetworkPool(TemporalData<Network> networks, int leavesInParallel) {
        return networks.map(network -> AbstractNetworkPool.create(network, network.getVariantManager().getWorkingVariantId(), leavesInParallel, false));
    }

    /** waits the shutdown of every timestamp's pool */
    private static void shutdownNetworkPools(TemporalData<AbstractNetworkPool> networkPools) throws InterruptedException {
        for (AbstractNetworkPool networkPool : networkPools.getDataPerTimestamp().values()) {
            networkPool.shutdownAndAwaitTermination(24, TimeUnit.HOURS);
        }
    }

    /** Closes every timestamp's pool and release its network clones */
    private static void closeNetworkPools(TemporalData<AbstractNetworkPool> networkPools) {
        networkPools.getDataPerTimestamp().values().forEach(AbstractNetworkPool::close);
    }

    /**
     * Applies, on every timestamp's network clone, the setpoint given by the setpoint
     * provider to every range action of that timestamp's perimeter
     */
    private void resetRangeActionsToSetpoints(TemporalData<Network> networkClones, BiFunction<OffsetDateTime, RangeAction<?>, Double> setpointProvider) {
        input.getOptimizationPerimeters().getDataPerTimestamp().forEach((timestamp, perimeter) -> {
            Network networkClone = networkClones.getData(timestamp).orElseThrow();
            HvdcUtils.filterOutHvdcRangeActionsOnHvdcLineInAcEmulation(perimeter.getRangeActions(), networkClone).forEach(rangeAction -> rangeAction.apply(networkClone, setpointProvider.apply(timestamp, rangeAction)));
        });
    }

    /**
     * Merges the network actions available on every timestamp's perimeter into a single set.
     */
    private Set<NetworkAction> getAvailableNetworkActionsAcrossTimestamps() {
        Map<String, NetworkAction> networkActionsById = new HashMap<>();
        input.getOptimizationPerimeters().getDataPerTimestamp().values().forEach(
                optimizationPerimeter -> optimizationPerimeter.getNetworkActions().forEach(
                        networkAction -> networkActionsById.putIfAbsent(networkAction.getId(), networkAction)
                )
        );
        return new HashSet<>(networkActionsById.values());
    }
}
