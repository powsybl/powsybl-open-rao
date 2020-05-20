/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.process.search_tree;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.RandomizedString;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.data.crac_api.UsageMethod;
import com.farao_community.farao.data.crac_result_extensions.*;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_api.RaoResult;
import com.farao_community.farao.search_tree_rao.config.SearchTreeRaoParameters;
import com.farao_community.farao.util.FaraoNetworkPool;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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

    private static final Logger LOGGER = LoggerFactory.getLogger(Tree.class);
    private static final int MAX_LOGS_LIMITING_ELEMENTS = 10;

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
        String newNetworkVariant = RandomizedString.getRandomizedString(network.getVariantManager().getVariantIds());
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
            LOGGER.info(format("Research depth: %d, Leaves to evaluate: %d", depth, generatedLeaves.size()));

            if (generatedLeaves.isEmpty()) {
                break;
            }

            evaluateLeaves(network, crac, newNetworkVariant, parameters, generatedLeaves);
            List<Leaf> successfulLeaves = generatedLeaves.stream().filter(leaf -> leaf.getStatus() == Leaf.Status.EVALUATION_SUCCESS).collect(Collectors.toList());

            hasImproved = false;
            double oldOptimalCost = optimalLeaf.getCost(crac);
            logOptimalLeaf(optimalLeaf, crac);
            LOGGER.info("Leaves results:");
            for (Leaf currentLeaf: successfulLeaves) {
                logLeafResults(currentLeaf, crac);
                if (optimalLeaf.getCost(crac) > currentLeaf.getCost(crac)
                    && improvedEnough(oldOptimalCost, currentLeaf.getCost(crac), searchTreeRaoParameters)) {
                    hasImproved = true;
                    optimalLeaf.deletePostOptimResultVariant(crac);
                    optimalLeaf = currentLeaf;
                } else {
                    currentLeaf.deletePostOptimResultVariant(crac);
                }
                currentLeaf.deletePreOptimResultVariant(crac);
            }
            logOptimalLeaf(optimalLeaf, crac);
            if (!hasImproved) {
                LOGGER.info("No sufficient improvements at tree depth, optimization will stop");
            }
            depth += 1;
        }

        //TODO: refactor output format
        logMostLimitingElements(crac, optimalLeaf);
        return CompletableFuture.completedFuture(buildOutput(rootLeaf, optimalLeaf));
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

    /**
     * Evaluate all the leaves. We use FaraoNetworkPool to parallelize the computation
     */
    private static void evaluateLeaves(Network network, Crac crac, String referenceNetworkVariant, RaoParameters parameters, List<Leaf> generatedLeaves) {
        SearchTreeRaoParameters searchTreeRaoParameters = parameters.getExtensionByName("SearchTreeRaoParameters");
        AtomicInteger remainingLeaves = new AtomicInteger(generatedLeaves.size());
        try (FaraoNetworkPool networkPool = new FaraoNetworkPool(network, referenceNetworkVariant, searchTreeRaoParameters.getLeavesInParallel())) {
            networkPool.submit(() -> generatedLeaves.parallelStream().forEach(leaf -> {
                try {
                    Network networkClone = networkPool.getAvailableNetwork();
                    leaf.evaluate(networkClone, crac, referenceNetworkVariant, parameters);
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

    static RaoResult buildOutput(Leaf rootLeaf, Leaf optimalLeaf) {
        RaoResult raoResult = new RaoResult(optimalLeaf.getRaoResult().getStatus());
        raoResult.setPreOptimVariantId(rootLeaf.getRaoResult().getPreOptimVariantId());
        raoResult.setPostOptimVariantId(optimalLeaf.getRaoResult().getPostOptimVariantId());
        return raoResult;
    }

    private static void logOptimalLeaf(Leaf leaf, Crac crac) {
        LOGGER.info(format("Optimal leaf - %s", generateLeafResults(leaf, crac)));
    }

    private static void logLeafResults(Leaf leaf, Crac crac) {
        LOGGER.info(generateLeafResults(leaf, crac));
    }

    private static String generateLeafResults(Leaf leaf, Crac crac) {
        String rangeActionResults = crac.getRangeActions()
            .stream()
            .map(rangeAction -> format("%s: %d",
                rangeAction.getName(),
                ((PstRangeResult) rangeAction.getExtension(RangeActionResultExtension.class)
                    .getVariant(leaf.getRaoResult().getPostOptimVariantId()))
                    .getTap(crac.getPreventiveState().getId())))
            .collect(Collectors.joining(", "));
        return format("%s: minimum margin = %f (%s)",
            leaf.isRoot() ? "root leaf" : leaf.getNetworkActions().stream().map(NetworkAction::getName).collect(Collectors.joining(", ")),
            -leaf.getCost(crac),
            rangeActionResults);
    }

    private static void logMostLimitingElements(Crac crac, Leaf optimalLeaf) {
        List<Cnec> sortedCnecs = new ArrayList<>(crac.getCnecs());
        String postOptimVariantId = optimalLeaf.getRaoResult().getPostOptimVariantId();
        sortedCnecs.sort(Comparator.comparingDouble(cnec -> computeMarginInMW(cnec, postOptimVariantId)));

        for (int i = 0; i < Math.min(MAX_LOGS_LIMITING_ELEMENTS, sortedCnecs.size()); i++) {
            Cnec cnec = sortedCnecs.get(i);
            LOGGER.info(format("Limiting element #%d: element %s at state %s with a margin of %f",
                i + 1,
                cnec.getNetworkElement().getName(),
                cnec.getState().getId(),
                computeMarginInMW(cnec, postOptimVariantId)));
        }
    }

    private static double computeMarginInMW(Cnec cnec, String variantId) {
        CnecResult cnecResult = cnec.getExtension(CnecResultExtension.class).getVariant(variantId);
        return Math.min(cnecResult.getMaxThresholdInMW() - cnecResult.getFlowInMW(),
            cnecResult.getFlowInMW() - cnecResult.getMinThresholdInMW());
    }
}
