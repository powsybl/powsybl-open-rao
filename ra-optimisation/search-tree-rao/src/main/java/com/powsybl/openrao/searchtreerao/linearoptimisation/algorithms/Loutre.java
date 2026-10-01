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
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.RaoProvider;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoutreParameters;
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
import com.powsybl.openrao.searchtreerao.result.api.NetworkActionsResult;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

/**
 * <b>LOUTRe (<i>Linear Optimizer Using Transformers and Redispatching</i>)</b> is a RAO provider running a single
 * global linear optimization of all the range actions of the CRAC (preventive and curative).
 *
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
@AutoService(RaoProvider.class)
public class Loutre implements RaoProvider {

    static {
        try {
            Loader.loadNativeLibraries();
        } catch (UnsatisfiedLinkError e) {
            TECHNICAL_LOGS.error("Native library jniortools could not be loaded. You can ignore this message if it is not needed.");
        }
    }

    private static final String LINEAR_RAO = "LinearRao";
    private static final String LINEAR_RAO_VARIANT = "LinearRaoVariant";
    private static final String LINEAR_RAO_BASE_VARIANT = "LinearRaoBaseVariant";
    private static final int NUMBER_LOGGED_ELEMENTS_DURING_RAO = 2;
    private static final int NUMBER_LOGGED_ELEMENTS_END_RAO = 10;

    // Do not store any big object in this class as it is a static RaoProvider
    // Objects stored in memory will not be released at the end of the RAO run
    // Network actions are only supported through the "forced-network-actions" parameter of LoutreParameters

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
            for (String variantId : List.of(LINEAR_RAO_VARIANT, LINEAR_RAO_BASE_VARIANT)) {
                if (network.getVariantManager().getVariantIds().contains(variantId)) {
                    network.getVariantManager().removeVariant(variantId);
                }
            }
        }
    }

    static void warnIfNetworkActionsInCrac(Crac crac, ReportNode reportNode) {
        if (!crac.getNetworkActions().isEmpty()) {
            LinearRaoReports.reportLinearRaoNetworkActionsIgnored(reportNode, crac.getNetworkActions().size());
        }
    }

    static List<String> getForcedNetworkActionIds(RaoParameters raoParameters) {
        LoutreParameters loutreParameters = raoParameters.getExtension(LoutreParameters.class);
        return loutreParameters == null ? List.of() : loutreParameters.getForcedNetworkActions();
    }

    /**
     * Retrieves the forced network actions in the CRAC. They must exist and be available in the preventive state.
     */
    static Set<NetworkAction> getForcedNetworkActions(Crac crac, RaoParameters raoParameters) {
        Set<NetworkAction> availablePreventiveNetworkActions = crac.getNetworkActions(crac.getPreventiveState());
        Set<NetworkAction> forcedNetworkActions = new HashSet<>();
        for (String networkActionId : getForcedNetworkActionIds(raoParameters)) {
            NetworkAction networkAction = crac.getNetworkAction(networkActionId);
            if (networkAction == null || !availablePreventiveNetworkActions.contains(networkAction)) {
                throw new OpenRaoException(String.format("Forced network action '%s' does not exist in the CRAC or is not a preventive network action", networkActionId));
            }
            forcedNetworkActions.add(networkAction);
        }
        return forcedNetworkActions;
    }

    private static RaoResult optimize(RaoInput raoInput, RaoParameters raoParameters, String originalVariantId, ReportNode reportNode) {
        Crac crac = raoInput.getCrac();
        Network network = raoInput.getNetwork();

        // ----- FORCED NETWORK ACTIONS -----
        // applied on a dedicated variant so that they are taken into account from the initial sensitivity analysis on
        Set<NetworkAction> forcedNetworkActions = getForcedNetworkActions(crac, raoParameters);
        String initialVariantId = LINEAR_RAO_BASE_VARIANT;
        network.getVariantManager().cloneVariant(originalVariantId, initialVariantId, true);
        network.getVariantManager().setWorkingVariant(initialVariantId);
        forcedNetworkActions.forEach(networkAction -> networkAction.apply(network));
        LinearRaoReports.reportLinearRaoForcedNetworkActions(reportNode, forcedNetworkActions);
        ToolProvider toolProvider = ToolProvider.buildFromRaoInputAndParameters(raoInput, raoParameters);

        // ----- STATES IN SCOPE -----
        Set<State> statesInScope = getStatesInScope(crac, raoInput.getPerimeter());
        State mainOptimizationState = getMainOptimizationState(crac, raoInput.getPerimeter());
        Set<FlowCnec> flowCnecsInScope = crac.getFlowCnecs().stream()
            .filter(cnec -> statesInScope.contains(cnec.getState()))
            .collect(Collectors.toSet());
        Set<RangeAction<?>> rangeActionsInScope = statesInScope.stream()
            .flatMap(state -> crac.getRangeActions(state).stream())
            .collect(Collectors.toSet());

        // ----- INITIAL SENSI -----
        PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(
            crac, flowCnecsInScope, rangeActionsInScope, raoParameters, toolProvider, true);
        PrePerimeterResult initialResult = prePerimeterSensitivityAnalysis.runInitialSensitivityAnalysis(network, reportNode);
        if (flowCnecsInScope.isEmpty()) {
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
        NetworkActionsResult forcedNetworkActionsResult = new NetworkActionsResultImpl(Map.of(mainOptimizationState, forcedNetworkActions));

        // ----- GLOBAL LINEAR OPTIMIZATION -----
        ReportNode optimizationReportNode = LinearRaoReports.reportLinearRaoGlobalOptimization(reportNode);
        network.getVariantManager().cloneVariant(initialVariantId, LINEAR_RAO_VARIANT, true);
        network.getVariantManager().setWorkingVariant(LINEAR_RAO_VARIANT);

        OptimizationPerimeter perimeter = GlobalOptimizationPerimeter.build(crac, network, raoParameters, initialResult, mainOptimizationState, statesInScope, reportNode)
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
            .withAppliedNetworkActionsInPrimaryState(forcedNetworkActionsResult)
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
            .withRaLimitationParameters(buildRaLimitationParameters(perimeter, searchTreeParameters, forcedNetworkActions))
            .withSolverParameters(searchTreeParameters.getSolverParameters())
            .withMaxNumberOfIterations(searchTreeParameters.getMaxNumberOfIterations())
            .withRaRangeShrinking(searchTreeParameters.getTreeParameters().raRangeShrinking())
            .build();

        LinearOptimizationResult linearResult = IteratingLinearOptimizer.optimize(linearOptimizerInput, linearOptimizerParameters, optimizationReportNode);
        OptimizationResult optimizationResult = new OptimizationResultImpl(
            linearResult, linearResult, linearResult, forcedNetworkActionsResult, linearResult.getRangeActionActivationResult());

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
        // the final sensitivity analysis is run without activation: the cost of the range actions must be added back
        ObjectiveFunctionResult finalObjectiveFunctionResult = objectiveFunction.evaluate(
            finalResult, new RemedialActionActivationResultImpl(linearResult.getRangeActionActivationResult(), forcedNetworkActionsResult), reportNode);
        LinearRaoReports.reportLinearRaoFinalResult(reportNode, finalObjectiveFunctionResult, finalResult, raoParameters, NUMBER_LOGGED_ELEMENTS_END_RAO);

        PostPerimeterResult postPerimeterResult = new PostPerimeterResult(optimizationResult, finalResult);
        Map<State, PostPerimeterResult> postContingencyResults = new HashMap<>();
        perimeter.getRangeActionOptimizationStates().stream()
            .filter(state -> !state.isPreventive())
            .forEach(state -> postContingencyResults.put(state, new PostPerimeterResult(
                buildCurativeStateResult(state, perimeter, linearResult, finalResult, initialResult, raoParameters, stateTree, reportNode), finalResult)));

        return new PreventiveAndCurativesRaoResultImpl(stateTree, initialResult, postPerimeterResult, postPerimeterResult,
            postContingencyResults, crac, raoParameters, reportNode);
    }

    /**
     * Builds the result of a curative state, whose cost only contains the cost of the range actions activated
     * in this state (the cost of the preventive range actions is already accounted for in the preventive result).
     */
    private static OptimizationResult buildCurativeStateResult(State state,
                                                               OptimizationPerimeter perimeter,
                                                               LinearOptimizationResult linearResult,
                                                               PrePerimeterResult finalResult,
                                                               PrePerimeterResult initialResult,
                                                               RaoParameters raoParameters,
                                                               StateTree stateTree,
                                                               ReportNode reportNode) {
        Set<FlowCnec> flowCnecs = raoParameters.getObjectiveFunctionParameters().getType().costOptimization() ?
            perimeter.getFlowCnecs().stream().filter(flowCnec -> flowCnec.getState().equals(state)).collect(Collectors.toSet()) :
            perimeter.getFlowCnecs();
        Set<FlowCnec> loopFlowCnecs = perimeter.getLoopFlowCnecs().stream().filter(flowCnecs::contains).collect(Collectors.toSet());
        ObjectiveFunction stateObjectiveFunction = ObjectiveFunction.build(
            flowCnecs, loopFlowCnecs, initialResult, initialResult, stateTree.getOperatorsNotSharingCras(), raoParameters, Set.of(state));
        NetworkActionsResult noNetworkActions = new NetworkActionsResultImpl(new HashMap<>());
        ObjectiveFunctionResult stateObjectiveFunctionResult = stateObjectiveFunction.evaluate(
            finalResult, new RemedialActionActivationResultImpl(linearResult.getRangeActionActivationResult(), noNetworkActions), reportNode);
        return new OptimizationResultImpl(stateObjectiveFunctionResult, finalResult, finalResult, noNetworkActions, linearResult.getRangeActionActivationResult());
    }

    /**
     * Without perimeter (null or empty), all the states of the CRAC are considered. Otherwise, only the given states
     * are, plus (if several states are given) the preventive state and the outage and auto states associated to the
     * given curative states.
     */
    static Set<State> getStatesInScope(Crac crac, Set<State> perimeter) {
        if (perimeter == null || perimeter.isEmpty()) {
            return new HashSet<>(crac.getStates());
        }
        Set<State> states = new HashSet<>(perimeter);
        if (perimeter.size() > 1) {
            states.add(crac.getPreventiveState());
            perimeter.stream()
                .filter(state -> state.getContingency().isPresent())
                .forEach(state -> crac.getStates(state.getContingency().get()).stream()
                    .filter(other -> other.getInstant().comesBefore(state.getInstant()))
                    .forEach(states::add));
        }
        return states;
    }

    /**
     * The main optimization state is the preventive state, unless a single state is given in the perimeter.
     */
    static State getMainOptimizationState(Crac crac, Set<State> perimeter) {
        if (perimeter != null && perimeter.size() == 1) {
            return perimeter.iterator().next();
        }
        return crac.getPreventiveState();
    }

    /**
     * Computes the remedial action limitation parameters. The forced network actions (all preventive) are already
     * applied, so they are deducted from the limits of the preventive state.
     */
    private static RangeActionLimitationParameters buildRaLimitationParameters(OptimizationPerimeter perimeter, SearchTreeParameters parameters, Set<NetworkAction> forcedNetworkActions) {
        RangeActionLimitationParameters limitationParameters = new RangeActionLimitationParameters();
        for (State state : perimeter.getRangeActionOptimizationStates()) {
            RaUsageLimits raUsageLimits = parameters.getRaLimitationParameters().get(state.getInstant());
            if (raUsageLimits != null) {
                Set<NetworkAction> appliedNetworkActions = state.isPreventive() ? forcedNetworkActions : Set.of();
                Integer maxRa = raUsageLimits.getMaxRa();
                if (maxRa != null) {
                    maxRa = Math.max(0, maxRa - appliedNetworkActions.size());
                }
                Map<String, Integer> maxRaPerTso = new HashMap<>(raUsageLimits.getMaxRaPerTso());
                maxRaPerTso.replaceAll((tso, max) -> Math.max(0, max - (int) appliedNetworkActions.stream()
                    .filter(na -> tso.equals(na.getOperator())).count()));
                Map<String, Integer> maxElementaryActionsPerTso = new HashMap<>(raUsageLimits.getMaxElementaryActionsPerTso());
                maxElementaryActionsPerTso.replaceAll((tso, max) -> Math.max(0, max - appliedNetworkActions.stream()
                    .filter(na -> tso.equals(na.getOperator()))
                    .mapToInt(na -> na.getElementaryActions().size())
                    .sum()));

                limitationParameters.setMaxRangeAction(state, maxRa);
                limitationParameters.setMaxPstPerTso(state, raUsageLimits.getMaxPstPerTso());
                limitationParameters.setMaxRangeActionPerTso(state, maxRaPerTso);
                limitationParameters.setMaxElementaryActionsPerTso(state, maxElementaryActionsPerTso);
            }
        }
        return limitationParameters;
    }
}
