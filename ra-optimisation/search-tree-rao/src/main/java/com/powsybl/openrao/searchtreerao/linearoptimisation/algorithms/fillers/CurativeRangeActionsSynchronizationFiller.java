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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * This filler adds constraints to the linear problem that force a curative range action shared by several
 * timestamp's CRACs under the same ID to be activated identically across all of them, i.e. all the common
 * range actions must share the same setpoint value.
 * <p>
 *     Since the range actions are matched by their CRAC ID only (and not by their network elements). The
 *     synchronization will not work if the same range action carries different CRAC IDs from one timestamp
 *     to another.
 * </p>
 *
 * @author Atena Amnache {@literal <atena.amnache at rte-france.com>}
 */
public class CurativeRangeActionsSynchronizationFiller implements ProblemFiller {
    private final List<OffsetDateTime> timestamps;
    // for each timestamp, for each state, which range actions are available
    private final TemporalData<Map<State, Set<RangeAction<?>>>> availableRangeActionsPerStatePerTimestamp;

    public CurativeRangeActionsSynchronizationFiller(TemporalData<Map<State, Set<RangeAction<?>>>> availableRangeActionsPerStatePerTimestamp) {
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
        // group by state in order to synchronize by contingency
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
     * Adds the synchronization constraints for one contingency.
     *
     * @param statesPerTimestamp the state to synchronize in every timestamp sharing the contingency.
     */
    private void addRangeActionsSynchronizationConstraintsForContingency(LinearProblem linearProblem, Map<OffsetDateTime, State> statesPerTimestamp) {
        // for this contingency, the range action objects carrying the same ID, keyed by this ID then by timestamp
        Map<String, SortedMap<OffsetDateTime, RangeAction<?>>> rangeActionPerTimestampPerId = new HashMap<>();
        statesPerTimestamp.forEach((timestamp, state) ->
                availableRangeActionsPerStatePerTimestamp.getData(timestamp).orElseThrow().get(state).forEach(
                        rangeAction -> rangeActionPerTimestampPerId.computeIfAbsent(rangeAction.getId(),
                            id -> new TreeMap<>()).put(timestamp, rangeAction) // timestamps in chronological order
                )
        );
        // sort range actions by id
        rangeActionPerTimestampPerId.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(
                entry -> addRangeActionsSynchronizationConstraints(linearProblem, entry.getValue(), statesPerTimestamp)
        );
    }

    /**
     * Adds the setpoint equality constraints for one range action ID. One constraint is added between the first timestamp
     * (the reference timestamp) and all the other timestamps sharing the range action. Meaning n timestamps create n constraints.
     * The equality condition between the non-reference timestamps is implied by transitivity.
     *
     * @param timestampRangeActionMap the range action instances carrying the common ID keyed by timestamp.
     * @param timestampStateMap the state to synchronize in every timestamp sharing the contingency.
     */
    private void addRangeActionsSynchronizationConstraints(LinearProblem linearProblem,
                                                           SortedMap<OffsetDateTime, RangeAction<?>> timestampRangeActionMap,
                                                           Map<OffsetDateTime, State> timestampStateMap) {
        if (timestampRangeActionMap.size() < 2) {
            return;
        }
        // the reference timestamp is the first one
        OffsetDateTime referenceTimestamp = timestampRangeActionMap.firstKey(); // earliest timestamp
        timestampRangeActionMap.keySet().stream()
            .filter(timestamp -> !timestamp.equals(referenceTimestamp))
            .forEach(timestamp -> addSetpointEqualityConstraint(
                linearProblem,
                timestampRangeActionMap.get(referenceTimestamp),
                timestampRangeActionMap.get(timestamp),
                timestampStateMap.get(referenceTimestamp),
                timestampStateMap.get(timestamp)
            ));
    }

    /**
     * Adds a single setpoint equality constraint between the setpoint variables of the same range action at two different timestamps.
     *
     * @param referenceTimestampRangeAction the range action instance of the reference timestamp.
     * @param otherTimestampRangeAction the range action instance, carrying the same ID, of the timestamp to synchronize with the reference one.
     * @param referenceTimestampState the state of the reference timestamp.
     * @param otherTimestampState the state of the timestamp to synchronize with the reference one.
     */
    private void addSetpointEqualityConstraint(LinearProblem linearProblem,
                                               RangeAction<?> referenceTimestampRangeAction,
                                               RangeAction<?> otherTimestampRangeAction,
                                               State referenceTimestampState,
                                               State otherTimestampState) {
        OpenRaoMPVariable referenceTimestampRangeActionSetpoint = getSetpointVariable(linearProblem, referenceTimestampRangeAction, referenceTimestampState);
        OpenRaoMPVariable otherTimestampRangeActionSetpoint = getSetpointVariable(linearProblem, otherTimestampRangeAction, otherTimestampState);
        OpenRaoMPConstraint rangeActionSynchronizationConstraint = linearProblem.addRangeActionSynchronizationConstraint(
                referenceTimestampRangeAction.getId(),
                referenceTimestampState,
                otherTimestampState
        );
        // +1 * referenceTimestampRangeActionSetpoint - 1 * otherTimestampRangeActionSetpoint = 0
        rangeActionSynchronizationConstraint.setCoefficient(referenceTimestampRangeActionSetpoint, 1.0);
        rangeActionSynchronizationConstraint.setCoefficient(otherTimestampRangeActionSetpoint, -1.0);
    }

    // Utility methods
    /**
     * Returns the setpoint variable of a range action at a given state
     */
    private OpenRaoMPVariable getSetpointVariable(LinearProblem linearProblem, RangeAction<?> rangeAction, State state) {
        try {
            return linearProblem.getRangeActionSetpointVariable(rangeAction, state);
        } catch (OpenRaoException e) {
            throw new OpenRaoException("The setpoint variable of range action %s was not found at state : %s.".formatted(rangeAction.getId(), state.getId()), e);
        }
    }
}
