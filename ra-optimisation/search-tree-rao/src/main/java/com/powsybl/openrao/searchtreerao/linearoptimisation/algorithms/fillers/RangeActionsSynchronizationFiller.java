/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPVariable;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This filler forces a range action that appears in several timestamps under the same CRAC ID to be applied
 * identically on all the timestamps at once, i.e. all common range actions must have the same setpoint.
 * <p>
 *     Range actions are matched across timestamps by their CRAC ID only and not by the network element. It will not
 *     work if the range action ID of the same network element is different from a timestamp to another.
 * </p>
 * <p>
 *     The filler is currently only used in curative optimization and second preventive optimization
 *     when curative range actions are involved.
 * </p>
 *
 * @author Atena Amnache {@literal <atena.amnache at rte-france.com>}
 */
public class RangeActionsSynchronizationFiller implements ProblemFiller {
    private final List<OffsetDateTime> timestamps;
    private final TemporalData<Map<State, Set<RangeAction<?>>>> availableRangeActionsPerStatePerTimestamp;

    public RangeActionsSynchronizationFiller(TemporalData<Map<State, Set<RangeAction<?>>>> availableRangeActionsPerStatePerTimestamp) {
        this.timestamps = availableRangeActionsPerStatePerTimestamp.getTimestamps();
        this.availableRangeActionsPerStatePerTimestamp = availableRangeActionsPerStatePerTimestamp;
    }

    @Override
    public void fill(LinearProblem linearProblem,
                     FlowResult flowResult,
                     SensitivityResult sensitivityResult,
                     RangeActionActivationResult rangeActionActivationResult) {
        if (timestamps.size() < 2) {
            return;
        }
        // group the states to synchronize by contingency
        Map<String, Map<OffsetDateTime, State>> statesPerTimestampPerContingency = new HashMap<>();
        availableRangeActionsPerStatePerTimestamp.getDataPerTimestamp().forEach(
                (timestamp, rangeActionsPerState) -> rangeActionsPerState.keySet().forEach(
                        state -> statesPerTimestampPerContingency.computeIfAbsent(state.getContingency().orElseThrow().getId(), contingencyId -> new HashMap<>()).put(timestamp, state)
                )
        );
        // sort the contingencies by id
        statesPerTimestampPerContingency.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(
                entry -> addRangeActionsSynchronizationConstraintsForContingency(linearProblem, entry.getValue())
        );
    }

    @Override
    public void updateBetweenMipIteration(LinearProblem linearProblem, RangeActionActivationResult rangeActionActivationResult) {
        // nothing to do
    }

    // Constraints
    /**
     * Adds the synchronization constraint for one contingency.
     *
     * @param statesPerTimestamp the state to synchronize in every timestamp sharing the contingency.
     */
    private void addRangeActionsSynchronizationConstraintsForContingency(LinearProblem linearProblem, Map<OffsetDateTime, State> statesPerTimestamp) {
        // for this contingency, the timestamps sharing every range action, keyed by the range action id
        Map<String, List<OffsetDateTime>> timestampsPerRangeActionId = new HashMap<>();
        statesPerTimestamp.forEach((timestamp, state) ->
                availableRangeActionsPerStatePerTimestamp.getData(timestamp).orElseThrow().get(state).forEach(
                        rangeAction -> timestampsPerRangeActionId.computeIfAbsent(rangeAction.getId(), id -> new ArrayList<>()).add(timestamp)
                )
        );
        timestampsPerRangeActionId.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(
                entry -> addRangeActionsSynchronizationConstraints(linearProblem, entry.getKey(), entry.getValue(), statesPerTimestamp)
        );
    }

    /**
     * Adds the setpoint equality constraints for one range action ID. one constraint is added between the first timestamp
     * of the list (the reference timestamp) and all the other timestamps of the list. Meaning n timestamps create n-1 constraints.
     * The equality condition between the non-reference timestamps is implied by transitivity.
     *
     * @param rangeActionId the ID of the common range action to synchronize.
     * @param timestampsSharingTheRangeAction the timestamps whose CRAC contains a range action with rangeActionId as their ID.
     */
    private void addRangeActionsSynchronizationConstraints(LinearProblem linearProblem, String rangeActionId, List<OffsetDateTime> timestampsSharingTheRangeAction, Map<OffsetDateTime, State> statesPerTimestamp) {
        if (timestampsSharingTheRangeAction.size() < 2) {
            return;
        }
        // the reference timestamp is the first one
        OffsetDateTime referenceTimestamp = timestampsSharingTheRangeAction.getFirst();
        timestampsSharingTheRangeAction.stream()
                .filter(timestamp -> !timestamp.equals(referenceTimestamp))
                .forEach(timestamp -> addSetpointEqualityConstraint(linearProblem, rangeActionId, statesPerTimestamp.get(referenceTimestamp), statesPerTimestamp.get(timestamp), referenceTimestamp, timestamp));
    }

    /**
     * Adds a single setpoint equality constraint between the setpoint variables of the same range action at two different timestamps.
     *
     * @param rangeActionId the ID of the common range action to synchronize.
     * @param referenceTimestamp timestamp for which the constraint is created.
     * @param otherTimestamp timestamp sharing the constraint with referenceTimestamp.
     */
    private void addSetpointEqualityConstraint(LinearProblem linearProblem, String rangeActionId, State referenceTimestampState, State otherTimestampState, OffsetDateTime referenceTimestamp, OffsetDateTime otherTimestamp) {
        OpenRaoMPVariable referenceTimestampRangeActionSetpoint = getSetpointVariable(linearProblem, rangeActionId, referenceTimestamp, referenceTimestampState);
        OpenRaoMPVariable otherTimestampRangeActionSetpoint = getSetpointVariable(linearProblem, rangeActionId, otherTimestamp, otherTimestampState);
        OpenRaoMPConstraint rangeActionSynchronizationConstraint = linearProblem.addRangeActionSynchronizationConstraint(rangeActionId, referenceTimestampState, otherTimestampState);
        // +1 * referenceTimestampRangeActionSetpoint - 1 * otherTimestampRangeActionSetpoint = 0
        rangeActionSynchronizationConstraint.setCoefficient(referenceTimestampRangeActionSetpoint, 1.0);
        rangeActionSynchronizationConstraint.setCoefficient(otherTimestampRangeActionSetpoint, -1.0);
    }

    // Utility methods
    /**
     * Finds in a timestamp the range action instance carrying the ID "rangeActionId"
     */
    private RangeAction<?> getRangeAction(String rangeActionId, OffsetDateTime timestamp, State state) {
        return availableRangeActionsPerStatePerTimestamp.getData(timestamp).orElseThrow().get(state).stream()
                .filter(rangeAction -> rangeAction.getId().equals(rangeActionId))
                .findFirst()
                .orElseThrow(() -> new OpenRaoException("Could not find range action with id " + rangeActionId));
    }

    /**
     * Returns the setpoint variable of a range action at a given state.
     */
    private OpenRaoMPVariable getSetpointVariable(LinearProblem linearProblem, String rangeActionId, OffsetDateTime timestamp, State state) {
        try {
            return linearProblem.getRangeActionSetpointVariable(getRangeAction(rangeActionId, timestamp, state), state);
        } catch (OpenRaoException e) {
            throw new OpenRaoException("Setpoint variable of range action %s was not found at state %s.".formatted(rangeActionId, state.getId()), e);
        }
    }
}
