/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms;

import com.google.auto.service.AutoService;
import com.google.ortools.Loader;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.RaUsageLimits;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.RaoProvider;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.castor.algorithm.PrePerimeterSensitivityAnalysis;
import com.powsybl.openrao.searchtreerao.castor.algorithm.StateTree;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.GlobalOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.RangeActionLimitationParameters;
import com.powsybl.openrao.searchtreerao.commons.parameters.TreeParameters;
import com.powsybl.openrao.searchtreerao.commons.parameters.UnoptimizedCnecParameters;
import com.powsybl.openrao.searchtreerao.linearoptimisation.inputs.IteratingLinearOptimizerInput;
import com.powsybl.openrao.searchtreerao.linearoptimisation.parameters.IteratingLinearOptimizerParameters;
import com.powsybl.openrao.searchtreerao.reports.CommonReports;
import com.powsybl.openrao.searchtreerao.reports.LinearRaoReports;
import com.powsybl.openrao.searchtreerao.result.api.LinearOptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.FailedRaoResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.NetworkActionsResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.OptimizationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.PostPerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.PreventiveAndCurativesRaoResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RemedialActionActivationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.UnoptimizedRaoResultImpl;
import com.powsybl.openrao.searchtreerao.searchtree.parameters.SearchTreeParameters;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

