/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.commons.logs.FaraoLogger;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.OnFlowConstraint;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.rao_api.parameters.LinearOptimizerParameters;
import com.farao_community.farao.rao_commons.SensitivityComputer;
import com.farao_community.farao.rao_commons.linear_optimisation.IteratingLinearOptimizer;
import com.farao_community.farao.rao_commons.objective_function_evaluator.ObjectiveFunction;
import com.farao_community.farao.rao_commons.result_api.FlowResult;
import com.farao_community.farao.rao_commons.result_api.OptimizationResult;
import com.farao_community.farao.rao_commons.result_api.PrePerimeterResult;
import com.farao_community.farao.util.AbstractNetworkPool;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.NotImplementedException;

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
    private FaraoLogger topLevelLogger;
    private static final int NUMBER_LOGGED_ELEMENTS_DURING_TREE = 2;
    private static final int NUMBER_LOGGED_ELEMENTS_END_TREE = 5;

    private Network network;
    private State optimizedState;
    private Set<NetworkAction> availableNetworkActions;
    private Set<RangeAction<?>> availableRangeActions;
    private PrePerimeterResult prePerimeterOutput;
    private double preOptimFunctionalCost;
    private double preOptimVirtualCost;
    private SearchTreeComputer searchTreeComputer;
    private SearchTreeProblem searchTreeProblem;
    private SearchTreeBloomer bloomer;
    private ObjectiveFunction objectiveFunction;
    private IteratingLinearOptimizer iteratingLinearOptimizer;
    private boolean purelyVirtual = false;

    private Map<RangeAction<?>, Double> prePerimeterRangeActionSetPoints;

    private Leaf rootLeaf;
    private Leaf optimalLeaf;
    private Leaf previousDepthOptimalLeaf;
    private TreeParameters treeParameters;
    private LinearOptimizerParameters linearOptimizerParameters;

    void initLeaves() {
        rootLeaf = makeLeaf(network, prePerimeterOutput);
        optimalLeaf = rootLeaf;
        previousDepthOptimalLeaf = rootLeaf;
    }

    Leaf makeLeaf(Network network, PrePerimeterResult prePerimeterOutput) {
        return new Leaf(network, prePerimeterOutput);
    }

    void setTreeParameters(TreeParameters parameters) {
        treeParameters = parameters;
    }

    void setAvailableRangeActions(Set<RangeAction<?>> rangeActions) {
        availableRangeActions = rangeActions;
    }

    void setPrePerimeterRangeActionSetPoints(Map<RangeAction<?>, Double> prePerimeterRangeActionSetPoints) {
        // TODO : try to remove this method by finding another way in unit tests
        this.prePerimeterRangeActionSetPoints = prePerimeterRangeActionSetPoints;
    }

    /**
     * If the allowed number of range actions (or RAs) is limited (by tso or globally), this function filters out
     * the range actions with the least impact
     */
    Set<RangeAction<?>> applyRangeActionsFilters(Leaf leaf, Set<RangeAction<?>> fromRangeActions, boolean deprioritizeIgnoredRangeActions) {
        RangeActionFilter filter = new RangeActionFilter(leaf, fromRangeActions, optimizedState, treeParameters, prePerimeterRangeActionSetPoints, deprioritizeIgnoredRangeActions);
        filter.filterUnavailableRangeActions();
        filter.filterPstPerTso();
        filter.filterTsos();
        filter.filterMaxRas();
        return filter.getRangeActionsToOptimize();
    }

    public CompletableFuture<OptimizationResult> run(SearchTreeInput searchTreeInput, TreeParameters treeParameters, LinearOptimizerParameters linearOptimizerParameters, boolean verbose) {
        this.network = searchTreeInput.getNetwork();
        this.optimizedState = searchTreeInput.getOptimizedState();
        this.availableNetworkActions = searchTreeInput.getNetworkActions();
        this.availableRangeActions = searchTreeInput.getRangeActions();
        this.prePerimeterOutput = searchTreeInput.getPrePerimeterOutput();
        this.searchTreeComputer = searchTreeInput.getSearchTreeComputer();
        this.searchTreeProblem = searchTreeInput.getSearchTreeProblem();
        this.bloomer = searchTreeInput.getSearchTreeBloomer();
        this.objectiveFunction = searchTreeInput.getObjectiveFunction();
        this.iteratingLinearOptimizer = searchTreeInput.getIteratingLinearOptimizer();
        setTreeParameters(treeParameters);
        this.linearOptimizerParameters = linearOptimizerParameters;
        this.topLevelLogger = verbose ? BUSINESS_LOGS : TECHNICAL_LOGS;
        this.purelyVirtual = searchTreeInput.getFlowCnecs().stream().noneMatch(FlowCnec::isOptimized);
        initLeaves();

        this.prePerimeterRangeActionSetPoints = new HashMap<>();
        rootLeaf.getRangeActions().stream().forEach(rangeAction -> prePerimeterRangeActionSetPoints.put(rangeAction, prePerimeterOutput.getOptimizedSetPoint(rangeAction)));

        TECHNICAL_LOGS.info("Evaluating root leaf");
        rootLeaf.evaluate(objectiveFunction, getSensitivityComputerForEvaluationBasedOn(prePerimeterOutput, availableRangeActions));
        this.preOptimFunctionalCost = rootLeaf.getFunctionalCost();
        this.preOptimVirtualCost = rootLeaf.getVirtualCost();

        if (rootLeaf.getStatus().equals(Leaf.Status.ERROR)) {
            topLevelLogger.info("Could not evaluate leaf: {}", rootLeaf);
            logOptimizationSummary(rootLeaf);
            return CompletableFuture.completedFuture(rootLeaf);
        } else if (stopCriterionReached(rootLeaf)) {
            topLevelLogger.info("Stop criterion reached on {}", rootLeaf);
            SearchTreeRaoLogger.logMostLimitingElementsResults(topLevelLogger, rootLeaf, linearOptimizerParameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_END_TREE);
            logOptimizationSummary(rootLeaf);
            return CompletableFuture.completedFuture(rootLeaf);
        }

        TECHNICAL_LOGS.info("{}", rootLeaf);
        SearchTreeRaoLogger.logMostLimitingElementsResults(TECHNICAL_LOGS, rootLeaf, linearOptimizerParameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_DURING_TREE);

        TECHNICAL_LOGS.info("Linear optimization on root leaf");
        optimizeLeaf(rootLeaf, prePerimeterOutput);

        topLevelLogger.info("{}", rootLeaf);
        SearchTreeRaoLogger.logRangeActions(TECHNICAL_LOGS, optimalLeaf, availableRangeActions);
        SearchTreeRaoLogger.logMostLimitingElementsResults(topLevelLogger, optimalLeaf, linearOptimizerParameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_DURING_TREE);

        if (stopCriterionReached(rootLeaf)) {
            return CompletableFuture.completedFuture(rootLeaf);
        }

        iterateOnTree();

        TECHNICAL_LOGS.info("Search-tree RAO completed with status {}", optimalLeaf.getSensitivityStatus());
        TECHNICAL_LOGS.info("Best leaf: {}", optimalLeaf);
        SearchTreeRaoLogger.logRangeActions(TECHNICAL_LOGS, optimalLeaf, availableRangeActions, "Best leaf: ");
        SearchTreeRaoLogger.logMostLimitingElementsResults(TECHNICAL_LOGS, optimalLeaf, linearOptimizerParameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_END_TREE);

        logOptimizationSummary(optimalLeaf);
        return CompletableFuture.completedFuture(optimalLeaf);
    }

    private void logOptimizationSummary(Leaf leaf) {
        SearchTreeRaoLogger.logOptimizationSummary(BUSINESS_LOGS, optimizedState, leaf.getActivatedNetworkActions().size(), getNumberOfActivatedRangeActions(leaf), preOptimFunctionalCost, preOptimVirtualCost, leaf);
    }

    private long getNumberOfActivatedRangeActions(Leaf leaf) {
        return leaf.getOptimizedSetPoints().entrySet().stream().filter(entry ->
            Math.abs(entry.getValue() - prePerimeterRangeActionSetPoints.get(entry.getKey())) > 1e-6
        ).count();
    }

    private void iterateOnTree() {
        int depth = 0;
        boolean hasImproved = true;
        int leavesInParallel = Math.min(availableNetworkActions.size(), treeParameters.getLeavesInParallel());
        if (availableNetworkActions.isEmpty()) {
            topLevelLogger.info("No network action available");
            return;
        }
        TECHNICAL_LOGS.debug("Evaluating {} leaves in parallel", leavesInParallel);
        try (AbstractNetworkPool networkPool = makeFaraoNetworkPool(network, leavesInParallel)) {
            while (depth < treeParameters.getMaximumSearchDepth() && hasImproved && !stopCriterionReached(optimalLeaf)) {
                TECHNICAL_LOGS.info("Search depth {} [start]", depth + 1);
                previousDepthOptimalLeaf = optimalLeaf;
                updateOptimalLeafWithNextDepthBestLeaf(networkPool);
                hasImproved = previousDepthOptimalLeaf != optimalLeaf; // It means this depth evaluation has improved the global cost
                if (hasImproved) {
                    TECHNICAL_LOGS.info("Search depth {} [end]", depth + 1);
                    topLevelLogger.info("Search depth {} best leaf: {}", depth + 1, optimalLeaf);
                    SearchTreeRaoLogger.logRangeActions(TECHNICAL_LOGS, optimalLeaf, availableRangeActions, String.format("Search depth %s best leaf: ", depth + 1));
                    SearchTreeRaoLogger.logMostLimitingElementsResults(topLevelLogger, optimalLeaf, linearOptimizerParameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_DURING_TREE);
                } else {
                    topLevelLogger.info("No better result found in search depth {}, exiting search tree", depth + 1);
                }
                depth += 1;
                if (depth >= treeParameters.getMaximumSearchDepth()) {
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
        // Recompute the list of available network actions with last margin results
        Set<NetworkAction> availableActionsOnNewMargins = availableNetworkActions.stream().filter(na -> isRemedialActionAvailable(na, optimizedState, optimalLeaf)).collect(Collectors.toSet());
        // Bloom
        final List<NetworkActionCombination> naCombinations = bloomer.bloom(optimalLeaf, availableActionsOnNewMargins);
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
                try {
                    Network networkClone = networkPool.getAvailableNetwork();
                    // Apply range actions that has been changed by the previous leaf on the network to start next depth leaves
                    // from previous optimal leaf starting point
                    // TODO: we can wonder if it's better to do this here or at creation of each leaves or at each evaluation/optimization
                    previousDepthOptimalLeaf.getRangeActions()
                        .forEach(ra -> ra.apply(networkClone, previousDepthOptimalLeaf.getOptimizedSetPoint(ra)));
                    optimizeNextLeafAndUpdate(naCombination, networkClone, networkPool);
                    networkPool.releaseUsedNetwork(networkClone);
                    TECHNICAL_LOGS.info("Remaining leaves to evaluate: {}", remainingLeaves.decrementAndGet());
                } catch (Exception e) {
                    BUSINESS_WARNS.warn("Cannot apply remedial action combination {}: {}", naCombination.getConcatenatedId(), e.getMessage());
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            })
        );
        // TODO : change the 24 hours to something more useful when a target end time is known by the RAO
        boolean success = latch.await(24, TimeUnit.HOURS);
        if (!success) {
            throw new FaraoException("At least one network action combination could not be evaluated within the given time (24 hours). This should not happen.");
        }
    }

    private String printNetworkActions(Set<NetworkAction> networkActions) {
        return networkActions.stream().map(NetworkAction::getId).collect(Collectors.joining(" + "));
    }

    AbstractNetworkPool makeFaraoNetworkPool(Network network, int leavesInParallel) {
        return AbstractNetworkPool.create(network, network.getVariantManager().getWorkingVariantId(), leavesInParallel);
    }

    void optimizeNextLeafAndUpdate(NetworkActionCombination naCombination, Network network, AbstractNetworkPool networkPool) throws InterruptedException {
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
            networkPool.releaseUsedNetwork(network);
            throw e;
        }
        // We evaluate the leaf with taking the results of the previous optimal leaf if we do not want to update some results
        leaf.evaluate(objectiveFunction, getSensitivityComputerForEvaluationBasedOn(previousDepthOptimalLeaf, availableRangeActions));
        TECHNICAL_LOGS.debug("Evaluated {}", leaf);
        if (!leaf.getStatus().equals(Leaf.Status.ERROR)) {
            if (!stopCriterionReached(leaf)) {
                // We base the results on the results of the evaluation of the leaf in case something has been updated
                optimizeLeaf(leaf, leaf.getPreOptimBranchResult());
                topLevelLogger.info("Optimized {}", leaf);
            }
            updateOptimalLeaf(leaf);
        } else {
            topLevelLogger.info("Could not evaluate leaf: {}", leaf);
        }
    }

    Leaf createChildLeaf(Network network, NetworkActionCombination naCombination) {
        return new Leaf(network, previousDepthOptimalLeaf.getActivatedNetworkActions(), naCombination, previousDepthOptimalLeaf);
    }

    private void optimizeLeaf(Leaf leaf, FlowResult baseFlowResult) {
        int iteration = 0;
        double previousCost = Double.MAX_VALUE;
        Set<RangeAction<?>> previousIterationRangeActions = null;
        Set<RangeAction<?>> rangeActions = applyRangeActionsFilters(leaf, availableRangeActions, false);
        // Iterate on optimizer until the list of range actions stops changing
        while (!rangeActions.equals(previousIterationRangeActions) && Math.abs(previousCost - leaf.getCost()) >= 1e-6 && iteration < 10) {
            previousCost = leaf.getCost();
            iteration++;
            if (iteration > 1) {
                TECHNICAL_LOGS.info("{}", leaf);
                TECHNICAL_LOGS.debug("The list of available range actions has changed, the leaf will be optimized again (iteration {})", iteration);
            }
            if (!rangeActions.isEmpty()) {
                leaf.optimize(
                    iteratingLinearOptimizer,
                    getSensitivityComputerForOptimizationBasedOn(baseFlowResult, rangeActions),
                    searchTreeProblem.getLeafProblem(rangeActions)
                );
                if (!leaf.getStatus().equals(Leaf.Status.OPTIMIZED)) {
                    topLevelLogger.info("Failed to optimize leaf: {}", leaf);
                }
                // Check if result is worse than before (even though it should not happen). If it is, go back to
                // previous result. The only way to do this is to re-run a linear optimization
                // TODO : check if this actually happens. If it never does, delete this extra LP
                if (leaf.getCost() > previousCost) {
                    BUSINESS_WARNS.warn("The new iteration found a worse result (abnormal). The leaf will be optimized again with the previous list of range actions.");
                    leaf.optimize(
                        iteratingLinearOptimizer,
                        getSensitivityComputerForOptimizationBasedOn(baseFlowResult, previousIterationRangeActions),
                        searchTreeProblem.getLeafProblem(rangeActions)
                    );
                    if (!leaf.getStatus().equals(Leaf.Status.OPTIMIZED)) {
                        topLevelLogger.info("Failed to optimize leaf: {}", leaf);
                    }
                    break;
                }
            } else {
                TECHNICAL_LOGS.info("No range actions to optimize");
            }
            previousIterationRangeActions = rangeActions;
            rangeActions = applyRangeActionsFilters(leaf, availableRangeActions, true);
        }
        leaf.finalizeOptimization();
    }

    private SensitivityComputer getSensitivityComputerForEvaluationBasedOn(FlowResult flowResult, Set<RangeAction<?>> rangeActions) {
        if (linearOptimizerParameters.isRaoWithLoopFlowLimitation()) {
            if (linearOptimizerParameters.getLoopFlowParameters().getLoopFlowApproximationLevel().shouldUpdatePtdfWithTopologicalChange()) {
                return searchTreeComputer.getSensitivityComputerWithComputedCommercialFlows(rangeActions);
            } else {
                return searchTreeComputer.getSensitivityComputerWithFixedCommercialFlows(flowResult, rangeActions);
            }
        } else {
            return searchTreeComputer.getSensitivityComputer(rangeActions);
        }
    }

    private SensitivityComputer getSensitivityComputerForOptimizationBasedOn(FlowResult flowResult, Set<RangeAction<?>> rangeActions) {
        if (linearOptimizerParameters.isRaoWithLoopFlowLimitation()) {
            if (linearOptimizerParameters.getLoopFlowParameters().getLoopFlowApproximationLevel().shouldUpdatePtdfWithPstChange()) {
                return searchTreeComputer.getSensitivityComputerWithComputedCommercialFlows(rangeActions);
            } else {
                return searchTreeComputer.getSensitivityComputerWithFixedCommercialFlows(flowResult, rangeActions);
            }
        } else {
            return searchTreeComputer.getSensitivityComputer(rangeActions);
        }
    }

    private synchronized void updateOptimalLeaf(Leaf leaf) {
        if (improvedEnough(leaf)) {
            optimalLeaf = leaf;
        }
    }

    /**
     * This method evaluates stop criterion on the leaf.
     *
     * @param leaf: Leaf to evaluate.
     * @return True if the stop criterion has been reached on this leaf.
     */
    private boolean stopCriterionReached(Leaf leaf) {
        if (purelyVirtual && leaf.getVirtualCost() < 1e-6) {
            TECHNICAL_LOGS.debug("Perimeter is purely virtual and virtual cost is zero. Exiting search tree.");
            return true;
        }
        if (treeParameters.getStopCriterion().equals(TreeParameters.StopCriterion.MIN_OBJECTIVE)) {
            return false;
        } else if (treeParameters.getStopCriterion().equals(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE)) {
            return leaf.getCost() < treeParameters.getTargetObjectiveValue();
        } else {
            throw new FaraoException("Unexpected stop criterion: " + treeParameters.getStopCriterion());
        }
    }

    /**
     * This method compares the leaf best cost to the optimal leaf best cost taking into account the minimum impact
     * thresholds (absolute and relative)
     *
     * @param leaf: Leaf that has to be compared with the optimal leaf.
     * @return True if the leaf cost diminution is enough compared to optimal leaf.
     */
    private boolean improvedEnough(Leaf leaf) {
        double relativeImpact = Math.max(treeParameters.getRelativeNetworkActionMinimumImpactThreshold(), 0);
        double absoluteImpact = Math.max(treeParameters.getAbsoluteNetworkActionMinimumImpactThreshold(), 0);

        double currentBestCost = optimalLeaf.getCost();
        double previousDepthBestCost = previousDepthOptimalLeaf.getCost();
        double newCost = leaf.getCost();

        return newCost < currentBestCost
            && previousDepthBestCost - absoluteImpact > newCost // enough absolute impact
            && (1 - Math.signum(previousDepthBestCost) * relativeImpact) * previousDepthBestCost > newCost; // enough relative impact
    }

    /**
     * Returns true if a remedial action is available depending on its usage rules
     * If it has a OnFlowConstraint usage rule, then the margins are needed
     */
    static boolean isRemedialActionAvailable(RemedialAction<?> remedialAction, State optimizedState, FlowResult flowResult) {
        switch (remedialAction.getUsageMethod(optimizedState)) {
            case AVAILABLE:
                return true;
            case TO_BE_EVALUATED:
                return remedialAction.getUsageRules().stream()
                    .anyMatch(usageRule -> (usageRule instanceof OnFlowConstraint)
                        && isOnFlowConstraintAvailable((OnFlowConstraint) usageRule, optimizedState, flowResult));
            default:
                return false;
        }
    }

    /**
     * Returns true if a OnFlowConstraint usage rule is verified, ie if the associated CNEC has a negative margin
     * It needs a FlowResult to get the margin of the flow cnec
     */
    static boolean isOnFlowConstraintAvailable(OnFlowConstraint onFlowConstraint, State optimizedState, FlowResult flowResult) {
        if (!onFlowConstraint.getUsageMethod(optimizedState).equals(UsageMethod.TO_BE_EVALUATED)) {
            return false;
        } else {
            // We don't actually need to know the unit of the objective function, we just need to know if the margin is negative
            return flowResult.getMargin(onFlowConstraint.getFlowCnec(), Unit.MEGAWATT) <= 0;
        }
    }
}
