/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_api.RaoResult;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.util.FaraoNetworkPool;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

    private RaoParameters raoParameters;
    private Leaf rootLeaf;
    private Leaf optimalLeaf;
    private Leaf previousDepthOptimalLeaf;
    private boolean relativePositiveMargins;
    private TreeParameters treeParameters;

    void initParameters(RaoParameters raoParameters) {
        this.raoParameters = raoParameters;
        relativePositiveMargins =
                raoParameters.getObjectiveFunction().equals(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE) ||
                        raoParameters.getObjectiveFunction().equals(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT);
    }

    void initLeaves(RaoData raoData) {
        rootLeaf = new Leaf(raoData, raoParameters, treeParameters);
        optimalLeaf = rootLeaf;
        previousDepthOptimalLeaf = rootLeaf;
    }

    public CompletableFuture<RaoResult> run(RaoData raoData, RaoParameters raoParameters, TreeParameters treeParameters) {
        this.treeParameters = treeParameters;
        initParameters(raoParameters);
        initLeaves(raoData);

        LOGGER.info("Evaluate root leaf");
        rootLeaf.evaluateRootLeaf(treeParameters.getShouldComputeInitialSensitivity());
        SearchTreeRaoLogger.logMostLimitingElementsResults(rootLeaf, raoParameters.getObjectiveFunction().getUnit(), relativePositiveMargins);
        LOGGER.debug("{}", rootLeaf);
        if (rootLeaf.getStatus().equals(Leaf.Status.ERROR)) {
            //TODO : improve error messages depending on leaf error (infeasible optimisation, time-out, ...)
            RaoResult raoResult = new RaoResult(RaoResult.Status.FAILURE);
            return CompletableFuture.completedFuture(raoResult);
        } else if (stopCriterionReached(rootLeaf)) {
            SearchTreeRaoLogger.logMostLimitingElementsResults(optimalLeaf, raoParameters.getObjectiveFunction().getUnit(), relativePositiveMargins);
            return CompletableFuture.completedFuture(buildOutput());
        }
        rootLeaf.optimize();
        LOGGER.info("{}", rootLeaf);
        SearchTreeRaoLogger.logRangeActions(optimalLeaf);
        if (stopCriterionReached(rootLeaf)) {
            return CompletableFuture.completedFuture(buildOutput());
        }

        iterateOnTree();

        //TODO: refactor output format
        LOGGER.info("Search-tree RAO completed with status {}", optimalLeaf.getStatus());
        SearchTreeRaoLogger.logMostLimitingElementsResults(optimalLeaf, raoParameters.getObjectiveFunction().getUnit(), relativePositiveMargins);
        return CompletableFuture.completedFuture(buildOutput());
    }

    private void iterateOnTree() {
        int depth = 0;
        boolean hasImproved = true;
        while (depth < treeParameters.getMaximumSearchDepth() && hasImproved && !stopCriterionReached(optimalLeaf)) {
            LOGGER.info("Research depth: {} - [start]", depth);
            previousDepthOptimalLeaf = optimalLeaf;
            updateOptimalLeafWithNextDepthBestLeaf();
            hasImproved = previousDepthOptimalLeaf != optimalLeaf; // It means this depth evaluation has improved the global cost
            if (hasImproved) {
                if (previousDepthOptimalLeaf == rootLeaf) {
                    previousDepthOptimalLeaf.clearAllVariantsExceptInitialOne(); // to keep the results without optim
                } else {
                    previousDepthOptimalLeaf.clearAllVariants();
                }
            } else {
                LOGGER.debug("Objective function value has not improved");
            }
            LOGGER.info("Optimal leaf - {}", optimalLeaf);
            SearchTreeRaoLogger.logRangeActions(optimalLeaf, "Optimal leaf");
            SearchTreeRaoLogger.logMostLimitingElementsResults(optimalLeaf, raoParameters.getObjectiveFunction().getUnit(), relativePositiveMargins);
            depth += 1;
        }
    }

    /**
     * Evaluate all the leaves. We use FaraoNetworkPool to parallelize the computation
     */
    private void updateOptimalLeafWithNextDepthBestLeaf() {
        final Set<NetworkAction> networkActions = optimalLeaf.bloom();
        if (networkActions.isEmpty()) {
            LOGGER.info("No new leaves to evaluate");
        } else {
            LOGGER.info("Leaves to evaluate: {}", networkActions.size());
        }
        AtomicInteger remainingLeaves = new AtomicInteger(networkActions.size());
        Network network = rootLeaf.getRaoData().getNetwork(); // NetworkPool starts from root leaf network to keep initial optimization of range actions
        LOGGER.debug("Evaluating {} leaves in parallel", treeParameters.getLeavesInParallel());
        try (FaraoNetworkPool networkPool = new FaraoNetworkPool(network, network.getVariantManager().getWorkingVariantId(), treeParameters.getLeavesInParallel())) {
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
            leaf = new Leaf(previousDepthOptimalLeaf, networkAction, network, raoParameters, treeParameters);
        } catch (NotImplementedException e) {
            networkPool.releaseUsedNetwork(network);
            throw e;
        }
        leaf.evaluate();
        LOGGER.debug("{}", leaf);
        if (leaf.getStatus().equals(Leaf.Status.ERROR)) {
            leaf.clearAllVariants();
        } else {
            if (!stopCriterionReached(leaf)) {
                leaf.optimize();
                LOGGER.info("{}", leaf);
            }
            leaf.clearAllVariantsExceptOptimizedOne();
            updateOptimalLeafAndCleanVariants(leaf);
        }
    }

    private synchronized void updateOptimalLeafAndCleanVariants(Leaf leaf) {
        if (improvedEnough(leaf)) {
            if (optimalLeaf != previousDepthOptimalLeaf) {
                optimalLeaf.clearAllVariants();
            }
            optimalLeaf = leaf;
        } else {
            leaf.clearAllVariants();
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
            return leaf.getBestCost() < treeParameters.getTargetObjectiveValue();
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

        double currentBestCost = optimalLeaf.getBestCost();
        double previousDepthBestCost = previousDepthOptimalLeaf.getBestCost();
        double newCost = leaf.getBestCost();

        return newCost < currentBestCost
                && previousDepthBestCost - absoluteImpact > newCost // enough absolute impact
                && (1 - Math.signum(previousDepthBestCost) * relativeImpact) * previousDepthBestCost > newCost; // enough relative impact
    }

    private RaoResult buildOutput() {
        RaoResult raoResult = new RaoResult(optimalLeaf.getStatus().equals(Leaf.Status.ERROR) ? RaoResult.Status.FAILURE : RaoResult.Status.SUCCESS);
        raoResult.setPreOptimVariantId(rootLeaf.getInitialVariantId());
        raoResult.setPostOptimVariantId(optimalLeaf.getBestVariantId());
        return raoResult;
    }
}
