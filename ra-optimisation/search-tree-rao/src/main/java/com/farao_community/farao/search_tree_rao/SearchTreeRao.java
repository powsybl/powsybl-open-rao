/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_api.RaoProvider;
import com.farao_community.farao.rao_api.RaoResult;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.RaoUtil;
import com.farao_community.farao.util.FaraoNetworkPool;
import com.google.auto.service.AutoService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;

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
@AutoService(RaoProvider.class)
public class SearchTreeRao implements RaoProvider {
    static final Logger LOGGER = LoggerFactory.getLogger(SearchTreeRao.class);

    private RaoParameters raoParameters;
    private SearchTreeRaoParameters searchTreeRaoParameters;
    private Leaf rootLeaf;
    private Leaf optimalLeaf;
    private Leaf previousDepthOptimalLeaf;

    @Override
    public String getName() {
        return "SearchTreeRao";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    private void init(Network network, Crac crac, String variantId, RaoParameters raoParameters) {
        this.raoParameters = raoParameters;
        if (!Objects.isNull(raoParameters.getExtension(SearchTreeRaoParameters.class))) {
            searchTreeRaoParameters = raoParameters.getExtension(SearchTreeRaoParameters.class);
        } else {
            searchTreeRaoParameters = new SearchTreeRaoParameters();
        }
        RaoData rootRaoData = RaoUtil.initRaoData(network, crac, variantId, raoParameters);
        rootLeaf = new Leaf(rootRaoData, raoParameters);
        optimalLeaf = rootLeaf;
        previousDepthOptimalLeaf = rootLeaf;
    }

    @Override
    public CompletableFuture<RaoResult> run(Network network, Crac crac, String variantId, ComputationManager computationManager, RaoParameters raoParameters) {
        init(network, crac, variantId, raoParameters);

        LOGGER.info("Evaluate root leaf");
        rootLeaf.evaluate();
        LOGGER.debug(rootLeaf.toString());
        if (rootLeaf.getStatus().equals(Leaf.Status.ERROR)) {
            //TODO : improve error messages depending on leaf error (infeasible optimisation, time-out, ...)
            RaoResult raoResult = new RaoResult(RaoResult.Status.FAILURE);
            return CompletableFuture.completedFuture(raoResult);
        } else if (stopCriterionReached(rootLeaf)) {
            return CompletableFuture.completedFuture(buildOutput());
        }
        rootLeaf.optimize();
        LOGGER.info(rootLeaf.toString());
        if (stopCriterionReached(rootLeaf)) {
            return CompletableFuture.completedFuture(buildOutput());
        }

        iterateOnTree();

        //TODO: refactor output format
        SearchTreeRaoLogger.logMostLimitingElementsResults(optimalLeaf, crac);
        return CompletableFuture.completedFuture(buildOutput());
    }

    private void iterateOnTree() {
        int depth = 0;
        boolean hasImproved = true;
        while (depth < searchTreeRaoParameters.getMaximumSearchDepth() && hasImproved && !stopCriterionReached(optimalLeaf)) {
            LOGGER.info(format("Research depth: %d - [start]", depth));
            previousDepthOptimalLeaf = optimalLeaf;
            updateOptimalLeafWithNextDepthBestLeaf();
            hasImproved = previousDepthOptimalLeaf != optimalLeaf; // It means this depth evaluation has improved the global cost
            if (hasImproved && previousDepthOptimalLeaf != rootLeaf) {
                previousDepthOptimalLeaf.clearVariants();
            }
            LOGGER.info(format("Optimal leaf - %s", optimalLeaf.toString()));
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
            LOGGER.info(format("Leaves to evaluate: %d", networkActions.size()));
        }
        AtomicInteger remainingLeaves = new AtomicInteger(networkActions.size());
        Network network = rootLeaf.getRaoData().getNetwork(); // NetworkPool starts from root leaf network to keep initial optimization of range actions
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

    private void optimizeNextLeaf(NetworkAction networkAction, Network network) {
        Leaf leaf = new Leaf(previousDepthOptimalLeaf, networkAction, network, raoParameters);
        leaf.evaluate();
        LOGGER.debug(leaf.toString());
        if (leaf.getStatus().equals(Leaf.Status.ERROR)) {
            leaf.clearVariants();
        } else {
            if (!stopCriterionReached(leaf)) {
                leaf.optimize();
                LOGGER.info(leaf.toString());
            }
            leaf.cleanVariants(); // delete pre-optim variant if post-optim variant is better
            updateOptimalLeafAndCleanVariants(leaf);
        }
    }

    private synchronized void updateOptimalLeafAndCleanVariants(Leaf leaf) {
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
    private boolean stopCriterionReached(Leaf leaf) {
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
    private boolean improvedEnough(Leaf leaf) {
        double relativeImpact = Math.max(searchTreeRaoParameters.getRelativeNetworkActionMinimumImpactThreshold(), 0);
        double absoluteImpact = Math.max(searchTreeRaoParameters.getAbsoluteNetworkActionMinimumImpactThreshold(), 0);

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
