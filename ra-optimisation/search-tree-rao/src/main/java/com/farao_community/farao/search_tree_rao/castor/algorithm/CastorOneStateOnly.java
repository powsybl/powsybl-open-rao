/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.castor.algorithm;

import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.rao_api.RaoInput;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.commons.RaoUtil;
import com.farao_community.farao.search_tree_rao.commons.ToolProvider;
import com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator.ObjectiveFunction;
import com.farao_community.farao.search_tree_rao.commons.optimization_perimeters.CurativeOptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.commons.optimization_perimeters.OptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.commons.optimization_perimeters.PreventiveOptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.commons.parameters.TreeParameters;
import com.farao_community.farao.search_tree_rao.commons.parameters.UnoptimizedCnecParameters;
import com.farao_community.farao.search_tree_rao.result.api.OptimizationResult;
import com.farao_community.farao.search_tree_rao.result.api.PrePerimeterResult;
import com.farao_community.farao.search_tree_rao.result.impl.FailedRaoResultImpl;
import com.farao_community.farao.search_tree_rao.result.impl.OneStateOnlyRaoResultImpl;
import com.farao_community.farao.search_tree_rao.search_tree.algorithms.SearchTree;
import com.farao_community.farao.search_tree_rao.search_tree.inputs.SearchTreeInput;
import com.farao_community.farao.search_tree_rao.search_tree.parameters.SearchTreeParameters;
import com.farao_community.farao.sensitivity_analysis.AppliedRemedialActions;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.farao_community.farao.commons.logs.FaraoLoggerProvider.BUSINESS_LOGS;

/**
 * Flow controller to compute a RAO taking into account only the cnecs and range actions
 * on a given state.
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Godelaine De-Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CastorOneStateOnly {

    private final RaoInput raoInput;
    private final RaoParameters raoParameters;

    public CastorOneStateOnly(RaoInput raoInput, RaoParameters raoParameters) {
        this.raoInput = raoInput;
        this.raoParameters = raoParameters;
    }

    public CompletableFuture<RaoResult> run() {

        RaoUtil.initData(raoInput, raoParameters);
        StateTree stateTree = new StateTree(raoInput.getCrac());
        ToolProvider toolProvider = ToolProvider.buildFromRaoInputAndParameters(raoInput, raoParameters);

        // compute initial sensitivity on CNECs of the only optimized state
        PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(
                raoInput.getCrac().getFlowCnecs(raoInput.getOptimizedState()),
                raoInput.getCrac().getRangeActions(raoInput.getOptimizedState(), UsageMethod.AVAILABLE, UsageMethod.TO_BE_EVALUATED),
                raoParameters,
                toolProvider);

        PrePerimeterResult initialResults;
        initialResults = prePerimeterSensitivityAnalysis.runInitialSensitivityAnalysis(raoInput.getNetwork(), raoInput.getCrac());
        if (initialResults.getSensitivityStatus() == ComputationStatus.FAILURE) {
            BUSINESS_LOGS.error("Initial sensitivity analysis failed");
            return CompletableFuture.completedFuture(new FailedRaoResultImpl());
        }

        // run search-tree optimization, on the required preventive or curative state
        OptimizationPerimeter optPerimeter;
        TreeParameters treeParameters;
        Set<String> operatorsNotToOptimize = new HashSet<>();

        OptimizationResult optimizationResult;
        Set<FlowCnec> perimeterFlowCnecs;

        if (raoInput.getOptimizedState().getInstant().getInstantKind().equals(InstantKind.AUTO)) {
            perimeterFlowCnecs = raoInput.getCrac().getFlowCnecs(raoInput.getOptimizedState());
            String curativeInstantId = raoInput.getCrac().getInstant(InstantKind.CURATIVE).getId();
            State curativeState = raoInput.getCrac().getState(raoInput.getOptimizedState().getContingency().orElseThrow().getId(), curativeInstantId);
            AutomatonSimulator automatonSimulator = new AutomatonSimulator(raoInput.getCrac(), raoParameters, toolProvider, initialResults, initialResults, initialResults, stateTree.getOperatorsNotSharingCras(), 2);
            optimizationResult = automatonSimulator.simulateAutomatonState(raoInput.getOptimizedState(), curativeState, raoInput.getNetwork());
        } else {
            if (raoInput.getOptimizedState().equals(raoInput.getCrac().getPreventiveState())) {
                optPerimeter = PreventiveOptimizationPerimeter.buildWithPreventiveCnecsOnly(raoInput.getCrac(), raoInput.getNetwork(), raoParameters, initialResults);
                treeParameters = TreeParameters.buildForPreventivePerimeter(raoParameters);
            } else {
                optPerimeter = CurativeOptimizationPerimeter.build(raoInput.getOptimizedState(), raoInput.getCrac(), raoInput.getNetwork(), raoParameters, initialResults);
                treeParameters = TreeParameters.buildForCurativePerimeter(raoParameters, -Double.MAX_VALUE);
                operatorsNotToOptimize.addAll(stateTree.getOperatorsNotSharingCras());
            }
            perimeterFlowCnecs = optPerimeter.getFlowCnecs();
            SearchTreeParameters searchTreeParameters = SearchTreeParameters.create()
                    .withConstantParametersOverAllRao(raoParameters, raoInput.getCrac())
                    .withTreeParameters(treeParameters)
                    .withUnoptimizedCnecParameters(UnoptimizedCnecParameters.build(raoParameters.getNotOptimizedCnecsParameters(), stateTree.getOperatorsNotSharingCras(), raoInput.getCrac()))
                    .build();
            SearchTreeInput searchTreeInput = SearchTreeInput.create()
                    .withNetwork(raoInput.getNetwork())
                    .withOptimizationPerimeter(optPerimeter)
                    .withInitialFlowResult(initialResults)
                    .withPrePerimeterResult(initialResults)
                    .withPreOptimizationAppliedNetworkActions(new AppliedRemedialActions()) //no remedial Action applied
                    .withObjectiveFunction(ObjectiveFunction.create().build(optPerimeter.getFlowCnecs(), optPerimeter.getLoopFlowCnecs(), initialResults, initialResults, initialResults, raoInput.getCrac(), operatorsNotToOptimize, raoParameters))
                    .withToolProvider(toolProvider)
                    .build();
            optimizationResult = new SearchTree(searchTreeInput, searchTreeParameters, true).run(raoInput.getCrac().getInstant(InstantKind.OUTAGE)).join();

            // apply RAs and return results
            optimizationResult.getRangeActions().forEach(rangeAction -> rangeAction.apply(raoInput.getNetwork(), optimizationResult.getOptimizedSetpoint(rangeAction, raoInput.getOptimizedState())));
            optimizationResult.getActivatedNetworkActions().forEach(networkAction -> networkAction.apply(raoInput.getNetwork()));
        }

        return CompletableFuture.completedFuture(new OneStateOnlyRaoResultImpl(raoInput.getOptimizedState(), initialResults, optimizationResult, perimeterFlowCnecs));
    }
}
