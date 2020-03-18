/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.process.search_tree;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.data.crac_api.UsageMethod;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_api.RaoResult;
import com.powsybl.iidm.network.Network;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

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

        //TODO: manage result variants
        Leaf rootLeaf = new Leaf();
        rootLeaf.evaluate(network, crac, referenceNetworkVariant, parameters);

        if (rootLeaf.getStatus() == Leaf.Status.EVALUATION_ERROR) {
            //TODO : improve error messages depending on leaf error (infeasible optimisation, time-out, ...)
            RaoResult raoResult = new RaoResult(RaoResult.Status.FAILURE);
            return CompletableFuture.completedFuture(raoResult);
        }

        Leaf optimalLeaf = rootLeaf;
        boolean hasImproved = true;

        //TODO: generalize to handle different stop criterion
        while (optimalLeaf.getCost(crac) > 0 && hasImproved) {
            Set<NetworkAction> availableNetworkActions = crac.getNetworkActions(network, crac.getPreventiveState(), UsageMethod.AVAILABLE);
            List<Leaf> generatedLeaves = optimalLeaf.bloom(availableNetworkActions);

            if (generatedLeaves.isEmpty()) {
                break;
            }

            //TODO: manage parallel computation
            generatedLeaves.forEach(leaf -> leaf.evaluate(network, crac, referenceNetworkVariant, parameters));

            hasImproved = false;
            for (Leaf currentLeaf: generatedLeaves) {
                if (currentLeaf.getStatus() == Leaf.Status.EVALUATION_SUCCESS && currentLeaf.getCost(crac) < optimalLeaf.getCost(crac)) {
                    hasImproved = true;
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

    static RaoResult buildOutput(Leaf rootLeaf, Leaf optimalLeaf) {
        RaoResult raoResult = new RaoResult(optimalLeaf.getRaoResult().getStatus());
        raoResult.setPreOptimVariantId(rootLeaf.getRaoResult().getPreOptimVariantId());
        raoResult.setPostOptimVariantId(optimalLeaf.getRaoResult().getPostOptimVariantId());
        return raoResult;
    }
}
