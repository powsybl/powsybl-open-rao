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
import com.farao_community.farao.data.crac_api.UsageMethod;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_api.RaoResult;
import com.farao_community.farao.search_tree_rao.config.SearchTreeRaoParameters;
import com.powsybl.iidm.network.Network;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
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
        double relativeImpact = 0;
        double absoluteImpact = 0;
        int maximumSearchDepth = Integer.MAX_VALUE;
        SearchTreeRaoParameters.StopCriterion stopCriterion = SearchTreeRaoParameters.StopCriterion.POSITIVE_MARGIN;

        if (searchTreeRaoParameters != null) {
            stopCriterion = searchTreeRaoParameters.getStopCriterion();
            maximumSearchDepth = Math.max(searchTreeRaoParameters.getMaximumSearchDepth(), 0);
            relativeImpact = Math.max(searchTreeRaoParameters.getRelativeNetworkActionMinimumImpactThreshold(), 0);
            absoluteImpact = Math.max(searchTreeRaoParameters.getAbsoluteNetworkActionMinimumImpactThreshold(), 0);
        }

        Leaf rootLeaf = new Leaf();
        rootLeaf.evaluate(network, crac, referenceNetworkVariant, parameters);
        int depth = 0;

        if (rootLeaf.getStatus() == Leaf.Status.EVALUATION_ERROR) {
            //TODO : improve error messages depending on leaf error (infeasible optimisation, time-out, ...)
            RaoResult raoResult = new RaoResult(RaoResult.Status.FAILURE);
            return CompletableFuture.completedFuture(raoResult);
        }

        Leaf optimalLeaf = rootLeaf;
        boolean hasImproved = true;

        while (goDeeper(depth, maximumSearchDepth) && doNewIteration(stopCriterion, hasImproved, optimalLeaf.getCost(crac))) {
            Set<NetworkAction> availableNetworkActions = crac.getNetworkActions(network, crac.getPreventiveState(), UsageMethod.AVAILABLE);
            List<Leaf> generatedLeaves = optimalLeaf.bloom(availableNetworkActions);

            if (generatedLeaves.isEmpty()) {
                break;
            }

            //TODO: manage parallel computation
            generatedLeaves.forEach(leaf -> leaf.evaluate(network, crac, referenceNetworkVariant, parameters));
            generatedLeaves = generatedLeaves.stream().filter(leaf -> leaf.getStatus() == Leaf.Status.EVALUATION_SUCCESS).collect(Collectors.toList());

            hasImproved = false;
            for (Leaf currentLeaf: generatedLeaves) {
                if (improvedEnough(optimalLeaf.getCost(crac), currentLeaf.getCost(crac), relativeImpact, absoluteImpact)) {
                    hasImproved = true;
                    depth = depth + 1;
                    resultVariantManager.deleteVariant(optimalLeaf.getRaoResult().getPostOptimVariantId());
                    optimalLeaf = currentLeaf;
                } else {
                    resultVariantManager.deleteVariant(currentLeaf.getRaoResult().getPostOptimVariantId());
                }
                resultVariantManager.deleteVariant(currentLeaf.getRaoResult().getPreOptimVariantId());
            }
        }

        //TODO: refactor output format
        return CompletableFuture.completedFuture(buildOutput(rootLeaf, optimalLeaf));
    }

    /**
     * Stop criterion check: maximum research depth reached
     */
    static boolean goDeeper(int currentDepth, int maxDepth) {
        return currentDepth < maxDepth;
    }

    /**
     * Stop criterion check: the remedial action has enough impact on the cost
     */
    static boolean improvedEnough(double oldCost, double newCost, double relativeImpact, double absoluteImpact) {
        return oldCost - absoluteImpact > newCost && (1 - Math.signum(oldCost) * relativeImpact) * oldCost > newCost;
    }

    /**
     * Stop criterion check: is positive or maximum margin reached?
     */
    static boolean doNewIteration(SearchTreeRaoParameters.StopCriterion stopCriterion, boolean hasImproved, double optimalCost) {
        if (stopCriterion.equals(SearchTreeRaoParameters.StopCriterion.POSITIVE_MARGIN)) {
            return hasImproved && optimalCost > 0;
        } else if (stopCriterion.equals(SearchTreeRaoParameters.StopCriterion.MAXIMUM_MARGIN)) {
            return hasImproved;
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
