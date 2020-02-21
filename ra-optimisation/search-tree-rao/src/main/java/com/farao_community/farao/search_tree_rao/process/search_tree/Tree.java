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
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.PstWithRange;
import com.farao_community.farao.ra_optimisation.*;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.search_tree_rao.SearchTreeRaoResult;
import com.powsybl.iidm.network.Network;

import java.util.ArrayList;
import java.util.HashMap;
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

    public static CompletableFuture<RaoComputationResult> search(Network network, Crac crac, String referenceNetworkVariant, RaoParameters parameters) {
        Leaf rootLeaf = new Leaf();
        rootLeaf.evaluate(network, crac, referenceNetworkVariant, parameters);

        if (rootLeaf.getStatus() == Leaf.Status.EVALUATION_ERROR) {
            //TODO : improve error messages depending on leaf error (Sensi divergent, infeasible optimisation, time-out, ...)
            throw new FaraoException("Initial case returns an error");
        }

        Leaf optimalLeaf = rootLeaf;
        boolean hasImproved = true;

        //TODO: generalize to handle different stop criterion
        while (optimalLeaf.getCost() > 0 && hasImproved) {
            Set<NetworkAction> availableNetworkActions = crac.getNetworkActions(network, crac.getPreventiveState(), UsageMethod.AVAILABLE);
            List<Leaf> generatedLeaves = optimalLeaf.bloom(availableNetworkActions);

            if (generatedLeaves.isEmpty()) {
                break;
            }

            //TODO: manage parallel computation
            generatedLeaves.forEach(leaf -> leaf.evaluate(network, crac, referenceNetworkVariant, parameters));

            hasImproved = false;
            for (Leaf currentLeaf: generatedLeaves) {
                if (currentLeaf.getStatus() == Leaf.Status.EVALUATION_SUCCESS && currentLeaf.getCost() < optimalLeaf.getCost()) {
                    hasImproved = true;
                    optimalLeaf = currentLeaf;
                }
            }
        }

        //TODO: refactor output format
        return CompletableFuture.completedFuture(buildOutput(rootLeaf, optimalLeaf, crac));
    }

    static RaoComputationResult buildOutput(Leaf rootLeaf, Leaf optimalLeaf, Crac crac) {

        RaoComputationResult output = new RaoComputationResult(optimalLeaf.getRaoResult().getStatus(), buildPreContingencyResult(rootLeaf, optimalLeaf, crac));

        optimalLeaf.getRaoResult().getContingencyResults().forEach(contingencyResult ->
                output.addContingencyResult(buildContingencyResult(contingencyResult, rootLeaf)));

        output.addExtension(SearchTreeRaoResult.class, buildExtension(optimalLeaf));

        return output;
    }

    @SuppressWarnings("checkstyle:RegexpSingleline")
    private static PreContingencyResult buildPreContingencyResult(Leaf rootLeaf, Leaf optimalLeaf, Crac crac) {
        RaoComputationResult outputRoot = rootLeaf.getRaoResult();
        RaoComputationResult outputOptimal = optimalLeaf.getRaoResult();

        List<MonitoredBranchResult> monitoredBranchResultList = new ArrayList<>();
        List<RemedialActionResult> remedialActionResultList = new ArrayList<>();

        // preventive monitored branches
        outputRoot.getPreContingencyResult().getMonitoredBranchResults().forEach(mbrRoot -> {
            double preOptimisationFlow = mbrRoot.getPreOptimisationFlow();
            double postOptimisationFlow = outputOptimal.getPreContingencyResult().getMonitoredBranchResults().stream()
                    .filter(mbrOptimal -> mbrOptimal.getId().equals(mbrRoot.getId())).findAny().orElseThrow(FaraoException::new)
                    .getPostOptimisationFlow();
            monitoredBranchResultList.add(new MonitoredBranchResult(mbrRoot.getId(), mbrRoot.getName(), mbrRoot.getBranchId(), mbrRoot.getMaximumFlow(), preOptimisationFlow, postOptimisationFlow));
        });

        // preventive Network Actions
        optimalLeaf.getNetworkActions().forEach(na -> {
            remedialActionResultList.add(new RemedialActionResult(na.getId(), na.getName(), true, buildRemedialActionElementResult(na)));
        });

        HashMap<String, ArrayList<RemedialActionElementResult>> remedialActionElementResultListMapping = new HashMap<>();
        crac.getRangeActions().forEach(rangeAction -> {
            // TODO: handle other range actions
            if (rangeAction instanceof PstWithRange) {
                String pstWithRangeId = rangeAction.getId();
                optimalLeaf.getRaoResult().getPreContingencyResult().getRemedialActionResults()
                        .forEach(remedialActionResult -> {
                            remedialActionResult.getRemedialActionElementResults()
                                    .forEach(remedialActionElementResult -> {
                                        if (remedialActionElementResult.getId().equals(pstWithRangeId)) {
                                            ArrayList<RemedialActionElementResult> pstElementResults = new ArrayList<>();
                                            pstElementResults.add(remedialActionElementResult);
                                            remedialActionElementResultListMapping.put(pstWithRangeId, pstElementResults);
                                        }
                                    });
                        });
                ArrayList<RemedialActionElementResult> pstElementResults = remedialActionElementResultListMapping.get(pstWithRangeId);
                remedialActionResultList.add(new RemedialActionResult(
                        rangeAction.getId(),
                        rangeAction.getName(),
                        true,
                        pstElementResults));
            }
        });

        return new PreContingencyResult(monitoredBranchResultList, remedialActionResultList);
    }

    private static ContingencyResult buildContingencyResult(ContingencyResult optimalContingencyResult, Leaf rootLeaf) {

        RaoComputationResult outputRoot = rootLeaf.getRaoResult();
        List<MonitoredBranchResult> monitoredBranchResultList = new ArrayList<>();

        // N-1 monitored branches
        optimalContingencyResult.getMonitoredBranchResults().forEach(mbr -> {
            double preOptimisationFlow = outputRoot.getContingencyResults().stream()
                    .filter(cr -> cr.getId().equals(optimalContingencyResult.getId())).findFirst().orElseThrow(FaraoException::new)
                    .getMonitoredBranchResults().stream()
                    .filter(mbr2 -> mbr2.getId().equals(mbr.getId())).findAny().orElseThrow(FaraoException::new)
                    .getPreOptimisationFlow();
            double postOptimisationFlow = mbr.getPostOptimisationFlow();
            monitoredBranchResultList.add(new MonitoredBranchResult(mbr.getId(), mbr.getName(), mbr.getBranchId(), mbr.getMaximumFlow(), preOptimisationFlow, postOptimisationFlow));
        });

        // no curative RA yet
        return new ContingencyResult(optimalContingencyResult.getId(), optimalContingencyResult.getName(), monitoredBranchResultList);
    }

    private static List<RemedialActionElementResult> buildRemedialActionElementResult(NetworkAction na) {
        // Return always topological remedial action element result with state "OPEN".
        // PST setpoints appears as topological remedial action as they cannot be handled
        // differently with the current RaoComputationResult object
        ArrayList<RemedialActionElementResult> raer = new ArrayList<>();
        raer.add(new TopologicalActionElementResult(na.getId(), TopologicalActionElementResult.TopologicalState.OPEN));
        return raer;
    }

    private static SearchTreeRaoResult buildExtension(Leaf optimalLeaf) {
        SearchTreeRaoResult.ComputationStatus computationStatus = optimalLeaf.getCost() > 0 ? SearchTreeRaoResult.ComputationStatus.UNSECURE : SearchTreeRaoResult.ComputationStatus.SECURE;
        SearchTreeRaoResult.StopCriterion stopCriterion = SearchTreeRaoResult.StopCriterion.OPTIMIZATION_FINISHED;
        return new SearchTreeRaoResult(computationStatus, stopCriterion);
    }
}
