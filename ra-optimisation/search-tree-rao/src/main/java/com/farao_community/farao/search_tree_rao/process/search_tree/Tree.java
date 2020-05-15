/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.process.search_tree;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.RandomizedString;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.data.crac_api.UsageMethod;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_api.RaoResult;
import com.farao_community.farao.search_tree_rao.config.SearchTreeRaoParameters;
import com.farao_community.farao.util.FaraoNetworkPool;
import com.powsybl.iidm.network.Network;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

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

    private Tree() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static CompletableFuture<RaoResult> search(Network network, Crac crac, String referenceNetworkVariant, RaoParameters parameters) {
        ResultVariantManager resultVariantManager = crac.getExtension(ResultVariantManager.class);
        if (resultVariantManager == null) {
            resultVariantManager = new ResultVariantManager();
            crac.addExtension(ResultVariantManager.class, resultVariantManager);
        }

        SearchTreeRaoParameters searchTreeRaoParameters = parameters.getExtensionByName("SearchTreeRaoParameters");

        Leaf rootLeaf = new Leaf();
        String initialNetworkVariant = network.getVariantManager().getWorkingVariantId();
        String newNetworkVariant = RandomizedString.getRandomizedString(Collections.singleton(initialNetworkVariant));
        network.getVariantManager().cloneVariant(initialNetworkVariant, newNetworkVariant);
        rootLeaf.evaluate(network, crac, newNetworkVariant, parameters);
        network.getVariantManager().setWorkingVariant(initialNetworkVariant);
        int depth = 0;

        if (rootLeaf.getStatus() == Leaf.Status.EVALUATION_ERROR) {
            //TODO : improve error messages depending on leaf error (infeasible optimisation, time-out, ...)
            RaoResult raoResult = new RaoResult(RaoResult.Status.FAILURE);
            return CompletableFuture.completedFuture(raoResult);
        }

        Leaf optimalLeaf = rootLeaf;
        boolean hasImproved = true;

        while (doNewIteration(searchTreeRaoParameters, hasImproved, optimalLeaf.getCost(crac), depth)) {
            Set<NetworkAction> availableNetworkActions = crac.getNetworkActions(network, crac.getPreventiveState(), UsageMethod.AVAILABLE);
            final List<Leaf> generatedLeaves = optimalLeaf.bloom(availableNetworkActions);

            if (generatedLeaves.isEmpty()) {
                break;
            }

            evaluateLeaves(network, crac, referenceNetworkVariant, parameters, generatedLeaves);

            List<Leaf> successfulLeaves = generatedLeaves.stream().filter(leaf -> leaf.getStatus() == Leaf.Status.EVALUATION_SUCCESS).collect(Collectors.toList());

            hasImproved = false;
            for (Leaf currentLeaf: successfulLeaves) {
                if (improvedEnough(optimalLeaf.getCost(crac), currentLeaf.getCost(crac), searchTreeRaoParameters)) {
                    hasImproved = true;
                    depth = depth + 1;
                    optimalLeaf.deletePostOptimResultVariant(crac);
                    optimalLeaf = currentLeaf;
                } else {
                    currentLeaf.deletePostOptimResultVariant(crac);
                }
                currentLeaf.deletePreOptimResultVariant(crac);
            }
        }

        //TODO: refactor output format
        return CompletableFuture.completedFuture(buildOutput(rootLeaf, optimalLeaf));
    }

    /**
     * Evaluate all the leaves. We use FaraoNetworkPool to parallelize the computation
     */
    private static void evaluateLeaves(Network network, Crac crac, String referenceNetworkVariant, RaoParameters parameters, List<Leaf> generatedLeaves) {
        try (FaraoNetworkPool networkPool = new FaraoNetworkPool(network, referenceNetworkVariant)) {
            networkPool.submit(() -> generatedLeaves.parallelStream().forEach(leaf -> {
                try {
                    Network networkClone = networkPool.getAvailableNetwork();
                    leaf.evaluate(networkClone, crac, referenceNetworkVariant, parameters);
                    networkPool.releaseUsedNetwork(networkClone);
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

    /**
     * Stop criterion check: the remedial action has enough impact on the cost
     */
    private static boolean improvedEnough(double oldCost, double newCost, SearchTreeRaoParameters searchTreeRaoParameters) {
        // check if defined
        double relativeImpact = 0;
        double absoluteImpact = 0;
        if (searchTreeRaoParameters != null) {
            relativeImpact = Math.max(searchTreeRaoParameters.getRelativeNetworkActionMinimumImpactThreshold(), 0);
            absoluteImpact = Math.max(searchTreeRaoParameters.getAbsoluteNetworkActionMinimumImpactThreshold(), 0);
        }

        // stop criterion check
        return oldCost - absoluteImpact > newCost // enough absolute impact
            && (1 - Math.signum(oldCost) * relativeImpact) * oldCost > newCost; // enough relative impact
    }

    /**
     * Stop criterion check 1: maximum research depth reached
     * Stop criterion check 2: is positive or maximum margin reached?
     */
    private static boolean doNewIteration(SearchTreeRaoParameters searchTreeRaoParameters, boolean hasImproved, double optimalCost, int currentDepth) {
        // check if defined
        SearchTreeRaoParameters.StopCriterion stopCriterion = SearchTreeRaoParameters.StopCriterion.POSITIVE_MARGIN;
        int maximumSearchDepth = Integer.MAX_VALUE;
        if (searchTreeRaoParameters != null) {
            stopCriterion = searchTreeRaoParameters.getStopCriterion();
            maximumSearchDepth = Math.max(searchTreeRaoParameters.getMaximumSearchDepth(), 0);
        }

        // stop criterion check
        if (stopCriterion.equals(SearchTreeRaoParameters.StopCriterion.POSITIVE_MARGIN)) {
            return currentDepth < maximumSearchDepth // maximum research depth reached
                &&  hasImproved && optimalCost > 0; // positive margin
        } else if (stopCriterion.equals(SearchTreeRaoParameters.StopCriterion.MAXIMUM_MARGIN)) {
            return currentDepth < maximumSearchDepth // maximum research depth reached
                && hasImproved; // maximum margin
        } else {
            throw new FaraoException("Unexpected stop criterion: " + stopCriterion);
        }
    }

    static RaoResult buildOutput(Leaf rootLeaf, Leaf optimalLeaf) {
        RaoResult raoResult = new RaoResult(optimalLeaf.getRaoResult().getStatus());
        raoResult.setPreOptimVariantId(rootLeaf.getRaoResult().getPreOptimVariantId());
        raoResult.setPostOptimVariantId(optimalLeaf.getRaoResult().getPostOptimVariantId());
        return raoResult;
    }
}
