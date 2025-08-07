/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Identifiable;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class FillersUtil {
    private FillersUtil() {
    }

    static Set<State> getPreviousStates(State refState, Map<State, Set<RangeAction<?>>> availableRangeActionsPerState) {
        return availableRangeActionsPerState.keySet().stream()
                .filter(s -> s.getContingency().equals(refState.getContingency()) || s.getContingency().isEmpty())
                .filter(s -> s.getInstant().comesBefore(refState.getInstant()) || s.getInstant().equals(refState.getInstant()))
                .collect(Collectors.toSet());
    }

    /**
     * Filters out flow CNECs with a state that failed sensitivity computation
     *
     * @param flowCnecs:         the flow CNECs to filter through
     * @param sensitivityResult: the sensitivity result containing computation statuses for the flow CNECs' states
     * @return a set of filtered CNECs, containing only flow CNECs with a state that succeeded sensitivity computation
     */
    static Set<FlowCnec> getFlowCnecsComputationStatusOk(Set<FlowCnec> flowCnecs, SensitivityResult sensitivityResult) {
        Set<State> skippedStates = flowCnecs.stream().map(Cnec::getState).distinct()
            .filter(state -> sensitivityResult.getSensitivityStatus(state).equals(ComputationStatus.FAILURE)).collect(Collectors.toSet());
        return flowCnecs.stream().filter(cnec -> !skippedStates.contains(cnec.getState()))
            .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Identifiable::getId))));
    }

    /**
     * Filters out flow CNECs that failed flow computation
     *
     * @param flowCnecs:  the flow CNECs to filter through
     * @param flowResult: the flow result containing real or NaN values for CNEC flows
     * @return a set of filtered CNECs, containing only flow CNECs with a non-NaN flow value
     */
    static Set<FlowCnec> getFlowCnecsNotNaNFlow(Set<FlowCnec> flowCnecs, FlowResult flowResult) {
        // TODO : add a computation status per state to FlowResult and filter on states, like with SensitivityComputationResult
        return flowCnecs.stream().filter(cnec ->
            cnec.getMonitoredSides().stream().noneMatch(side ->
                Double.isNaN(flowResult.getFlow(cnec, side, Unit.MEGAWATT)))
        ).collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Identifiable::getId))));
    }
}
