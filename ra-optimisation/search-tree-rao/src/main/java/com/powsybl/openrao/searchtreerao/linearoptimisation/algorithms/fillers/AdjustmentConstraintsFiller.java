/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.crac.api.Identifiable;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.timecoupledconstraints.AdjustmentConstraints;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPVariable;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem.BoundExtension.LOWER_BOUND;
import static com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem.BoundExtension.UPPER_BOUND;
import static com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem.VariationDirectionExtension.DOWNWARD;
import static com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem.VariationDirectionExtension.UPWARD;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class AdjustmentConstraintsFiller implements ProblemFiller {
    private final TemporalData<Set<RangeAction<?>>> rangeActionsPerTimestamp;
    private final TemporalData<State> preventiveStates;
    private final TemporalData<RangeActionSetpointResult> prePerimeterSetpoints;
    private final Set<AdjustmentConstraints> adjustmentConstraints;
    private final List<OffsetDateTime> timestamps;
    private final double timestampDuration;

    private static final double DEFAULT_POWER_GRADIENT = 100000.0;
    private static final double DEFAULT_P_MAX = 10000.0;
    private static final double OFF_POWER_THRESHOLD = 1.0;

    // TODO: check that all temporal data are correctly filled with the same timestamps
    public AdjustmentConstraintsFiller(TemporalData<Set<RangeAction<?>>> rangeActionsPerTimestamp, TemporalData<State> preventiveStates, Set<AdjustmentConstraints> adjustmentConstraints, TemporalData<RangeActionSetpointResult> prePerimeterSetpoints) {
        this.rangeActionsPerTimestamp = rangeActionsPerTimestamp;
        this.preventiveStates = preventiveStates;
        this.adjustmentConstraints = adjustmentConstraints;
        this.prePerimeterSetpoints = prePerimeterSetpoints;
        this.timestampDuration = computeTimestampDuration(rangeActionsPerTimestamp.getTimestamps());
        this.timestamps = rangeActionsPerTimestamp.getTimestamps();
    }

    // TODO: reflect upon how to deal with loads constraints-wise (i.e. does it make sense to define lead/lag times or p min/max?)
    // TODO: move this check at a prior moment
    private double computeTimestampDuration(List<OffsetDateTime> timestamps) {
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

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        int numberOfTimestamps = rangeActionsPerTimestamp.getTimestamps().size();
        for (AdjustmentConstraints individualAdjustmentConstraints : adjustmentConstraints) {
            String rangeActionId = individualAdjustmentConstraints.getRangeActionId();
            Optional<Double> minimumAdjustmentTime = individualAdjustmentConstraints.getMinimumAdjustmentTime();
            Optional<TemporalData<RangeAction<?>>> rangeActions = getRangeActions(rangeActionId);
            if (rangeActions.isPresent()) {
                // Add variables
                for (int timestampIndex = 0; timestampIndex < numberOfTimestamps; timestampIndex++) {
                    OffsetDateTime timestamp = timestamps.get(timestampIndex);
                    addStateVariables(linearProblem, rangeActionId, timestamp, rangeActions.orElseThrow().getData(timestamp).orElseThrow());

                    if (timestampIndex < numberOfTimestamps - 1) {
                        addStateTransitionVariables(linearProblem, rangeActionId, timestamp);
                    }
                }
                // Add constraints
                for (int timestampIndex = 0; timestampIndex < numberOfTimestamps; timestampIndex++) {
                    OffsetDateTime timestamp = timestamps.get(timestampIndex);
                    // Constraints not involving state transition variables
                    addUniqueAdjustmentStateConstraint(linearProblem, rangeActionId, timestamp);

                    // Constraints involving state transition variables, defined on indexes [0, numberOfTimestamps - 2]
                    if (timestampIndex < numberOfTimestamps - 1) {
                        OffsetDateTime nextTimestamp = timestamps.get(timestampIndex + 1);
                        // Change objective function
                        changeObjectiveFunctionCoefficients(linearProblem, rangeActionId, timestamp, nextTimestamp, rangeActions.orElseThrow().getData(timestamp).orElseThrow());
                        // link transition to current state
                        addStateFromTransitionConstraints(linearProblem, rangeActionId, timestamp);
                        // link transition to next state
                        addStateToTransitionConstraints(linearProblem, rangeActionId, timestamp, nextTimestamp);
                        // link state to power variation
                        addStateConstraints(linearProblem, individualAdjustmentConstraints, timestamp, nextTimestamp);
                        // add constant ramp constraints
                        addConstantRampConstraints(linearProblem, individualAdjustmentConstraints, timestamp, nextTimestamp);
                        // add minAdjustmentTime constraint
                        if (minimumAdjustmentTime.isPresent()) {
                            addMinAdjustmentTimeConstraints(linearProblem, individualAdjustmentConstraints, timestamp);
                        }
                    }
                }
            }
        }
    }

    private void changeObjectiveFunctionCoefficients(LinearProblem linearProblem, String rangeActionId, OffsetDateTime timestamp, OffsetDateTime nextTimestamp, RangeAction<?> rangeAction) {
        OpenRaoMPVariable activationVariation = linearProblem.getRangeActionVariationBinary(rangeAction, preventiveStates.getData(timestamp).orElseThrow());
        // remove cost of activation for each timestamp
        double coefficient = linearProblem.getObjective().getCoefficient(activationVariation);
        linearProblem.getObjective().setCoefficient(activationVariation, 0.);
        if (nextTimestamp == timestamps.getLast()) {
            OpenRaoMPVariable lastActivationVariation = linearProblem.getRangeActionVariationBinary(rangeAction, preventiveStates.getData(nextTimestamp).orElseThrow());
            linearProblem.getObjective().setCoefficient(lastActivationVariation, 0.);
        }
        // for psts, remove cost of being far from initial setpoint
        if (rangeActionId.contains("_PST")) {
            PstRangeAction pstRangeAction = (PstRangeAction) rangeAction;
            OpenRaoMPVariable tapVariationUpward = linearProblem.getTotalPstRangeActionTapVariationVariable(pstRangeAction, preventiveStates.getData(timestamp).orElseThrow(), UPWARD);
            linearProblem.getObjective().setCoefficient(tapVariationUpward, 0.);
            OpenRaoMPVariable tapVariationDownward = linearProblem.getTotalPstRangeActionTapVariationVariable(pstRangeAction, preventiveStates.getData(timestamp).orElseThrow(), DOWNWARD);
            linearProblem.getObjective().setCoefficient(tapVariationDownward, 0.);
            if (nextTimestamp == timestamps.getLast()) {
                OpenRaoMPVariable lastTapVariationUpward = linearProblem.getTotalPstRangeActionTapVariationVariable(pstRangeAction, preventiveStates.getData(nextTimestamp).orElseThrow(), UPWARD);
                linearProblem.getObjective().setCoefficient(lastTapVariationUpward, 0.);
                OpenRaoMPVariable lastTapVariationDownward = linearProblem.getTotalPstRangeActionTapVariationVariable(pstRangeAction, preventiveStates.getData(nextTimestamp).orElseThrow(), DOWNWARD);
                linearProblem.getObjective().setCoefficient(lastTapVariationDownward, 0.);

            }
        }
        // instead penalize number of adjustments
        OpenRaoMPVariable upFlatTransition = linearProblem.getAdjustmentStateTransitionVariable(rangeActionId, timestamp, LinearProblem.AdjustmentState.UP, LinearProblem.AdjustmentState.FLAT);
        OpenRaoMPVariable downFlatTransition = linearProblem.getAdjustmentStateTransitionVariable(rangeActionId, timestamp, LinearProblem.AdjustmentState.DOWN, LinearProblem.AdjustmentState.FLAT);
        OpenRaoMPVariable offFlatTransition = linearProblem.getAdjustmentStateTransitionVariable(rangeActionId, timestamp, LinearProblem.AdjustmentState.OFF, LinearProblem.AdjustmentState.FLAT);
        OpenRaoMPVariable flatUpTransition = linearProblem.getAdjustmentStateTransitionVariable(rangeActionId, timestamp, LinearProblem.AdjustmentState.FLAT, LinearProblem.AdjustmentState.UP);
        OpenRaoMPVariable flatDownTransition = linearProblem.getAdjustmentStateTransitionVariable(rangeActionId, timestamp, LinearProblem.AdjustmentState.FLAT, LinearProblem.AdjustmentState.DOWN);
        OpenRaoMPVariable flatOffTransition = linearProblem.getAdjustmentStateTransitionVariable(rangeActionId, timestamp, LinearProblem.AdjustmentState.FLAT, LinearProblem.AdjustmentState.OFF);
        linearProblem.getObjective().setCoefficient(upFlatTransition, coefficient);
        linearProblem.getObjective().setCoefficient(downFlatTransition, coefficient);
        linearProblem.getObjective().setCoefficient(offFlatTransition, coefficient);
        linearProblem.getObjective().setCoefficient(flatUpTransition, coefficient);
        linearProblem.getObjective().setCoefficient(flatDownTransition, coefficient);
        linearProblem.getObjective().setCoefficient(flatOffTransition, coefficient);
        /*// and number of off -> something, to avoid using different groups?
        OpenRaoMPVariable offDownTransition = linearProblem.getAdjustmentStateTransitionVariable(rangeActionId, timestamp, LinearProblem.AdjustmentState.UP, LinearProblem.AdjustmentState.FLAT);
        OpenRaoMPVariable offUpTransition = linearProblem.getAdjustmentStateTransitionVariable(rangeActionId, timestamp, LinearProblem.AdjustmentState.UP, LinearProblem.AdjustmentState.FLAT);
        linearProblem.getObjective().setCoefficient(upFlatTransition, coefficient);
        linearProblem.getObjective().setCoefficient(downFlatTransition, coefficient);
        linearProblem.getObjective().setCoefficient(offFlatTransition, coefficient);*/
    }

    private void addStateVariables(LinearProblem linearProblem, String rangeActionId, OffsetDateTime timestamp, RangeAction<?> rangeAction) {
        linearProblem.addAdjustmentStateVariable(rangeActionId, timestamp, LinearProblem.AdjustmentState.UP);
        linearProblem.addAdjustmentStateVariable(rangeActionId, timestamp, LinearProblem.AdjustmentState.DOWN);
        linearProblem.addAdjustmentStateVariable(rangeActionId, timestamp, LinearProblem.AdjustmentState.FLAT);
        linearProblem.addAdjustmentStateVariable(rangeActionId, timestamp, LinearProblem.AdjustmentState.OFF);
        State preventiveState = preventiveStates.getData(timestamp).orElseThrow();
        if (Objects.isNull(linearProblem.getRangeActionVariationBinary(rangeAction, preventiveState))) {
            //TODO: CREATE VARIABLE
            throw new OpenRaoException("range action variation binary should have been created by CostCoreProblemFiller");
        }
    }

    private void addStateTransitionVariables(LinearProblem linearProblem, String rangeActionId, OffsetDateTime timestamp) {
        for (LinearProblem.AdjustmentState stateFrom : LinearProblem.AdjustmentState.values()) {
            for (LinearProblem.AdjustmentState stateTo : LinearProblem.AdjustmentState.values()) {
                if (stateFrom == stateTo || !Set.of(stateFrom, stateTo).equals(Set.of(LinearProblem.AdjustmentState.UP, LinearProblem.AdjustmentState.DOWN))) {
                    OpenRaoMPVariable transitionVariable = linearProblem.addAdjustmentStateTransitionVariable(rangeActionId, timestamp, stateFrom, stateTo);
                    // ct specific constraints
                    if (rangeActionId.contains("_CT")) {
                        // ct should move in one ts
                        if (stateFrom == LinearProblem.AdjustmentState.UP && stateTo == LinearProblem.AdjustmentState.UP ||
                            stateFrom == LinearProblem.AdjustmentState.DOWN && stateTo == LinearProblem.AdjustmentState.DOWN) {
                            transitionVariable.setUb(0.);
                        }
                        // ct variations only on round hours
                        if ((stateFrom == LinearProblem.AdjustmentState.UP || stateFrom == LinearProblem.AdjustmentState.DOWN) && (timestamp.getMinute() + Math.round(timestampDuration * 60)) % 60 != 0) {
                            transitionVariable.setUb(0.);
                        }
                    }
                }
            }
        }
    }

    // ---- Constraints

    /**
     * C1 - The adjustment must and can only be in one state.
     * <br/>
     * ON + OFF = 1
     */
    private void addUniqueAdjustmentStateConstraint(LinearProblem linearProblem, String rangeActionId, OffsetDateTime timestamp) {
        OpenRaoMPConstraint uniqueAdjustmentStateConstraint = linearProblem.addUniqueAdjustmentStateConstraint(rangeActionId, timestamp);
        uniqueAdjustmentStateConstraint.setCoefficient(linearProblem.getAdjustmentStateVariable(rangeActionId, timestamp, LinearProblem.AdjustmentState.UP), 1);
        uniqueAdjustmentStateConstraint.setCoefficient(linearProblem.getAdjustmentStateVariable(rangeActionId, timestamp, LinearProblem.AdjustmentState.DOWN), 1);
        uniqueAdjustmentStateConstraint.setCoefficient(linearProblem.getAdjustmentStateVariable(rangeActionId, timestamp, LinearProblem.AdjustmentState.FLAT), 1);
        uniqueAdjustmentStateConstraint.setCoefficient(linearProblem.getAdjustmentStateVariable(rangeActionId, timestamp, LinearProblem.AdjustmentState.OFF), 1);
    }

    /**
     * C2 - The previous state of the generator must match the transition.
     * <br/>
     * state_j{t} = /Sigma T{state_i -> state_j}
     */
    private void addStateFromTransitionConstraints(LinearProblem linearProblem, String rangeActionId, OffsetDateTime timestamp) {
        for (LinearProblem.AdjustmentState stateFrom : LinearProblem.AdjustmentState.values()) {
            OpenRaoMPConstraint fromConstraint = linearProblem.addAdjustmentStateFromTransitionConstraint(rangeActionId, timestamp, stateFrom);
            fromConstraint.setCoefficient(linearProblem.getAdjustmentStateVariable(rangeActionId, timestamp, stateFrom), 1);
            for (LinearProblem.AdjustmentState stateTo : LinearProblem.AdjustmentState.values()) {
                if (stateFrom == stateTo || !Set.of(stateFrom, stateTo).equals(Set.of(LinearProblem.AdjustmentState.UP, LinearProblem.AdjustmentState.DOWN))) {
                    fromConstraint.setCoefficient(linearProblem.getAdjustmentStateTransitionVariable(rangeActionId, timestamp, stateFrom, stateTo), -1);
                }
            }
        }
    }

    /**
     * C3 - The current state of the generator must match the transition.
     * <br/>
     * state_j{t+1} = /Sigma T{state_j -> state_i}
     */
    private void addStateToTransitionConstraints(LinearProblem linearProblem, String rangeActionId, OffsetDateTime timestamp, OffsetDateTime nextTimestamp) {
        for (LinearProblem.AdjustmentState stateTo : LinearProblem.AdjustmentState.values()) {
            OpenRaoMPConstraint toConstraint = linearProblem.addAdjustmentStateToTransitionConstraint(rangeActionId, timestamp, stateTo);
            toConstraint.setCoefficient(linearProblem.getAdjustmentStateVariable(rangeActionId, nextTimestamp, stateTo), 1);
            for (LinearProblem.AdjustmentState stateFrom : LinearProblem.AdjustmentState.values()) {
                if (stateFrom == stateTo || !Set.of(stateFrom, stateTo).equals(Set.of(LinearProblem.AdjustmentState.UP, LinearProblem.AdjustmentState.DOWN))) {
                    toConstraint.setCoefficient(linearProblem.getAdjustmentStateTransitionVariable(rangeActionId, timestamp, stateFrom, stateTo), -1);
                }
            }
        }
    }

    /**
     * C6 - Constraints linking power variations to adjustment states
     * <br/>
     */
    private void addStateConstraints(LinearProblem linearProblem, AdjustmentConstraints adjustmentConstraints, OffsetDateTime timestamp, OffsetDateTime nextTimestamp) {
        String rangeActionId = adjustmentConstraints.getRangeActionId();
        State preventiveState = preventiveStates.getData(timestamp).orElseThrow();
        State nextPreventiveState = preventiveStates.getData(nextTimestamp).orElseThrow();
        RangeAction<?> rangeAction = rangeActionsPerTimestamp.getData(timestamp).orElseThrow().stream()
            .filter(ra -> ra.getId().equals(rangeActionId))
            .findFirst().orElseThrow();
        double prePerimeterSetpoint = prePerimeterSetpoints.getData(timestamp).orElseThrow().getSetpoint(rangeAction);
        RangeAction<?> nextRangeAction = rangeActionsPerTimestamp.getData(nextTimestamp).orElseThrow().stream()
            .filter(ra -> ra.getId().equals(rangeActionId))
            .findFirst().orElseThrow();
        double nextPrePerimeterSetpoint = prePerimeterSetpoints.getData(nextTimestamp).orElseThrow().getSetpoint(nextRangeAction);
        double maxChange = getMaxChange(rangeAction);

        OpenRaoMPVariable setpoint = linearProblem.getRangeActionSetpointVariable(rangeAction, preventiveState);
        OpenRaoMPVariable nextSetpoint = linearProblem.getRangeActionSetpointVariable(rangeAction, nextPreventiveState);
        OpenRaoMPVariable offVariable = linearProblem.getAdjustmentStateVariable(rangeActionId, timestamp, LinearProblem.AdjustmentState.OFF);
        OpenRaoMPVariable upVariable = linearProblem.getAdjustmentStateVariable(rangeActionId, timestamp, LinearProblem.AdjustmentState.UP);
        OpenRaoMPVariable downVariable = linearProblem.getAdjustmentStateVariable(rangeActionId, timestamp, LinearProblem.AdjustmentState.DOWN);
        OpenRaoMPVariable flatVariable = linearProblem.getAdjustmentStateVariable(rangeActionId, timestamp, LinearProblem.AdjustmentState.FLAT);
        OpenRaoMPVariable rangeActionVariationBinary = linearProblem.getRangeActionVariationBinary(rangeAction, preventiveState);
        OpenRaoMPVariable nextRangeActionVariationBinary = linearProblem.getRangeActionVariationBinary(rangeAction, nextPreventiveState);

        double upwardPowerGradient = Math.min(adjustmentConstraints.getUpwardPowerGradient().orElse(DEFAULT_POWER_GRADIENT), maxChange);
        double downwardPowerGradient = Math.max(adjustmentConstraints.getDownwardPowerGradient().orElse(-DEFAULT_POWER_GRADIENT), -maxChange);

        // UP : If Pt+1 > Pt then UP or OFF is true
        // Pt+1 - Pt <= OFF * maxChange + UP * gradientMax i.e. Pt+1 - Pt - UP * gradientMax - OFF * maxChange <= 0
        OpenRaoMPConstraint upConstraint = linearProblem.addAdjustmentStateConstraint(-linearProblem.infinity(), 0., rangeActionId, timestamp, LinearProblem.AdjustmentState.UP);
        upConstraint.setCoefficient(nextSetpoint, 1.0);
        upConstraint.setCoefficient(setpoint, -1.0);
        upConstraint.setCoefficient(upVariable, -upwardPowerGradient);
        upConstraint.setCoefficient(offVariable, -maxChange);
        if (rangeActionId.contains("_CT")) {
            upConstraint.setUb(upConstraint.ub() + nextPrePerimeterSetpoint - prePerimeterSetpoint);
        }

        // DOWN : If Pt+1 < Pt then DOWN or OFF is true
        // Pt+1 - Pt >= -OFF * maxChange + DOWN * gradientMin i.e. Pt+1 - Pt - DOWN * gradientMin + OFF * maxChange >= 0
        OpenRaoMPConstraint downConstraint = linearProblem.addAdjustmentStateConstraint(0., linearProblem.infinity(), rangeActionId, timestamp, LinearProblem.AdjustmentState.DOWN);
        downConstraint.setCoefficient(nextSetpoint, 1.0);
        downConstraint.setCoefficient(setpoint, -1.0);
        downConstraint.setCoefficient(downVariable, -downwardPowerGradient);
        downConstraint.setCoefficient(offVariable, maxChange);
        if (rangeActionId.contains("_CT")) {
            downConstraint.setLb(downConstraint.lb() + nextPrePerimeterSetpoint - prePerimeterSetpoint);
        }

        // OFF : range action is not used at timestamp or next timestamp
        // bt = 1 or bt-1 = 1 => OFF = false
        // OFF <= 1 - (bt + bt+1) / 2 i.e. OFF + (bt + bt+1) / 2 <= 1
        OpenRaoMPConstraint offConstraint = linearProblem.addAdjustmentStateConstraint(-linearProblem.infinity(), 1., rangeActionId, timestamp, LinearProblem.AdjustmentState.OFF);
        offConstraint.setCoefficient(offVariable, 1.0);
        offConstraint.setCoefficient(nextRangeActionVariationBinary, 0.5);
        offConstraint.setCoefficient(rangeActionVariationBinary, 0.5);

        //FLAT : sum of states is 1 constraint should be sufficient
    }

    /**
     * C7 - ramp should be constant while ramping up or down (equal to gradient)
     * <br/>
     */
    private void addConstantRampConstraints(LinearProblem linearProblem, AdjustmentConstraints adjustmentConstraints, OffsetDateTime timestamp, OffsetDateTime nextTimestamp) {
        String rangeActionId = adjustmentConstraints.getRangeActionId();
        State preventiveState = preventiveStates.getData(timestamp).orElseThrow();
        State nextPreventiveState = preventiveStates.getData(nextTimestamp).orElseThrow();
        RangeAction<?> rangeAction = rangeActionsPerTimestamp.getData(timestamp).orElseThrow().stream()
            .filter(ra -> ra.getId().equals(rangeActionId))
            .findFirst().orElseThrow();
        double prePerimeterSetpoint = prePerimeterSetpoints.getData(timestamp).orElseThrow().getSetpoint(rangeAction);
        RangeAction<?> nextRangeAction = rangeActionsPerTimestamp.getData(nextTimestamp).orElseThrow().stream()
            .filter(ra -> ra.getId().equals(rangeActionId))
            .findFirst().orElseThrow();
        double nextPrePerimeterSetpoint = prePerimeterSetpoints.getData(nextTimestamp).orElseThrow().getSetpoint(nextRangeAction);
        double maxChange = getMaxChange(rangeAction);

        OpenRaoMPVariable setpoint = linearProblem.getRangeActionSetpointVariable(rangeAction, preventiveState);
        OpenRaoMPVariable nextSetpoint = linearProblem.getRangeActionSetpointVariable(rangeAction, nextPreventiveState);

        OpenRaoMPVariable upUpTransitionVariable = linearProblem.getAdjustmentStateTransitionVariable(rangeActionId, timestamp, LinearProblem.AdjustmentState.UP, LinearProblem.AdjustmentState.UP);
        OpenRaoMPVariable downDownTransitionVariable = linearProblem.getAdjustmentStateTransitionVariable(rangeActionId, timestamp, LinearProblem.AdjustmentState.DOWN, LinearProblem.AdjustmentState.DOWN);

        double upwardPowerGradient = Math.min(adjustmentConstraints.getUpwardPowerGradient().orElse(DEFAULT_POWER_GRADIENT), maxChange);
        double downwardPowerGradient = Math.max(adjustmentConstraints.getDownwardPowerGradient().orElse(-DEFAULT_POWER_GRADIENT), maxChange);

        // If Tr(UP, UP), Pt+1 - Pt = gradientUp
        // i.e. gradientUp - 2*maxChange * (1-Tr(Up, UP)) <= Pt+1 - Pt <= gradientUp + 2*maxChange * (1-Tr(UP, UP))
        // i.e. Pt+1 - Pt + 2*maxChange * Tr(UP, UP) <= gradientUp + 2*maxChange
        // and  Pt+1 - Pt - 2*maxChange * Tr(UP, UP) >= gradientUp - 2*maxChange
        // If CT, then Pt+1 - P0t+1 - Pt + POt instead i.e. add P0t+1 - P0t to upper and lower bounds
        OpenRaoMPConstraint constantRampUpwardUpperConstraint = linearProblem.addAdjustmentConstantRampConstraint(-linearProblem.infinity(), upwardPowerGradient + 2 * maxChange, rangeActionId, timestamp, UPWARD, UPPER_BOUND);
        constantRampUpwardUpperConstraint.setCoefficient(nextSetpoint, 1.);
        constantRampUpwardUpperConstraint.setCoefficient(setpoint, -1.);
        constantRampUpwardUpperConstraint.setCoefficient(upUpTransitionVariable, 2 * maxChange);
        if (rangeActionId.contains("_CT")) {
            constantRampUpwardUpperConstraint.setUb(constantRampUpwardUpperConstraint.ub() + nextPrePerimeterSetpoint - prePerimeterSetpoint);
        }

        OpenRaoMPConstraint constantRampUpwardLowerConstraint = linearProblem.addAdjustmentConstantRampConstraint(upwardPowerGradient - 2 * maxChange, linearProblem.infinity(), rangeActionId, timestamp, UPWARD, LOWER_BOUND);
        constantRampUpwardLowerConstraint.setCoefficient(nextSetpoint, 1.);
        constantRampUpwardLowerConstraint.setCoefficient(setpoint, -1.);
        constantRampUpwardLowerConstraint.setCoefficient(upUpTransitionVariable, -2 * maxChange);
        if (rangeActionId.contains("_CT")) {
            constantRampUpwardLowerConstraint.setLb(constantRampUpwardLowerConstraint.lb() + nextPrePerimeterSetpoint - prePerimeterSetpoint);
        }

        // If Tr(DOWN, DOWN), Pt+1 - Pt = gradientDown
        // i.e. gradientDown - 2*maxChange * (1-Tr(DOWN, DOWN)) <= Pt+1 - Pt <= gradientDown + 2*maxChange * (1-Tr(DOWN, DOWN))
        // i.e. Pt+1 - Pt + 2*maxChange * Tr(DOWN, DOWN) <= gradientDown + 2*maxChange
        // and  Pt+1 - Pt - 2*maxChange * Tr(DOWN, DOWN) >= gradientDown - 2*maxChange
        // If CT, then Pt+1 - P0t+1 - Pt + POt instead i.e. add P0t+1 - P0t to upper and lower bounds
        OpenRaoMPConstraint constantRampDownwardUpperConstraint = linearProblem.addAdjustmentConstantRampConstraint(-linearProblem.infinity(), downwardPowerGradient + 2 * maxChange, rangeActionId, timestamp, DOWNWARD, UPPER_BOUND);
        constantRampDownwardUpperConstraint.setCoefficient(nextSetpoint, 1.);
        constantRampDownwardUpperConstraint.setCoefficient(setpoint, -1.);
        constantRampDownwardUpperConstraint.setCoefficient(downDownTransitionVariable, 2 * maxChange);
        if (rangeActionId.contains("_CT")) {
            constantRampDownwardUpperConstraint.setUb(constantRampDownwardUpperConstraint.ub() + nextPrePerimeterSetpoint - prePerimeterSetpoint);
        }

        OpenRaoMPConstraint constantRampDownwardLowerConstraint = linearProblem.addAdjustmentConstantRampConstraint(downwardPowerGradient - 2 * maxChange, linearProblem.infinity(), rangeActionId, timestamp, DOWNWARD, LOWER_BOUND);
        constantRampDownwardLowerConstraint.setCoefficient(nextSetpoint, 1.);
        constantRampDownwardLowerConstraint.setCoefficient(setpoint, -1.);
        constantRampDownwardLowerConstraint.setCoefficient(downDownTransitionVariable, -2 * maxChange);
        if (rangeActionId.contains("_CT")) {
            constantRampDownwardLowerConstraint.setLb(constantRampDownwardLowerConstraint.lb() + nextPrePerimeterSetpoint - prePerimeterSetpoint);
        }

    }

    /**
     * C8 - min adjustment time constraint
     * if Tr(UP,FLAT) or Tr(DOWN,FLAT) then the for the next few timestamps t', FLAT(t') = 1
     * i.e. FLAT(t') >= Tr(UP,FLAT) + Tr(DOWN,FLAT)
     * <br/>
     */
    private void addMinAdjustmentTimeConstraints(LinearProblem linearProblem, AdjustmentConstraints adjustmentConstraints, OffsetDateTime toFlatTimestamp) {
        String rangeActionId = adjustmentConstraints.getRangeActionId();

        OpenRaoMPVariable upFlatTransitionVariable = linearProblem.getAdjustmentStateTransitionVariable(rangeActionId, toFlatTimestamp, LinearProblem.AdjustmentState.UP, LinearProblem.AdjustmentState.FLAT);
        OpenRaoMPVariable downFlatTransitionVariable = linearProblem.getAdjustmentStateTransitionVariable(rangeActionId, toFlatTimestamp, LinearProblem.AdjustmentState.DOWN, LinearProblem.AdjustmentState.FLAT);

        timestamps.stream()
            .filter(t -> t.isAfter(toFlatTimestamp))
            .filter(t -> t.isBefore(toFlatTimestamp.plusSeconds(Math.round(3600 * adjustmentConstraints.getMinimumAdjustmentTime().orElseThrow()))))
            .forEach(flatTimestamp -> {
                OpenRaoMPConstraint minAdjustmentTimeConstraint = linearProblem.addAdjustmentMinTimeConstraint(0., linearProblem.infinity(), rangeActionId, toFlatTimestamp, flatTimestamp);
                OpenRaoMPVariable flatVariable = linearProblem.getAdjustmentStateVariable(rangeActionId, flatTimestamp, LinearProblem.AdjustmentState.FLAT);
                minAdjustmentTimeConstraint.setCoefficient(flatVariable, 1.0);
                minAdjustmentTimeConstraint.setCoefficient(upFlatTransitionVariable, -1.0);
                minAdjustmentTimeConstraint.setCoefficient(downFlatTransitionVariable, -1.0);
            });

        OpenRaoMPVariable upOffTransitionVariable = linearProblem.getAdjustmentStateTransitionVariable(rangeActionId, toFlatTimestamp, LinearProblem.AdjustmentState.UP, LinearProblem.AdjustmentState.OFF);
        OpenRaoMPVariable downOffTransitionVariable = linearProblem.getAdjustmentStateTransitionVariable(rangeActionId, toFlatTimestamp, LinearProblem.AdjustmentState.DOWN, LinearProblem.AdjustmentState.OFF);

        timestamps.stream()
            .filter(t -> t.isAfter(toFlatTimestamp))
            .filter(t -> t.isBefore(toFlatTimestamp.plusSeconds(Math.round(3600 * adjustmentConstraints.getMinimumAdjustmentTime().orElseThrow()))))
            .forEach(flatTimestamp -> {
                OpenRaoMPConstraint minAdjustmentOffTimeConstraint = linearProblem.addAdjustmentMinOffTimeConstraint(0., linearProblem.infinity(), rangeActionId, toFlatTimestamp, flatTimestamp);
                OpenRaoMPVariable offVariable = linearProblem.getAdjustmentStateVariable(rangeActionId, flatTimestamp, LinearProblem.AdjustmentState.OFF);
                minAdjustmentOffTimeConstraint.setCoefficient(offVariable, 1.0);
                minAdjustmentOffTimeConstraint.setCoefficient(upOffTransitionVariable, -1.0);
                minAdjustmentOffTimeConstraint.setCoefficient(downOffTransitionVariable, -1.0);
            });

        //TODO: manage OFF -> FLAT and FLAT -> OFF transitions
        linearProblem.getAdjustmentStateTransitionVariable(rangeActionId, toFlatTimestamp, LinearProblem.AdjustmentState.OFF, LinearProblem.AdjustmentState.FLAT).setUb(0.);
        linearProblem.getAdjustmentStateTransitionVariable(rangeActionId, toFlatTimestamp, LinearProblem.AdjustmentState.FLAT, LinearProblem.AdjustmentState.OFF).setUb(0.);
    }

    /**
     * C7 - Constraints linking power variations to state transitions
     * <br/>
     */
    private void addPowerVariationConstraints(LinearProblem linearProblem, AdjustmentConstraints adjustmentConstraints, OffsetDateTime timestamp, OffsetDateTime nextTimestamp) {
        //TODO
        double upwardPowerGradient = adjustmentConstraints.getUpwardPowerGradient().orElse(DEFAULT_POWER_GRADIENT);
        double downwardPowerGradient = adjustmentConstraints.getDownwardPowerGradient().orElse(-DEFAULT_POWER_GRADIENT);
    }

    private double getMaxChange(RangeAction<?> rangeAction) {
        // TODO: calculate this properly somehow? This works if there's only one range but not when you mix relative and absolute ranges, and the ranges dont change between ts
        //return rangeAction.getMaxAdmissibleSetpoint(0.) - rangeAction.getMinAdmissibleSetpoint(0.);
        if (rangeAction.getId().contains("_CT")) {
            return 1500.;
        }
        if (rangeAction.getId().contains("_PST")) {
            return 100.;
        }
        if (rangeAction.getId().contains("_RD")) {
            return 1000.;
        }
        throw new OpenRaoException("Unsupported range action type: " + rangeAction.getClass().getSimpleName());
    }

    // ** Utility methods
    private static double computeTimeGap(OffsetDateTime timestamp1, OffsetDateTime timestamp2) {
        if (timestamp1 == null || timestamp2 == null) {
            throw new OpenRaoException("timestamp1 and timestamp2 cannot both be null");
        } else if (timestamp1.isAfter(timestamp2)) {
            throw new OpenRaoException("timestamp1 is expected to come before timestamp2");
        }
        return timestamp1.until(timestamp2, ChronoUnit.SECONDS) / 3600.0;
    }

    private Optional<TemporalData<RangeAction<?>>> getRangeActions(String rangeActionId) {
        Map<OffsetDateTime, RangeAction<?>> rangeActionPerTimestamp = new HashMap<>();
        for (OffsetDateTime timestamp : rangeActionsPerTimestamp.getTimestamps()) {
            Optional<RangeAction<?>> rangeAction = getRangeAction(rangeActionId, rangeActionsPerTimestamp.getData(timestamp).orElse(Set.of()));
            if (rangeAction.isEmpty()) {
                OpenRaoLoggerProvider.TECHNICAL_LOGS.warn("Range action {} is not present for timestamp {} and will thus be ignored.", rangeActionId, timestamp);
                return Optional.empty();
            }
            rangeActionPerTimestamp.put(timestamp, rangeAction.get());
        }
        return Optional.of(new TemporalDataImpl<>(rangeActionPerTimestamp));
    }

    private static Optional<RangeAction<?>> getRangeAction(String rangeActionId, Set<RangeAction<?>> allRangeActions) {
        return allRangeActions.stream().filter(rangeAction -> rangeAction.getId().contains(rangeActionId)).min(Comparator.comparing(Identifiable::getId));
    }

    @Override
    public void updateBetweenMipIteration(LinearProblem linearProblem, RangeActionActivationResult rangeActionActivationResult) {
        // nothing to do
    }
}
