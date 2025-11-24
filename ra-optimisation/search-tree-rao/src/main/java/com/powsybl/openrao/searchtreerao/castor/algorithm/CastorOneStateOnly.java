/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.CurativeOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.PreventiveOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.TreeParameters;
import com.powsybl.openrao.searchtreerao.commons.parameters.UnoptimizedCnecParameters;
import com.powsybl.openrao.searchtreerao.reports.CastorReports;
import com.powsybl.openrao.searchtreerao.reports.CommonReports;
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

import static com.powsybl.openrao.searchtreerao.commons.HvdcUtils.getHvdcRangeActionsOnHvdcLineInAcEmulation;

/**
 * Flow controller to compute a RAO taking into account only the cnecs and range actions
 * on a given state.
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CastorOneStateOnly {

    private final RaoInput raoInput;
    private final RaoParameters raoParameters;
    private final ReportNode reportNode;

    public CastorOneStateOnly(final RaoInput raoInput, final RaoParameters raoParameters, final ReportNode reportNode) {
        this.raoInput = raoInput;
        this.raoParameters = raoParameters;
        this.reportNode = reportNode;
    }

    public CompletableFuture<RaoResult> run() {
        final ReportNode optimizationReportNode = CastorReports.reportCastorOneStateOnly(reportNode);

        RaoUtil.initData(raoInput, raoParameters, reportNode);
        StateTree stateTree = new StateTree(raoInput.getCrac(), optimizationReportNode);
        ToolProvider toolProvider = ToolProvider.buildFromRaoInputAndParameters(raoInput, raoParameters);

        // compute initial sensitivity on CNECs of the only optimized state
        PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(
            raoInput.getCrac(),
            raoInput.getCrac().getFlowCnecs(raoInput.getOptimizedState()),
            raoInput.getCrac().getRangeActions(raoInput.getOptimizedState()),
            raoParameters,
            toolProvider);

        PrePerimeterResult initialResults;
        initialResults = prePerimeterSensitivityAnalysis.runInitialSensitivityAnalysis(raoInput.getNetwork(), Set.of(raoInput.getOptimizedState()), optimizationReportNode);
        if (initialResults.getSensitivityStatus() == ComputationStatus.FAILURE) {
            CommonReports.reportInitialSensitivityAnalysisFailed(optimizationReportNode);
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
            AutomatonSimulator automatonSimulator = new AutomatonSimulator(raoInput.getCrac(), raoParameters, toolProvider, initialResults, initialResults, stateTree.getOperatorsNotSharingCras(), 2, optimizationReportNode);
            optimizationResult = automatonSimulator.simulateAutomatonState(raoInput.getOptimizedState(), Set.of(curativeState), raoInput.getNetwork());
        } else {
            if (raoInput.getOptimizedState().equals(raoInput.getCrac().getPreventiveState())) {
                optPerimeter = PreventiveOptimizationPerimeter.buildWithPreventiveCnecsOnly(raoInput.getCrac(), raoInput.getNetwork(), raoParameters, initialResults, optimizationReportNode);
                treeParameters = TreeParameters.buildForPreventivePerimeter(raoParameters);
            } else {
                optPerimeter = CurativeOptimizationPerimeter.build(raoInput.getOptimizedState(), raoInput.getCrac(), raoInput.getNetwork(), raoParameters, initialResults, optimizationReportNode);
                treeParameters = TreeParameters.buildForCurativePerimeter(raoParameters, -Double.MAX_VALUE);
                operatorsNotToOptimize.addAll(stateTree.getOperatorsNotSharingCras());
            }
            perimeterFlowCnecs = optPerimeter.getFlowCnecs();

            SearchTreeParameters.SearchTreeParametersBuilder searchTreeParametersBuilder = SearchTreeParameters.create(reportNode)
                .withConstantParametersOverAllRao(raoParameters, raoInput.getCrac())
                .withTreeParameters(treeParameters)
                .withUnoptimizedCnecParameters(UnoptimizedCnecParameters.build(raoParameters.getNotOptimizedCnecsParameters(), stateTree.getOperatorsNotSharingCras()));

            if (!getHvdcRangeActionsOnHvdcLineInAcEmulation(raoInput.getCrac().getHvdcRangeActions(), raoInput.getNetwork()).isEmpty()) {
                LoadFlowAndSensitivityParameters loadFlowAndSensitivityParameters =
                    raoParameters.hasExtension(OpenRaoSearchTreeParameters.class)
                        ? raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getLoadFlowAndSensitivityParameters()
                        : new LoadFlowAndSensitivityParameters(reportNode);
                searchTreeParametersBuilder.withLoadFlowAndSensitivityParameters(loadFlowAndSensitivityParameters);
            }

            SearchTreeParameters searchTreeParameters = searchTreeParametersBuilder.build();

            Set<State> statesToOptimize = new HashSet<>(optPerimeter.getMonitoredStates());
            statesToOptimize.add(optPerimeter.getMainOptimizationState());
            SearchTreeInput searchTreeInput = SearchTreeInput.create()
                    .withNetwork(raoInput.getNetwork())
                    .withOptimizationPerimeter(optPerimeter)
                    .withInitialFlowResult(initialResults)
                    .withPrePerimeterResult(initialResults)
                    .withPreOptimizationAppliedNetworkActions(new AppliedRemedialActions()) //no remedial Action applied
                    .withObjectiveFunction(ObjectiveFunction.build(optPerimeter.getFlowCnecs(), optPerimeter.getLoopFlowCnecs(), initialResults, initialResults, operatorsNotToOptimize, raoParameters, statesToOptimize))
                    .withToolProvider(toolProvider)
                    .withOutageInstant(raoInput.getCrac().getOutageInstant())
                    .build();
            optimizationResult = new SearchTree(searchTreeInput, searchTreeParameters, true, optimizationReportNode).run().join();

            // apply RAs and return results
            // network actions need to be applied BEFORE range actions because to apply HVDC range actions we need to apply AC emulation deactivation network actions beforehand
            optimizationResult.getActivatedNetworkActions().forEach(networkAction -> networkAction.apply(raoInput.getNetwork()));
            optimizationResult.getRangeActions().forEach(rangeAction -> rangeAction.apply(raoInput.getNetwork(), optimizationResult.getOptimizedSetpoint(rangeAction, raoInput.getOptimizedState())));
        }

        return CompletableFuture.completedFuture(new OneStateOnlyRaoResultImpl(raoInput.getOptimizedState(), initialResults, optimizationResult, perimeterFlowCnecs));
    }
}
