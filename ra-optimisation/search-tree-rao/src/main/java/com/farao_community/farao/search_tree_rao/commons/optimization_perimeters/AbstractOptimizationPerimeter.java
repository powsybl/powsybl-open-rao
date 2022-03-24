/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.commons.optimization_perimeters;

import com.farao_community.farao.data.crac_api.Identifiable;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_loopflow_extension.LoopFlowThreshold;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionSetpointResult;
import com.powsybl.iidm.network.Network;

import java.util.*;
import java.util.stream.Collectors;

import static com.farao_community.farao.commons.logs.FaraoLoggerProvider.BUSINESS_WARNS;

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

    public AbstractOptimizationPerimeter(State mainOptimizationState,
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

    static Set<FlowCnec> getLoopFlowCnecs(Set<FlowCnec> flowCnecs, RaoParameters raoParameters, Network network) {

        Set<FlowCnec> loopFlowCnecs;
        if (raoParameters.isRaoWithLoopFlowLimitation() && !raoParameters.getLoopflowCountries().isEmpty()) {

            // loopFlow limited, and set of country for which loop-flow are monitored is defined
            return flowCnecs.stream()
                .filter(cnec -> !Objects.isNull(cnec.getExtension(LoopFlowThreshold.class)) &&
                    cnec.getLocation(network).stream().anyMatch(country -> country.isPresent() && raoParameters.getLoopflowCountries().contains(country.get())))
                .collect(Collectors.toSet());
        } else if (raoParameters.isRaoWithLoopFlowLimitation()) {

            // loopFlow limited, but no set of country defined
            return flowCnecs.stream()
                .filter(cnec -> !Objects.isNull(cnec.getExtension(LoopFlowThreshold.class)))
                .collect(Collectors.toSet());
        } else {

            //no loopFLow limitation
            return Collections.emptySet();
        }
    }

    static boolean doesPrePerimeterSetpointRespectRange(RangeAction<?> rangeAction, RangeActionSetpointResult prePerimeterSetpoints) {
        double preperimeterSetPoint = prePerimeterSetpoints.getSetpoint(rangeAction);
        double minSetPoint = rangeAction.getMinAdmissibleSetpoint(preperimeterSetPoint);
        double maxSetPoint = rangeAction.getMaxAdmissibleSetpoint(preperimeterSetPoint);

        if (preperimeterSetPoint < minSetPoint || preperimeterSetPoint > maxSetPoint) {
            BUSINESS_WARNS.warn("Range action {} has an initial setpoint of {} that does not respect its allowed range [{} {}]. It will be filtered out of the linear problem.",
                rangeAction.getId(), preperimeterSetPoint, minSetPoint, maxSetPoint);
            return false;
        } else {
            return true;
        }
    }

}
