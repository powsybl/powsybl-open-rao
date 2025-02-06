/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.commons.optimizationperimeters;

import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;

import java.util.Map;
import java.util.Set;

/**
 * Data structure to store the information needed to run a search tree on a perimeter.
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface OptimizationPerimeter {

    /**
     The main optimization state is:
     - the state on which the network actions are optimized
     - the first state (chronologically speaking) on which range actions are optimized
     */
    State getMainOptimizationState(); // TODO: this is ambiguous for 2P

    /**
     Returns the set of states on which range actions are optimized.

     In most cases, getRangeActionOptimizationStates() will only contain getMainOptimizationState()

     Though, if RangeActions are optimized in preventive and in curative (for instance, in the
     2nd prev optimization), getRangeActionOptimizationStates() will gather all the states on which
     at least one RangeAction can be optimized.
     */
    Set<State> getRangeActionOptimizationStates();

    /**
     Returns the set of states which are monitored by the perimeter

     A 'monitored state' is a state which contains at least one FlowCnec:
     - which is optimized, or
     - which is monitored, or
     - which has loop-flows which are monitored

     A 'monitored state' does not necessarily have RemedialActions
     */
    Set<State> getMonitoredStates();

    /**
     Returns the set of FlowCnec which are either optimized or monitored by the perimeter
     */
    Set<FlowCnec> getFlowCnecs();

    /**
     Returns the set of FlowCnec which are optimized by the perimeter
     */
    Set<FlowCnec> getOptimizedFlowCnecs();

    /**
     Returns the set of FlowCnec which are monitored by the perimeter
     */
    Set<FlowCnec> getMonitoredFlowCnecs();

    /**
     Returns the set of FlowCnec whose loop-flows are monitored
     */
    Set<FlowCnec> getLoopFlowCnecs();

    /**
     Returns the set of network actions which will be optimized on the main optimization state
     */
    Set<NetworkAction> getNetworkActions();

    /**
     For each state of getRangeActionOptimizationStates(), returns the set of range actions which
     will be optimized on this state
     */
    Map<State, Set<RangeAction<?>>> getRangeActionsPerState();

    /**
     Returns a set of all the RangeActions that will be optimized on any state
     */
    Set<RangeAction<?>> getRangeActions();

}
