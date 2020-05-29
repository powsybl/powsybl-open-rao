/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.process.search_tree;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.data.crac_result_extensions.CnecResult;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_api.RaoResult;
import com.farao_community.farao.rao_commons.RaoData;
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

    private static Leaf previousDepthOptimalLeaf;
    private static Leaf optimalLeaf;

    private Tree() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static CompletableFuture<RaoResult> search(Network network, Crac crac, String variantId, RaoParameters raoParameters) {
        RaoData raoData = RaoUtil.initRaoData(network, crac, variantId, raoParameters);

        LOGGER.info("Evaluate root leaf");
        Leaf rootLeaf = new Leaf(raoData);
        optimalLeaf = rootLeaf;
        optimalLeaf.evaluate(raoParameters);
        previousDepthOptimalLeaf = optimalLeaf;

        if (optimalLeaf.getStatus() == Leaf.Status.EVALUATION_ERROR) {
            //TODO : improve error messages depending on leaf error (infeasible optimisation, time-out, ...)
            RaoResult raoResult = new RaoResult(RaoResult.Status.FAILURE);
            return CompletableFuture.completedFuture(raoResult);
        }

        SearchTreeRaoParameters searchTreeRaoParameters = raoParameters.getExtensionByName("SearchTreeRaoParameters");
        int depth = 0;
        boolean hasImproved = true;
        while (doNewIteration(searchTreeRaoParameters, hasImproved, optimalLeaf, depth)) {
            LOGGER.info(format("Research depth: %d - [start]", depth));
            updateOptimalLeafWithNextDepth(optimalLeaf, raoParameters);
            optimalLeaf.getRaoData().setWorkingVariant(optimalLeaf.getBestVariantId());
            optimalLeaf.getRaoData().applyRangeActionResultsOnNetwork();
            if (previousDepthOptimalLeaf != optimalLeaf) {
                hasImproved = true;
                if (previousDepthOptimalLeaf != rootLeaf) {
                    previousDepthOptimalLeaf.deletePostOptimVariant();
                }
            } else {
                hasImproved = false;
            }
            previousDepthOptimalLeaf = optimalLeaf;
            logOptimalLeaf();
            depth += 1;
        }

        //TODO: refactor output format
        logMostLimitingElements();
        return CompletableFuture.completedFuture(buildOutput(rootLeaf, optimalLeaf));
    }

    /**
     * Stop criterion check 1: maximum research depth reached
     * Stop criterion check 2: is positive or maximum margin reached?
     */
    private static boolean doNewIteration(SearchTreeRaoParameters searchTreeRaoParameters, boolean hasImproved, Leaf leaf, int currentDepth) {
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
                &&  hasImproved && leaf.getBestCost() > 0; // positive margin
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
    private static void updateOptimalLeafWithNextDepth(Leaf parentLeaf, RaoParameters parameters) {
        //  Generate empty leaves
        final List<Leaf> generatedLeaves = optimalLeaf.bloom();
        if (generatedLeaves.isEmpty()) {
            LOGGER.info("No new leaves to evaluate");
        } else {
            LOGGER.info(format("Leaves to evaluate: %d", generatedLeaves.size()));
        }
        SearchTreeRaoParameters searchTreeRaoParameters = parameters.getExtensionByName("SearchTreeRaoParameters");
        AtomicInteger remainingLeaves = new AtomicInteger(generatedLeaves.size());
        try (FaraoNetworkPool networkPool = new FaraoNetworkPool(parentLeaf.getRaoData().getNetwork(),
            parentLeaf.getRaoData().getNetwork().getVariantManager().getWorkingVariantId(),
            searchTreeRaoParameters.getLeavesInParallel())) {
            networkPool.submit(() -> generatedLeaves.parallelStream().forEach(leaf -> {
                try {
                    Network networkClone = networkPool.getAvailableNetwork();
                    leaf.init(networkClone, parentLeaf.getRaoData().getCrac());
                    leaf.evaluate(parameters);
                    if (leaf.getStatus().equals(Leaf.Status.EVALUATION_ERROR)) {
                        leaf.getRaoData().clear();
                    } else {
                        updateBestLeaf(leaf, searchTreeRaoParameters);
                    }
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

    private static synchronized void updateBestLeaf(Leaf leaf, SearchTreeRaoParameters searchTreeRaoParameters) {
        if (!leaf.getPreOptimVariantId().equals(leaf.getPostOptimVariantId())) { // It means that postOptim is a better variant than preOptim so we can delete it
            leaf.deletePreOptimVariant();
        }
        if (improvedEnough(leaf, searchTreeRaoParameters)) {
            if (optimalLeaf != previousDepthOptimalLeaf) {
                optimalLeaf.deletePostOptimVariant();
            }
            optimalLeaf = leaf;
        } else {
            leaf.deletePostOptimVariant();
        }
    }

    /**
     * Stop criterion check: the remedial action has enough impact on the cost
     */
    private static boolean improvedEnough(Leaf leaf, SearchTreeRaoParameters searchTreeRaoParameters) {
        // check if defined
        double relativeImpact = 0;
        double absoluteImpact = 0;
        if (searchTreeRaoParameters != null) {
            relativeImpact = Math.max(searchTreeRaoParameters.getRelativeNetworkActionMinimumImpactThreshold(), 0);
            absoluteImpact = Math.max(searchTreeRaoParameters.getAbsoluteNetworkActionMinimumImpactThreshold(), 0);
        }

        double currentDepthBestCost = optimalLeaf.getBestCost();
        double previousDepthCost = previousDepthOptimalLeaf.getBestCost();
        double newCost = leaf.getBestCost();
        // stop criterion check
        return newCost < currentDepthBestCost
            && previousDepthCost - absoluteImpact > newCost // enough absolute impact
            && (1 - Math.signum(previousDepthCost) * relativeImpact) * previousDepthCost > newCost; // enough relative impact
    }

    static RaoResult buildOutput(Leaf rootLeaf, Leaf optimalLeaf) {
        RaoResult raoResult = new RaoResult(optimalLeaf.getLeafStatus());
        raoResult.setPreOptimVariantId(rootLeaf.getPreOptimVariantId());
        raoResult.setPostOptimVariantId(optimalLeaf.getPostOptimVariantId());
        return raoResult;
    }

    private static void logMostLimitingElements() {
        List<Cnec> sortedCnecs = new ArrayList<>(optimalLeaf.getRaoData().getCrac().getCnecs());
        sortedCnecs.sort(Comparator.comparingDouble(cnec -> computeMarginInMW(cnec, optimalLeaf.getBestVariantId())));

        for (int i = 0; i < Math.min(MAX_LOGS_LIMITING_ELEMENTS, sortedCnecs.size()); i++) {
            Cnec cnec = sortedCnecs.get(i);
            LOGGER.info(format("Limiting element #%d: element %s at state %s with a margin of %f",
                i + 1,
                cnec.getNetworkElement().getName(),
                cnec.getState().getId(),
                computeMarginInMW(cnec, optimalLeaf.getBestVariantId())));
        }
    }

    private static double computeMarginInMW(Cnec cnec, String variantId) {
        CnecResult cnecResult = cnec.getExtension(CnecResultExtension.class).getVariant(variantId);
        return Math.min(cnecResult.getMaxThresholdInMW() - cnecResult.getFlowInMW(),
            cnecResult.getFlowInMW() - cnecResult.getMinThresholdInMW());
    }

    private static void logOptimalLeaf() {
        LOGGER.info(format("Optimal leaf - %s", generateLeafResults(optimalLeaf)));
    }

    /*private static void logLeafResults(Leaf leaf, Crac crac) {
        LOGGER.info(generateLeafResults(leaf, crac));
        LOGGER.debug(generateRangeActionResults(leaf, crac));
    }*/

    private static String generateLeafResults(Leaf leaf) {
        return format("%s: minimum margin = %f",
            leaf.isRoot() ? "root leaf" : leaf.getNetworkActions().stream().map(NetworkAction::getName).collect(Collectors.joining(", ")),
            -leaf.getBestCost());
    }

    /*private static String generateRangeActionResults(Leaf leaf, Crac crac) {
        String rangeActionResults = crac.getRangeActions()
            .stream()
            .map(rangeAction -> format("%s: %d",
                rangeAction.getName(),
                ((PstRangeResult) rangeAction.getExtension(RangeActionResultExtension.class)
                    .getVariant(leaf.getPostOptimVariantId()))
                    .getTap(crac.getPreventiveState().getId())))
            .collect(Collectors.joining(", "));
        return format("%s: range actions = %s",
            leaf.isRoot() ? "root leaf" : leaf.getNetworkActions().stream().map(NetworkAction::getName).collect(Collectors.joining(", ")),
            rangeActionResults);
    }*/
}
