/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.process.search_tree;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_api.RaoResult;
import com.farao_community.farao.rao_commons.RaoUtil;
import com.farao_community.farao.search_tree_rao.config.SearchTreeRaoParameters;
import com.farao_community.farao.util.FaraoNetworkPool;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.*;

/**
 * The "tree" is one of the core object of the search-tree algorithm.
 * It aims at finding a good combination of Network Actions.
 *
 * The tree is composed of leaves which evaluate the impact of Network Actions,
 * one by one. The tree is orchestrating the leaves : it looks for a smart
 * routing among the leaves in order to converge as quickly as possible to a local
 * minimum of the objective function.
 *
 * The leaves of a same depth can be evaluated simultaneously.
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class Tree {
    static final Logger LOGGER = LoggerFactory.getLogger(Tree.class);

    private static Leaf rootLeaf;
    private static Leaf previousDepthOptimalLeaf;
    private static Leaf optimalLeaf;
    private static RaoParameters raoParameters;
    private static SearchTreeRaoParameters searchTreeRaoParameters;

    private Tree() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    private static void init(Network network, Crac crac, String variantId, RaoParameters raoParameters) {
        Tree.raoParameters = raoParameters;
        searchTreeRaoParameters = raoParameters.getExtensionByName("SearchTreeRaoParameters");
        rootLeaf = new Leaf(RaoUtil.initRaoData(network, crac, variantId, raoParameters), raoParameters);
        optimalLeaf = rootLeaf;
    }

    public static CompletableFuture<RaoResult> search(Network network, Crac crac, String variantId, RaoParameters raoParameters) {
        init(network, crac, variantId, raoParameters);

        LOGGER.info("Evaluate root leaf");
        rootLeaf.evaluate();
        LOGGER.debug(rootLeaf.toString());
        if (rootLeaf.getStatus().equals(Leaf.Status.ERROR)) {
            //TODO : improve error messages depending on leaf error (infeasible optimisation, time-out, ...)
            RaoResult raoResult = new RaoResult(RaoResult.Status.FAILURE);
            return CompletableFuture.completedFuture(raoResult);
        } else if (stopCriterionChecked(rootLeaf)) {
            return CompletableFuture.completedFuture(buildOutput());
        }
        rootLeaf.optimize();
        LOGGER.info(rootLeaf.toString());
        if (stopCriterionChecked(rootLeaf)) {
            return CompletableFuture.completedFuture(buildOutput());
        }

        int depth = 0;
        boolean hasImproved = true;
        while (depth < searchTreeRaoParameters.getMaximumSearchDepth() && hasImproved && !stopCriterionChecked(optimalLeaf)) {
            LOGGER.info(format("Research depth: %d - [start]", depth));
            previousDepthOptimalLeaf = optimalLeaf;
            updateOptimalLeafWithNextDepthLeaves();
            hasImproved = previousDepthOptimalLeaf != optimalLeaf; // It means this depth evaluation has improved the global cost
            optimalLeaf.applyRangeActionResultsOnNetwork(); // Store range action optimization results in the network, because next depth will start based on this network
            if (hasImproved && previousDepthOptimalLeaf != rootLeaf) {
                previousDepthOptimalLeaf.clearVariants();
            }
            LOGGER.info(format("Optimal leaf - %s", optimalLeaf.toString()));
            depth += 1;
        }

        //TODO: refactor output format
        TreeLogger.logMostLimitingElementsResults(optimalLeaf, crac);
        return CompletableFuture.completedFuture(buildOutput());
    }

    /**
     * Evaluate all the leaves. We use FaraoNetworkPool to parallelize the computation
     */
    private static void updateOptimalLeafWithNextDepthLeaves() {
        final Set<NetworkAction> networkActions = optimalLeaf.bloom();
        if (networkActions.isEmpty()) {
            LOGGER.info("No new leaves to evaluate");
        } else {
            LOGGER.info(format("Leaves to evaluate: %d", networkActions.size()));
        }
        AtomicInteger remainingLeaves = new AtomicInteger(networkActions.size());
        Network network = previousDepthOptimalLeaf.getRaoData().getNetwork(); // NetworkPool starts from optimal previous depth network to keep previous optimization of range actions
        LOGGER.debug(format("Evaluating %d leaves in parallel", searchTreeRaoParameters.getLeavesInParallel()));
        try (FaraoNetworkPool networkPool = new FaraoNetworkPool(network, network.getVariantManager().getWorkingVariantId(), searchTreeRaoParameters.getLeavesInParallel())) {
            networkPool.submit(() -> networkActions.parallelStream().forEach(networkAction -> {
                try {
                    Network networkClone = networkPool.getAvailableNetwork();
                    optimizeNextLeaf(networkAction, networkClone);
                    networkPool.releaseUsedNetwork(networkClone);
                    LOGGER.info(format("Remaining leaves to evaluate: %d", remainingLeaves.decrementAndGet()));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            })).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            throw new FaraoException(e);
        }
    }

    private static void optimizeNextLeaf(NetworkAction networkAction, Network network) {
        Leaf leaf = new Leaf(previousDepthOptimalLeaf, networkAction, network, raoParameters);
        leaf.evaluate();
        LOGGER.debug(leaf.toString());
        if (leaf.getStatus().equals(Leaf.Status.ERROR)) {
            leaf.clearVariants();
        } else {
            if (!stopCriterionChecked(leaf) || !improvedEnough(leaf)) {
                leaf.optimize();
                LOGGER.info(leaf.toString());
            }
            updateOptimalLeafAndCleanVariants(leaf);
        }
    }

    private static synchronized void updateOptimalLeafAndCleanVariants(Leaf leaf) {
        leaf.cleanVariants(); // delete pre-optim variant if post-optim variant is better
        if (improvedEnough(leaf)) {
            if (optimalLeaf != previousDepthOptimalLeaf) {
                optimalLeaf.clearVariants();
            }
            optimalLeaf = leaf;
        } else {
            leaf.clearVariants();
        }
    }

    /**
     * This method evaluates stop criterion on the leaf.
     *
     * @param leaf: Leaf to evaluate.
     * @return True if the stop criterion has been reached on this leaf.
     */
    private static boolean stopCriterionChecked(Leaf leaf) {
        // stop criterion check
        if (searchTreeRaoParameters.getStopCriterion().equals(SearchTreeRaoParameters.StopCriterion.POSITIVE_MARGIN)) {
            return leaf.getBestCost() < 0;
        } else if (searchTreeRaoParameters.getStopCriterion().equals(SearchTreeRaoParameters.StopCriterion.MAXIMUM_MARGIN)) {
            return false;
        } else {
            throw new FaraoException("Unexpected stop criterion: " + searchTreeRaoParameters.getStopCriterion());
        }
    }

    /**
     * This method compares the leaf best cost to the optimal leaf best cost taking into account the minimum impact
     * thresholds (absolute and relative)
     *
     * @param leaf: Leaf that has to be compared with the optimal leaf.
     * @return True if the leaf cost diminution is enough compared to optimal leaf.
     */
    private static boolean improvedEnough(Leaf leaf) {
        double relativeImpact = Math.max(searchTreeRaoParameters.getRelativeNetworkActionMinimumImpactThreshold(), 0);
        double absoluteImpact = Math.max(searchTreeRaoParameters.getAbsoluteNetworkActionMinimumImpactThreshold(), 0);

        double currentBestCost = optimalLeaf.getBestCost();
        double previousDepthBestCost = previousDepthOptimalLeaf.getBestCost();
        double newCost = leaf.getBestCost();

        return newCost < currentBestCost
            && previousDepthBestCost - absoluteImpact > newCost // enough absolute impact
            && (1 - Math.signum(previousDepthBestCost) * relativeImpact) * previousDepthBestCost > newCost; // enough relative impact
    }

    static RaoResult buildOutput() {
        RaoResult raoResult = new RaoResult(optimalLeaf.getStatus().equals(Leaf.Status.ERROR) ? RaoResult.Status.FAILURE : RaoResult.Status.SUCCESS);
        raoResult.setPreOptimVariantId(rootLeaf.getInitialVariantId());
        raoResult.setPostOptimVariantId(optimalLeaf.getBestVariantId());
        return raoResult;
    }
}
