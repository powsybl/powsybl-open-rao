/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.*;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.impl.PostContingencyState;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.OptimizationStepsExecuted;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.castor.algorithm.Perimeter;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.result.api.*;
import com.powsybl.openrao.searchtreerao.castor.algorithm.StateTree;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import static com.powsybl.openrao.data.raoresult.api.ComputationStatus.*;
import static com.powsybl.openrao.searchtreerao.commons.RaoUtil.getDuplicateCnecs;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class PreventiveAndCurativesRaoResultImpl extends AbstractFlowRaoResult {
    private final State preventiveState;
    private final PrePerimeterResult initialResult;
    private final OptimizationResult preventiveAndOutageOnlyResult;
    private final PostPerimeterResult firstPreventivePerimeterResult;
    private final PostPerimeterResult secondPreventivePerimeterResult;
    private final Set<RemedialAction<?>> remedialActionsExcludedFromSecondPreventive;
    private final Map<State, PostPerimeterResult> postContingencyResults;
    private final Crac crac;
    private String executionDetails = OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY;
    private final RaoParameters raoParameters;

    private final Map<Instant, Map<State, State>> optimizedStateForInstantAndState = new HashMap<>();

    /**
     * Constructor used when no post-contingency RAO has been run. Then the post-contingency results will be the
     * same as the post-preventive RAO results.
     */
    public PreventiveAndCurativesRaoResultImpl(StateTree stateTree,
                                               PrePerimeterResult initialResult,
                                               PostPerimeterResult preventivePerimeterResult,
                                               Crac crac,
                                               RaoParameters raoParameters) {
        this(stateTree, initialResult, preventivePerimeterResult, preventivePerimeterResult, new HashSet<>(), new HashMap<>(), crac, raoParameters);
    }

    /**
     * Constructor used when preventive and post-contingency RAOs have been run
     */
    public PreventiveAndCurativesRaoResultImpl(StateTree stateTree,
                                               PrePerimeterResult initialResult,
                                               PostPerimeterResult preventiveResult,
                                               Map<State, PostPerimeterResult> postContingencyResults,
                                               Crac crac,
                                               RaoParameters raoParameters) {
        this(stateTree, initialResult, preventiveResult, preventiveResult, new HashSet<>(), postContingencyResults, crac, raoParameters);
    }

    /**
     * Constructor used when preventive and post-contingency RAOs have been run, if 2 preventive RAOs were run, and 2 AUTO RAOs were run
     */
    public PreventiveAndCurativesRaoResultImpl(StateTree stateTree,
                                               PrePerimeterResult initialResult,
                                               PostPerimeterResult firstPreventivePerimeterResult,
                                               PostPerimeterResult secondPreventivePerimeterResult,
                                               Set<RemedialAction<?>> remedialActionsExcludedFromSecondPreventive,
                                               Map<State, PostPerimeterResult> postContingencyPerimeterResults,
                                               Crac crac,
                                               RaoParameters raoParameters) {
        this.preventiveState = crac.getPreventiveState();
        this.initialResult = initialResult;
        this.firstPreventivePerimeterResult = firstPreventivePerimeterResult;
        this.secondPreventivePerimeterResult = secondPreventivePerimeterResult;
        this.remedialActionsExcludedFromSecondPreventive = remedialActionsExcludedFromSecondPreventive;
        this.postContingencyResults = postContingencyPerimeterResults;
        this.crac = crac;
        this.raoParameters = raoParameters;
        this.preventiveAndOutageOnlyResult = generatePreventiveAndOutageOnlyResult();
        completePostContingencyResultsMap(stateTree);
        excludeContingencies(getContingenciesToExclude(stateTree));
        excludeDuplicateCnecs();
    }

    private OptimizationResult generatePreventiveAndOutageOnlyResult() {
        Set<FlowCnec> flowCnecs = crac.getFlowCnecs().stream()
            .filter(flowCnec -> flowCnec.getState().isPreventive() || flowCnec.getState().getInstant().getKind().equals(InstantKind.OUTAGE))
            .collect(Collectors.toSet());
        //For non loopflow cnecs, the result returns NaN
        Set<FlowCnec> loopFlowCnecs = flowCnecs.stream()
            .filter(flowCnec -> !Double.isNaN(initialResult.getLoopFlow(flowCnec, flowCnec.getMonitoredSides().iterator().next(), Unit.MEGAWATT)))
            .collect(Collectors.toSet());
        ObjectiveFunction objectiveFunction = ObjectiveFunction.build(flowCnecs, loopFlowCnecs, initialResult, initialResult, Collections.emptySet(), raoParameters, Set.of(crac.getPreventiveState()));
        RemedialActionActivationResult remedialActionActivationResult = new RemedialActionActivationResultImpl(secondPreventivePerimeterResult.getOptimizationResult(), secondPreventivePerimeterResult.getOptimizationResult());
        ObjectiveFunctionResult objectiveFunctionResult = objectiveFunction.evaluate(secondPreventivePerimeterResult.getOptimizationResult(), remedialActionActivationResult);
        return new OptimizationResultImpl(objectiveFunctionResult, secondPreventivePerimeterResult.getOptimizationResult(), secondPreventivePerimeterResult.getOptimizationResult(), secondPreventivePerimeterResult.getOptimizationResult(), secondPreventivePerimeterResult.getOptimizationResult());
    }

    //Fill in results for states which were not optimized separately (either in preventive, or for states with no elements at all)
    private void completePostContingencyResultsMap(StateTree stateTree) {
        crac.getContingencies().forEach(contingency -> {
            crac.getSortedInstants().stream().filter(instant -> !instant.isPreventive() && !instant.isOutage()).forEach(instant -> {
                State state = crac.getState(contingency, instant);
                if (Objects.isNull(state)) {
                    state = new PostContingencyState(contingency, instant, crac.getTimestamp().orElse(null));
                }
                if (!postContingencyResults.containsKey(state)) {
                    postContingencyResults.put(state, generateResultForUnoptimizedState(state, stateTree));
                }
            });
        });
    }

    private PostPerimeterResult generateResultForUnoptimizedState(State state, StateTree stateTree) {
        //Get previous result (either preventive if no preceding state, an optimized contingency state result, or a newly generated state result)
        PrePerimeterResult previousResult = postContingencyResults.keySet().stream()
            .filter(s -> s.getInstant().comesBefore(state.getInstant()))
            .filter(s -> s.getContingency().equals(state.getContingency()))
            .sorted(Comparator.comparing(s -> -s.getInstant().getOrder()))
            .map(s -> postContingencyResults.get(s).getPrePerimeterResultForAllFollowingStates())
            .findFirst().orElse(secondPreventivePerimeterResult.getPrePerimeterResultForAllFollowingStates());

        //compute objective function only considering that state cnecs
        Set<FlowCnec> stateCnecs = crac.getFlowCnecs(state);
        Set<FlowCnec> loopFlowCnecs = stateCnecs.stream()
            .filter(flowCnec -> !Double.isNaN(previousResult.getLoopFlow(flowCnec, flowCnec.getMonitoredSides().iterator().next(), Unit.MEGAWATT)))
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
        ).evaluate(previousResult, raActivationResult);
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
            .filter(flowCnec -> !Double.isNaN(previousResult.getLoopFlow(flowCnec, flowCnec.getMonitoredSides().iterator().next(), Unit.MEGAWATT)))
            .collect(Collectors.toSet());
        ObjectiveFunctionResult followingStatesOfResult = ObjectiveFunction.build(
            allFollowingStatesCnecs,
            allFollowingStatesLoopFlowCnecs,
            initialResult,
            previousResult,
            stateTree.getOperatorsNotSharingCras(),
            raoParameters,
            Set.of(state)
        ).evaluate(previousResult, raActivationResult);
        PrePerimeterResult prePerimeterResult = new PrePerimeterSensitivityResultImpl(previousResult, previousResult, previousResult, followingStatesOfResult);

        return new PostPerimeterResult(optimizationResult, prePerimeterResult);
    }

    private Set<String> getContingenciesToExclude(StateTree stateTree) {
        Set<String> contingenciesToExclude = new HashSet<>();
        stateTree.getContingencyScenarios().forEach(contingencyScenario -> {
            Optional<State> automatonState = contingencyScenario.getAutomatonState();
            if (automatonState.isPresent()) {
                OptimizationResult automatonResult = postContingencyResults.get(automatonState.get()).getOptimizationResult();
                if (!automatonResult.getContingencies().contains(contingencyScenario.getContingency().getId())) {
                    contingenciesToExclude.add(contingencyScenario.getContingency().getId());
                    return;
                }
            }
            for (Perimeter curativePerimeter : contingencyScenario.getCurativePerimeters()) {
                OptimizationResult curativeResult = postContingencyResults.get(curativePerimeter.getRaOptimisationState()).getOptimizationResult();
                if (!curativeResult.getContingencies().contains(contingencyScenario.getContingency().getId())) {
                    contingenciesToExclude.add(contingencyScenario.getContingency().getId());
                }
            }
        });
        return contingenciesToExclude;
    }

    private void excludeDuplicateCnecs() {
        Set<FlowCnec> flowCnecs = crac.getFlowCnecs();
        Set<String> cnecsToExclude = getDuplicateCnecs(flowCnecs);
        // exclude fictional cnec from the results
        initialResult.excludeCnecs(cnecsToExclude);
        firstPreventivePerimeterResult.getOptimizationResult().excludeCnecs(cnecsToExclude);
        secondPreventivePerimeterResult.getOptimizationResult().excludeCnecs(cnecsToExclude);
        firstPreventivePerimeterResult.getPrePerimeterResultForAllFollowingStates().excludeCnecs(cnecsToExclude);
        secondPreventivePerimeterResult.getPrePerimeterResultForAllFollowingStates().excludeCnecs(cnecsToExclude);
        postContingencyResults.values().forEach(result -> {
            result.getOptimizationResult().excludeCnecs(cnecsToExclude);
            result.getPrePerimeterResultForAllFollowingStates().excludeCnecs(cnecsToExclude);
        });
    }

    private void excludeContingencies(Set<String> contingenciesToExclude) {
        initialResult.excludeContingencies(contingenciesToExclude);
        firstPreventivePerimeterResult.getOptimizationResult().excludeContingencies(contingenciesToExclude);
        secondPreventivePerimeterResult.getOptimizationResult().excludeContingencies(contingenciesToExclude);
        firstPreventivePerimeterResult.getPrePerimeterResultForAllFollowingStates().excludeContingencies(contingenciesToExclude);
        secondPreventivePerimeterResult.getPrePerimeterResultForAllFollowingStates().excludeContingencies(contingenciesToExclude);
        postContingencyResults.values().forEach(result -> {
            result.getOptimizationResult().excludeContingencies(contingenciesToExclude);
            result.getPrePerimeterResultForAllFollowingStates().excludeContingencies(contingenciesToExclude);
        });
    }

    @Override
    public ComputationStatus getComputationStatus() {
        if (initialResult.getSensitivityStatus() == FAILURE
            || secondPreventivePerimeterResult.getOptimizationResult().getSensitivityStatus() == FAILURE) {
            return FAILURE;
        }
        if (initialResult.getSensitivityStatus() == PARTIAL_FAILURE ||
            secondPreventivePerimeterResult.getOptimizationResult().getSensitivityStatus() == PARTIAL_FAILURE ||
            postContingencyResults.entrySet().stream().anyMatch(entry ->
                entry.getValue() == null || entry.getValue().getOptimizationResult().getSensitivityStatus(entry.getKey()) != DEFAULT)) {
            return PARTIAL_FAILURE;
        }
        return DEFAULT;
    }

    @Override
    public ComputationStatus getComputationStatus(State state) {
        Instant instant = state.getInstant();
        while (instant != null) {
            OptimizationResult perimeterResult = getOptimizationResult(instant, state);
            if (Objects.nonNull(perimeterResult)) {
                return perimeterResult.getSensitivityStatus(state);
            }
            instant = crac.getInstantBefore(instant);
        }
        return FAILURE;
    }

    public OptimizationResult getOptimizationResult(Instant optimizedInstant, State state) {
        if (optimizedInstant == null) {
            throw new OpenRaoException("No OptimizationResult for INITIAL optimization state");
        }
        if (state.getInstant().comesBefore(optimizedInstant)) {
            throw new OpenRaoException(String.format("Trying to access results for instant %s at optimization state %s is not allowed", state.getInstant(), optimizedInstant));
        }
        if (optimizedInstant.isPreventive() || optimizedInstant.isOutage()) {
            return secondPreventivePerimeterResult.getOptimizationResult();
        }
        if (optimizedInstant.isAuto()) {
            return postContingencyResults.keySet().stream()
                .filter(optimizedState -> optimizedState.getInstant().isAuto() && optimizedState.getContingency().equals(state.getContingency()))
                .findAny().map(s -> postContingencyResults.get(s).getOptimizationResult()).orElse(null);
        }
        if (optimizedInstant.isCurative()) {
            if (postContingencyResults.containsKey(state)) {
                return postContingencyResults.get(state).getOptimizationResult();
            } else {
                return null;
            }
        }
        throw new OpenRaoException(String.format("Optimized instant %s was not recognized", optimizedInstant));
    }

    @Override
    public double getFunctionalCost(Instant optimizedInstant) {
        if (optimizedInstant == null) {
            return initialResult.getFunctionalCost();
        } else if (optimizedInstant.isPreventive() || optimizedInstant.isOutage()) {
            if (raoParameters.getObjectiveFunctionParameters().getType().costOptimization()) {
                //for costly we only care about the cost of preventive actions (for after PRA result)
                return preventiveAndOutageOnlyResult.getFunctionalCost();
            } else {
                //for min margin, we care about the cost of all cnecs
                return secondPreventivePerimeterResult.getPrePerimeterResultForAllFollowingStates().getFunctionalCost();
            }
        } else {
            BinaryOperator<Double> operator;
            if (raoParameters.getObjectiveFunctionParameters().getType().costOptimization()) {
                operator = Double::sum;
            } else {
                operator = Math::max;
            }
            //initialize cost to preventive optimization cost
            AtomicReference<Double> totalCost = new AtomicReference<>(preventiveAndOutageOnlyResult.getFunctionalCost());
            //for states which come strictly before optimizedInstant, consider optimizationResult
            postContingencyResults.entrySet().stream()
                .filter(stateAndResult -> stateAndResult.getKey().getInstant().comesBefore(optimizedInstant))
                .forEach(stateAndResult -> totalCost.set(operator.apply(totalCost.get(), stateAndResult.getValue().getOptimizationResult().getFunctionalCost())));
            //for states which have same instant as optimizedInstant, consider prePerimeterResultForAllFollowingStates
            postContingencyResults.entrySet().stream()
                .filter(stateAndResult -> stateAndResult.getKey().getInstant().equals(optimizedInstant))
                .forEach(stateAndResult -> totalCost.set(operator.apply(totalCost.get(),
                    //for costly use optim result; for max min margin usel prePerim result
                    raoParameters.getObjectiveFunctionParameters().getType().costOptimization() ?
                        stateAndResult.getValue().getOptimizationResult().getFunctionalCost() :
                        stateAndResult.getValue().getPrePerimeterResultForAllFollowingStates().getFunctionalCost()))
            );

            return totalCost.get();
        }
    }

    @Override
    public double getMargin(Instant optimizedInstant, FlowCnec flowCnec, Unit unit) {
        FlowResult flowResult = getFlowResult(optimizedInstant, flowCnec);
        if (Objects.nonNull(flowResult)) {
            return flowResult.getMargin(flowCnec, unit);
        } else {
            return Double.NaN;
        }
    }

    @Override
    public double getRelativeMargin(Instant optimizedInstant, FlowCnec flowCnec, Unit unit) {
        FlowResult flowResult = getFlowResult(optimizedInstant, flowCnec);
        if (Objects.nonNull(flowResult)) {
            return flowResult.getRelativeMargin(flowCnec, unit);
        } else {
            return Double.NaN;
        }
    }

    @Override
    public double getFlow(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit) {
        FlowResult flowResult = getFlowResult(optimizedInstant, flowCnec);
        if (Objects.nonNull(flowResult)) {
            return flowResult.getFlow(flowCnec, side, unit, optimizedInstant);
        } else {
            return Double.NaN;
        }
    }

    @Override
    public double getCommercialFlow(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit) {
        FlowResult flowResult = getFlowResult(optimizedInstant, flowCnec);
        if (Objects.nonNull(flowResult)) {
            return flowResult.getCommercialFlow(flowCnec, side, unit);
        } else {
            return Double.NaN;
        }
    }

    @Override
    public double getLoopFlow(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit) {
        FlowResult flowResult = getFlowResult(optimizedInstant, flowCnec);
        if (Objects.nonNull(flowResult)) {
            return flowResult.getLoopFlow(flowCnec, side, unit);
        } else {
            return Double.NaN;
        }
    }

    @Override
    public double getPtdfZonalSum(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side) {
        FlowResult flowResult = getFlowResult(optimizedInstant, flowCnec);
        if (Objects.nonNull(flowResult)) {
            return flowResult.getPtdfZonalSum(flowCnec, side);
        } else {
            return Double.NaN;
        }
    }

    private FlowResult getFlowResult(Instant optimizedInstant, FlowCnec flowCnec) {
        if (optimizedInstant == null) {
            return initialResult;
        } else if (flowCnec.getState().getInstant().comesBefore(optimizedInstant)) {
            throw new OpenRaoException(String.format("Trying to access results for instant %s at optimization state %s is not allowed", flowCnec.getState().getInstant(), optimizedInstant));
        } else if (optimizedInstant.isPreventive() || optimizedInstant.isOutage()) {
            return secondPreventivePerimeterResult.getPrePerimeterResultForAllFollowingStates();
        } else {
            return postContingencyResults.get(findStateOptimizedFor(optimizedInstant, flowCnec)).getPrePerimeterResultForAllFollowingStates();
        }
    }

    private State findStateOptimizedFor(Instant optimizedInstant, FlowCnec flowCnec) {
        if (optimizedInstant.isPreventive()) {
            return null;
        }
        optimizedStateForInstantAndState.putIfAbsent(optimizedInstant, new HashMap<>());
        Map<State, State> optimizedStateForState = optimizedStateForInstantAndState.get(optimizedInstant);
        State cnecState = flowCnec.getState();
        if (optimizedStateForState.containsKey(cnecState)) {
            return optimizedStateForState.get(cnecState);
        } else {
            State optimizedState = postContingencyResults.keySet().stream().filter(state ->
                state.getInstant().equals(optimizedInstant) && state.getContingency().equals(cnecState.getContingency())
            ).findAny().orElseThrow(() -> new OpenRaoException("Contingency Results does not contain a result for every state"));
            optimizedStateForState.put(cnecState, optimizedState);
            return optimizedState;
        }
    }

    public List<FlowCnec> getMostLimitingElements() {
        //TODO : store values to be able to merge easily
        return null;
    }

    @Override
    public double getVirtualCost(Instant optimizedInstant) {
        AtomicReference<Double> s = new AtomicReference<>(0.);
        getVirtualCostNames().forEach(name -> {
            s.getAndUpdate(v -> v + this.getVirtualCost(optimizedInstant, name));
        });
        return s.get();
    }

    @Override
    public Set<String> getVirtualCostNames() {
        Set<String> virtualCostNames = new HashSet<>();
        if (initialResult.getVirtualCostNames() != null) {
            virtualCostNames.addAll(initialResult.getVirtualCostNames());
        }
        if (firstPreventivePerimeterResult.getOptimizationResult().getVirtualCostNames() != null) {
            virtualCostNames.addAll(firstPreventivePerimeterResult.getOptimizationResult().getVirtualCostNames());
        }
        if (secondPreventivePerimeterResult.getOptimizationResult().getVirtualCostNames() != null) {
            virtualCostNames.addAll(secondPreventivePerimeterResult.getOptimizationResult().getVirtualCostNames());
        }
        postContingencyResults.values().stream()
            .filter(optimizationResult -> optimizationResult.getOptimizationResult().getVirtualCostNames() != null)
            .forEach(optimizationResult -> virtualCostNames.addAll(optimizationResult.getOptimizationResult().getVirtualCostNames()));

        return virtualCostNames;
    }

    @Override
    public double getVirtualCost(Instant optimizedInstant, String virtualCostName) {
        if (optimizedInstant == null) {
            double virtualCost = initialResult.getVirtualCost(virtualCostName);
            //The cost will be NaN for mnecs and loopflows for the initial result because we do not bother computing them because they are always 0 by definition.
            return Double.isNaN(virtualCost) ? 0 : virtualCost;
        } else if (optimizedInstant.isPreventive() || optimizedInstant.isOutage()) {
            return secondPreventivePerimeterResult.getPrePerimeterResultForAllFollowingStates().getVirtualCost(virtualCostName);
        } else {
            BinaryOperator<Double> operator;
            if (virtualCostName.equals("min-margin-violation-evaluator") || virtualCostName.equals("sensitivity-failure-cost")) {
                operator = Math::max;
            } else {
                operator = Double::sum;
            }
            //initialize cost to preventive optimization cost
            AtomicReference<Double> totalCost = new AtomicReference<>(preventiveAndOutageOnlyResult.getVirtualCost(virtualCostName));
            //for states which come strictly before optimizedInstant, consider optimizationResult
            postContingencyResults.entrySet().stream()
                .filter(stateAndResult -> stateAndResult.getKey().getInstant().comesBefore(optimizedInstant))
                .forEach(stateAndResult -> totalCost.set(operator.apply(totalCost.get(), stateAndResult.getValue().getOptimizationResult().getVirtualCost(virtualCostName))));
            //for states which have same instant as optimizedInstant, consider prePerimeterResultForAllFollowingStates
            postContingencyResults.entrySet().stream()
                .filter(stateAndResult -> stateAndResult.getKey().getInstant().equals(optimizedInstant))
                .forEach(stateAndResult -> totalCost.set(operator.apply(totalCost.get(), stateAndResult.getValue().getPrePerimeterResultForAllFollowingStates().getVirtualCost(virtualCostName))));

            return totalCost.get();
        }
    }

    @Override
    public boolean wasActivatedBeforeState(State state, NetworkAction networkAction) {
        if (state.getInstant().isPreventive()) {
            return false;
        }
        State previousState = getStateOptimizedBefore(state);
        return isActivatedDuringState(previousState, networkAction) || wasActivatedBeforeState(previousState, networkAction);
    }

    @Override
    public boolean isActivatedDuringState(State state, NetworkAction networkAction) {
        if (state.getInstant().isPreventive()) {
            return (remedialActionsExcludedFromSecondPreventive.contains(networkAction) ? firstPreventivePerimeterResult : secondPreventivePerimeterResult)
                .getOptimizationResult().getActivatedNetworkActions().contains(networkAction);
        } else if (postContingencyResults.containsKey(state)) {
            return postContingencyResults.get(state).getOptimizationResult().getActivatedNetworkActions().contains(networkAction);
        } else {
            return false;
        }
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActionsDuringState(State state) {
        if (state.getInstant().isPreventive()) {
            Set<NetworkAction> set = secondPreventivePerimeterResult.getOptimizationResult().getActivatedNetworkActions();
            firstPreventivePerimeterResult.getOptimizationResult().getActivatedNetworkActions().stream()
                .filter(remedialActionsExcludedFromSecondPreventive::contains)
                .forEach(set::add);
            return set;
        } else if (postContingencyResults.containsKey(state)) {
            return postContingencyResults.get(state).getOptimizationResult().getActivatedNetworkActions();
        } else {
            return new HashSet<>();
        }
    }

    @Override
    public boolean isActivatedDuringState(State state, RangeAction<?> rangeAction) {
        if (state.getInstant().isPreventive()) {
            return (remedialActionsExcludedFromSecondPreventive.contains(rangeAction) ? firstPreventivePerimeterResult : secondPreventivePerimeterResult)
                .getOptimizationResult().getActivatedRangeActions(state).contains(rangeAction);
        } else if (postContingencyResults.containsKey(state)) {
            return postContingencyResults.get(state).getOptimizationResult().getActivatedRangeActions(state).contains(rangeAction);
        } else {
            return false;
        }
    }

    private void throwIfNotOptimized(State state) {
        if (!postContingencyResults.containsKey(state)) {
            throw new OpenRaoException(String.format("State %s was not optimized and does not have pre-optim values", state.getId()));
        }
    }

    @Override
    public int getPreOptimizationTapOnState(State state, PstRangeAction pstRangeAction) {
        if (state.getInstant().isPreventive()) {
            return initialResult.getTap(pstRangeAction);
        }
        throwIfNotOptimized(state);
        State previousState = getStateOptimizedBefore(state);
        if (preventiveState.equals(previousState)) {
            return (remedialActionsExcludedFromSecondPreventive.contains(pstRangeAction) ? firstPreventivePerimeterResult : secondPreventivePerimeterResult)
                .getOptimizationResult().getOptimizedTap(pstRangeAction, preventiveState);
        } else {
            return postContingencyResults.get(previousState).getOptimizationResult().getOptimizedTap(pstRangeAction, previousState);
        }
    }

    @Override
    public int getOptimizedTapOnState(State state, PstRangeAction pstRangeAction) {
        if (state.getInstant().isPreventive() || !postContingencyResults.containsKey(state)) {
            return (remedialActionsExcludedFromSecondPreventive.contains(pstRangeAction) ? firstPreventivePerimeterResult : secondPreventivePerimeterResult)
                .getOptimizationResult().getOptimizedTap(pstRangeAction, state);
        } else {
            return postContingencyResults.get(state).getOptimizationResult().getOptimizedTap(pstRangeAction, state);
        }
    }

    @Override
    public double getPreOptimizationSetPointOnState(State state, RangeAction<?> rangeAction) {
        if (state.getInstant().isPreventive()) {
            return initialResult.getSetpoint(rangeAction);
        }
        throwIfNotOptimized(state);
        State previousState = getStateOptimizedBefore(state);
        if (preventiveState.equals(previousState)) {
            return (remedialActionsExcludedFromSecondPreventive.contains(rangeAction) ? firstPreventivePerimeterResult : secondPreventivePerimeterResult)
                .getOptimizationResult().getOptimizedSetpoint(rangeAction, preventiveState);
        } else {
            return postContingencyResults.get(previousState).getOptimizationResult().getOptimizedSetpoint(rangeAction, previousState);
        }
    }

    @Override
    public double getOptimizedSetPointOnState(State state, RangeAction<?> rangeAction) {
        if (state.getInstant().isPreventive() || !postContingencyResults.containsKey(state)) {
            return (remedialActionsExcludedFromSecondPreventive.contains(rangeAction) ? firstPreventivePerimeterResult : secondPreventivePerimeterResult)
                .getOptimizationResult().getOptimizedSetpoint(rangeAction, state);
        } else {
            return postContingencyResults.get(state).getOptimizationResult().getOptimizedSetpoint(rangeAction, state);
        }
    }

    @Override
    public Set<RangeAction<?>> getActivatedRangeActionsDuringState(State state) {
        if (state.getInstant().isPreventive()) {
            Set<RangeAction<?>> set = secondPreventivePerimeterResult.getOptimizationResult().getActivatedRangeActions(state);
            firstPreventivePerimeterResult.getOptimizationResult().getActivatedRangeActions(state).stream()
                .filter(remedialActionsExcludedFromSecondPreventive::contains)
                .forEach(set::add);
            return set;
        } else if (postContingencyResults.containsKey(state)) {
            return postContingencyResults.get(state).getOptimizationResult().getActivatedRangeActions(state);
        } else {
            return new HashSet<>();
        }
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        if (state.getInstant().isPreventive() || !postContingencyResults.containsKey(state)) {
            Map<PstRangeAction, Integer> map = new HashMap<>(secondPreventivePerimeterResult.getOptimizationResult().getOptimizedTapsOnState(state));
            firstPreventivePerimeterResult.getOptimizationResult().getOptimizedTapsOnState(state).entrySet().stream()
                .filter(entry -> remedialActionsExcludedFromSecondPreventive.contains(entry.getKey()))
                .forEach(entry -> map.put(entry.getKey(), entry.getValue()));
            return map;
        } else {
            return postContingencyResults.get(state).getOptimizationResult().getOptimizedTapsOnState(state);
        }
    }

    @Override
    public Map<RangeAction<?>, Double> getOptimizedSetPointsOnState(State state) {
        if (state.getInstant().isPreventive() || !postContingencyResults.containsKey(state)) {
            Map<RangeAction<?>, Double> map = new HashMap<>(secondPreventivePerimeterResult.getOptimizationResult().getOptimizedSetpointsOnState(state));
            firstPreventivePerimeterResult.getOptimizationResult().getOptimizedSetpointsOnState(state).entrySet().stream()
                .filter(entry -> remedialActionsExcludedFromSecondPreventive.contains(entry.getKey()))
                .forEach(entry -> map.put(entry.getKey(), entry.getValue()));
            return map;
        } else {
            return postContingencyResults.get(state).getOptimizationResult().getOptimizedSetpointsOnState(state);
        }
    }

    private State getStateOptimizedBefore(State state) {
        if (state.getInstant().isPreventive()) {
            throw new OpenRaoException("No state before preventive.");
        } else if (state.getInstant().isOutage() || state.getInstant().isAuto()) {
            return preventiveState;
        } else {
            // curative
            Contingency contingency = state.getContingency().orElseThrow();
            return postContingencyResults.keySet().stream()
                .filter(mapState -> mapState.getContingency().equals(Optional.of(contingency)))
                .filter(mapState -> mapState.getInstant().isAuto() || mapState.getInstant().isCurative())
                .filter(mapState -> mapState.getInstant().comesBefore(state.getInstant()))
                .max(Comparator.comparingInt(mapState -> mapState.getInstant().getOrder()))
                .orElse(preventiveState);
        }
    }

    @Override
    public void setExecutionDetails(String executionDetails) {
        this.executionDetails = executionDetails;
    }

    @Override
    public boolean isSecure(PhysicalParameter... u) {
        return isSecure(crac.getLastInstant(), u);
    }

    @Override
    public String getExecutionDetails() {
        return executionDetails;
    }
}
