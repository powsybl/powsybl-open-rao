/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.commons.logs.OpenRaoLogger;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import com.powsybl.openrao.searchtreerao.commons.RaoLogger;
import com.powsybl.openrao.searchtreerao.commons.SensitivityComputer;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.GlobalOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.TreeParameters;
import com.powsybl.openrao.searchtreerao.result.impl.MultiStateRemedialActionResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.PerimeterResultWithCnecs;
import com.powsybl.openrao.searchtreerao.result.impl.SearchTreeResult;
import com.powsybl.openrao.searchtreerao.searchtree.inputs.SearchTreeInput;
import com.powsybl.openrao.searchtreerao.searchtree.parameters.SearchTreeParameters;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import com.powsybl.openrao.util.AbstractNetworkPool;
import com.google.common.hash.Hashing;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.NotImplementedException;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.*;
import static com.powsybl.openrao.searchtreerao.castor.algorithm.AutomatonSimulator.getRangeActionsAndTheirTapsApplied;

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
    private static final int NUMBER_LOGGED_ELEMENTS_DURING_TREE = 2;
    private static final int NUMBER_LOGGED_ELEMENTS_END_TREE = 5;
    private static final int NUMBER_LOGGED_VIRTUAL_COSTLY_ELEMENTS = 10;

    /**
     * attribute defined in constructor of the search tree class
     */

    private final SearchTreeInput input;
    private final SearchTreeParameters parameters;
    private final OpenRaoLogger topLevelLogger;

    /**
     * attribute defined and used within the class
     */

    private final boolean purelyVirtual;
    private final SearchTreeBloomer bloomer;

    private Leaf rootLeaf;
    private Leaf optimalLeaf;
    private SearchTreeResult optimalResult;
    private SearchTreeResult previousDepthOptimalResult;

    private Optional<NetworkActionCombination> combinationFulfillingStopCriterion = Optional.empty();

    public SearchTree(SearchTreeInput input,
                      SearchTreeParameters parameters,
                      boolean verbose) {
        // inputs
        this.input = input;
        this.parameters = parameters;
        this.topLevelLogger = verbose ? BUSINESS_LOGS : TECHNICAL_LOGS;

        // build from inputs
        this.purelyVirtual = input.getOptimizationPerimeter().getOptimizedFlowCnecs().isEmpty();
        this.bloomer = new SearchTreeBloomer(input, parameters);
    }

    public CompletableFuture<SearchTreeResult> run() {

        initLeaves(input);

        TECHNICAL_LOGS.debug("Evaluating root leaf");
        rootLeaf.evaluate(input.getObjectiveFunction(), getSensitivityComputerForEvaluation(true));
        if (rootLeaf.getStatus().equals(Leaf.Status.ERROR)) {
            topLevelLogger.info("Could not evaluate leaf: {}", rootLeaf);
            logOptimizationSummary(rootLeaf);
            rootLeaf.finalizeOptimization();
            return CompletableFuture.completedFuture(rootLeaf.getResult());
        } else if (stopCriterionReached(rootLeaf)) {
            topLevelLogger.info("Stop criterion reached on {}", rootLeaf);
            RaoLogger.logMostLimitingElementsResults(topLevelLogger, rootLeaf.getResult().getPerimeterResultWithCnecs(), parameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_END_TREE);
            logOptimizationSummary(rootLeaf);
            rootLeaf.finalizeOptimization();
            return CompletableFuture.completedFuture(rootLeaf.getResult());
        }

        TECHNICAL_LOGS.info("{}", rootLeaf);
        RaoLogger.logMostLimitingElementsResults(TECHNICAL_LOGS, rootLeaf.getResult().getPerimeterResultWithCnecs(), parameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_DURING_TREE);

        TECHNICAL_LOGS.info("Linear optimization on root leaf");
        optimizeLeaf(rootLeaf);

        topLevelLogger.info("{}", rootLeaf);
        RaoLogger.logRangeActions(TECHNICAL_LOGS, optimalResult, input.getOptimizationPerimeter(), null);
        RaoLogger.logMostLimitingElementsResults(topLevelLogger, optimalResult.getPerimeterResultWithCnecs(), parameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_DURING_TREE);
        logVirtualCostInformation(rootLeaf, "");

        if (stopCriterionReached(rootLeaf)) {
            logOptimizationSummary(rootLeaf);
            rootLeaf.finalizeOptimization();
            return CompletableFuture.completedFuture(rootLeaf.getResult());
        }

        iterateOnTree();

        TECHNICAL_LOGS.info("Search-tree RAO completed with status {}", optimalResult.getPerimeterResultWithCnecs().getSensitivityStatus());

        TECHNICAL_LOGS.info("Best leaf: {}", optimalLeaf);
        RaoLogger.logRangeActions(TECHNICAL_LOGS, optimalResult, input.getOptimizationPerimeter(), "Best leaf: ");
        RaoLogger.logMostLimitingElementsResults(TECHNICAL_LOGS, optimalLeaf.getResult().getPerimeterResultWithCnecs(), parameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_END_TREE);

        logOptimizationSummary(optimalLeaf);
        optimalLeaf.finalizeOptimization();
        return CompletableFuture.completedFuture(optimalLeaf.getResult());
    }

    void initLeaves(SearchTreeInput input) {
        rootLeaf = makeLeaf(input.getOptimizationPerimeter(), input.getNetwork(), input.getPrePerimeterResult(), input.getPreOptimizationAppliedRemedialActions());
        optimalLeaf = rootLeaf;
        optimalResult = rootLeaf.getResult();
        previousDepthOptimalResult = optimalResult;
    }

    Leaf makeLeaf(OptimizationPerimeter optimizationPerimeter, Network network, PerimeterResultWithCnecs prePerimeterOutput, AppliedRemedialActions appliedRemedialActionsInSecondaryStates) {
        return new Leaf(optimizationPerimeter, network, prePerimeterOutput, appliedRemedialActionsInSecondaryStates);
    }

    private void logOptimizationSummary(Leaf optimalLeaf) {
        State state = input.getOptimizationPerimeter().getMainOptimizationState();
        RaoLogger.logOptimizationSummary(BUSINESS_LOGS, state, optimalLeaf.getResult().getPerimeterResultWithCnecs().getActivatedNetworkActions(), getRangeActionsAndTheirTapsApplied(optimalLeaf.getRangeActionResult()), rootLeaf.getPreOptimObjectiveFunctionResult(), optimalLeaf.getResult().getPerimeterResultWithCnecs());
        logVirtualCostInformation(optimalLeaf, "");
    }

    private void iterateOnTree() {
        int depth = 0;
        boolean hasImproved = true;
        if (input.getOptimizationPerimeter().getNetworkActions().isEmpty()) {
            topLevelLogger.info("No network action available");
            return;
        }

        int leavesInParallel = Math.min(input.getOptimizationPerimeter().getNetworkActions().size(), parameters.getTreeParameters().leavesInParallel());
        TECHNICAL_LOGS.debug("Evaluating {} leaves in parallel", leavesInParallel);
        try (AbstractNetworkPool networkPool = makeOpenRaoNetworkPool(input.getNetwork(), leavesInParallel)) {
            while (depth < parameters.getTreeParameters().maximumSearchDepth() && hasImproved && !stopCriterionReached(optimalLeaf)) {
                TECHNICAL_LOGS.info("Search depth {} [start]", depth + 1);
                previousDepthOptimalResult = optimalResult;
                updateOptimalLeafWithNextDepthBestLeaf(networkPool);
                hasImproved = previousDepthOptimalResult != optimalResult; // It means this depth evaluation has improved the global cost
                if (hasImproved) {
                    TECHNICAL_LOGS.info("Search depth {} [end]", depth + 1);

                    topLevelLogger.info("Search depth {} best leaf: {}", depth + 1, optimalLeaf);
                    RaoLogger.logRangeActions(TECHNICAL_LOGS, optimalResult, input.getOptimizationPerimeter(), String.format("Search depth %s best leaf: ", depth + 1));
                    RaoLogger.logMostLimitingElementsResults(topLevelLogger, optimalLeaf.getResult().getPerimeterResultWithCnecs(), parameters.getObjectiveFunction(), NUMBER_LOGGED_ELEMENTS_DURING_TREE);
                } else {
                    topLevelLogger.info("No better result found in search depth {}, exiting search tree", depth + 1);
                }
                depth += 1;
                if (depth >= parameters.getTreeParameters().maximumSearchDepth()) {
                    topLevelLogger.info("maximum search depth has been reached, exiting search tree");
                }
            }
            networkPool.shutdownAndAwaitTermination(24, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            TECHNICAL_LOGS.warn("A computation thread was interrupted");
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Evaluate all the leaves. We use OpenRaoNetworkPool to parallelize the computation
     */
    private void updateOptimalLeafWithNextDepthBestLeaf(AbstractNetworkPool networkPool) throws InterruptedException {

        TreeSet<NetworkActionCombination> naCombinationsSorted = new TreeSet<>(this::deterministicNetworkActionCombinationComparison);
        naCombinationsSorted.addAll(bloomer.bloom(optimalResult.getPerimeterResultWithCnecs(), input.getOptimizationPerimeter().getNetworkActions()));
        int numberOfCombinations = naCombinationsSorted.size();

        networkPool.initClones(numberOfCombinations);
        if (naCombinationsSorted.isEmpty()) {
            TECHNICAL_LOGS.info("No more network action available");
            return;
        } else {
            TECHNICAL_LOGS.info("Leaves to evaluate: {}", numberOfCombinations);
        }
        AtomicInteger remainingLeaves = new AtomicInteger(numberOfCombinations);
        List<ForkJoinTask<Object>> tasks = naCombinationsSorted.stream().map(naCombination ->
            networkPool.submit(() -> optimizeOneLeaf(networkPool, naCombination, remainingLeaves))
        ).toList();
        for (ForkJoinTask<Object> task : tasks) {
            try {
                task.get();
            } catch (ExecutionException e) {
                throw new OpenRaoException(e);
            }
        }
    }

    private Object optimizeOneLeaf(AbstractNetworkPool networkPool, NetworkActionCombination naCombination, AtomicInteger remainingLeaves) throws InterruptedException {
        Network networkClone = networkPool.getAvailableNetwork(); //This is where the threads actually wait for available networks
        try {
            if (combinationFulfillingStopCriterion.isEmpty() || deterministicNetworkActionCombinationComparison(naCombination, combinationFulfillingStopCriterion.get()) < 0) {
                boolean shouldRangeActionBeRemoved = bloomer.shouldRangeActionsBeRemovedToApplyNa(naCombination, optimalLeaf.getResult().getPerimeterResultWithCnecs());
                if (shouldRangeActionBeRemoved) {
                    // Remove parentLeaf range actions to respect every maxRa or maxOperator limitation
                    input.getOptimizationPerimeter().getRangeActions().forEach(ra ->
                        ra.apply(networkClone, input.getPrePerimeterResult().getOptimizedSetpoint(ra))
                    );
                } else {
                    // Apply range actions that have been changed by the previous leaf on the network to start next depth leaves
                    // from previous optimal leaf starting point
                    // todo : Not sure previousDepthOptimalLeaf.getRangeActions() returns what we expect, this needs to be investigated
                    previousDepthOptimalResult.getPerimeterResultWithCnecs().getActivatedRangeActions().forEach(
                        ra -> ra.apply(networkClone, previousDepthOptimalResult.getPerimeterResultWithCnecs().getOptimizedSetpoint(ra))
                    );
                }
                optimizeNextLeafAndUpdate(naCombination, shouldRangeActionBeRemoved, networkClone);

            } else {
                topLevelLogger.info("Skipping {} optimization because earlier combination fulfills stop criterion.", naCombination.getConcatenatedId());
            }
        } catch (Exception e) {
            BUSINESS_WARNS.warn("Cannot optimize remedial action combination {}: {}", naCombination.getConcatenatedId(), e.getMessage());
        }
        TECHNICAL_LOGS.info("Remaining leaves to evaluate: {}", remainingLeaves.decrementAndGet());
        networkPool.releaseUsedNetwork(networkClone);
        return null;
    }

    int deterministicNetworkActionCombinationComparison(NetworkActionCombination ra1, NetworkActionCombination ra2) {
        // 1. First priority given to combinations detected during RAO
        int comp1 = compareIsDetectedDuringRao(ra1, ra2);
        if (comp1 != 0) {
            return comp1;
        }
        // 2. Second priority given to pre-defined combinations
        int comp2 = compareIsPreDefined(ra1, ra2);
        if (comp2 != 0) {
            return comp2;
        }
        // 3. Third priority given to large combinations
        int comp3 = compareSize(ra1, ra2);
        if (comp3 != 0) {
            return comp3;
        }
        // 4. Last priority is random but deterministic
        return Integer.compare(Hashing.crc32().hashString(ra1.getConcatenatedId(), StandardCharsets.UTF_8).hashCode(),
                Hashing.crc32().hashString(ra2.getConcatenatedId(), StandardCharsets.UTF_8).hashCode());
    }

    /**
     * Prioritizes the better network action combination that was detected by the RAO
     */
    private int compareIsDetectedDuringRao(NetworkActionCombination ra1, NetworkActionCombination ra2) {
        return -Boolean.compare(ra1.isDetectedDuringRao(), ra2.isDetectedDuringRao());
    }

    /**
     * Prioritizes the network action combination that pre-defined by the user
     */
    private int compareIsPreDefined(NetworkActionCombination ra1, NetworkActionCombination ra2) {
        return -Boolean.compare(this.bloomer.hasPreDefinedNetworkActionCombination(ra1), this.bloomer.hasPreDefinedNetworkActionCombination(ra2));
    }

    /**
     * Prioritizes the bigger network action combination
     */
    private int compareSize(NetworkActionCombination ra1, NetworkActionCombination ra2) {
        return -Integer.compare(ra1.getNetworkActionSet().size(), ra2.getNetworkActionSet().size());
    }

    private String printNetworkActions(Set<NetworkAction> networkActions) {
        return networkActions.stream().map(NetworkAction::getId).collect(Collectors.joining(" + "));
    }

    AbstractNetworkPool makeOpenRaoNetworkPool(Network network, int leavesInParallel) {
        return AbstractNetworkPool.create(network, network.getVariantManager().getWorkingVariantId(), leavesInParallel, false);
    }

    void optimizeNextLeafAndUpdate(NetworkActionCombination naCombination, boolean shouldRangeActionBeRemoved, Network network) {
        Leaf leaf;
        try {
            // We get initial range action results from the previous optimal leaf
            leaf = createChildLeaf(network, naCombination, shouldRangeActionBeRemoved);
        } catch (OpenRaoException e) {
            Set<NetworkAction> networkActions = new HashSet<>(previousDepthOptimalResult.getPerimeterResultWithCnecs().getActivatedNetworkActions());
            networkActions.addAll(naCombination.getNetworkActionSet());
            topLevelLogger.info("Could not evaluate network action combination \"{}\": {}", printNetworkActions(networkActions), e.getMessage());
            return;
        } catch (NotImplementedException e) {
            throw e;
        }
        // We evaluate the leaf with taking the results of the previous optimal leaf if we do not want to update some results
        leaf.evaluate(input.getObjectiveFunction(), getSensitivityComputerForEvaluation(shouldRangeActionBeRemoved));

        topLevelLogger.info("Evaluated {}", leaf);
        if (!leaf.getStatus().equals(Leaf.Status.ERROR)) {
            if (!stopCriterionReached(leaf)) {
                if (combinationFulfillingStopCriterion.isPresent() && deterministicNetworkActionCombinationComparison(naCombination, combinationFulfillingStopCriterion.get()) > 0) {
                    topLevelLogger.info("Skipping {} optimization because earlier combination fulfills stop criterion.", naCombination.getConcatenatedId());
                } else {
                    optimizeLeaf(leaf);

                    topLevelLogger.info("Optimized {}", leaf);
                    logVirtualCostInformation(leaf, "Optimized ");
                }
            } else {
                topLevelLogger.info("Optimized {}", leaf);
            }
            updateOptimalLeaf(leaf, naCombination);
        } else {
            topLevelLogger.info("Could not evaluate {}", leaf);
        }
    }

    Leaf createChildLeaf(Network network, NetworkActionCombination naCombination, boolean shouldPreviousDepthMainStateRangeActionBeRemoved) {
        return new Leaf(
            input.getOptimizationPerimeter(),
            network,
            input.getPrePerimeterResult(),
            previousDepthOptimalResult,
            naCombination,
            input.getPreOptimizationAppliedRemedialActions(),
            shouldPreviousDepthMainStateRangeActionBeRemoved
        );
    }

    private void optimizeLeaf(Leaf leaf) {
        if (!input.getOptimizationPerimeter().getRangeActions().isEmpty()) {
            leaf.optimize(input, parameters);
            if (!leaf.getStatus().equals(Leaf.Status.OPTIMIZED)) {
                topLevelLogger.info("Failed to optimize leaf: {}", leaf);
            }
        } else {
            TECHNICAL_LOGS.info("No range actions to optimize");
        }
    }

    private SensitivityComputer getSensitivityComputerForEvaluation(boolean isRootLeaf) {

        SensitivityComputer.SensitivityComputerBuilder sensitivityComputerBuilder = SensitivityComputer.create()
            .withToolProvider(input.getToolProvider())
            .withCnecs(input.getOptimizationPerimeter().getFlowCnecs())
            .withRangeActions(input.getOptimizationPerimeter().getRangeActions())
            .withOutageInstant(input.getOutageInstant());

        if (isRootLeaf) {
            sensitivityComputerBuilder.withAppliedRemedialActions(input.getPreOptimizationAppliedRemedialActions());
        } else {
            sensitivityComputerBuilder.withAppliedRemedialActions(getPreviousDepthAppliedRemedialActionsBeforeNewLeafEvaluation(previousDepthOptimalResult.getAllStatesRemedialActionResult()));
        }

        if (parameters.getObjectiveFunction().relativePositiveMargins()) {
            if (parameters.getMaxMinRelativeMarginParameters().getPtdfApproximation().shouldUpdatePtdfWithTopologicalChange()) {
                sensitivityComputerBuilder.withPtdfsResults(input.getToolProvider().getAbsolutePtdfSumsComputation(), input.getOptimizationPerimeter().getFlowCnecs());
            } else {
                sensitivityComputerBuilder.withPtdfsResults(input.getInitialFlowResult());
            }
        }

        if (parameters.getLoopFlowParameters() != null && parameters.getLoopFlowParameters().getPtdfApproximation().shouldUpdatePtdfWithTopologicalChange()) {
            sensitivityComputerBuilder.withCommercialFlowsResults(input.getToolProvider().getLoopFlowComputation(), input.getOptimizationPerimeter().getLoopFlowCnecs());
        } else if (parameters.getLoopFlowParameters() != null) {
            sensitivityComputerBuilder.withCommercialFlowsResults(input.getInitialFlowResult());
        }

        return sensitivityComputerBuilder.build();
    }

    private synchronized void updateOptimalLeaf(Leaf leaf, NetworkActionCombination networkActionCombination) {

        if (improvedEnough(leaf)) {
            // nominal case: stop criterion hasn't been reached yet
            if (combinationFulfillingStopCriterion.isEmpty() && leaf.getResult().getPerimeterResultWithCnecs().getCost() < optimalResult.getPerimeterResultWithCnecs().getCost()) {
                optimalLeaf = leaf;
                optimalResult = leaf.getResult();
                if (stopCriterionReached(leaf)) {
                    TECHNICAL_LOGS.info("Stop criterion reached, other threads may skip optimization.");
                    combinationFulfillingStopCriterion = Optional.of(networkActionCombination);
                }
            }
            // special case: stop criterion has been reached
            if (combinationFulfillingStopCriterion.isPresent()
                && stopCriterionReached(leaf)
                && deterministicNetworkActionCombinationComparison(networkActionCombination, combinationFulfillingStopCriterion.get()) < 0) {
                optimalLeaf = leaf;
                optimalResult = leaf.getResult();
                combinationFulfillingStopCriterion = Optional.of(networkActionCombination);
            }
        }
    }

    /**
     * This method evaluates stop criterion on the leaf.
     *
     * @param leaf: Leaf to evaluate.
     * @return True if the stop criterion has been reached on this leaf.
     */
    private boolean stopCriterionReached(Leaf leaf) {
        PerimeterResultWithCnecs perimeterResult = leaf.getResult().getPerimeterResultWithCnecs();
        if (perimeterResult.getVirtualCost() > 1e-6) {
            return false;
        }
        if (purelyVirtual && perimeterResult.getVirtualCost() < 1e-6) {
            TECHNICAL_LOGS.debug("Perimeter is purely virtual and virtual cost is zero. Exiting search tree.");
            return true;
        }
        return costSatisfiesStopCriterion(perimeterResult.getCost());
    }

    /**
     * Returns true if a given cost value satisfies the stop criterion
     */
    boolean costSatisfiesStopCriterion(double cost) {
        if (parameters.getTreeParameters().stopCriterion().equals(TreeParameters.StopCriterion.MIN_OBJECTIVE)) {
            return false;
        } else if (parameters.getTreeParameters().stopCriterion().equals(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE)) {
            return cost < parameters.getTreeParameters().targetObjectiveValue();
        } else {
            throw new OpenRaoException("Unexpected stop criterion: " + parameters.getTreeParameters().stopCriterion());
        }
    }

    /**
     * This method checks if the leaf's cost respects the minimum impact thresholds
     * (absolute and relative) compared to the previous depth's optimal leaf.
     *
     * @param leaf: Leaf that has to be compared with the optimal leaf.
     * @return True if the leaf cost diminution is enough compared to optimal leaf.
     */
    private boolean improvedEnough(Leaf leaf) {
        PerimeterResultWithCnecs perimeterResult = leaf.getResult().getPerimeterResultWithCnecs();
        double relativeImpact = Math.max(parameters.getNetworkActionParameters().getRelativeNetworkActionMinimumImpactThreshold(), 0);
        double absoluteImpact = Math.max(parameters.getNetworkActionParameters().getAbsoluteNetworkActionMinimumImpactThreshold(), 0);

        double previousDepthBestCost = previousDepthOptimalResult.getPerimeterResultWithCnecs().getCost();
        double newCost = perimeterResult.getCost();

        if (previousDepthBestCost > newCost && stopCriterionReached(leaf)) {
            return true;
        }

        return previousDepthBestCost - absoluteImpact > newCost // enough absolute impact
            && (1 - Math.signum(previousDepthBestCost) * relativeImpact) * previousDepthBestCost > newCost; // enough relative impact
    }

    private AppliedRemedialActions getPreviousDepthAppliedRemedialActionsBeforeNewLeafEvaluation(MultiStateRemedialActionResultImpl previousDepthRangeActionActivations) {
        AppliedRemedialActions alreadyAppliedRa = input.getPreOptimizationAppliedRemedialActions().copy();
        if (input.getOptimizationPerimeter() instanceof GlobalOptimizationPerimeter) {
            input.getOptimizationPerimeter().getRangeActionsPerState().entrySet().stream()
                    .filter(e -> !e.getKey().equals(input.getOptimizationPerimeter().getMainOptimizationState())) // remove preventive state
                    .forEach(e -> e.getValue().forEach(ra -> alreadyAppliedRa.addAppliedRangeAction(e.getKey(), ra, previousDepthRangeActionActivations.getOptimizedSetpointOnState(ra, e.getKey()))));
        }
        return alreadyAppliedRa;
    }

    /**
     * This method logs information about positive virtual costs
     */
    private void logVirtualCostInformation(Leaf leaf, String prefix) {
        PerimeterResultWithCnecs perimeterResult = leaf.getResult().getPerimeterResultWithCnecs();
        perimeterResult.getVirtualCostNames().stream()
                .filter(virtualCostName -> perimeterResult.getVirtualCost(virtualCostName) > 1e-6)
                .forEach(virtualCostName -> logVirtualCostDetails(leaf, virtualCostName, prefix));
    }

    /**
     * If stop criterion could have been reached without the given virtual cost, this method logs a message, in order
     * to inform the user that the given network action was rejected because of a virtual cost
     * (message is not logged if it has already been logged at previous depth)
     * In all cases, this method also logs most costly elements for given virtual cost
     */
    void logVirtualCostDetails(Leaf leaf, String virtualCostName, String prefix) {
        PerimeterResultWithCnecs perimeterResult = leaf.getResult().getPerimeterResultWithCnecs();
        OpenRaoLogger logger = topLevelLogger;
        if (!costSatisfiesStopCriterion(perimeterResult.getCost())
                && costSatisfiesStopCriterion(perimeterResult.getCost() - perimeterResult.getVirtualCost(virtualCostName))
                && (leaf.isRoot() || !costSatisfiesStopCriterion(previousDepthOptimalResult.getPerimeterResultWithCnecs().getFunctionalCost()))) {
            // Stop criterion would have been reached without virtual cost, for the first time at this depth
            // and for the given leaf
            BUSINESS_LOGS.info("{}{}, stop criterion could have been reached without \"{}\" virtual cost", prefix, leaf.getIdentifier(), virtualCostName);
            // Promote detailed logs about costly elements to BUSINESS_LOGS
            logger = BUSINESS_LOGS;
        }
        getVirtualCostlyElementsLogs(leaf, virtualCostName, prefix).forEach(logger::info);
    }

    List<String> getVirtualCostlyElementsLogs(Leaf leaf, String virtualCostName, String prefix) {
        PerimeterResultWithCnecs perimeterResult = leaf.getResult().getPerimeterResultWithCnecs();
        Unit unit = parameters.getObjectiveFunction().getUnit();
        List<String> logs = new ArrayList<>();
        int i = 1;
        for (FlowCnec flowCnec : perimeterResult.getCostlyElements(virtualCostName, NUMBER_LOGGED_VIRTUAL_COSTLY_ELEMENTS)) {
            Side limitingSide = perimeterResult.getMargin(flowCnec, Side.LEFT, unit) < perimeterResult.getMargin(flowCnec, Side.RIGHT, unit) ? Side.LEFT : Side.RIGHT;
            double flow = perimeterResult.getFlow(flowCnec, limitingSide, unit);
            Double limitingThreshold = flow >= 0 ? flowCnec.getUpperBound(limitingSide, unit).orElse(flowCnec.getLowerBound(limitingSide, unit).orElse(Double.NaN))
                    : flowCnec.getLowerBound(limitingSide, unit).orElse(flowCnec.getUpperBound(limitingSide, unit).orElse(Double.NaN));
            logs.add(String.format(Locale.ENGLISH,
                    "%s%s, limiting \"%s\" constraint #%02d: flow = %.2f %s, threshold = %.2f %s, margin = %.2f %s, element %s at state %s, CNEC ID = \"%s\", CNEC name = \"%s\"",
                    prefix,
                    leaf.getIdentifier(),
                    virtualCostName,
                    i,
                    flow, unit,
                    limitingThreshold, unit,
                    perimeterResult.getMargin(flowCnec, limitingSide, unit), unit,
                    flowCnec.getNetworkElement().getId(), flowCnec.getState().getId(),
                    flowCnec.getId(), flowCnec.getName()));
            i++;
        }
        return logs;
    }
}