/**
 * RAO provider running a single global linear optimization of all the range actions of the CRAC
 * (preventive and curative).
 *
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
@AutoService(RaoProvider.class)
public class LinearRao implements RaoProvider {

    static {
        try {
            Loader.loadNativeLibraries();
        } catch (UnsatisfiedLinkError e) {
            TECHNICAL_LOGS.error("Native library jniortools could not be loaded. You can ignore this message if it is not needed.");
        }
    }

    private static final String LINEAR_RAO = "LinearRao";
    private static final String LINEAR_RAO_VARIANT = "LinearRaoVariant";
    private static final int NUMBER_LOGGED_ELEMENTS_DURING_RAO = 2;
    private static final int NUMBER_LOGGED_ELEMENTS_END_RAO = 10;

    // Do not store any big object in this class as it is a static RaoProvider
    // Objects stored in memory will not be released at the end of the RAO run

    @Override
    public String getName() {
        return LINEAR_RAO;
    }

    @Override
    public CompletableFuture<RaoResult> run(final RaoInput raoInput, final RaoParameters parameters, final ReportNode reportNode) {
        return run(raoInput, parameters, null, reportNode);
    }

    @Override
    public CompletableFuture<RaoResult> run(final RaoInput raoInput, final RaoParameters parameters, final Instant targetEndInstant, final ReportNode reportNode) {
        try {
            RaoUtil.initData(raoInput, parameters, reportNode);
        } catch (OpenRaoException e) {
            String failure = String.format("Data initialisation failed: %s", e);
            CommonReports.reportExceptionMessage(reportNode, failure);
            return CompletableFuture.completedFuture(new FailedRaoResultImpl(failure));
        }

        warnIfNetworkActionsInCrac(raoInput.getCrac(), reportNode);

        Network network = raoInput.getNetwork();
        String initialVariantId = network.getVariantManager().getWorkingVariantId();
        try {
            return CompletableFuture.completedFuture(optimize(raoInput, parameters, initialVariantId, reportNode));
        } catch (Exception e) {
            String failure = String.format("Linear RAO failed: %s", e.getMessage());
            TECHNICAL_LOGS.error(failure, e);
            return CompletableFuture.completedFuture(new FailedRaoResultImpl(failure));
        } finally {
            network.getVariantManager().setWorkingVariant(initialVariantId);
            if (network.getVariantManager().getVariantIds().contains(LINEAR_RAO_VARIANT)) {
                network.getVariantManager().removeVariant(LINEAR_RAO_VARIANT);
            }
        }
    }

    static void warnIfNetworkActionsInCrac(Crac crac, ReportNode reportNode) {
        if (!crac.getNetworkActions().isEmpty()) {
            LinearRaoReports.reportLinearRaoNetworkActionsIgnored(reportNode, crac.getNetworkActions().size());
        }
    }

    private static RaoResult optimize(RaoInput raoInput, RaoParameters raoParameters, String initialVariantId, ReportNode reportNode) {
        Crac crac = raoInput.getCrac();
        Network network = raoInput.getNetwork();
        ToolProvider toolProvider = ToolProvider.buildFromRaoInputAndParameters(raoInput, raoParameters);

        // ----- INITIAL SENSI -----
        PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(
            crac, crac.getFlowCnecs(), crac.getRangeActions(), raoParameters, toolProvider, true);
        PrePerimeterResult initialResult = prePerimeterSensitivityAnalysis.runInitialSensitivityAnalysis(network, reportNode);
        if (crac.getFlowCnecs().isEmpty()) {
            return new UnoptimizedRaoResultImpl(initialResult);
        }
        if (initialResult.getSensitivityStatus() == ComputationStatus.FAILURE) {
            CommonReports.reportInitialSensitivityAnalysisFailed(reportNode);
            return new FailedRaoResultImpl("Initial sensitivity analysis failed");
        }
        LinearRaoReports.reportLinearRaoInitialSensitivityAnalysisResults(reportNode,
            prePerimeterSensitivityAnalysis.getObjectiveFunction(),
            RemedialActionActivationResultImpl.empty(initialResult),
            initialResult,
            raoParameters,
            NUMBER_LOGGED_ELEMENTS_DURING_RAO);

        StateTree stateTree = new StateTree(crac, reportNode);

        // ----- GLOBAL LINEAR OPTIMIZATION -----
        ReportNode optimizationReportNode = LinearRaoReports.reportLinearRaoGlobalOptimization(reportNode);
        network.getVariantManager().cloneVariant(initialVariantId, LINEAR_RAO_VARIANT, true);
        network.getVariantManager().setWorkingVariant(LINEAR_RAO_VARIANT);

        OptimizationPerimeter perimeter = GlobalOptimizationPerimeter.build(crac, network, raoParameters, initialResult, reportNode)
            .copyWithFilteredAvailableHvdcRangeAction(network);

        Set<State> statesToOptimize = new HashSet<>(perimeter.getMonitoredStates());
        statesToOptimize.add(perimeter.getMainOptimizationState());
        ObjectiveFunction objectiveFunction = ObjectiveFunction.build(
            perimeter.getFlowCnecs(), perimeter.getLoopFlowCnecs(), initialResult, initialResult, new HashSet<>(), raoParameters, statesToOptimize);

        SearchTreeParameters searchTreeParameters = SearchTreeParameters.create(reportNode)
            .withConstantParametersOverAllRao(raoParameters, crac)
            .withTreeParameters(TreeParameters.buildForSecondPreventivePerimeter(raoParameters))
            .withUnoptimizedCnecParameters(UnoptimizedCnecParameters.build(raoParameters.getNotOptimizedCnecsParameters(), stateTree.getOperatorsNotSharingCras()))
            .build();

        IteratingLinearOptimizerInput linearOptimizerInput = IteratingLinearOptimizerInput.create()
            .withNetwork(network)
            .withOptimizationPerimeter(perimeter)
            .withInitialFlowResult(initialResult)
            .withPrePerimeterFlowResult(initialResult)
            .withPrePerimeterSetpoints(initialResult)
            .withPreOptimizationFlowResult(initialResult)
            .withPreOptimizationSensitivityResult(initialResult)
            .withPreOptimizationAppliedRemedialActions(new AppliedRemedialActions())
            .withRaActivationFromParentLeaf(new RangeActionActivationResultImpl(initialResult))
            .withAppliedNetworkActionsInPrimaryState(new NetworkActionsResultImpl(Map.of(perimeter.getMainOptimizationState(), Set.of())))
            .withObjectiveFunction(objectiveFunction)
            .withToolProvider(toolProvider)
            .withOutageInstant(crac.getOutageInstant())
            .build();

        IteratingLinearOptimizerParameters linearOptimizerParameters = IteratingLinearOptimizerParameters.create()
            .withObjectiveFunction(searchTreeParameters.getObjectiveFunction())
            .withFlowUnit(searchTreeParameters.getFlowUnit())
            .withRangeActionParameters(searchTreeParameters.getRangeActionParameters())
            .withRangeActionParametersExtension(searchTreeParameters.getRangeActionParametersExtension())
            .withMnecParameters(searchTreeParameters.getMnecParameters())
            .withMnecParametersExtension(searchTreeParameters.getMnecParametersExtension())
            .withMaxMinRelativeMarginParameters(searchTreeParameters.getMaxMinRelativeMarginParameters())
            .withMinMarginParameters(searchTreeParameters.getMaxMinMarginsParameters())
            .withLoopFlowParameters(searchTreeParameters.getLoopFlowParameters())
            .withLoopFlowParametersExtension(searchTreeParameters.getLoopFlowParametersExtension())
            .withUnoptimizedCnecParameters(searchTreeParameters.getUnoptimizedCnecParameters())
            .withRaLimitationParameters(buildRaLimitationParameters(perimeter, searchTreeParameters))
            .withSolverParameters(searchTreeParameters.getSolverParameters())
            .withMaxNumberOfIterations(searchTreeParameters.getMaxNumberOfIterations())
            .withRaRangeShrinking(searchTreeParameters.getTreeParameters().raRangeShrinking())
            .build();

        LinearOptimizationResult linearResult = IteratingLinearOptimizer.optimize(linearOptimizerInput, linearOptimizerParameters, optimizationReportNode);
        OptimizationResult optimizationResult = new OptimizationResultImpl(
            linearResult, linearResult, linearResult, new NetworkActionsResultImpl(new HashMap<>()), linearResult.getRangeActionActivationResult());

        // ----- FINAL SENSI -----
        // apply preventive range actions on the network and gather post-contingency ones
        network.getVariantManager().cloneVariant(initialVariantId, LINEAR_RAO_VARIANT, true);
        network.getVariantManager().setWorkingVariant(LINEAR_RAO_VARIANT);
        AppliedRemedialActions appliedPostContingencyRangeActions = IteratingLinearOptimizer.applyRangeActions(linearResult.getRangeActionActivationResult(), linearOptimizerInput);
        PrePerimeterResult finalResult = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(
            network, initialResult, stateTree.getOperatorsNotSharingCras(), appliedPostContingencyRangeActions, reportNode);
        if (finalResult.getSensitivityStatus() == ComputationStatus.FAILURE) {
            return new FailedRaoResultImpl("Systematic sensitivity analysis after linear optimization failed");
        }
        LinearRaoReports.reportLinearRaoActivatedRangeActions(reportNode, linearResult.getRangeActionActivationResult());
        LinearRaoReports.reportLinearRaoFinalResult(reportNode, finalResult, raoParameters, NUMBER_LOGGED_ELEMENTS_END_RAO);

        PostPerimeterResult postPerimeterResult = new PostPerimeterResult(optimizationResult, finalResult);
        Map<State, PostPerimeterResult> postContingencyResults = new HashMap<>();
        perimeter.getRangeActionOptimizationStates().stream()
            .filter(state -> !state.isPreventive())
            .forEach(state -> postContingencyResults.put(state, postPerimeterResult));

        return new PreventiveAndCurativesRaoResultImpl(stateTree, initialResult, postPerimeterResult, postPerimeterResult,
            postContingencyResults, crac, raoParameters, reportNode);
    }

    private static RangeActionLimitationParameters buildRaLimitationParameters(OptimizationPerimeter perimeter, SearchTreeParameters parameters) {
        RangeActionLimitationParameters limitationParameters = new RangeActionLimitationParameters();
        for (State state : perimeter.getRangeActionOptimizationStates()) {
            RaUsageLimits raUsageLimits = parameters.getRaLimitationParameters().get(state.getInstant());
            if (raUsageLimits != null) {
                limitationParameters.setMaxRangeAction(state, raUsageLimits.getMaxRa());
                limitationParameters.setMaxPstPerTso(state, raUsageLimits.getMaxPstPerTso());
                limitationParameters.setMaxRangeActionPerTso(state, raUsageLimits.getMaxRaPerTso());
                limitationParameters.setMaxElementaryActionsPerTso(state, raUsageLimits.getMaxElementaryActionsPerTso());
            }
        }
        return limitationParameters;
    }
}
