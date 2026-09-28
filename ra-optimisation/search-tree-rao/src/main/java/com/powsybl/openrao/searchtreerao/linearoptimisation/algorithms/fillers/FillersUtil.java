/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Identifiable;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
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

    static Set<State> getPreviousStates(State refState, OptimizationPerimeter optimizationContext) {
        return optimizationContext.getRangeActionsPerState().keySet().stream()
                .filter(s -> s.getContingency().equals(refState.getContingency()) || s.getContingency().isEmpty())
                .filter(s -> s.getInstant().comesBefore(refState.getInstant()) || s.getInstant().equals(refState.getInstant()))
                .collect(Collectors.toSet());
    }

    /**
     * Filters out flow CNECs with a state that failed sensitivity computation
     *
     * @param flowCnecs         the flow CNECs to filter through
     * @param sensitivityResult the sensitivity result containing computation statuses for the flow CNECs' states
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
     * @param flowCnecs  the flow CNECs to filter through
     * @param flowResult the flow result containing real or NaN values for CNEC flows
     * @return a set of filtered CNECs, containing only flow CNECs with a non-NaN flow value
     */
    static Set<FlowCnec> getFlowCnecsNotNaNFlow(Set<FlowCnec> flowCnecs, FlowResult flowResult) {
        // TODO : add a computation status per state to FlowResult and filter on states, like with SensitivityComputationResult
        return flowCnecs.stream().filter(cnec ->
            cnec.getMonitoredSides().stream().noneMatch(side ->
                Double.isNaN(flowResult.getFlow(cnec, side, Unit.MEGAWATT)))
        ).collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Identifiable::getId))));
    }

    public static double computeTimestampDuration(List<OffsetDateTime> timestamps) {
        if (timestamps.size() < 2) {
            throw new OpenRaoException("There must be at least two timestamps.");
        }
        double referenceTimestampDuration = computeTimeGap(timestamps.getFirst(), timestamps.get(1));
        for (int timestampIndex = 1; timestampIndex < timestamps.size() - 1; timestampIndex++) {
            double timestampDuration = computeTimeGap(timestamps.get(timestampIndex), timestamps.get(timestampIndex + 1));
            if (timestampDuration != referenceTimestampDuration) {
                throw new OpenRaoException("All timestamps are not evenly spread.");
            }
        }
        return referenceTimestampDuration;
    }

    private static double computeTimeGap(OffsetDateTime timestamp1, OffsetDateTime timestamp2) {
        if (timestamp1 == null || timestamp2 == null) {
            throw new OpenRaoException("timestamp1 and timestamp2 cannot both be null");
        } else if (timestamp1.isAfter(timestamp2)) {
            throw new OpenRaoException("timestamp1 is expected to come before timestamp2");
        }
        return timestamp1.until(timestamp2, ChronoUnit.SECONDS) / 3600.0;
    }
}
