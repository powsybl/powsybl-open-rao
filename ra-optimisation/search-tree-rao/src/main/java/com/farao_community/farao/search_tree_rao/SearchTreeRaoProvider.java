/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_result_extensions.*;
import com.farao_community.farao.rao_api.RaoInput;
import com.farao_community.farao.rao_api.parameters.*;
import com.farao_community.farao.rao_api.RaoProvider;
import com.farao_community.farao.rao_api.RaoResultImpl;
import com.farao_community.farao.rao_commons.*;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimizerParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.parameters.*;
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
import java.util.stream.Collectors;

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
    public CompletableFuture<RaoResultImpl> run(RaoInput raoInput, RaoParameters parameters) {
        RaoUtil.initData(raoInput, parameters);

        stateTree = new StateTree(raoInput.getCrac(), raoInput.getCrac().getPreventiveState());

        // optimization is made on one given state only
        if (raoInput.getOptimizedState() != null) {
            return optimizeOneStateOnly(raoInput, parameters);
        }

        // compute initial sensitivity on all CNECs
        // this is necessary to have initial flows for MNEC and loopflow constraints on CNECs, in preventive and curative perimeters
        PrePerimeterSensitivityAnalysisOutput prePerimeterSensitivityAnalysisOutput;
        try {
            prePerimeterSensitivityAnalysisOutput = prePerimeterSensitivityAnalysisOnAllPerimeters(raoInput, parameters);
        } catch (SensitivityAnalysisException e) {
            LOGGER.error("Initial sensitivity analysis failed :", e);
            return CompletableFuture.completedFuture(new RaoResultImpl(RaoResultImpl.Status.FAILURE));
        }

        // optimize preventive perimeter
        LOGGER.info("Preventive perimeter optimization [start]");

        Network network = raoInput.getNetwork();
        network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), PREVENTIVE_STATE);
        network.getVariantManager().setWorkingVariant(PREVENTIVE_STATE);

        if (stateTree.getOptimizedStates().size() == 1) {
            return optimizePreventivePerimeter(raoInput, parameters, prePerimeterSensitivityAnalysisOutput);
        }

        RaoResultImpl preventiveRaoResult = optimizePreventivePerimeter(raoInput, parameters, prePerimeterSensitivityAnalysisOutput).join();
        LOGGER.info("Preventive perimeter optimization [end]");

        // optimize curative perimeters
        double preventiveOptimalCost = raoInput.getCrac().getExtension(CracResultExtension.class).getVariant(preventiveRaoResult.getPostOptimVariantId()).getCost();
        raoInput.getCrac().getExtension(ResultVariantManager.class).setPrePerimeterVariantId(preventiveRaoResult.getPostOptimVariantId());
        TreeParameters curativeTreeParameters = TreeParameters.buildForCurativePerimeter(parameters.getExtension(SearchTreeRaoParameters.class), preventiveOptimalCost);
        CracResultUtil.applyRemedialActionsForState(raoInput.getNetwork(), raoInput.getCrac(), preventiveRaoResult.getPostOptimVariantId(), raoInput.getCrac().getPreventiveState());
        //TODO: PRE COMPUTE PRE PERIMETER SENSI ANALYSIS FOR ALL CURATIVES
        Map<State, RaoResultImpl> curativeResults = optimizeCurativePerimeters(raoInput, parameters, curativeTreeParameters, network, prePerimeterSensitivityAnalysisOutput);

        // merge variants
        LOGGER.info("Merging preventive and curative RAO results.");
        RaoResultImpl mergedRaoResults = mergeRaoResults(raoInput.getCrac(), preventiveRaoResult, curativeResults);

        // log results
        if (mergedRaoResults.isSuccessful()) {
            SearchTreeRaoLogger.logMostLimitingElementsResults(raoInput.getCrac().getBranchCnecs(), mergedRaoResults.getPostOptimVariantId(), parameters.getObjectiveFunction().getUnit(), parameters.getObjectiveFunction().relativePositiveMargins(), NUMBER_LOGGED_ELEMENTS_END_RAO);
        }
        return CompletableFuture.completedFuture(mergedRaoResults);

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

    private CompletableFuture<RaoResultImpl> optimizeOneStateOnly(RaoInput raoInput, RaoParameters raoParameters) {
        /*RaoData raoData = new  RaoData(
            raoInput.getNetwork(),
                raoInput.getCrac(),
                raoInput.getOptimizedState(),
                raoInput.getPerimeter(),
                raoInput.getReferenceProgram(),
                raoInput.getGlskProvider(),
                raoInput.getBaseCracVariantId(),
                raoParameters);*/
        Set<BranchCnec> cnecs = RaoUtil.computePerimeterCnecs(raoInput.getCrac(), raoInput.getPerimeter());
        TreeParameters treeParameters = raoInput.getOptimizedState().equals(raoInput.getCrac().getPreventiveState()) ?
                TreeParameters.buildForPreventivePerimeter(raoParameters.getExtension(SearchTreeRaoParameters.class)) :
                TreeParameters.buildForCurativePerimeter(raoParameters.getExtension(SearchTreeRaoParameters.class), -Double.MAX_VALUE);
        LinearOptimizerParameters linearOptimizerParameters = createLinearOptimizerParameters(raoParameters, stateTree, cnecs);
        PrePerimeterSensitivityAnalysisOutput prePerimeterSensitivityAnalysisOutput = new PrePerimeterSensitivityAnalysis(raoInput, raoInput.getOptimizedState(), raoInput.getPerimeter(), raoParameters).run();

        SearchTreeInput searchTreeInput = buildSearchTreeInput(raoInput, raoInput.getOptimizedState(), raoInput.getPerimeter(), prePerimeterSensitivityAnalysisOutput, raoParameters);

        RaoResultImpl raoResult = new SearchTree().run(searchTreeInput, raoParameters, treeParameters, linearOptimizerParameters).join();
        SearchTreeRaoLogger.logMostLimitingElementsResults(raoInput.getCrac().getBranchCnecs(), raoResult.getPostOptimVariantId(), raoParameters.getObjectiveFunction().getUnit(), raoParameters.getObjectiveFunction().relativePositiveMargins(), NUMBER_LOGGED_ELEMENTS_END_RAO);
        return CompletableFuture.completedFuture(raoResult);
    }

    private PrePerimeterSensitivityAnalysisOutput prePerimeterSensitivityAnalysisOnAllPerimeters(RaoInput raoInput, RaoParameters parameters) {
        return new PrePerimeterSensitivityAnalysis(raoInput, raoInput.getCrac().getPreventiveState(), raoInput.getCrac().getStates(), parameters).run();
    }

    private CompletableFuture<RaoResultImpl> optimizePreventivePerimeter(RaoInput raoInput, RaoParameters raoParameters, PrePerimeterSensitivityAnalysisOutput prePerimeterSensitivityAnalysisOutput) {
        /*String baseVariantId = raoInput.getCrac().getExtension(ResultVariantManager.class).getInitialVariantId();
        preventiveRaoData = new RaoData(
            raoInput.getNetwork(),
            raoInput.getCrac(),
            raoInput.getCrac().getPreventiveState(),
            stateTree.getPerimeter(raoInput.getCrac().getPreventiveState()),
            raoInput.getReferenceProgram(),
            raoInput.getGlskProvider(),
            baseVariantId,
            parameters);
        preventiveRaoData.setSystematicSensitivityResult(prePerimeterSensitivityAnalysisOutput);*/

        TreeParameters preventiveTreeParameters = TreeParameters.buildForPreventivePerimeter(raoParameters.getExtension(SearchTreeRaoParameters.class));
        LinearOptimizerParameters linearOptimizerParameters = createLinearOptimizerParameters(raoParameters);

        SearchTreeInput searchTreeInput = buildSearchTreeInput(raoInput, raoInput.getCrac().getPreventiveState(), stateTree.getPerimeter(raoInput.getCrac().getPreventiveState()), prePerimeterSensitivityAnalysisOutput, raoParameters);

        return new SearchTree().run(searchTreeInput, raoParameters, preventiveTreeParameters, linearOptimizerParameters);
    }

    private Map<State, RaoResultImpl> optimizeCurativePerimeters(RaoInput raoInput, RaoParameters raoParameters, TreeParameters curativeTreeParameters, Network network, PrePerimeterSensitivityAnalysisOutput prePerimeterSensitivityAnalysisOutput) {
        String initialVariantId = raoInput.getCrac().getExtension(ResultVariantManager.class).getInitialVariantId();
        Map<State, RaoResultImpl> curativeResults = new ConcurrentHashMap<>();
        network.getVariantManager().setWorkingVariant(PREVENTIVE_STATE);
        network.getVariantManager().cloneVariant(PREVENTIVE_STATE, CURATIVE_STATE);
        network.getVariantManager().setWorkingVariant(CURATIVE_STATE);
        Map<String, String> initialVariantIdPerOptimizedStateId = new ConcurrentHashMap<>();
        /*stateTree.getOptimizedStates().forEach(optimizedState -> {
            if (!optimizedState.equals(raoInput.getCrac().getPreventiveState())) {
                initialVariantIdPerOptimizedStateId.put(optimizedState.getId(), preventiveRaoData.getCracVariantManager().cloneWorkingVariant());
            }
        });*/
        try (FaraoNetworkPool networkPool = new FaraoNetworkPool(network, CURATIVE_STATE, raoParameters.getPerimetersInParallel())) {
            stateTree.getOptimizedStates().forEach(optimizedState -> {
                if (!optimizedState.equals(raoInput.getCrac().getPreventiveState())) {
                    networkPool.submit(() -> {
                        try {
                            LOGGER.info("Optimizing curative state {}.", optimizedState.getId());
                            Network networkClone = networkPool.getAvailableNetwork();
                            /*RaoData curativeRaoData = new RaoData(
                                networkClone,
                                raoInput.getCrac(),
                                optimizedState,
                                stateTree.getPerimeter(optimizedState),
                                raoInput.getReferenceProgram(),
                                raoInput.getGlskProvider(),
                                initialVariantIdPerOptimizedStateId.get(optimizedState.getId()),
                                parameters);
                            curativeRaoData.getCracResultManager().copyAbsolutePtdfSumsBetweenVariants(initialVariantId, curativeRaoData.getWorkingVariantId());
                            curativeRaoData.getCracResultManager().copyAbsolutePtdfSumsBetweenVariants(initialVariantId, curativeRaoData.getCrac().getExtension(ResultVariantManager.class).getPrePerimeterVariantId());
                            if (!parameters.getLoopFlowApproximationLevel().shouldUpdatePtdfWithTopologicalChange()) {
                                curativeRaoData.getCracResultManager().copyCommercialFlowsBetweenVariants(initialVariantId, curativeRaoData.getWorkingVariantId());
                            }*/
                            Set<BranchCnec> cnecs = RaoUtil.computePerimeterCnecs(raoInput.getCrac(), stateTree.getPerimeter(optimizedState));
                            LinearOptimizerParameters linearOptimizerParameters = createLinearOptimizerParameters(raoParameters, stateTree, cnecs);

                            SearchTreeInput searchTreeInput = buildSearchTreeInput(raoInput, optimizedState, stateTree.getPerimeter(optimizedState), prePerimeterSensitivityAnalysisOutput, raoParameters);

                            RaoResultImpl curativeResult = new SearchTree().run(searchTreeInput, raoParameters, curativeTreeParameters, linearOptimizerParameters).join();
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

    RaoResultImpl mergeRaoResults(Crac crac, RaoResultImpl preventiveRaoResult, Map<State, RaoResultImpl> curativeRaoResults) {
        mergeRaoResultStatus(preventiveRaoResult, curativeRaoResults);
        mergeCnecResults(crac, preventiveRaoResult, curativeRaoResults);
        mergeRemedialActionsResults(crac, preventiveRaoResult, curativeRaoResults);
        mergeObjectiveFunctionValues(crac, preventiveRaoResult, curativeRaoResults);
        deleteCurativeVariants(crac, preventiveRaoResult.getPostOptimVariantId());
        return preventiveRaoResult;
    }

    private void mergeRaoResultStatus(RaoResultImpl preventiveRaoResult, Map<State, RaoResultImpl> curativeRaoResults) {
        if (curativeRaoResults.values().stream().anyMatch(curativeRaoResult -> curativeRaoResult.getStatus().equals(RaoResultImpl.Status.FAILURE))) {
            preventiveRaoResult.setStatus(RaoResultImpl.Status.FAILURE);
        }
    }

    private void mergeCnecResults(Crac crac, RaoResultImpl preventiveRaoResult, Map<State, RaoResultImpl> curativeRaoResults) {
        crac.getBranchCnecs().forEach(cnec -> {
            State optimizedState = stateTree.getOptimizedState(cnec.getState());
            if (!optimizedState.equals(crac.getPreventiveState())) {
                String optimizedVariantId = curativeRaoResults.get(optimizedState).getPostOptimVariantId();
                CnecResult optimizedCnecResult = ((FlowCnec) cnec).getExtension(CnecResultExtension.class).getVariant(optimizedVariantId);
                CnecResult targetResult = ((FlowCnec) cnec).getExtension(CnecResultExtension.class).getVariant(preventiveRaoResult.getPostOptimVariantId());
                targetResult.setAbsolutePtdfSum(optimizedCnecResult.getAbsolutePtdfSum());
                targetResult.setFlowInA(optimizedCnecResult.getFlowInA());
                targetResult.setFlowInMW(optimizedCnecResult.getFlowInMW());
                targetResult.setLoopflowInMW(optimizedCnecResult.getLoopflowInMW());
                targetResult.setLoopflowThresholdInMW(optimizedCnecResult.getLoopflowThresholdInMW());
                targetResult.setMaxThresholdInA(optimizedCnecResult.getMaxThresholdInA());
                targetResult.setMaxThresholdInMW(optimizedCnecResult.getMaxThresholdInMW());
                targetResult.setMinThresholdInA(optimizedCnecResult.getMinThresholdInA());
                targetResult.setMinThresholdInMW(optimizedCnecResult.getMinThresholdInMW());
                targetResult.setAbsolutePtdfSum(optimizedCnecResult.getAbsolutePtdfSum());
            }
        });
    }

    private void mergeRemedialActionsResults(Crac crac, RaoResultImpl preventiveRaoResult, Map<State, RaoResultImpl> curativeRaoResults) {
        stateTree.getOptimizedStates().forEach(optimizedState -> {
            if (!optimizedState.equals(crac.getPreventiveState())) {
                String optimizedVariantId = curativeRaoResults.get(optimizedState).getPostOptimVariantId();
                crac.getNetworkActions().forEach(networkAction -> {
                    NetworkActionResult naResult = networkAction.getExtension(NetworkActionResultExtension.class).getVariant(optimizedVariantId);
                    NetworkActionResult targetNaResult = networkAction.getExtension(NetworkActionResultExtension.class).getVariant(preventiveRaoResult.getPostOptimVariantId());
                    if (naResult.isActivated(optimizedState.getId())) {
                        targetNaResult.activate(optimizedState.getId());
                    }
                });
                crac.getRangeActions().forEach(rangeAction -> {
                    RangeActionResult raResult = rangeAction.getExtension(RangeActionResultExtension.class).getVariant(optimizedVariantId);
                    RangeActionResult targetRaResult = rangeAction.getExtension(RangeActionResultExtension.class).getVariant(preventiveRaoResult.getPostOptimVariantId());
                    stateTree.getPerimeter(optimizedState).forEach(state -> {
                        targetRaResult.setSetPoint(state.getId(), raResult.getSetPoint(state.getId()));
                        if (raResult instanceof PstRangeResult && targetRaResult instanceof PstRangeResult
                            && ((PstRangeResult) raResult).getTap(state.getId()) != null) {
                            ((PstRangeResult) targetRaResult).setTap(state.getId(), ((PstRangeResult) raResult).getTap(state.getId()));
                        }
                    });
                });
            }
        });
    }

    private void mergeObjectiveFunctionValues(Crac crac, RaoResultImpl preventiveRaoResult, Map<State, RaoResultImpl> curativeRaoResults) {
        // Save the objective function value of the "worst" perimeter (maximum obj function value)
        // Skip perimeters with pure MNECs as their functional cost can be 0 (artificial)
        CracResultExtension cracResultMap = crac.getExtension(CracResultExtension.class);
        List<Map.Entry<State, RaoResultImpl>> curativeCosts = curativeRaoResults.entrySet().stream()
                .filter(entry -> crac.getBranchCnecs(entry.getKey()).stream().anyMatch(Cnec::isOptimized))
                .sorted(Comparator.comparingDouble(entry -> -crac.getExtension(CracResultExtension.class).getVariant(entry.getValue().getPostOptimVariantId()).getCost()))
                .collect(Collectors.toList());
        if (curativeCosts.isEmpty()) {
            return;
        }
        RaoResultImpl worstCurativeRaoResult = curativeCosts.get(0).getValue();
        if (cracResultMap.getVariant(worstCurativeRaoResult.getPostOptimVariantId()).getCost() > cracResultMap.getVariant(preventiveRaoResult.getPostOptimVariantId()).getCost()) {
            cracResultMap.getVariant(preventiveRaoResult.getPostOptimVariantId()).setFunctionalCost(cracResultMap.getVariant(worstCurativeRaoResult.getPostOptimVariantId()).getFunctionalCost());
            cracResultMap.getVariant(preventiveRaoResult.getPostOptimVariantId()).setVirtualCost(cracResultMap.getVariant(worstCurativeRaoResult.getPostOptimVariantId()).getVirtualCost());
        }
    }

    private void deleteCurativeVariants(Crac crac, String postOptimVariantId) {
        ResultVariantManager resultVariantManager = crac.getExtension(ResultVariantManager.class);
        List<String> variantToDelete = resultVariantManager.getVariants().stream().
            filter(name -> !name.equals(resultVariantManager.getInitialVariantId())).
            filter(name -> !name.equals(postOptimVariantId)).
            collect(Collectors.toList());
        variantToDelete.forEach(variantId -> crac.getExtension(ResultVariantManager.class).deleteVariant(variantId));
    }

    private SearchTreeInput buildSearchTreeInput(RaoInput raoInput, State optimizedState, Set<State> perimeter, PrePerimeterSensitivityAnalysisOutput prePerimeterSensitivityAnalysisOutput, RaoParameters raoParameters) {
        Set<BranchCnec> cnecs = RaoUtil.computePerimeterCnecs(raoInput.getCrac(), perimeter);

        SearchTreeInput searchTreeInput = new SearchTreeInput();

        searchTreeInput.setNetwork(raoInput.getNetwork());
        searchTreeInput.setCnecs(cnecs);
        searchTreeInput.setNetworkActions(raoInput.getCrac().getNetworkActions(raoInput.getNetwork(), optimizedState, UsageMethod.AVAILABLE));
        searchTreeInput.setRangeActions(raoInput.getCrac().getRangeActions(raoInput.getNetwork(), optimizedState, UsageMethod.AVAILABLE));

        searchTreeInput.setLoopflowCnecs(LoopFlowUtil.computeLoopflowCnecs(cnecs, raoInput.getNetwork(), raoParameters));
        searchTreeInput.setGlskProvider(raoInput.getGlskProvider());
        searchTreeInput.setReferenceProgram(raoInput.getReferenceProgram());

        searchTreeInput.setInitialCnecResults(prePerimeterSensitivityAnalysisOutput.getCnecResults());
        searchTreeInput.setPrePerimeterSensitivityAndLoopflowResults(prePerimeterSensitivityAnalysisOutput.getSensitivityAndLoopflowResults());
        searchTreeInput.setPrePerimeterSetpoints(prePerimeterSensitivityAnalysisOutput.getOptimizedSetPoints());
        searchTreeInput.setPrePerimeterCommercialFlows(prePerimeterSensitivityAnalysisOutput.getCommercialFlows(MEGAWATT));
        Map<BranchCnec, Double> prePerimeterMarginsInAbsoluteMW = new HashMap<>();
        cnecs.forEach(cnec -> prePerimeterMarginsInAbsoluteMW.put(cnec, prePerimeterSensitivityAnalysisOutput.getMargin(cnec, MEGAWATT)));
        searchTreeInput.setPrePerimeterMarginsInAbsoluteMW(prePerimeterMarginsInAbsoluteMW);

        return searchTreeInput;
    }
}
