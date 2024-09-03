/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.CurativeOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.PreventiveOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.TreeParameters;
import com.powsybl.openrao.searchtreerao.commons.parameters.UnoptimizedCnecParameters;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.FailedRaoResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.OneStateOnlyRaoResultImpl;
import com.powsybl.openrao.searchtreerao.searchtree.algorithms.SearchTree;
import com.powsybl.openrao.searchtreerao.searchtree.inputs.SearchTreeInput;
import com.powsybl.openrao.searchtreerao.searchtree.parameters.SearchTreeParameters;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_LOGS;

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
                raoInput.getCrac().getRangeActions(raoInput.getOptimizedState(), UsageMethod.AVAILABLE),
                raoParameters,
                toolProvider);

        PrePerimeterResult initialResults;
        initialResults = prePerimeterSensitivityAnalysis.runInitialSensitivityAnalysis(raoInput.getNetwork(), raoInput.getCrac());
        if (initialResults.getSensitivityStatus() == ComputationStatus.FAILURE) {
            BUSINESS_LOGS.error("Initial sensitivity analysis failed");
            return CompletableFuture.completedFuture(new FailedRaoResultImpl("Initial sensitivity analysis failed"));
        }

        // run search-tree optimization, on the required preventive or curative state
        OptimizationPerimeter optPerimeter;
        TreeParameters treeParameters;
        Set<String> operatorsNotToOptimize = new HashSet<>();

        OptimizationResult optimizationResult;
        Set<FlowCnec> perimeterFlowCnecs;

        if (raoInput.getOptimizedState().getInstant().isAuto()) {
            perimeterFlowCnecs = raoInput.getCrac().getFlowCnecs(raoInput.getOptimizedState());
            // TODO: see how to handle multiple curative instants here
            State curativeState = raoInput.getCrac().getState(raoInput.getOptimizedState().getContingency().orElseThrow(), raoInput.getCrac().getInstant(InstantKind.CURATIVE));
            AutomatonSimulator automatonSimulator = new AutomatonSimulator(raoInput.getCrac(), raoParameters, toolProvider, initialResults, initialResults, stateTree.getOperatorsNotSharingCras(), 2);
            TreeParameters automatonTreeParameters = TreeParameters.buildForAutomatonPerimeter(raoParameters);
            optimizationResult = automatonSimulator.simulateAutomatonState(raoInput.getOptimizedState(), Set.of(curativeState), raoInput.getNetwork(), stateTree, automatonTreeParameters);
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
                    .withUnoptimizedCnecParameters(UnoptimizedCnecParameters.build(raoParameters.getNotOptimizedCnecsParameters(), stateTree.getOperatorsNotSharingCras()))
                    .build();
            SearchTreeInput searchTreeInput = SearchTreeInput.create()
                    .withNetwork(raoInput.getNetwork())
                    .withOptimizationPerimeter(optPerimeter)
                    .withInitialFlowResult(initialResults)
                    .withPrePerimeterResult(initialResults)
                    .withPreOptimizationAppliedNetworkActions(new AppliedRemedialActions()) //no remedial Action applied
                    .withObjectiveFunction(ObjectiveFunction.create().build(optPerimeter.getFlowCnecs(), optPerimeter.getLoopFlowCnecs(), initialResults, initialResults, operatorsNotToOptimize, raoParameters))
                    .withToolProvider(toolProvider)
                    .withOutageInstant(raoInput.getCrac().getOutageInstant())
                    .build();
            optimizationResult = new SearchTree(searchTreeInput, searchTreeParameters, true).run().join();

            // apply RAs and return results
            optimizationResult.getRangeActions().forEach(rangeAction -> rangeAction.apply(raoInput.getNetwork(), optimizationResult.getOptimizedSetpoint(rangeAction, raoInput.getOptimizedState())));
            optimizationResult.getActivatedNetworkActions().forEach(networkAction -> networkAction.apply(raoInput.getNetwork()));
        }

        return CompletableFuture.completedFuture(new OneStateOnlyRaoResultImpl(raoInput.getOptimizedState(), initialResults, optimizationResult, perimeterFlowCnecs));
    }
}
