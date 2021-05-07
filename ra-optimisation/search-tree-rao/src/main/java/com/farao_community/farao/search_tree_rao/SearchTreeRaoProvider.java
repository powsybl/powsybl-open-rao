/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.Side;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.rao_api.RaoInput;
import com.farao_community.farao.rao_api.parameters.*;
import com.farao_community.farao.rao_api.RaoProvider;
import com.farao_community.farao.rao_api.results.BranchResult;
import com.farao_community.farao.rao_api.results.OptimizationState;
import com.farao_community.farao.rao_api.results.PerimeterResult;
import com.farao_community.farao.rao_api.results.RaoResult;
import com.farao_community.farao.rao_commons.*;
import com.farao_community.farao.rao_api.parameters.LinearOptimizerParameters;
import com.farao_community.farao.search_tree_rao.output.FailedRaoOutput;
import com.farao_community.farao.search_tree_rao.output.OneStateOnlyRaoOutput;
import com.farao_community.farao.search_tree_rao.output.PreventiveAndCurativesRaoOutput;
import com.farao_community.farao.sensitivity_analysis.SensitivityAnalysisException;
import com.farao_community.farao.util.FaraoNetworkPool;
import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.farao_community.farao.commons.Unit.MEGAWATT;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@AutoService(RaoProvider.class)
public class SearchTreeRaoProvider implements RaoProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchTreeRaoProvider.class);
    private static final String SEARCH_TREE_RAO = "SearchTreeRao";
    private static final String PREVENTIVE_STATE = "PreventiveState";
    private static final String CURATIVE_STATE = "CurativeState";
    private static final int NUMBER_LOGGED_ELEMENTS_END_RAO = 10;

    private StateTree stateTree;

    @Override
    public String getName() {
        return SEARCH_TREE_RAO;
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    public SearchTreeRaoProvider() {
    }

    // Useful for tests
    SearchTreeRaoProvider(StateTree stateTree) {
        this.stateTree = stateTree;
    }

    @Override
    public CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters) {
        RaoUtil.initData(raoInput, parameters);

        stateTree = new StateTree(raoInput.getCrac(), raoInput.getCrac().getPreventiveState());

        // optimization is made on one given state only
        if (raoInput.getOptimizedState() != null) {
            return optimizeOneStateOnly(raoInput, parameters);
        }

        // compute initial sensitivity on all CNECs
        // this is necessary to have initial flows for MNEC and loopflow constraints on CNECs, in preventive and curative perimeters
        PrePerimeterSensitivityOutput initialSensitivityOutput;
        try {
            initialSensitivityOutput = prePerimeterSensitivityAnalysisOnAllPerimeters(raoInput, parameters, null);
        } catch (SensitivityAnalysisException e) {
            LOGGER.error("Initial sensitivity analysis failed :", e);
            return CompletableFuture.completedFuture(new FailedRaoOutput());
        }
        PrePerimeterOutput initialPerimeterOutput = new PrePerimeterOutput(initialSensitivityOutput, null);

        // optimize preventive perimeter
        LOGGER.info("Preventive perimeter optimization [start]");

        Network network = raoInput.getNetwork();
        network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), PREVENTIVE_STATE);
        network.getVariantManager().setWorkingVariant(PREVENTIVE_STATE);

        if (stateTree.getOptimizedStates().size() == 1) {
            return optimizePreventivePerimeter(raoInput, parameters, initialSensitivityOutput);
        }

        PerimeterResult preventiveResult = optimizePreventivePerimeter(raoInput, parameters, initialSensitivityOutput).join().getPerimeterResult(OptimizationState.AFTER_PRA, raoInput.getCrac().getPreventiveState());
        LOGGER.info("Preventive perimeter optimization [end]");

        // optimize curative perimeters
        double preventiveOptimalCost = preventiveResult.getCost();
        TreeParameters curativeTreeParameters = TreeParameters.buildForCurativePerimeter(parameters.getExtension(SearchTreeRaoParameters.class), preventiveOptimalCost);
        applyRemedialActions(raoInput.getNetwork(), preventiveResult);

        PrePerimeterSensitivityOutput preCurativeSensitivityAnalysisOutput = prePerimeterSensitivityAnalysisOnAllPerimeters(raoInput, parameters, initialSensitivityOutput.getBranchResult());
        PerimeterResult postPraPerimeterOutput = new PrePerimeterOutput(preCurativeSensitivityAnalysisOutput, preventiveResult);

        Map<State, PerimeterResult> curativeResults = optimizeCurativePerimeters(raoInput, parameters, curativeTreeParameters, network, initialSensitivityOutput, preCurativeSensitivityAnalysisOutput);

        // merge variants
        LOGGER.info("Merging preventive and curative RAO results.");
        RaoResult mergedRaoResults = mergeRaoResults(initialPerimeterOutput, postPraPerimeterOutput, curativeResults);

        // log results
        /*if (mergedRaoResults.isSuccessful()) {
            SearchTreeRaoLogger.logMostLimitingElementsResults(raoInput.getCrac().getBranchCnecs(), mergedRaoResults.getPostOptimVariantId(), parameters.getObjectiveFunction().getUnit(), parameters.getObjectiveFunction().relativePositiveMargins(), NUMBER_LOGGED_ELEMENTS_END_RAO);
        }*/
        return CompletableFuture.completedFuture(mergedRaoResults);
    }

    private void applyRemedialActions(Network network, PerimeterResult perimeterResult) {
        perimeterResult.getActivatedNetworkActions().forEach(networkAction -> networkAction.apply(network));
        perimeterResult.getActivatedRangeActions().forEach(rangeAction -> rangeAction.apply(network, perimeterResult.getOptimizedSetPoint(rangeAction)));
    }

    private static LinearOptimizerParameters.LinearOptimizerParametersBuilder basicLinearOptimizerBuilder(RaoParameters raoParameters) {
        LinearOptimizerParameters.LinearOptimizerParametersBuilder builder = LinearOptimizerParameters.create()
                .withObjectiveFunction(raoParameters.getObjectiveFunction())
                .withPstSensitivityThreshold(raoParameters.getPstSensitivityThreshold());
        if (raoParameters.getObjectiveFunction() == RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE
                || raoParameters.getObjectiveFunction() == RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT) {
            builder.withMaxMinMarginParameters(new MaxMinMarginParameters(raoParameters.getPstPenaltyCost()));
        } else if (raoParameters.getObjectiveFunction() == RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE
                || raoParameters.getObjectiveFunction() == RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT) {
            MaxMinRelativeMarginParameters maxMinRelativeMarginParameters = new MaxMinRelativeMarginParameters(
                    raoParameters.getPstPenaltyCost(),
                    raoParameters.getNegativeMarginObjectiveCoefficient(),
                    raoParameters.getPtdfSumLowerBound());
            builder.withMaxMinRelativeMarginParameters(maxMinRelativeMarginParameters);
        } else {
            throw new FaraoException("Not handled objective function");
        }

        MnecParameters mnecParameters = new MnecParameters(
                raoParameters.getMnecAcceptableMarginDiminution(),
                raoParameters.getMnecViolationCost(),
                raoParameters.getMnecConstraintAdjustmentCoefficient());
        builder.withMnecParameters(mnecParameters);

        if (raoParameters.isRaoWithLoopFlowLimitation()) {
            LoopFlowParameters loopFlowParameters = new LoopFlowParameters(
                    raoParameters.getLoopFlowApproximationLevel(),
                    raoParameters.getLoopFlowAcceptableAugmentation(),
                    raoParameters.getLoopFlowViolationCost(),
                    raoParameters.getLoopFlowConstraintAdjustmentCoefficient());
            builder.withLoopFlowParameters(loopFlowParameters);
        }
        return builder;
    }

    private static LinearOptimizerParameters createLinearOptimizerParameters(RaoParameters raoParameters) {
        return basicLinearOptimizerBuilder(raoParameters).build();
    }

    private static LinearOptimizerParameters createLinearOptimizerParameters(RaoParameters raoParameters, StateTree stateTree, Set<BranchCnec> cnecs) {
        LinearOptimizerParameters.LinearOptimizerParametersBuilder builder = basicLinearOptimizerBuilder(raoParameters);
        SearchTreeRaoParameters parameters = raoParameters.getExtension(SearchTreeRaoParameters.class);
        if (parameters != null && !parameters.getCurativeRaoOptimizeOperatorsNotSharingCras()) {
            UnoptimizedCnecParameters unoptimizedCnecParameters = new UnoptimizedCnecParameters(
                    stateTree.getOperatorsNotSharingCras(),
                    getLargestCnecThreshold(cnecs));
            builder.withUnoptimizedCnecParameters(unoptimizedCnecParameters);
        }

        return builder.build();
    }

    static double getLargestCnecThreshold(Set<BranchCnec> cnecs) {
        double max = 0;
        for (BranchCnec cnec : cnecs) {
            if (cnec.isOptimized()) {
                Optional<Double> minFlow = cnec.getLowerBound(Side.LEFT, MEGAWATT);
                if (minFlow.isPresent() && Math.abs(minFlow.get()) > max) {
                    max = Math.abs(minFlow.get());
                }
                Optional<Double> maxFlow = cnec.getUpperBound(Side.LEFT, MEGAWATT);
                if (maxFlow.isPresent() && Math.abs(maxFlow.get()) > max) {
                    max = Math.abs(maxFlow.get());
                }
            }
        }
        return max;
    }

    private CompletableFuture<RaoResult> optimizeOneStateOnly(RaoInput raoInput, RaoParameters raoParameters) {
        Set<BranchCnec> cnecs = RaoUtil.computePerimeterCnecs(raoInput.getCrac(), raoInput.getPerimeter());
        TreeParameters treeParameters = raoInput.getOptimizedState().equals(raoInput.getCrac().getPreventiveState()) ?
                TreeParameters.buildForPreventivePerimeter(raoParameters.getExtension(SearchTreeRaoParameters.class)) :
                TreeParameters.buildForCurativePerimeter(raoParameters.getExtension(SearchTreeRaoParameters.class), -Double.MAX_VALUE);
        LinearOptimizerParameters linearOptimizerParameters = createLinearOptimizerParameters(raoParameters, stateTree, cnecs);

        Set<String> countriesNotToOptimize;
        if (raoInput.getOptimizedState().getInstant() == Instant.CURATIVE) {
            countriesNotToOptimize = stateTree.getOperatorsNotSharingCras();
        } else {
            countriesNotToOptimize = new HashSet<>();
        }

        PrePerimeterSensitivityOutput prePerimeterSensitivityOutput = prePerimeterSensitivityAnalysisOnAllPerimeters(raoInput, raoParameters, null);
        PrePerimeterOutput prePerimeterOutput = new PrePerimeterOutput(prePerimeterSensitivityOutput, null);

        SearchTreeInput searchTreeInput = buildSearchTreeInput(raoInput, raoInput.getOptimizedState(), raoInput.getPerimeter(), prePerimeterSensitivityOutput, prePerimeterSensitivityOutput, raoParameters, countriesNotToOptimize);

        PerimeterResult perimeterResult = new SearchTree().run(searchTreeInput, raoParameters, treeParameters, linearOptimizerParameters).join();

        perimeterResult.getActivatedRangeActions().forEach(rangeAction -> rangeAction.apply(raoInput.getNetwork(), perimeterResult.getOptimizedSetPoint(rangeAction)));
        perimeterResult.getActivatedNetworkActions().forEach(networkAction -> networkAction.apply(raoInput.getNetwork()));

        return CompletableFuture.completedFuture(new OneStateOnlyRaoOutput(raoInput.getOptimizedState(), prePerimeterOutput, perimeterResult));
    }

    private PrePerimeterSensitivityOutput prePerimeterSensitivityAnalysisOnAllPerimeters(RaoInput raoInput, RaoParameters parameters, BranchResult initialBranchResult) {
        return new PrePerimeterSensitivityAnalysis(raoInput, raoInput.getCrac().getStates(), raoInput.getCrac().getStates(), parameters, new HashSet<>(), initialBranchResult).run();
    }

    private CompletableFuture<RaoResult> optimizePreventivePerimeter(RaoInput raoInput, RaoParameters raoParameters, PrePerimeterSensitivityOutput prePerimeterSensitivityOutput) {
        TreeParameters preventiveTreeParameters = TreeParameters.buildForPreventivePerimeter(raoParameters.getExtension(SearchTreeRaoParameters.class));
        LinearOptimizerParameters linearOptimizerParameters = createLinearOptimizerParameters(raoParameters);
        SearchTreeInput searchTreeInput = buildSearchTreeInput(raoInput, raoInput.getCrac().getPreventiveState(), stateTree.getPerimeter(raoInput.getCrac().getPreventiveState()), prePerimeterSensitivityOutput, prePerimeterSensitivityOutput, raoParameters, new HashSet<>());

        PerimeterResult perimeterResult = new SearchTree().run(searchTreeInput, raoParameters, preventiveTreeParameters, linearOptimizerParameters).join();

        perimeterResult.getActivatedRangeActions().forEach(rangeAction -> rangeAction.apply(raoInput.getNetwork(), perimeterResult.getOptimizedSetPoint(rangeAction)));
        perimeterResult.getActivatedNetworkActions().forEach(networkAction -> networkAction.apply(raoInput.getNetwork()));

        PrePerimeterOutput prePerimeterOutput = new PrePerimeterOutput(prePerimeterSensitivityOutput, null);
        return CompletableFuture.completedFuture(new OneStateOnlyRaoOutput(raoInput.getCrac().getPreventiveState(), prePerimeterOutput, perimeterResult));
    }

    private Map<State, PerimeterResult> optimizeCurativePerimeters(RaoInput raoInput, RaoParameters raoParameters, TreeParameters curativeTreeParameters, Network network, PrePerimeterSensitivityOutput initialSensitivityOutput, PrePerimeterSensitivityOutput prePerimeterSensitivityOutput) {
        Map<State, PerimeterResult> curativeResults = new ConcurrentHashMap<>();
        network.getVariantManager().setWorkingVariant(PREVENTIVE_STATE);
        network.getVariantManager().cloneVariant(PREVENTIVE_STATE, CURATIVE_STATE);
        network.getVariantManager().setWorkingVariant(CURATIVE_STATE);
        try (FaraoNetworkPool networkPool = new FaraoNetworkPool(network, CURATIVE_STATE, raoParameters.getPerimetersInParallel())) {
            stateTree.getOptimizedStates().forEach(optimizedState -> {
                if (!optimizedState.equals(raoInput.getCrac().getPreventiveState())) {
                    networkPool.submit(() -> {
                        try {
                            LOGGER.info("Optimizing curative state {}.", optimizedState.getId());
                            Network networkClone = networkPool.getAvailableNetwork();
                            Set<BranchCnec> cnecs = RaoUtil.computePerimeterCnecs(raoInput.getCrac(), stateTree.getPerimeter(optimizedState));
                            LinearOptimizerParameters linearOptimizerParameters = createLinearOptimizerParameters(raoParameters, stateTree, cnecs);

                            SearchTreeInput searchTreeInput = buildSearchTreeInput(raoInput, optimizedState, stateTree.getPerimeter(optimizedState), initialSensitivityOutput, prePerimeterSensitivityOutput, raoParameters, stateTree.getOperatorsNotSharingCras());

                            PerimeterResult curativeResult = new SearchTree().run(searchTreeInput, raoParameters, curativeTreeParameters, linearOptimizerParameters).join();
                            curativeResults.put(optimizedState, curativeResult);
                            networkPool.releaseUsedNetwork(networkClone);
                            LOGGER.info("Curative state {} has been optimized.", optimizedState.getId());
                        } catch (InterruptedException | NotImplementedException | FaraoException | NullPointerException e) {
                            LOGGER.error("Curative state {} could not be optimized.", optimizedState.getId());
                            Thread.currentThread().interrupt();
                        }
                    });
                }
            });
            networkPool.shutdown();
            networkPool.awaitTermination(24, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return curativeResults;
    }

    RaoResult mergeRaoResults(PerimeterResult initialResult, PerimeterResult preventiveRaoResult, Map<State, PerimeterResult> curativeRaoResults) {
        return new PreventiveAndCurativesRaoOutput(initialResult, preventiveRaoResult, curativeRaoResults);
    }

    private SearchTreeInput buildSearchTreeInput(RaoInput raoInput, State optimizedState, Set<State> perimeter, PrePerimeterSensitivityOutput initialSensitivityOutput, PrePerimeterSensitivityOutput prePerimeterSensitivityOutput, RaoParameters raoParameters, Set<String> countriesNotToOptimize) {
        Set<BranchCnec> cnecs = RaoUtil.computePerimeterCnecs(raoInput.getCrac(), perimeter);
        Set<BranchCnec> loopflowCnecs = LoopFlowUtil.computeLoopflowCnecs(cnecs, raoInput.getNetwork(), raoParameters);

        SearchTreeInput searchTreeInput = new SearchTreeInput();

        searchTreeInput.setNetwork(raoInput.getNetwork());
        searchTreeInput.setCnecs(cnecs);
        searchTreeInput.setNetworkActions(raoInput.getCrac().getNetworkActions(raoInput.getNetwork(), optimizedState, UsageMethod.AVAILABLE));
        searchTreeInput.setRangeActions(raoInput.getCrac().getRangeActions(raoInput.getNetwork(), optimizedState, UsageMethod.AVAILABLE));
        searchTreeInput.setCountriesNotToOptimize(countriesNotToOptimize);

        searchTreeInput.setLoopflowCnecs(loopflowCnecs);
        searchTreeInput.setGlskProvider(raoInput.getGlskProvider());
        searchTreeInput.setReferenceProgram(raoInput.getReferenceProgram());

        searchTreeInput.setInitialBranchResult(initialSensitivityOutput);
        searchTreeInput.setPrePerimeterBranchResult(prePerimeterSensitivityOutput);
        searchTreeInput.setPrePerimeterSensitivityResult(prePerimeterSensitivityOutput);
        searchTreeInput.setPrePerimeterSetpoints(prePerimeterSensitivityOutput.getOptimizedSetPoints());

        return searchTreeInput;
    }
}
