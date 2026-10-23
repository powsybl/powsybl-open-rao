/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.impl.PostContingencyState;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.OptimizationStepsExecuted;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.extension.CostResult;
import com.powsybl.openrao.data.raoresult.api.extension.FlowResult;
import com.powsybl.openrao.data.raoresult.api.extension.Metadata;
import com.powsybl.openrao.data.raoresult.impl.RaoResultImpl;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.castor.algorithm.CastorCostResultExtensionHelper;
import com.powsybl.openrao.searchtreerao.castor.algorithm.CastorFlowResultExtensionHelper;
import com.powsybl.openrao.searchtreerao.castor.algorithm.CastorMetadataHelper;
import com.powsybl.openrao.searchtreerao.castor.algorithm.Perimeter;
import com.powsybl.openrao.searchtreerao.castor.algorithm.StateTree;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.powsybl.openrao.searchtreerao.commons.RaoUtil.getFlowUnit;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class RaoResultGenerator {

    private RaoResultGenerator() {
    }

    public static RaoResult failed(final Crac crac, final String failureReason) {
        RaoResultImpl raoResult = new RaoResultImpl(crac);
        raoResult.addExtension(FlowResult.class, new FlowResult()); // the extension is created by convention
        CastorMetadataHelper.fillAndAddWithGlobalFailure(crac, raoResult, failureReason);
        return raoResult;
    }

    public static RaoResult empty(final Crac crac, final String executionDetails, final PrePerimeterResult initialResult, final RaoParameters raoParameters) {
        RaoResultImpl raoResult = new RaoResultImpl(crac);
        Unit flowUnit = RaoUtil.getFlowUnit(raoParameters);
        raoResult.addExtension(CostResult.class, CastorCostResultExtensionHelper.convertToExtension(initialResult));
        raoResult.addExtension(FlowResult.class, CastorFlowResultExtensionHelper.convertToExtension(initialResult, crac, flowUnit));
        CastorMetadataHelper.fillAndAddFromPrePerimeter(crac, raoResult, initialResult, executionDetails);
        return raoResult;
    }

    public static RaoResult preventive(final Crac crac,
                                       final PrePerimeterResult initialResult,
                                       final PostPerimeterResult postPreventiveResult,
                                       final RaoParameters raoParameters,
                                       final ReportNode reportNode) {
        RaoResultImpl raoResult = new RaoResultImpl(crac);
        Unit flowUnit = RaoUtil.getFlowUnit(raoParameters);
        ObjectiveFunctionResult preventiveAndOutageOnlyResult = generatePreventiveAndOutageOnlyResult(crac, initialResult, postPreventiveResult, raoParameters, reportNode);
        RaoUtil.fillWithActivatedRemedialActions(raoResult, postPreventiveResult.optimizationResult(), crac.getPreventiveState());
        raoResult.addExtension(CostResult.class, CastorCostResultExtensionHelper.convertToExtension(
            initialResult,
            preventiveAndOutageOnlyResult,
            postPreventiveResult.prePerimeterResultForAllFollowingStates(),
            raoParameters.getObjectiveFunctionParameters().getType().costOptimization(),
            crac.getPreventiveInstant()
        ));
        raoResult.addExtension(FlowResult.class, CastorFlowResultExtensionHelper.convertToExtension(
            initialResult,
            postPreventiveResult.prePerimeterResultForAllFollowingStates(),
            crac,
            flowUnit
        ));
        CastorMetadataHelper.fillAndAddFromPrePerimeter(
            crac,
            raoResult,
            postPreventiveResult.prePerimeterResultForAllFollowingStates(),
            OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY
        );
        return raoResult;
    }

    public static RaoResult preventiveAndCurative(final Crac crac,
                                                  final String executionDetails,
                                                  final PrePerimeterResult initialResult,
                                                  final PostPerimeterResult postPreventiveResult,
                                                  final Map<State, PostPerimeterResult> postContingencyResults,
                                                  final RaoParameters raoParameters,
                                                  final StateTree stateTree,
                                                  final ReportNode reportNode) {
        RaoResultImpl raoResult = new RaoResultImpl(crac);
        Unit flowUnit = RaoUtil.getFlowUnit(raoParameters);
        ObjectiveFunctionResult preventiveAndOutageOnlyResult = generatePreventiveAndOutageOnlyResult(crac, initialResult, postPreventiveResult, raoParameters, reportNode);
        completePostContingencyResultsMap(crac, initialResult, postPreventiveResult, postContingencyResults, stateTree, raoParameters, reportNode);
        excludeDuplicatedCnecs(crac, initialResult, preventiveAndOutageOnlyResult, postPreventiveResult, postContingencyResults);
        excludeContingencies(stateTree, initialResult, preventiveAndOutageOnlyResult, postPreventiveResult, postContingencyResults);
        RaoUtil.fillWithActivatedRemedialActions(raoResult, postPreventiveResult.optimizationResult(), crac.getPreventiveState(), postContingencyResults);
        raoResult.addExtension(CostResult.class, CastorCostResultExtensionHelper.convertToExtension(
            initialResult,
            preventiveAndOutageOnlyResult,
            postPreventiveResult.prePerimeterResultForAllFollowingStates(),
            postContingencyResults,
            raoParameters.getObjectiveFunctionParameters().getType().costOptimization(),
            crac
        ));
        raoResult.addExtension(FlowResult.class, CastorFlowResultExtensionHelper.convertToExtension(
            initialResult,
            postPreventiveResult.prePerimeterResultForAllFollowingStates(),
            postContingencyResults,
            crac,
            flowUnit
        ));
        CastorMetadataHelper.fillAndAddFromPrePerimeter(crac, raoResult, postPreventiveResult.prePerimeterResultForAllFollowingStates(), executionDetails);
        Metadata metadata = raoResult.getExtension(Metadata.class);
        crac.getStates()
            .stream()
            .filter(postContingencyResults::containsKey)
            .filter(state -> postContingencyResults.get(state).prePerimeterResultForAllFollowingStates().getComputationStatus(state) != ComputationStatus.DEFAULT)
            .forEach(state -> metadata.setComputationStatus(state, postContingencyResults.get(state).prePerimeterResultForAllFollowingStates().getComputationStatus(state)));
        return raoResult;
    }

    /* Utility methods */

    /**
     * Fill in results for states which were not optimized separately (either in preventive, or for states with no elements at all)
     * We go through only 2nd if statement for cases with CNECs without actions : state is defined, but no optimization was performed
     */
    private static void completePostContingencyResultsMap(final Crac crac,
                                                          final PrePerimeterResult initialResult,
                                                          final PostPerimeterResult postPreventiveResult,
                                                          final Map<State, PostPerimeterResult> postContingencyResults,
                                                          final StateTree stateTree,
                                                          final RaoParameters raoParameters,
                                                          final ReportNode reportNode) {
        crac.getContingencies().forEach(contingency ->
            crac.getSortedInstants().stream().filter(instant -> !instant.isPreventive() && !instant.isOutage()).forEach(instant -> {
                State state = crac.getState(contingency, instant);
                // States are defined in crac when there are associated cnecs or actions.
                // When no state is defined, we still want to evaluate objective functions at given contingency/instant
                if (Objects.isNull(state)) {
                    state = new PostContingencyState(contingency, instant, crac.getTimestamp().orElse(null));
                }
                if (!postContingencyResults.containsKey(state)) {
                    postContingencyResults.put(state, generateResultForUnoptimizedState(
                        crac,
                        state,
                        initialResult,
                        postPreventiveResult,
                        postContingencyResults,
                        stateTree,
                        raoParameters,
                        reportNode
                    ));
                }
            }));
    }

    private static PostPerimeterResult generateResultForUnoptimizedState(final Crac crac,
                                                                         final State state,
                                                                         final PrePerimeterResult initialResult,
                                                                         final PostPerimeterResult postPreventiveResult,
                                                                         final Map<State, PostPerimeterResult> postContingencyResults,
                                                                         final StateTree stateTree,
                                                                         final RaoParameters raoParameters,
                                                                         final ReportNode reportNode) {
        //Get previous result (either preventive if no preceding state, an optimized contingency state result, or a newly generated state result)
        PrePerimeterResult previousResult = postContingencyResults.keySet().stream()
            .filter(s -> s.getInstant().comesBefore(state.getInstant()))
            .filter(s -> s.getContingency().equals(state.getContingency()))
            .sorted(Comparator.comparing(s -> -s.getInstant().getOrder()))
            .map(s -> postContingencyResults.get(s).prePerimeterResultForAllFollowingStates())
            .findFirst().orElse(postPreventiveResult.prePerimeterResultForAllFollowingStates());

        //compute objective function only considering that state cnecs
        Set<FlowCnec> stateCnecs = crac.getFlowCnecs(state);
        Set<FlowCnec> loopFlowCnecs = stateCnecs.stream()
            .filter(flowCnec -> initialResultContainsLoopFlowResult(flowCnec, initialResult, raoParameters))
            .collect(Collectors.toSet());
        RemedialActionActivationResult raActivationResult = RemedialActionActivationResultImpl.empty(previousResult);
        ObjectiveFunctionResult stateOfResult = ObjectiveFunction.build(
            stateCnecs,
            loopFlowCnecs,
            initialResult,
            previousResult,
            stateTree.getOperatorsNotSharingCras(),
            raoParameters,
            Set.of(state)
        ).evaluate(previousResult, raActivationResult, reportNode);
        OptimizationResult optimizationResult = new OptimizationResultImpl(stateOfResult, previousResult, previousResult, raActivationResult, raActivationResult);

        //compute objective function considering all the cnecs from the state and following states
        Set<FlowCnec> allFollowingStatesCnecs = crac.getStates(state.getContingency().orElseThrow(() -> new OpenRaoException("State should have a contingency."))).stream()
            .filter(s -> !s.getInstant().comesBefore(state.getInstant()))
            .map(crac::getFlowCnecs)
            .reduce(new HashSet<>(), (x, y) -> {
                x.addAll(y);
                return x;
            });
        Set<FlowCnec> allFollowingStatesLoopFlowCnecs = stateCnecs.stream()
            .filter(flowCnec -> initialResultContainsLoopFlowResult(flowCnec, initialResult, raoParameters))
            .collect(Collectors.toSet());
        ObjectiveFunctionResult followingStatesOfResult = ObjectiveFunction.build(
            allFollowingStatesCnecs,
            allFollowingStatesLoopFlowCnecs,
            initialResult,
            previousResult,
            stateTree.getOperatorsNotSharingCras(),
            raoParameters,
            Set.of(state)
        ).evaluate(previousResult, raActivationResult, reportNode);
        PrePerimeterResult prePerimeterResult = new PrePerimeterSensitivityResultImpl(previousResult, previousResult, previousResult, followingStatesOfResult);

        return new PostPerimeterResult(optimizationResult, prePerimeterResult);
    }

    private static void excludeContingencies(final StateTree stateTree,
                                             final PrePerimeterResult initialResult,
                                             final ObjectiveFunctionResult preventiveAndOutageOnlyResult,
                                             final PostPerimeterResult postPreventiveResult,
                                             final Map<State, PostPerimeterResult> postContingencyResults) {
        Set<String> contingenciesToExclude = getContingenciesToExclude(stateTree, postContingencyResults);
        initialResult.excludeContingencies(contingenciesToExclude);
        preventiveAndOutageOnlyResult.excludeContingencies(contingenciesToExclude);
        postPreventiveResult.optimizationResult().excludeContingencies(contingenciesToExclude);
        postPreventiveResult.optimizationResult().excludeContingencies(contingenciesToExclude);
        postPreventiveResult.prePerimeterResultForAllFollowingStates().excludeContingencies(contingenciesToExclude);
        postPreventiveResult.prePerimeterResultForAllFollowingStates().excludeContingencies(contingenciesToExclude);
        postContingencyResults.values().forEach(result -> {
            result.optimizationResult().excludeContingencies(contingenciesToExclude);
            result.prePerimeterResultForAllFollowingStates().excludeContingencies(contingenciesToExclude);
        });
    }

    private static Set<String> getContingenciesToExclude(final StateTree stateTree, final Map<State, PostPerimeterResult> postContingencyResults) {
        if (postContingencyResults.isEmpty()) {
            return Set.of();
        }
        Set<String> contingenciesToExclude = new HashSet<>();
        stateTree.getContingencyScenarios().forEach(contingencyScenario -> {
            Optional<State> automatonState = contingencyScenario.getAutomatonState();
            if (automatonState.isPresent()) {
                OptimizationResult automatonResult = postContingencyResults.get(automatonState.get()).optimizationResult();
                if (!automatonResult.getContingencies().contains(contingencyScenario.getContingency().getId())) {
                    contingenciesToExclude.add(contingencyScenario.getContingency().getId());
                    return;
                }
            }
            for (Perimeter curativePerimeter : contingencyScenario.getCurativePerimeters()) {
                OptimizationResult curativeResult = postContingencyResults.get(curativePerimeter.getRaOptimisationState()).optimizationResult();
                if (!curativeResult.getContingencies().contains(contingencyScenario.getContingency().getId())) {
                    contingenciesToExclude.add(contingencyScenario.getContingency().getId());
                }
            }
        });
        return contingenciesToExclude;
    }

    private static OptimizationResult generatePreventiveAndOutageOnlyResult(final Crac crac,
                                                                            final PrePerimeterResult initialResult,
                                                                            final PostPerimeterResult finalPreventivePerimeterResult,
                                                                            final RaoParameters raoParameters,
                                                                            final ReportNode reportNode) {
        Set<FlowCnec> flowCnecs = crac.getFlowCnecs().stream()
            .filter(flowCnec -> flowCnec.getState().isPreventive() || flowCnec.getState().getInstant().getKind().equals(InstantKind.OUTAGE))
            .collect(Collectors.toSet());
        // For non loopflow CNECs, the result returns NaN or is missing commercial flows
        Set<FlowCnec> loopFlowCnecs = flowCnecs.stream()
            .filter(flowCnec -> initialResultContainsLoopFlowResult(flowCnec, initialResult, raoParameters))
            .collect(Collectors.toSet());
        ObjectiveFunction objectiveFunction = ObjectiveFunction.build(
            flowCnecs,
            loopFlowCnecs,
            initialResult,
            initialResult,
            Collections.emptySet(),
            raoParameters,
            Set.of(crac.getPreventiveState())
        );
        RemedialActionActivationResult remedialActionActivationResult = new RemedialActionActivationResultImpl(
            finalPreventivePerimeterResult.optimizationResult(),
            finalPreventivePerimeterResult.optimizationResult()
        );
        ObjectiveFunctionResult objectiveFunctionResult = objectiveFunction.evaluate(finalPreventivePerimeterResult.optimizationResult(), remedialActionActivationResult, reportNode);
        return new OptimizationResultImpl(
            objectiveFunctionResult,
            finalPreventivePerimeterResult.optimizationResult(),
            finalPreventivePerimeterResult.optimizationResult(),
            finalPreventivePerimeterResult.optimizationResult(),
            finalPreventivePerimeterResult.optimizationResult()
        );
    }

    private static boolean initialResultContainsLoopFlowResult(final FlowCnec flowCnec, final PrePerimeterResult initialResult, final RaoParameters raoParameters) {
        boolean loopflowPresent;
        try {
            loopflowPresent = !Double.isNaN(initialResult.getLoopFlow(flowCnec, flowCnec.getMonitoredSides().iterator().next(), getFlowUnit(raoParameters)));
        } catch (OpenRaoException e) {
            if (e.getMessage().contains("No commercial flow")) {
                loopflowPresent = false;
            } else {
                throw e;
            }
        }
        return loopflowPresent;
    }

    private static void excludeDuplicatedCnecs(final Crac crac,
                                               final PrePerimeterResult initialResult,
                                               final ObjectiveFunctionResult preventiveAndOutageOnlyResult,
                                               final PostPerimeterResult postPreventiveResult,
                                               final Map<State, PostPerimeterResult> postContingencyResults) {
        Set<String> cnecsToExclude = getDuplicateCnecs(crac.getFlowCnecs());
        initialResult.excludeCnecs(cnecsToExclude);
        preventiveAndOutageOnlyResult.excludeCnecs(cnecsToExclude);
        postPreventiveResult.optimizationResult().excludeCnecs(cnecsToExclude);
        postPreventiveResult.optimizationResult().excludeCnecs(cnecsToExclude);
        postPreventiveResult.prePerimeterResultForAllFollowingStates().excludeCnecs(cnecsToExclude);
        postPreventiveResult.prePerimeterResultForAllFollowingStates().excludeCnecs(cnecsToExclude);
        postContingencyResults.values().forEach(result -> {
            result.optimizationResult().excludeCnecs(cnecsToExclude);
            result.prePerimeterResultForAllFollowingStates().excludeCnecs(cnecsToExclude);
        });
    }

    public static Set<String> getDuplicateCnecs(final Set<FlowCnec> flowCnecs) {
        return flowCnecs.stream()
            .map(FlowCnec::getId)
            .filter(id -> id.contains("OUTAGE DUPLICATE"))
            .collect(Collectors.toSet());
    }
}
