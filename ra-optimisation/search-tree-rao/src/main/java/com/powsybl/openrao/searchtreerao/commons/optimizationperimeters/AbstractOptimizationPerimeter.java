/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.optimizationperimeters;

import com.powsybl.openrao.data.crac.api.Identifiable;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.loopflowextension.LoopFlowThreshold;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.iidm.network.Network;

import java.util.*;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public abstract class AbstractOptimizationPerimeter implements OptimizationPerimeter {

    private final State mainOptimizationState;
    private final Set<State> monitoredStates;
    private final Set<FlowCnec> flowCnecs;
    private final Set<FlowCnec> optimizedFlowCnecs;
    private final Set<FlowCnec> monitoredFlowCnecs;
    private final Set<FlowCnec> loopFlowCnecs;
    private final Set<NetworkAction> availableNetworkActions;
    private final Map<State, Set<RangeAction<?>>> availableRangeActions;
    private static final double EPSILON = 1e-6;

    protected AbstractOptimizationPerimeter(State mainOptimizationState,
                                         Set<FlowCnec> flowCnecs,
                                         Set<FlowCnec> loopFlowCnecs,
                                         Set<NetworkAction> availableNetworkActions,
                                         Map<State, Set<RangeAction<?>>> availableRangeActions) {
        this.mainOptimizationState = mainOptimizationState;

        this.monitoredStates = flowCnecs.stream().map(FlowCnec::getState).collect(Collectors.toSet());

        this.flowCnecs = new TreeSet<>(Comparator.comparing(Identifiable::getId));
        this.flowCnecs.addAll(flowCnecs);

        this.optimizedFlowCnecs = new TreeSet<>(Comparator.comparing(Identifiable::getId));
        this.optimizedFlowCnecs.addAll(flowCnecs.stream().filter(Cnec::isOptimized).collect(Collectors.toSet()));

        this.monitoredFlowCnecs = new TreeSet<>(Comparator.comparing(Identifiable::getId));
        this.monitoredFlowCnecs.addAll(flowCnecs.stream().filter(Cnec::isMonitored).collect(Collectors.toSet()));

        this.loopFlowCnecs = new TreeSet<>(Comparator.comparing(Identifiable::getId));
        this.loopFlowCnecs.addAll(loopFlowCnecs);

        this.availableNetworkActions = new TreeSet<>(Comparator.comparing(Identifiable::getId));
        this.availableNetworkActions.addAll(availableNetworkActions);

        this.availableRangeActions = new TreeMap<>(Comparator.comparing(State::getId));
        availableRangeActions.forEach((state, rangeActions) -> {
            Set<RangeAction<?>> rangeActionSet = new TreeSet<>(Comparator.comparing(Identifiable::getId));
            rangeActionSet.addAll(availableRangeActions.get(state));
            this.availableRangeActions.put(state, rangeActionSet);
        });
    }

    @Override
    public State getMainOptimizationState() {
        return mainOptimizationState;
    }

    @Override
    public Set<State> getRangeActionOptimizationStates() {
        return availableRangeActions.keySet();
    }

    @Override
    public Set<State> getMonitoredStates() {
        return monitoredStates;
    }

    @Override
    public Set<FlowCnec> getFlowCnecs() {
        return flowCnecs;
    }

    @Override
    public Set<FlowCnec> getOptimizedFlowCnecs() {
        return optimizedFlowCnecs;
    }

    @Override
    public Set<FlowCnec> getMonitoredFlowCnecs() {
        return monitoredFlowCnecs;
    }

    @Override
    public Set<FlowCnec> getLoopFlowCnecs() {
        return loopFlowCnecs;
    }

    @Override
    public Set<NetworkAction> getNetworkActions() {
        return availableNetworkActions;
    }

    @Override
    public Map<State, Set<RangeAction<?>>> getRangeActionsPerState() {
        return availableRangeActions;
    }

    @Override
    public Set<RangeAction<?>> getRangeActions() {
        return availableRangeActions.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
    }

    public static Set<FlowCnec> getLoopFlowCnecs(Set<FlowCnec> flowCnecs, RaoParameters raoParameters, Network network) {
        Optional<com.powsybl.openrao.raoapi.parameters.LoopFlowParameters> loopFlowParametersOptional = raoParameters.getLoopFlowParameters();
        if (loopFlowParametersOptional.isPresent()) {
            if (!loopFlowParametersOptional.get().getCountries().isEmpty()) {
                // loopFlow limited, and set of country for which loop-flow are monitored is defined
                return flowCnecs.stream()
                    .filter(cnec -> !Objects.isNull(cnec.getExtension(LoopFlowThreshold.class)) &&
                        cnec.getLocation(network).stream().anyMatch(country -> loopFlowParametersOptional.get().getCountries().contains(country)))
                    .collect(Collectors.toSet());
            } else {
                // loopFlow limited, but no set of country defined
                return flowCnecs.stream()
                    .filter(cnec -> !Objects.isNull(cnec.getExtension(LoopFlowThreshold.class)))
                    .collect(Collectors.toSet());
            }
        } else {
            //no loopFLow limitation
            return Collections.emptySet();
        }
    }

    static boolean doesPrePerimeterSetpointRespectRange(RangeAction<?> rangeAction, RangeActionSetpointResult prePerimeterSetpoints) {
        double preperimeterSetPoint = prePerimeterSetpoints.getSetpoint(rangeAction);
        double minSetPoint = rangeAction.getMinAdmissibleSetpoint(preperimeterSetPoint);
        double maxSetPoint = rangeAction.getMaxAdmissibleSetpoint(preperimeterSetPoint);

        if (preperimeterSetPoint < minSetPoint - EPSILON || preperimeterSetPoint > maxSetPoint + EPSILON) {
            BUSINESS_WARNS.warn("Range action {} has an initial setpoint of {} that does not respect its allowed range [{} {}]. It will be filtered out of the linear problem.",
                rangeAction.getId(), preperimeterSetPoint, minSetPoint, maxSetPoint);
            return false;
        } else {
            return true;
        }
    }

    /**
     * If aligned range actions' initial setpoint are different, this function filters them out
     */
    static void removeAlignedRangeActionsWithDifferentInitialSetpoints(Set<RangeAction<?>> rangeActions, RangeActionSetpointResult prePerimeterSetPoints) {
        Set<String> groups = rangeActions.stream().map(RangeAction::getGroupId)
            .filter(Optional::isPresent).map(Optional::get).collect(Collectors.toSet());
        for (String group : groups) {
            Set<RangeAction<?>> groupRangeActions = rangeActions.stream().filter(rangeAction -> rangeAction.getGroupId().isPresent() && rangeAction.getGroupId().get().equals(group)).collect(Collectors.toSet());
            double preperimeterSetPoint = prePerimeterSetPoints.getSetpoint(groupRangeActions.iterator().next());
            if (groupRangeActions.stream().anyMatch(rangeAction -> Math.abs(prePerimeterSetPoints.getSetpoint(rangeAction) - preperimeterSetPoint) > EPSILON)) {
                BUSINESS_WARNS.warn("Range actions of group {} do not have the same prePerimeter setpoint. They will be filtered out of the linear problem.", group);
                rangeActions.removeAll(groupRangeActions);
            }
        }
    }
}
