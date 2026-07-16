/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.timecoupledsearchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.timecoupledsearchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.timecoupledsearchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
import com.powsybl.openrao.timecoupledsearchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPVariable;
import com.powsybl.openrao.timecoupledsearchtreerao.result.api.FlowResult;
import com.powsybl.openrao.timecoupledsearchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.timecoupledsearchtreerao.result.api.SensitivityResult;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This filler forces a range action that appears in several timestamps under the same CRAC id to be applied
 * identically on all the timestamps at once, i.e. all common range actions must have the same setpoint.
 */
public class RangeActionsSynchronizationFiller implements ProblemFiller {
    private final List<OffsetDateTime> timestamps;
    private final TemporalData<State> optimizationState;
    private final TemporalData<Set<RangeAction<?>>> availableRangeActionsPerTimestamp;
    // map range action id : list of the timestamps sharing this range action in chronological order
    private final Map<String, List<OffsetDateTime>> timestampsByRangeActionId;

    public RangeActionsSynchronizationFiller(TemporalData<State> optimizationState,
                                             TemporalData<Set<RangeAction<?>>> availableRangeActionsPerTimestamp) {
        this.optimizationState = optimizationState;
        this.timestamps = availableRangeActionsPerTimestamp.getTimestamps();
        this.availableRangeActionsPerTimestamp = availableRangeActionsPerTimestamp;
        this.timestampsByRangeActionId = buildTimestampsPerRangeActionIdsMap(availableRangeActionsPerTimestamp);
    }

    @Override
    public void fill(LinearProblem linearProblem,
                     FlowResult flowResult,
                     SensitivityResult sensitivityResult,
                     RangeActionActivationResult rangeActionActivationResult) {
        if (timestamps.size() < 2) {
            return;
        }
        timestampsByRangeActionId.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry ->
                addRangeActionsSynchronizationConstraints(linearProblem, entry.getKey(), entry.getValue()));
    }

    @Override
    public void updateBetweenMipIteration(LinearProblem linearProblem, RangeActionActivationResult rangeActionActivationResult) {
        // nothing to do
    }

    // Constraints

    /**
     * Adds the setpoint equality constraints for one range action id. It adds one constraint between the first timestamp of the list called the reference timestamp
     * and all the other timestamps of the list meaning n timestamps create n-1 constraints.
     * The equality condition between the non-reference timestamps is implied by transitivity.
     */
    private void addRangeActionsSynchronizationConstraints(LinearProblem linearProblem, String rangeActionId, List<OffsetDateTime> timestampsSharingTheRangeAction) {
        if (timestampsSharingTheRangeAction.size() < 2) {
            return;
        }
        // the reference timestamp is the first one
        OffsetDateTime referenceTimestamp = timestampsSharingTheRangeAction.getFirst();
        timestampsSharingTheRangeAction.stream()
                .filter(timestamp -> !timestamp.equals(referenceTimestamp))
                .forEach(timestamp -> addSetpointEqualityConstraint(linearProblem, rangeActionId, referenceTimestamp, timestamp));
    }

    /** Adds a single setpoint equality constraint between the setpoint variables of the same range action at two different timestamps. */
    private void addSetpointEqualityConstraint(LinearProblem linearProblem, String rangeActionId, OffsetDateTime referenceTimestamp, OffsetDateTime otherTimestamp) {
        State referenceTimestampState = getOptimizationState(referenceTimestamp);
        State otherTimestampState = getOptimizationState(otherTimestamp);
        OpenRaoMPVariable referenceTimestampRangeActionSetpoint = getSetpointVariable(linearProblem, rangeActionId, referenceTimestamp, referenceTimestampState);
        OpenRaoMPVariable otherTimestampRangeActionSetpoint = getSetpointVariable(linearProblem, rangeActionId, otherTimestamp, otherTimestampState);
        OpenRaoMPConstraint rangeActionSynchronizationConstraint = linearProblem.addRangeActionSynchronizationConstraint(rangeActionId, referenceTimestampState, otherTimestampState);
        // +1 * referenceTimestampRangeActionSetpoint - 1 * otherTimestampRangeActionSetpoint = 0
        rangeActionSynchronizationConstraint.setCoefficient(referenceTimestampRangeActionSetpoint, 1.0);
        rangeActionSynchronizationConstraint.setCoefficient(otherTimestampRangeActionSetpoint, -1.0);
    }

    // Utility methods
    /** Groups the available range actions of every timestamp by their CRAC id because each timestamp holds distinct RangeAction instance*/
    public static Map<String, List<OffsetDateTime>> buildTimestampsPerRangeActionIdsMap(TemporalData<Set<RangeAction<?>>> availableRangeActionsPerTimestamp) {
        Map<String, List<OffsetDateTime>> timestampsPerRangeActionIdsMap = new HashMap<>();
        availableRangeActionsPerTimestamp.getTimestamps().forEach(
                timestamp -> availableRangeActionsPerTimestamp.getData(timestamp).orElseThrow().forEach(
                        rangeAction -> timestampsPerRangeActionIdsMap.computeIfAbsent(rangeAction.getId(), id -> new ArrayList<>()).add(timestamp))
        );
        return timestampsPerRangeActionIdsMap;
    }

    /** Finds in a timestamp the range action instance carrying the given id. */
    private RangeAction<?> getRangeAction(String rangeActionId, OffsetDateTime timestamp) {
        return availableRangeActionsPerTimestamp.getData(timestamp).orElseThrow().stream()
                .filter(rangeAction -> rangeAction.getId().equals(rangeActionId))
                .findFirst()
                .orElseThrow(() -> new OpenRaoException("RangeActionSynchronizationFiller : could not find range action with id " + rangeActionId));
    }

    /** Returns the setpoint variable of a range action at a given state. */
    private OpenRaoMPVariable getSetpointVariable(LinearProblem linearProblem, String rangeActionId, OffsetDateTime timestamp, State state) {
        try {
            return linearProblem.getRangeActionSetpointVariable(getRangeAction(rangeActionId, timestamp), state);
        } catch (OpenRaoException e) {
            throw new OpenRaoException("RangeActionSynchronizationFiller : Setpoint variable of range action %s was not found at state %s.".formatted(rangeActionId, state.getId()), e);
        }
    }

    /** Returns the main optimization state at which a range action's setpoint variable is defined, for a given timestamp. */
    private State getOptimizationState(OffsetDateTime timestamp) {
        return optimizationState.getData(timestamp).orElseThrow(() -> new OpenRaoException("RangeActionSynchronizationFiller : optimization state was not found at timestamp " + timestamp));
    }
}
