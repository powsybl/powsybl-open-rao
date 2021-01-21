/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_result_extensions.*;
import com.farao_community.farao.rao_api.*;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.RaoUtil;
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

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@AutoService(RaoProvider.class)
public class SearchTreeRaoProvider implements RaoProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchTreeRaoProvider.class);
    private static final String SEARCH_TREE_RAO = "SearchTreeRao";
    private static final String PREVENTIVE_STATE = "PreventiveState";
    private static final String CURATIVE_STATE = "CurativeState";
    private RaoData preventiveRaoData;

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

        if (raoInput.getOptimizedState() != null) {
            RaoData raoData = new RaoData(
                    raoInput.getNetwork(),
                    raoInput.getCrac(),
                    raoInput.getOptimizedState(),
                    raoInput.getPerimeter(),
                    raoInput.getReferenceProgram(),
                    raoInput.getGlskProvider(),
                    raoInput.getBaseCracVariantId(),
                    parameters.getLoopflowCountries());
            TreeParameters treeParameters = TreeParameters.buildForPreventivePerimeter(parameters.getExtension(SearchTreeRaoParameters.class));
            return CompletableFuture.completedFuture(new SearchTree().run(raoData, parameters, treeParameters).join());
        }

        stateTree = new StateTree(raoInput.getCrac(), raoInput.getNetwork(), raoInput.getCrac().getPreventiveState());

        if (stateTree.getOptimizedStates().size() == 1) {
            // only preventive optimization
            return optimizePreventivePerimeter(raoInput, parameters);
        } else {
            // optimize preventive perimeter then all curative perimeters
            Network network = raoInput.getNetwork();
            network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), PREVENTIVE_STATE);
            network.getVariantManager().setWorkingVariant(PREVENTIVE_STATE);

            RaoResult preventiveRaoResult = optimizePreventivePerimeter(raoInput, parameters).join();
            LOGGER.info("Preventive perimeter has been optimized.");

            double preventiveOptimalCost = raoInput.getCrac().getExtension(CracResultExtension.class).getVariant(preventiveRaoResult.getPostOptimVariantId()).getCost();
            TreeParameters curativeTreeParameters = TreeParameters.buildForCurativePerimeter(parameters.getExtension(SearchTreeRaoParameters.class), preventiveOptimalCost);
            applyPreventiveRemedialActions(raoInput.getNetwork(), raoInput.getCrac(), preventiveRaoResult.getPostOptimVariantId());
            Map<State, RaoResult> curativeResults = optimizeCurativePerimeters(raoInput, parameters, curativeTreeParameters, network);

            LOGGER.info("Merging preventive and curative RAO results.");
            RaoResult mergedRaoResults = mergeRaoResults(raoInput.getCrac(), preventiveRaoResult, curativeResults);
            // For logs only
            if (mergedRaoResults.isSuccessful()) {
                boolean relativePositiveMargins =
                        parameters.getObjectiveFunction().equals(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE) ||
                                parameters.getObjectiveFunction().equals(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT);
                SearchTreeRaoLogger.logMostLimitingElementsResults(raoInput.getCrac().getBranchCnecs(), mergedRaoResults.getPostOptimVariantId(), parameters.getObjectiveFunction().getUnit(), relativePositiveMargins);
            }
            return CompletableFuture.completedFuture(mergedRaoResults);
        }
    }

    private CompletableFuture<RaoResult> optimizePreventivePerimeter(RaoInput raoInput, RaoParameters parameters) {
        preventiveRaoData = new RaoData(
                raoInput.getNetwork(),
                raoInput.getCrac(),
                raoInput.getCrac().getPreventiveState(),
                stateTree.getPerimeter(raoInput.getCrac().getPreventiveState()),
                raoInput.getReferenceProgram(),
                raoInput.getGlskProvider(),
                raoInput.getBaseCracVariantId(),
                parameters.getLoopflowCountries());
        TreeParameters preventiveTreeParameters = TreeParameters.buildForPreventivePerimeter(parameters.getExtension(SearchTreeRaoParameters.class));
        return new SearchTree().run(preventiveRaoData, parameters, preventiveTreeParameters);
    }

    private Map<State, RaoResult> optimizeCurativePerimeters(RaoInput raoInput, RaoParameters parameters, TreeParameters curativeTreeParameters, Network network) {
        Map<State, RaoResult> curativeResults = new ConcurrentHashMap<>();
        network.getVariantManager().setWorkingVariant(PREVENTIVE_STATE);
        network.getVariantManager().cloneVariant(PREVENTIVE_STATE, CURATIVE_STATE);
        network.getVariantManager().setWorkingVariant(CURATIVE_STATE);
        Map<String, String> initialVariantIdPerOptimizedStateId = new ConcurrentHashMap<>();
        stateTree.getOptimizedStates().forEach(optimizedState -> {
            if (!optimizedState.equals(raoInput.getCrac().getPreventiveState())) {
                initialVariantIdPerOptimizedStateId.put(optimizedState.getId(), preventiveRaoData.getCracVariantManager().cloneWorkingVariant());
            }
        });
        try (FaraoNetworkPool networkPool = new FaraoNetworkPool(network, CURATIVE_STATE, parameters.getPerimetersInParallel())) {
            stateTree.getOptimizedStates().forEach(optimizedState -> {
                if (!optimizedState.equals(raoInput.getCrac().getPreventiveState())) {
                    networkPool.submit(() -> {
                        try {
                            LOGGER.info("Optimizing curative state {}.", optimizedState.getId());
                            Network networkClone = networkPool.getAvailableNetwork();
                            RaoData curativeRaoData = new RaoData(
                                    networkClone,
                                    raoInput.getCrac(),
                                    optimizedState,
                                    stateTree.getPerimeter(optimizedState),
                                    raoInput.getReferenceProgram(),
                                    raoInput.getGlskProvider(),
                                    initialVariantIdPerOptimizedStateId.get(optimizedState.getId()),
                                    parameters.getLoopflowCountries());
                            RaoResult curativeResult = new SearchTree().run(curativeRaoData, parameters, curativeTreeParameters).join();
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

    private static void applyPreventiveRemedialActions(Network network, Crac crac, String cracVariantId) {
        String preventiveStateId = crac.getPreventiveState().getId();
        crac.getNetworkActions().forEach(na -> applyNetworkAction(na, network, cracVariantId, preventiveStateId));
        crac.getRangeActions().forEach(ra -> applyRangeAction(ra, network, cracVariantId, preventiveStateId));
    }

    private static void applyNetworkAction(NetworkAction networkAction, Network network, String cracVariantId, String preventiveStateId) {
        NetworkActionResultExtension resultExtension = networkAction.getExtension(NetworkActionResultExtension.class);
        if (resultExtension == null) {
            LOGGER.error("Could not find results on network action {}", networkAction.getId());
        } else {
            NetworkActionResult networkActionResult = resultExtension.getVariant(cracVariantId);
            if (networkActionResult != null) {
                if (networkActionResult.isActivated(preventiveStateId)) {
                    LOGGER.debug("Applying network action {}", networkAction.getName());
                    networkAction.apply(network);
                }
            } else {
                LOGGER.error("Could not find results for variant {} on network action {}", cracVariantId, networkAction.getId());
            }
        }
    }

    private static void applyRangeAction(RangeAction rangeAction, Network network, String cracVariantId, String preventiveStateId) {
        RangeActionResultExtension resultExtension = rangeAction.getExtension(RangeActionResultExtension.class);
        if (resultExtension == null) {
            LOGGER.error("Could not find results on range action {}", rangeAction.getId());
        } else {
            RangeActionResult rangeActionResult = resultExtension.getVariant(cracVariantId);
            if (rangeActionResult != null) {
                if (!Double.isNaN(rangeActionResult.getSetPoint(preventiveStateId))) {
                    LOGGER.debug("Applying range action {}: tap {}", rangeAction.getName(), ((PstRangeResult) rangeActionResult).getTap(preventiveStateId));
                }
                rangeAction.apply(network, rangeActionResult.getSetPoint(preventiveStateId));
            } else {
                LOGGER.error("Could not find results for variant {} on range action {}", cracVariantId, rangeAction.getId());
            }
        }
    }

    RaoResult mergeRaoResults(Crac crac, RaoResult preventiveRaoResult, Map<State, RaoResult> curativeRaoResults) {
        mergeRaoResultStatus(preventiveRaoResult, curativeRaoResults);
        mergeCnecResults(crac, preventiveRaoResult, curativeRaoResults);
        mergeRemedialActionsResults(crac, preventiveRaoResult, curativeRaoResults);
        deleteCurativeVariants(crac, preventiveRaoResult.getPostOptimVariantId());
        return preventiveRaoResult;
    }

    private void mergeRaoResultStatus(RaoResult preventiveRaoResult, Map<State, RaoResult> curativeRaoResults) {
        if (curativeRaoResults.values().stream().anyMatch(curativeRaoResult -> curativeRaoResult.getStatus().equals(RaoResult.Status.FAILURE))) {
            preventiveRaoResult.setStatus(RaoResult.Status.FAILURE);
        }
    }

    private void mergeCnecResults(Crac crac, RaoResult preventiveRaoResult, Map<State, RaoResult> curativeRaoResults) {
        crac.getBranchCnecs().forEach(cnec -> {
            State optimizedState = stateTree.getOptimizedState(cnec.getState());
            if (!optimizedState.equals(crac.getPreventiveState())) {
                String optimizedVariantId = curativeRaoResults.get(optimizedState).getPostOptimVariantId();
                CnecResult optimizedCnecResult = cnec.getExtension(CnecResultExtension.class).getVariant(optimizedVariantId);
                CnecResult targetResult = cnec.getExtension(CnecResultExtension.class).getVariant(preventiveRaoResult.getPostOptimVariantId());
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

    private void mergeRemedialActionsResults(Crac crac, RaoResult preventiveRaoResult, Map<State, RaoResult> curativeRaoResults) {
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

    private void deleteCurativeVariants(Crac crac, String postOptimVariantId) {

        ResultVariantManager resultVariantManager = crac.getExtension(ResultVariantManager.class);

        List<String> variantToDelete = resultVariantManager.getVariants().stream().
            filter(name -> !name.equals(resultVariantManager.getInitialVariantId())).
            filter(name -> !name.equals(postOptimVariantId)).
            collect(Collectors.toList());

        variantToDelete.forEach(variantId -> crac.getExtension(ResultVariantManager.class).deleteVariant(variantId));
    }
}
