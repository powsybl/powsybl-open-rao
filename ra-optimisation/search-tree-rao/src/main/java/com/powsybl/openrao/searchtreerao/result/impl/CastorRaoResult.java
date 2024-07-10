/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.*;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.data.raoresultapi.OptimizationStepsExecuted;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;

import java.util.*;
import java.util.stream.Stream;

import static com.powsybl.openrao.data.raoresultapi.ComputationStatus.DEFAULT;
import static com.powsybl.openrao.data.raoresultapi.ComputationStatus.FAILURE;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class CastorRaoResult implements RaoResult {
    private final PerimeterResultWithCnecs initialResult;
    private final Map<Instant, Map<Contingency, PerimeterResultWithCnecs>> optimResultsPerState;
    private final State preventiveState;
    private OptimizationStepsExecuted optimizationStepsExecuted;

    public CastorRaoResult(PerimeterResultWithCnecs initialResult,
                           Map<Instant, Map<Contingency, PerimeterResultWithCnecs>> optimResultsPerState,
                           State preventiveState,
                           OptimizationStepsExecuted optimizationStepsExecuted) {
        this.initialResult = initialResult;
        this.optimResultsPerState = optimResultsPerState;
        this.preventiveState = preventiveState;
        this.optimizationStepsExecuted = optimizationStepsExecuted;
    }

    public static CastorRaoResult buildWithOnlyPreventiveResult(PerimeterResultWithCnecs initialResult, PerimeterResultWithCnecs preventiveResult, Crac crac) {
        Map<Instant, Map<Contingency, PerimeterResultWithCnecs>> optimResultsPerState = new HashMap<>();

        Map<Contingency, PerimeterResultWithCnecs> preventiveMap = new HashMap<>();
        preventiveMap.put(null, preventiveResult);
        optimResultsPerState.put(crac.getPreventiveInstant(), preventiveMap);

        Instant previousInstant = crac.getPreventiveInstant();
        for (Instant instant : crac.getSortedInstants()) {
            if (instant.isAuto() || instant.isCurative()) {
                optimResultsPerState.put(instant, createInstantResultsPerContingencyOnlyPreventive(instant, previousInstant, crac, preventiveResult, optimResultsPerState));
            }
            previousInstant = instant;
        }
        return new CastorRaoResult(initialResult, optimResultsPerState, crac.getPreventiveState(), OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY);
    }

    private static Map<Contingency, PerimeterResultWithCnecs> createInstantResultsPerContingencyOnlyPreventive(
        Instant instant,
        Instant previousInstant,
        Crac crac,
        PerimeterResultWithCnecs preventiveResult,
        Map<Instant, Map<Contingency, PerimeterResultWithCnecs>> optimResultsPerState) {

        Map<Contingency, PerimeterResultWithCnecs> resultsPerContingency = new HashMap<>();

        crac.getStates(instant).forEach(state -> {
            Contingency contingency = state.getContingency().get();
            if (previousInstant.isPreventive()) {
                resultsPerContingency.put(contingency, PerimeterResultWithCnecs.buildFromPreviousResult(preventiveResult));
            } else {
                resultsPerContingency.put(contingency, PerimeterResultWithCnecs.buildFromPreviousResult(optimResultsPerState.get(previousInstant).get(contingency)));
            }
        });

        return resultsPerContingency;
    }

    public static RaoResult buildWithResults(
        PerimeterResultWithCnecs initialResult,
        PerimeterResultWithCnecs secondPreventiveResult,
        Map<State, PerimeterResultWithCnecs> autoResults,
        PerimeterResultWithCnecs postCraResult,
        AppliedRemedialActions appliedArasAndCras,
        Crac crac) {

        Map<Instant, Map<Contingency, PerimeterResultWithCnecs>> optimResultsPerState = new HashMap<>();

        Map<Contingency, PerimeterResultWithCnecs> preventiveMap = new HashMap<>();
        preventiveMap.put(null, secondPreventiveResult);
        optimResultsPerState.put(crac.getPreventiveInstant(), preventiveMap);

        crac.getSortedInstants().forEach(instant -> optimResultsPerState.putIfAbsent(instant, new HashMap<>()));

        for (Contingency contingency : crac.getContingencies()) {
            PerimeterResultWithCnecs previousResult = secondPreventiveResult;
            for (Instant instant : crac.getSortedInstants()) {
                State state = crac.getState(contingency, instant);
                if (Objects.isNull(state)) {
                    continue;
                }
                if (instant.isAuto()) {
                    if (autoResults.containsKey(state)) {
                        optimResultsPerState.get(instant).put(contingency, autoResults.get(state));
                        previousResult = autoResults.get(state);
                    } else {
                        PerimeterResultWithCnecs newResult = PerimeterResultWithCnecs.buildFromPreviousResult(previousResult);
                        optimResultsPerState.get(instant).put(contingency, newResult);
                        previousResult = newResult;
                    }
                } else if (instant.isCurative()) {
                    if (appliedArasAndCras.getAppliedNetworkActions(state).isEmpty() && appliedArasAndCras.getAppliedRangeActions(state).isEmpty()) {
                        PerimeterResultWithCnecs newResult = PerimeterResultWithCnecs.buildFromPreviousResult(previousResult);
                        optimResultsPerState.get(instant).put(contingency, newResult);
                        previousResult = newResult;
                    } else {
                        PerimeterResultWithCnecs newResult = PerimeterResultWithCnecs.buildFromSensiResultAndAppliedRas(postCraResult, appliedArasAndCras, state, previousResult);
                        optimResultsPerState.get(instant).put(contingency, newResult);
                        previousResult = newResult;
                    }
                }
            }
        }

        return new CastorRaoResult(initialResult, optimResultsPerState, crac.getPreventiveState(), OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY);
    }

    private static Map<Contingency, PerimeterResultWithCnecs> createInstantResultsPerContingency(
        Instant instant,
        Instant previousInstant,
        Crac crac,
        PerimeterResultWithCnecs preventiveResult,
        Map<Instant, Map<Contingency, PerimeterResultWithCnecs>> optimResultsPerState,
        Map<State, PerimeterResultWithCnecs> postContingencyResults) {

        Map<Contingency, PerimeterResultWithCnecs> resultsPerContingency = new HashMap<>();

        crac.getStates(instant).forEach(state -> {
            Contingency contingency = state.getContingency().get();
            if (postContingencyResults.containsKey(state)) {
                resultsPerContingency.put(contingency, postContingencyResults.get(state));
            } else {
                if (Objects.isNull(previousInstant)) {
                    resultsPerContingency.put(contingency, PerimeterResultWithCnecs.buildFromPreviousResult(preventiveResult));
                } else {
                    PerimeterResultWithCnecs previousResult = optimResultsPerState.get(previousInstant).get(contingency);
                    resultsPerContingency.put(contingency, PerimeterResultWithCnecs.buildFromPreviousResult(previousResult));
                }
            }
        });

        return resultsPerContingency;
    }

    public static RaoResult buildWithResults(PerimeterResultWithCnecs initialResult, PerimeterResultWithCnecs preventiveResult, Map<State, PerimeterResultWithCnecs> postContingencyResults, Crac crac) {
        Map<Instant, Map<Contingency, PerimeterResultWithCnecs>> optimResultsPerState = new HashMap<>();

        Map<Contingency, PerimeterResultWithCnecs> preventiveMap = new HashMap<>();
        preventiveMap.put(null, preventiveResult);
        optimResultsPerState.put(crac.getPreventiveInstant(), preventiveMap);

        Map<Integer, Instant> orderToInstantMap = new HashMap<>();
        for (Instant instant : crac.getSortedInstants()) {
            if (instant.isAuto() || instant.isCurative()) {
                orderToInstantMap.put(instant.getOrder(), instant);
                Instant previousInstant = orderToInstantMap.get(instant.getOrder() - 1);
                optimResultsPerState.put(instant, createInstantResultsPerContingency(instant, previousInstant, crac, preventiveResult, optimResultsPerState, postContingencyResults));
            }
        }
        return new CastorRaoResult(initialResult, optimResultsPerState, crac.getPreventiveState(), OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY);
    }

    private Stream<PerimeterResultWithCnecs> getAllResultsStream() {
        return optimResultsPerState.values().stream().flatMap(map -> map.values().stream());
    }

    @Override
    public ComputationStatus getComputationStatus() {
        if (initialResult.getSensitivityStatus() == FAILURE
            || getAllResultsStream().anyMatch(result -> result.getSensitivityStatus() == FAILURE)) {
            return FAILURE;
        }
        return DEFAULT;
    }

    private PerimeterResultWithCnecs getResult(State optimizedState) {
        return getResult(optimizedState.getInstant(), optimizedState.getContingency().orElse(null));
    }

    private PerimeterResultWithCnecs getResult(Instant optimizedInstant, Contingency contingency) {
        if (Objects.isNull(optimizedInstant)) {
            return initialResult;
        } else if (optimizedInstant.isPreventive() || optimizedInstant.isOutage()) {
            return optimResultsPerState.get(preventiveState.getInstant()).get(null);
        } else {
            return optimResultsPerState.get(optimizedInstant).get(contingency);
        }
    }

    @Override
    public ComputationStatus getComputationStatus(State state) {
        //second argument required for post PRA for instance
        return getResult(state).getSensitivityStatus(state);
    }

    @Override
    public double getFlow(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit) {
        return getResult(optimizedInstant, flowCnec.getState().getContingency().orElse(null)).getFlow(flowCnec, side, unit);
    }

    @Override
    public double getMargin(Instant optimizedInstant, FlowCnec flowCnec, Unit unit) {
        return getResult(optimizedInstant, flowCnec.getState().getContingency().orElse(null)).getMargin(flowCnec, unit);
    }

    @Override
    public double getRelativeMargin(Instant optimizedInstant, FlowCnec flowCnec, Unit unit) {
        return getResult(optimizedInstant, flowCnec.getState().getContingency().orElse(null)).getRelativeMargin(flowCnec, unit);
    }

    @Override
    public double getCommercialFlow(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit) {
        return getResult(optimizedInstant, flowCnec.getState().getContingency().orElse(null)).getCommercialFlow(flowCnec, side, unit);
    }

    @Override
    public double getLoopFlow(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit) {
        return getResult(optimizedInstant, flowCnec.getState().getContingency().orElse(null)).getLoopFlow(flowCnec, side, unit);
    }

    @Override
    public double getPtdfZonalSum(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side) {
        return getResult(optimizedInstant, flowCnec.getState().getContingency().orElse(null)).getPtdfZonalSum(flowCnec, side);
    }

    @Override
    public double getFunctionalCost(Instant optimizedInstant) {
        if (Objects.isNull(optimizedInstant)) {
            return initialResult.getFunctionalCost();
        } else {
            return optimResultsPerState.get(optimizedInstant).values().stream()
                .map(PerimeterResultWithCnecs::getFunctionalCost)
                .max(Double::compareTo)
                .orElse(-Double.MAX_VALUE);
        }
    }

    @Override
    public double getVirtualCost(Instant optimizedInstant) {
        if (Objects.isNull(optimizedInstant)) {
            return initialResult.getVirtualCost();
        } else {
            return optimResultsPerState.get(optimizedInstant).values().stream()
                .mapToDouble(PerimeterResultWithCnecs::getVirtualCost)
                .sum();
        }
    }

    @Override
    public Set<String> getVirtualCostNames() {
        Set<String> virtualCostNames = new HashSet<>();
        if (initialResult.getVirtualCostNames() != null) {
            virtualCostNames.addAll(initialResult.getVirtualCostNames());
        }
        getAllResultsStream().forEach(result -> virtualCostNames.addAll(result.getVirtualCostNames()));
        return virtualCostNames;
    }

    @Override
    public double getVirtualCost(Instant optimizedInstant, String virtualCostName) {
        if (Objects.isNull(optimizedInstant)) {
            return initialResult.getVirtualCost(virtualCostName);
        } else {
            return optimResultsPerState.get(optimizedInstant).values().stream()
                .mapToDouble(result -> result.getVirtualCost(virtualCostName))
                .sum();
        }
    }

    @Override
    public boolean isActivatedDuringState(State optimizedState, RemedialAction<?> remedialAction) {
        if (remedialAction instanceof NetworkAction networkAction) {
            return isActivatedDuringState(optimizedState, networkAction);
        } else if (remedialAction instanceof RangeAction<?> rangeAction) {
            return isActivatedDuringState(optimizedState, rangeAction);
        } else {
            throw new OpenRaoException("Unrecognized remedial action type");
        }
    }

    @Override
    public boolean wasActivatedBeforeState(State optimizedState, NetworkAction networkAction) {
        if (optimizedState.getInstant().isPreventive()) {
            return false;
        }
        return optimResultsPerState.keySet().stream().anyMatch(instant ->
            instant.comesBefore(optimizedState.getInstant()) &&
            getResult(instant, optimizedState.getContingency().orElse(null)).getActivatedNetworkActions().contains(networkAction)
        );

        //TODO: also check actions on same elements?
    }

    @Override
    public boolean isActivatedDuringState(State optimizedState, NetworkAction networkAction) {
        return getResult(optimizedState).getActivatedNetworkActions().contains(networkAction);
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActionsDuringState(State optimizedState) {
        return getResult(optimizedState).getActivatedNetworkActions();
    }

    @Override
    public boolean isActivatedDuringState(State optimizedState, RangeAction<?> rangeAction) {
        return getResult(optimizedState).getActivatedRangeActions().contains(rangeAction);
    }

    @Override
    public int getPreOptimizationTapOnState(State optimizedState, PstRangeAction pstRangeAction) {
        return getResult(optimizedState).getPreviousResult().getOptimizedTap(pstRangeAction);
    }

    @Override
    public int getOptimizedTapOnState(State optimizedState, PstRangeAction pstRangeAction) {
        return getResult(optimizedState).getOptimizedTap(pstRangeAction);
    }

    @Override
    public double getPreOptimizationSetPointOnState(State optimizedState, RangeAction<?> rangeAction) {
        return getResult(optimizedState).getPreviousResult().getOptimizedSetpoint(rangeAction);
    }

    @Override
    public double getOptimizedSetPointOnState(State optimizedState, RangeAction<?> rangeAction) {
        return getResult(optimizedState).getOptimizedSetpoint(rangeAction);
    }

    @Override
    public Set<RangeAction<?>> getActivatedRangeActionsDuringState(State optimizedState) {
        return getResult(optimizedState).getActivatedRangeActions();
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State optimizedState) {
        return getResult(optimizedState).getOptimizedTaps();
    }

    @Override
    public Map<RangeAction<?>, Double> getOptimizedSetPointsOnState(State optimizedState) {
        return getResult(optimizedState).getOptimizedSetpoints();
    }

    @Override
    public OptimizationStepsExecuted getOptimizationStepsExecuted() {
        return optimizationStepsExecuted;
    }

    @Override
    public void setOptimizationStepsExecuted(OptimizationStepsExecuted optimizationStepsExecuted) {
        this.optimizationStepsExecuted = optimizationStepsExecuted;
    }

    @Override
    public boolean isSecure(Instant optimizedInstant, PhysicalParameter... u) {
        if (ComputationStatus.FAILURE.equals(getComputationStatus())) {
            return false;
        }
        return getFunctionalCost(optimizedInstant) < 0;
    }

    @Override
    public boolean isSecure(PhysicalParameter... u) {
        Instant lastInstant = optimResultsPerState.keySet().stream().max(Comparator.comparingInt(Instant::getOrder)).orElseThrow();
        return isSecure(lastInstant, u);
    }
}
