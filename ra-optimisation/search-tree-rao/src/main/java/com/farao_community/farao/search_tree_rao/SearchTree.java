/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.data.crac_api.PstRangeAction;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_api.results.*;
import com.farao_community.farao.rao_commons.SensitivityComputer;
import com.farao_community.farao.rao_commons.linear_optimisation.IteratingLinearOptimizer;
import com.farao_community.farao.rao_api.parameters.LinearOptimizerParameters;
import com.farao_community.farao.rao_commons.objective_function_evaluator.ObjectiveFunction;
import com.farao_community.farao.util.FaraoNetworkPool;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
    static final Logger LOGGER = LoggerFactory.getLogger(SearchTree.class);
    private static final int NUMBER_LOGGED_ELEMENTS_DURING_TREE = 2;
    private static final int NUMBER_LOGGED_ELEMENTS_END_TREE = 5;

    private Network network;
    private Set<BranchCnec> cnecs;
    private Set<NetworkAction> availableNetworkActions;
    private Set<RangeAction> availableRangeActions;
    private PrePerimeterResult prePerimeterOutput;
    private SearchTreeComputer searchTreeComputer;
    private SearchTreeProblem searchTreeProblem;
    private SearchTreeBloomer bloomer;
    private ObjectiveFunction objectiveFunction;
    private IteratingLinearOptimizer iteratingLinearOptimizer;

    private Leaf rootLeaf;
    private Leaf optimalLeaf;
    private Leaf previousDepthOptimalLeaf;
    private TreeParameters treeParameters;
    private LinearOptimizerParameters linearOptimizerParameters;

    void initLeaves() {
        rootLeaf = new Leaf(network, prePerimeterOutput);
        optimalLeaf = rootLeaf;
        previousDepthOptimalLeaf = rootLeaf;
    }

    /**
     * If a TSO has a maximum number of usable ranges actions, this functions filters out the range actions with
     * the least impact on the most limiting element
     */
    Set<RangeAction> getRangeActionsToOptimize(Leaf leaf) {
        Map<String, Integer> maxPstPerTso = new HashMap<>(treeParameters.getMaxPstPerTso());
        treeParameters.getMaxRaPerTso().forEach((tso, raLimit) -> {
            int appliedNetworkActionsForTso = (int) leaf.getNetworkActions().stream().filter(networkAction -> networkAction.getOperator().equals(tso)).count();
            int pstLimit =  raLimit - appliedNetworkActionsForTso;
            maxPstPerTso.put(tso, Math.min(pstLimit, maxPstPerTso.getOrDefault(tso, Integer.MAX_VALUE)));
        });

        Set<RangeAction> rangeActionsToOptimize = new HashSet<>();
        if (!maxPstPerTso.isEmpty()) {
            maxPstPerTso.forEach((tso, maxPst) -> {
                Set<RangeAction> pstsForTso = availableRangeActions.stream()
                        .filter(rangeAction -> (rangeAction instanceof PstRangeAction) && rangeAction.getOperator().equals(tso))
                        .collect(Collectors.toSet());
                if (pstsForTso.size() > maxPst) {
                    LOGGER.debug("{} range actions will be filtered out, in order to respect the maximum number of range actions of {} for TSO {}", pstsForTso.size() - maxPst, maxPst, tso);
                    rangeActionsToOptimize.addAll(pstsForTso.stream()
                            .sorted((ra1, ra2) -> compareAbsoluteSensitivities(ra1, ra2, leaf.getMostLimitingElements(1).get(0), leaf))
                            .collect(Collectors.toList()).subList(pstsForTso.size() - maxPst, pstsForTso.size()));
                } else {
                    rangeActionsToOptimize.addAll(pstsForTso);
                }
            });
            return rangeActionsToOptimize;
        } else {
            return availableRangeActions;
        }
    }

    private static int compareAbsoluteSensitivities(RangeAction ra1, RangeAction ra2, BranchCnec cnec, SensitivityResult sensitivityResult) {
        Double sensi1 = Math.abs(sensitivityResult.getSensitivityValue(cnec, ra1, Unit.MEGAWATT));
        Double sensi2 = Math.abs(sensitivityResult.getSensitivityValue(cnec, ra2, Unit.MEGAWATT));
        return sensi1.compareTo(sensi2);
    }

    public CompletableFuture<OptimizationResult> run(SearchTreeInput searchTreeInput, TreeParameters treeParameters, LinearOptimizerParameters linearOptimizerParameters) {
        this.network = searchTreeInput.getNetwork();
        this.availableNetworkActions = searchTreeInput.getNetworkActions();
        this.availableRangeActions = searchTreeInput.getRangeActions();
        this.prePerimeterOutput = searchTreeInput.getPrePerimeterOutput();
        this.searchTreeComputer = searchTreeInput.getSearchTreeComputer();
        this.searchTreeProblem = searchTreeInput.getSearchTreeProblem();
        this.bloomer = searchTreeInput.getSearchTreeBloomer();
        this.objectiveFunction = searchTreeInput.getObjectiveFunction();
        this.iteratingLinearOptimizer = searchTreeInput.getIteratingLinearOptimizer();
        this.treeParameters = treeParameters;
        this.linearOptimizerParameters = linearOptimizerParameters;
        initLeaves();

        LOGGER.info("Evaluate root leaf");
        rootLeaf.evaluate(objectiveFunction, getSensitivityComputerForEvaluationBasedOn(prePerimeterOutput, availableRangeActions));
        LOGGER.info("{}", rootLeaf);
        if (rootLeaf.getSensitivityStatus().equals(Leaf.Status.ERROR)) {
            //TODO : improve error messages depending on leaf error (infeasible optimisation, time-out, ...)
            return CompletableFuture.completedFuture(rootLeaf);
        } else if (stopCriterionReached(rootLeaf)) {
            SearchTreeRaoLogger.logMostLimitingElementsResults(rootLeaf, linearOptimizerParameters.getUnit(),
                    linearOptimizerParameters.hasRelativeMargins(), NUMBER_LOGGED_ELEMENTS_END_TREE);
            return CompletableFuture.completedFuture(rootLeaf);
        } else {
            SearchTreeRaoLogger.logMostLimitingElementsResults(rootLeaf, linearOptimizerParameters.getUnit(),
                    linearOptimizerParameters.hasRelativeMargins(), NUMBER_LOGGED_ELEMENTS_DURING_TREE);
        }
        // todo: put somewhere else
        /*else if (linearOptimizerParameters.hasOperatorsNotToOptimize() && noCnecToOptimize(linearOptimizerParameters.getUnoptimizedCnecParameters().getOperatorsNotToOptimize())) {
            LOGGER.info("All CNECs belong to operators that are not being optimized. The search tree will stop.");
            SearchTreeRaoLogger.logMostLimitingElementsResults(rootLeaf, linearOptimizerParameters.getUnit(),
                    linearOptimizerParameters.hasRelativeMargins(), NUMBER_LOGGED_ELEMENTS_END_TREE);
            return CompletableFuture.completedFuture(rootLeaf);
        }*/

        LOGGER.info("Linear optimization on root leaf");
        optimizeLeaf(rootLeaf, prePerimeterOutput);
        LOGGER.info("{}", rootLeaf);
        SearchTreeRaoLogger.logRangeActions(optimalLeaf, availableRangeActions);
        SearchTreeRaoLogger.logMostLimitingElementsResults(optimalLeaf, linearOptimizerParameters.getUnit(),
                linearOptimizerParameters.hasRelativeMargins(), NUMBER_LOGGED_ELEMENTS_DURING_TREE);

        if (stopCriterionReached(rootLeaf)) {
            return CompletableFuture.completedFuture(rootLeaf);
        }

        iterateOnTree();

        LOGGER.info("Search-tree RAO completed with status {}", optimalLeaf.getSensitivityStatus());
        LOGGER.info("Best leaf - {}", optimalLeaf);
        SearchTreeRaoLogger.logRangeActions(optimalLeaf, availableRangeActions, "Best leaf");
        SearchTreeRaoLogger.logMostLimitingElementsResults(optimalLeaf, linearOptimizerParameters.getUnit(),
                linearOptimizerParameters.hasRelativeMargins(), NUMBER_LOGGED_ELEMENTS_END_TREE);
        return CompletableFuture.completedFuture(optimalLeaf);
    }

    /**
     * If all CNECs belong to operators not being optimized, then we can stop optimization after root leaf evaluation
     */
    //todo: put somewhere else, in the provider ?
    /*boolean noCnecToOptimize(Set<String> operatorsNotToOptimize) {
        if (Objects.isNull(operatorsNotToOptimize)) {
            return false;
        } else {
            return searchTreeInput.getCnecs().stream().noneMatch(cnec -> !operatorsNotToOptimize.contains(cnec.getOperator()));
        }
    }*/

    private void iterateOnTree() {
        int depth = 0;
        boolean hasImproved = true;
        while (depth < treeParameters.getMaximumSearchDepth() && hasImproved && !stopCriterionReached(optimalLeaf)) {
            LOGGER.info("Research depth: {} - [start]", depth + 1);
            previousDepthOptimalLeaf = optimalLeaf;
            updateOptimalLeafWithNextDepthBestLeaf();
            hasImproved = previousDepthOptimalLeaf != optimalLeaf; // It means this depth evaluation has improved the global cost
            if (hasImproved) {
                LOGGER.info("Research depth: {} - [end]", depth + 1);
                LOGGER.info("Best leaf so far - {}", optimalLeaf);
                SearchTreeRaoLogger.logRangeActions(optimalLeaf, availableRangeActions, "Best leaf so far");
                SearchTreeRaoLogger.logMostLimitingElementsResults(optimalLeaf, linearOptimizerParameters.getUnit(),
                        linearOptimizerParameters.hasRelativeMargins(), NUMBER_LOGGED_ELEMENTS_DURING_TREE);

            } else {
                LOGGER.info("End of search tree : no network action of depth {} improve the objective function", depth + 1);
            }
            depth += 1;
            if (depth >= treeParameters.getMaximumSearchDepth()) {
                LOGGER.info("End of search tree : maximum depth has been reached");
            }
        }
    }

    /**
     * Evaluate all the leaves. We use FaraoNetworkPool to parallelize the computation
     */
    private void updateOptimalLeafWithNextDepthBestLeaf() {
        final Set<NetworkAction> networkActions = bloomer.bloom(optimalLeaf, availableNetworkActions);
        if (networkActions.isEmpty()) {
            LOGGER.info("No more network action available");
            return;
        } else {
            LOGGER.info("Leaves to evaluate: {}", networkActions.size());
        }
        AtomicInteger remainingLeaves = new AtomicInteger(networkActions.size());
        // Apply range actions that has been changed by the previous leaf on the network to start next depth leaves
        // from previous optimal leaf starting point
        // TODO: we can wonder if it's better to do this here or at creation of each leaves or at each evaluation/optimization
        previousDepthOptimalLeaf.getRangeActions()
                .forEach(ra ->  ra.apply(network, previousDepthOptimalLeaf.getOptimizedSetPoint(ra)));
        int leavesInParallel = Math.min(networkActions.size(), treeParameters.getLeavesInParallel());
        LOGGER.debug("Evaluating {} leaves in parallel", leavesInParallel);
        try (FaraoNetworkPool networkPool = new FaraoNetworkPool(network, network.getVariantManager().getWorkingVariantId(), leavesInParallel)) {
            networkActions.forEach(networkAction ->
                    networkPool.submit(() -> {
                        try {
                            Network networkClone = networkPool.getAvailableNetwork();
                            optimizeNextLeafAndUpdate(networkAction, networkClone, networkPool);
                            networkPool.releaseUsedNetwork(networkClone);
                            LOGGER.info("Remaining leaves to evaluate: {}", remainingLeaves.decrementAndGet());
                        } catch (InterruptedException | NotImplementedException e) {
                            LOGGER.error("Cannot apply remedial action {}", networkAction.getId());
                            Thread.currentThread().interrupt();
                        }
                    }));
            networkPool.shutdown();
            networkPool.awaitTermination(24, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            LOGGER.error("A computation thread was interrupted");
            Thread.currentThread().interrupt();
        }
    }

    void optimizeNextLeafAndUpdate(NetworkAction networkAction, Network network, FaraoNetworkPool networkPool) throws InterruptedException {
        Leaf leaf;
        try {
            // We get initial range action results from the previous optimal leaf
            leaf = new Leaf(network, previousDepthOptimalLeaf.getNetworkActions(), networkAction, previousDepthOptimalLeaf);
        } catch (NotImplementedException e) {
            networkPool.releaseUsedNetwork(network);
            throw e;
        }
        // We evaluate the leaf with taking the results of the previous optimal leaf if we do not want to update some results
        leaf.evaluate(objectiveFunction, getSensitivityComputerForEvaluationBasedOn(previousDepthOptimalLeaf, availableRangeActions));
        LOGGER.debug("{}", leaf);
        if (!leaf.getStatus().equals(Leaf.Status.ERROR)) {
            if (!stopCriterionReached(leaf)) {
                // We base the results on the results of the evaluation of the leaf in case something has been updated
                optimizeLeaf(leaf, leaf.getPreOptimBranchResult());
                LOGGER.info("{}", leaf);
            }
            updateOptimalLeaf(leaf);
        }
    }

    private void optimizeLeaf(Leaf leaf, BranchResult baseBranchResult) {
        Set<RangeAction> rangeActions = getRangeActionsToOptimize(leaf);
        if (!rangeActions.isEmpty()) {
            leaf.optimize(
                    iteratingLinearOptimizer,
                    getSensitivityComputerForOptimizationBasedOn(baseBranchResult, rangeActions),
                    searchTreeProblem.getLeafProblem(rangeActions)
            );
        } else {
            LOGGER.info("No range actions to optimize");
        }
    }

    private SensitivityComputer getSensitivityComputerForEvaluationBasedOn(BranchResult branchResult, Set<RangeAction> rangeActions) {
        if (linearOptimizerParameters.isRaoWithLoopFlowLimitation()) {
            if (linearOptimizerParameters.getLoopFlowParameters().getLoopFlowApproximationLevel().shouldUpdatePtdfWithTopologicalChange()) {
                return searchTreeComputer.getSensitivityComputerWithComputedCommercialFlows(rangeActions);
            } else {
                return searchTreeComputer.getSensitivityComputerWithFixedCommercialFlows(branchResult, rangeActions);
            }
        } else {
            return searchTreeComputer.getSensitivityComputer(rangeActions);
        }
    }

    private SensitivityComputer getSensitivityComputerForOptimizationBasedOn(BranchResult branchResult, Set<RangeAction> rangeActions) {
        if (linearOptimizerParameters.isRaoWithLoopFlowLimitation()) {
            if (linearOptimizerParameters.getLoopFlowParameters().getLoopFlowApproximationLevel().shouldUpdatePtdfWithPstChange()) {
                return searchTreeComputer.getSensitivityComputerWithComputedCommercialFlows(rangeActions);
            } else {
                return searchTreeComputer.getSensitivityComputerWithFixedCommercialFlows(branchResult, rangeActions);
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
}
