/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.search_tree.algorithms;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.commons.logs.FaraoLogger;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.search_tree_rao.commons.NetworkActionCombination;
import com.farao_community.farao.search_tree_rao.commons.RaoLogger;
import com.farao_community.farao.search_tree_rao.commons.SensitivityComputer;
import com.farao_community.farao.search_tree_rao.commons.optimization_perimeters.CurativeOptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.commons.optimization_perimeters.OptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.commons.parameters.TreeParameters;
import com.farao_community.farao.search_tree_rao.result.api.OptimizationResult;
import com.farao_community.farao.search_tree_rao.result.api.PrePerimeterResult;
import com.farao_community.farao.search_tree_rao.search_tree.inputs.SearchTreeInput;
import com.farao_community.farao.search_tree_rao.search_tree.parameters.SearchTreeParameters;
import com.farao_community.farao.sensitivity_analysis.AppliedRemedialActions;
import com.farao_community.farao.util.AbstractNetworkPool;
import com.google.common.hash.Hashing;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.NotImplementedException;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.farao_community.farao.commons.logs.FaraoLoggerProvider.*;

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
    private final FaraoLogger topLevelLogger;

    /**
     * attribute defined and used within the class
     */

    private final boolean purelyVirtual;
    private final SearchTreeBloomer bloomer;

    private Leaf rootLeaf;
    private Leaf optimalLeaf;
    private Leaf previousDepthOptimalLeaf;

    private double preOptimFunctionalCost;
    private double preOptimVirtualCost;

    private Optional<NetworkActionCombination> combinationFulfillingStopCriterion = Optional.empty();

    public SearchTree(SearchTreeInput input,
                      SearchTreeParameters parameters,
                      boolean verbose) {
        // inputs
        this.input = input;
        this.parameters = parameters;
        this.topLevelLogger = verbose ? BUSINESS_LOGS : TECHNICAL_LOGS;

        // build from inputs
        this.purelyVirtual = input.getOptimizationPerimeter().getOptimizedFlowCnecs().isEmpty();

        if (input.getOptimizationPerimeter() instanceof CurativeOptimizationPerimeter) {
            this.bloomer = new SearchTreeBloomer(
                input.getNetwork(),
                parameters.getRaLimitationParameters().getMaxCurativeRa(),
                parameters.getRaLimitationParameters().getMaxCurativeTso(),
                parameters.getRaLimitationParameters().getMaxCurativeTopoPerTso(),
                parameters.getRaLimitationParameters().getMaxCurativeRaPerTso(),
                parameters.getNetworkActionParameters().skipNetworkActionFarFromMostLimitingElements(),
                parameters.getNetworkActionParameters().getMaxNumberOfBoundariesForSkippingNetworkActions(),
                parameters.getNetworkActionParameters().getNetworkActionCombinations()
            );
        } else {
            this.bloomer = new SearchTreeBloomer(
                input.getNetwork(),
                Integer.MAX_VALUE, //no limitation of RA in preventive
                Integer.MAX_VALUE, //no limitation of RA in preventive
                new HashMap<>(),   //no limitation of RA in preventive
                new HashMap<>(),   //no limitation of RA in preventive
                parameters.getNetworkActionParameters().skipNetworkActionFarFromMostLimitingElements(),
                parameters.getNetworkActionParameters().getMaxNumberOfBoundariesForSkippingNetworkActions(),
                parameters.getNetworkActionParameters().getNetworkActionCombinations()
            );
        }
    }

    public CompletableFuture<OptimizationResult> run() {

        initLeaves(input);

        applyForcedNetworkActionsOnRootLeaf();

        TECHNICAL_LOGS.info("Evaluating root leaf");
        rootLeaf.evaluate(input.getObjectiveFunction(), getSensitivityComputerForEvaluation());
        this.preOptimFunctionalCost = rootLeaf.getFunctionalCost();
        this.preOptimVirtualCost = rootLeaf.getVirtualCost();

        if (rootLeaf.getStatus().equals(Leaf.Status.ERROR)) {
            topLevelLogger.info("Could not evaluate leaf: {}", rootLeaf);
            logOptimizationSummary(rootLeaf);
            return CompletableFuture.completedFuture(rootLeaf);
        } else if (stopCriterionReached(rootLeaf)) {
            topLevelLogger.info("Stop criterion reached on {}", rootLeaf);
            RaoLogger.logMostLimitingElementsResults(topLevelLogger, rootLeaf, parameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_END_TREE);
            logOptimizationSummary(rootLeaf);
            return CompletableFuture.completedFuture(rootLeaf);
        }

        TECHNICAL_LOGS.info("{}", rootLeaf);
        RaoLogger.logMostLimitingElementsResults(TECHNICAL_LOGS, rootLeaf, parameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_DURING_TREE);

        TECHNICAL_LOGS.info("Linear optimization on root leaf");
        optimizeLeaf(rootLeaf);

        topLevelLogger.info("{}", rootLeaf);
        RaoLogger.logRangeActions(TECHNICAL_LOGS, optimalLeaf, input.getOptimizationPerimeter());
        RaoLogger.logMostLimitingElementsResults(topLevelLogger, optimalLeaf, parameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_DURING_TREE);
        logVirtualCostInformation(rootLeaf, "");

        if (stopCriterionReached(rootLeaf)) {
            logOptimizationSummary(optimalLeaf);
            return CompletableFuture.completedFuture(rootLeaf);
        }

        iterateOnTree();

        TECHNICAL_LOGS.info("Search-tree RAO completed with status {}", optimalLeaf.getSensitivityStatus());
        TECHNICAL_LOGS.info("Best leaf: {}", optimalLeaf);
        RaoLogger.logRangeActions(TECHNICAL_LOGS, optimalLeaf, input.getOptimizationPerimeter(), "Best leaf: ");
        RaoLogger.logMostLimitingElementsResults(TECHNICAL_LOGS, optimalLeaf, parameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_END_TREE);

        logOptimizationSummary(optimalLeaf);
        return CompletableFuture.completedFuture(optimalLeaf);
    }

    void initLeaves(SearchTreeInput input) {
        rootLeaf = makeLeaf(input.getOptimizationPerimeter(), input.getNetwork(), input.getPrePerimeterResult(), input.getPreOptimizationAppliedNetworkActions());
        optimalLeaf = rootLeaf;
        previousDepthOptimalLeaf = rootLeaf;
    }

    Leaf makeLeaf(OptimizationPerimeter optimizationPerimeter, Network network, PrePerimeterResult prePerimeterOutput, AppliedRemedialActions appliedRemedialActionsInSecondaryStates) {
        return new Leaf(optimizationPerimeter, network, prePerimeterOutput, appliedRemedialActionsInSecondaryStates);
    }

    /**
     * Detects forced network actions and applies them on root leaf, re-evaluating the leaf if needed.
     */
    private void applyForcedNetworkActionsOnRootLeaf() {
        State optimizedState = input.getOptimizationPerimeter().getMainOptimizationState();
        // Fetch available network actions, then apply those that should be forced
        Set<NetworkAction> forcedNetworkActions = input.getOptimizationPerimeter().getNetworkActions().stream()
            .filter(ra -> ra.getUsageRules().stream().anyMatch(usageRule -> usageRule.getUsageMethod(optimizedState).equals(UsageMethod.FORCED)))
            .collect(Collectors.toSet());
        if (!forcedNetworkActions.isEmpty()) {
            TECHNICAL_LOGS.info("{} network actions were forced on the network. The root leaf will be re-evaluated.", forcedNetworkActions.size());
            forcedNetworkActions.forEach(ra -> TECHNICAL_LOGS.debug("Network action {} is available and forced. It will be applied on the root leaf.", ra.getId()));
            input.getOptimizationPerimeter().getNetworkActions().removeAll(forcedNetworkActions);
            rootLeaf = new Leaf(input.getOptimizationPerimeter(),
                input.getNetwork(),
                forcedNetworkActions,
                null,
                input.getPrePerimeterResult(),
                input.getPreOptimizationAppliedNetworkActions());
            optimalLeaf = rootLeaf;
            previousDepthOptimalLeaf = rootLeaf;
        }
    }

    private void logOptimizationSummary(Leaf leaf) {
        RaoLogger.logOptimizationSummary(BUSINESS_LOGS, input.getOptimizationPerimeter().getMainOptimizationState(), leaf.getActivatedNetworkActions().size(), getNumberOfActivatedRangeActions(leaf), preOptimFunctionalCost, preOptimVirtualCost, leaf);
        logVirtualCostInformation(leaf, "");
    }

    private long getNumberOfActivatedRangeActions(Leaf leaf) {
        return leaf.getNumberOfActivatedRangeActions();
    }

    private void iterateOnTree() {
        int depth = 0;
        boolean hasImproved = true;
        if (input.getOptimizationPerimeter().getNetworkActions().isEmpty()) {
            topLevelLogger.info("No network action available");
            return;
        }

        int leavesInParallel = Math.min(input.getOptimizationPerimeter().getNetworkActions().size(), parameters.getTreeParameters().getLeavesInParallel());
        TECHNICAL_LOGS.debug("Evaluating {} leaves in parallel", leavesInParallel);
        try (AbstractNetworkPool networkPool = makeFaraoNetworkPool(input.getNetwork(), leavesInParallel)) {
            while (depth < parameters.getTreeParameters().getMaximumSearchDepth() && hasImproved && !stopCriterionReached(optimalLeaf)) {
                TECHNICAL_LOGS.info("Search depth {} [start]", depth + 1);
                previousDepthOptimalLeaf = optimalLeaf;
                updateOptimalLeafWithNextDepthBestLeaf(networkPool);
                hasImproved = previousDepthOptimalLeaf != optimalLeaf; // It means this depth evaluation has improved the global cost
                if (hasImproved) {
                    TECHNICAL_LOGS.info("Search depth {} [end]", depth + 1);
                    topLevelLogger.info("Search depth {} best leaf: {}", depth + 1, optimalLeaf);
                    RaoLogger.logRangeActions(TECHNICAL_LOGS, optimalLeaf, input.getOptimizationPerimeter(), String.format("Search depth %s best leaf: ", depth + 1));
                    RaoLogger.logMostLimitingElementsResults(topLevelLogger, optimalLeaf, parameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_DURING_TREE);
                } else {
                    topLevelLogger.info("No better result found in search depth {}, exiting search tree", depth + 1);
                }
                depth += 1;
                if (depth >= parameters.getTreeParameters().getMaximumSearchDepth()) {
                    topLevelLogger.info("maximum search depth has been reached, exiting search tree");
                }
            }
            networkPool.shutdownAndAwaitTermination(24, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            TECHNICAL_LOGS.warn("A computation thread was interrupted");
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Evaluate all the leaves. We use FaraoNetworkPool to parallelize the computation
     */
    private void updateOptimalLeafWithNextDepthBestLeaf(AbstractNetworkPool networkPool) throws InterruptedException {

        final List<NetworkActionCombination> naCombinations = bloomer.bloom(optimalLeaf, input.getOptimizationPerimeter().getNetworkActions());
        naCombinations.sort(this::deterministicNetworkActionCombinationComparison);
        if (naCombinations.isEmpty()) {
            TECHNICAL_LOGS.info("No more network action available");
            return;
        } else {
            TECHNICAL_LOGS.info("Leaves to evaluate: {}", naCombinations.size());
        }
        AtomicInteger remainingLeaves = new AtomicInteger(naCombinations.size());
        CountDownLatch latch = new CountDownLatch(naCombinations.size());
        naCombinations.forEach(naCombination ->
            networkPool.submit(() -> {
                Network networkClone;
                try {
                    networkClone = networkPool.getAvailableNetwork(); //This is where the threads actually wait for available networks
                } catch (InterruptedException e) {
                    latch.countDown();
                    Thread.currentThread().interrupt();
                    throw new FaraoException(e);
                }
                try {
                    if (combinationFulfillingStopCriterion.isEmpty() || deterministicNetworkActionCombinationComparison(naCombination, combinationFulfillingStopCriterion.get()) < 0) {
                        // Reset range action to their pre-perimeter set-points
                        previousDepthOptimalLeaf.getRangeActions().forEach(ra ->
                            ra.apply(networkClone, input.getPrePerimeterResult().getRangeActionSetpointResult().getSetpoint(ra))
                        );

                        optimizeNextLeafAndUpdate(naCombination, networkClone);
                    } else {
                        topLevelLogger.info("Skipping {} optimization because earlier combination fulfills stop criterion.", naCombination.getConcatenatedId());
                    }
                } catch (Exception e) {
                    BUSINESS_WARNS.warn("Cannot optimize remedial action combination {}: {}", naCombination.getConcatenatedId(), e.getMessage());
                }
                TECHNICAL_LOGS.info("Remaining leaves to evaluate: {}", remainingLeaves.decrementAndGet());
                try {
                    networkPool.releaseUsedNetwork(networkClone);
                    latch.countDown();
                } catch (InterruptedException ex) {
                    latch.countDown();
                    Thread.currentThread().interrupt();
                    throw new FaraoException(ex);
                }
            })
        );
        // TODO : change the 24 hours to something more useful when a target end time is known by the RAO
        boolean success = latch.await(24, TimeUnit.HOURS);
        if (!success) {
            throw new FaraoException("At least one network action combination could not be evaluated within the given time (24 hours). This should not happen.");
        }
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

    private String printNetworkActions(Set<NetworkAction> networkActions) {
        return networkActions.stream().map(NetworkAction::getId).collect(Collectors.joining(" + "));
    }

    AbstractNetworkPool makeFaraoNetworkPool(Network network, int leavesInParallel) {
        return AbstractNetworkPool.create(network, network.getVariantManager().getWorkingVariantId(), leavesInParallel);
    }

    void optimizeNextLeafAndUpdate(NetworkActionCombination naCombination, Network network) {
        Leaf leaf;
        try {
            // We get initial range action results from the previous optimal leaf
            leaf = createChildLeaf(network, naCombination);
        } catch (FaraoException e) {
            Set<NetworkAction> networkActions = new HashSet<>(previousDepthOptimalLeaf.getActivatedNetworkActions());
            networkActions.addAll(naCombination.getNetworkActionSet());
            topLevelLogger.info("Could not evaluate network action combination \"{}\": {}", printNetworkActions(networkActions), e.getMessage());
            return;
        } catch (NotImplementedException e) {
            throw e;
        }
        // We evaluate the leaf with taking the results of the previous optimal leaf if we do not want to update some results
        leaf.evaluate(input.getObjectiveFunction(), getSensitivityComputerForEvaluation());
        TECHNICAL_LOGS.debug("Evaluated {}", leaf);
        if (!leaf.getStatus().equals(Leaf.Status.ERROR)) {
            if (!stopCriterionReached(leaf)) {
                if (combinationFulfillingStopCriterion.isPresent() && deterministicNetworkActionCombinationComparison(naCombination, combinationFulfillingStopCriterion.get()) > 0) {
                    topLevelLogger.info("Skipping {} optimization because earlier combination fulfills stop criterion.", naCombination.getConcatenatedId());
                } else {
                    optimizeLeaf(leaf);
                    topLevelLogger.info("Optimized {}", leaf);
                    logVirtualCostInformation(leaf, "Optimized ");
                }
            } else {
                topLevelLogger.info("Evaluated {}", leaf);
            }
            updateOptimalLeaf(leaf, naCombination);
        } else {
            topLevelLogger.info("Could not evaluate leaf: {}", leaf);
        }
    }

    Leaf createChildLeaf(Network network, NetworkActionCombination naCombination) {
        return new Leaf(
            input.getOptimizationPerimeter(),
            network,
            previousDepthOptimalLeaf.getActivatedNetworkActions(),
            naCombination,
            input.getPrePerimeterResult(),
            input.getPreOptimizationAppliedNetworkActions());
    }

    private void optimizeLeaf(Leaf leaf) {
        if (!input.getOptimizationPerimeter().getRangeActions().isEmpty()) {
            leaf.optimize(input, parameters);
            if (!leaf.getStatus().equals(Leaf.Status.OPTIMIZED)) {
                topLevelLogger.info("Failed to optimize leaf: {}", leaf);
            }
        } else {
            TECHNICAL_LOGS.info("No range actions to optimize");
        }
        leaf.finalizeOptimization();
    }

    private SensitivityComputer getSensitivityComputerForEvaluation() {

        SensitivityComputer.SensitivityComputerBuilder sensitivityComputerBuilder = SensitivityComputer.create()
            .withToolProvider(input.getToolProvider())
            .withCnecs(input.getOptimizationPerimeter().getFlowCnecs())
            .withRangeActions(input.getOptimizationPerimeter().getRangeActions());

        sensitivityComputerBuilder.withAppliedRemedialActions(input.getPreOptimizationAppliedNetworkActions());

        if (parameters.getObjectiveFunction().relativePositiveMargins()) {
            sensitivityComputerBuilder.withPtdfsResults(input.getInitialFlowResult());
        }

        if (parameters.getLoopFlowParameters() != null && parameters.getLoopFlowParameters().getLoopFlowApproximationLevel().shouldUpdatePtdfWithTopologicalChange()) {
            sensitivityComputerBuilder.withCommercialFlowsResults(input.getToolProvider().getLoopFlowComputation(), input.getOptimizationPerimeter().getLoopFlowCnecs());
        } else if (parameters.getLoopFlowParameters() != null) {
            sensitivityComputerBuilder.withCommercialFlowsResults(input.getInitialFlowResult());
        }

        return sensitivityComputerBuilder.build();
    }

    private synchronized void updateOptimalLeaf(Leaf leaf, NetworkActionCombination networkActionCombination) {
        if (improvedEnough(leaf)) {
            // nominal case: stop criterion hasn't been reached yet
            if (combinationFulfillingStopCriterion.isEmpty() && leaf.getCost() < optimalLeaf.getCost()) {
                optimalLeaf = leaf;
                if (stopCriterionReached(leaf)) {
                    TECHNICAL_LOGS.info("Stop criterion reached, other threads may skip optimization.");
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
     * @param leaf: Leaf to evaluate.
     * @return True if the stop criterion has been reached on this leaf.
     */
    private boolean stopCriterionReached(Leaf leaf) {
        if (leaf.getVirtualCost() > 1e-6) {
            return false;
        }
        if (purelyVirtual && leaf.getVirtualCost() < 1e-6) {
            TECHNICAL_LOGS.debug("Perimeter is purely virtual and virtual cost is zero. Exiting search tree.");
            return true;
        }
        return costSatisfiesStopCriterion(leaf.getCost());
    }

    /**
     * Returns true if a given cost value satisfies the stop criterion
     */
    boolean costSatisfiesStopCriterion(double cost) {
        if (parameters.getTreeParameters().getStopCriterion().equals(TreeParameters.StopCriterion.MIN_OBJECTIVE)) {
            return false;
        } else if (parameters.getTreeParameters().getStopCriterion().equals(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE)) {
            return cost < parameters.getTreeParameters().getTargetObjectiveValue();
        } else {
            throw new FaraoException("Unexpected stop criterion: " + parameters.getTreeParameters().getStopCriterion());
        }
    }

    /**
     * This method checks if the leaf's cost respects the minimum impact thresholds
     * (absolute and relative) compared to the previous depth's optimal leaf.
     *
     * @param leaf: Leaf that has to be compared with the optimal leaf.
     * @return True if the leaf cost diminution is enough compared to optimal leaf.
     */
    private boolean improvedEnough(Leaf leaf) {
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

    /**
     * This method logs information about positive virtual costs
     */
    private void logVirtualCostInformation(Leaf leaf, String prefix) {
        leaf.getVirtualCostNames().stream()
                .filter(virtualCostName -> leaf.getVirtualCost(virtualCostName) > 1e-6)
                .forEach(virtualCostName -> logVirtualCostDetails(leaf, virtualCostName, prefix));
    }

    /**
     * If stop criterion could have been reached without the given virtual cost, this method logs a message, in order
     * to inform the user that the given network action was rejected because of a virtual cost
     * (message is not logged if it has already been logged at previous depth)
     * In all cases, this method also logs most costly elements for given virtual cost
     */
    void logVirtualCostDetails(Leaf leaf, String virtualCostName, String prefix) {
        FaraoLogger logger = topLevelLogger;
        if (!costSatisfiesStopCriterion(leaf.getCost())
                && costSatisfiesStopCriterion(leaf.getCost() - leaf.getVirtualCost(virtualCostName))
                && (leaf.isRoot() || !costSatisfiesStopCriterion(previousDepthOptimalLeaf.getFunctionalCost()))) {
            // Stop criterion would have been reached without virtual cost, for the first time at this depth
            // and for the given leaf
            BUSINESS_LOGS.info("{}{}, stop criterion could have been reached without \"{}\" virtual cost", prefix, leaf.getIdentifier(), virtualCostName);
            // Promote detailed logs about costly elements to BUSINESS_LOGS
            logger = BUSINESS_LOGS;
        }
        getVirtualCostlyElementsLogs(leaf, virtualCostName, prefix).forEach(logger::info);
    }

    List<String> getVirtualCostlyElementsLogs(Leaf leaf, String virtualCostName, String prefix) {
        Unit unit = parameters.getObjectiveFunction().getUnit();
        List<String> logs = new ArrayList<>();
        int i = 1;
        for (FlowCnec flowCnec : leaf.getCostlyElements(virtualCostName, NUMBER_LOGGED_VIRTUAL_COSTLY_ELEMENTS)) {
            Side limitingSide = leaf.getMargin(flowCnec, Side.LEFT, unit) < leaf.getMargin(flowCnec, Side.RIGHT, unit) ? Side.LEFT : Side.RIGHT;
            double flow = leaf.getFlow(flowCnec, limitingSide, unit);
            Double limitingThreshold = flow >= 0 ? flowCnec.getUpperBound(limitingSide, unit).orElse(flowCnec.getLowerBound(limitingSide, unit).orElse(Double.NaN))
                    : flowCnec.getLowerBound(limitingSide, unit).orElse(flowCnec.getUpperBound(limitingSide, unit).orElse(Double.NaN));
            logs.add(String.format(Locale.ENGLISH,
                    "%s%s, limiting \"%s\" constraint #%02d: flow = %.2f %s, threshold = %.2f %s, margin = %.2f %s, element %s at state %s, CNEC ID = \"%s\", CNEC name = \"%s\"",
                    prefix,
                    leaf.getIdentifier(),
                    virtualCostName,
                    i,
                    flow, unit,
                    limitingThreshold, unit,
                    leaf.getMargin(flowCnec, limitingSide, unit), unit,
                    flowCnec.getNetworkElement().getId(), flowCnec.getState().getId(),
                    flowCnec.getId(), flowCnec.getName()));
            i++;
        }
        return logs;
    }
}
