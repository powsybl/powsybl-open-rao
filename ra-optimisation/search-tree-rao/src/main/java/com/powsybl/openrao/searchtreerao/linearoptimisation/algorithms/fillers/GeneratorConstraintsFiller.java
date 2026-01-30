/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.crac.api.Identifiable;
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.intertemporalconstraints.GeneratorConstraints;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPVariable;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class GeneratorConstraintsFiller implements ProblemFiller {
    private final TemporalData<Network> networks;
    private final TemporalData<State> preventiveStates;
    private final TemporalData<Set<InjectionRangeAction>> injectionRangeActionsPerTimestamp;
    private final Set<GeneratorConstraints> generatorConstraints;
    private final List<OffsetDateTime> timestamps;

    private static final double DEFAULT_TIMESTAMP_DURATION = Double.MAX_VALUE;
    private static final double DEFAULT_P_MAX = 10000.0;
    private static final double DEFAULT_POWER_GRADIENT = 100000.0;
    private static final double MINIMAL_P = 1.;

    public GeneratorConstraintsFiller(TemporalData<Network> networks, TemporalData<State> preventiveStates, TemporalData<Set<InjectionRangeAction>> injectionRangeActionsPerTimestamp, Set<GeneratorConstraints> generatorConstraints) {
        this.networks = networks;
        this.preventiveStates = preventiveStates;
        this.injectionRangeActionsPerTimestamp = injectionRangeActionsPerTimestamp;
        this.generatorConstraints = generatorConstraints;
        this.timestamps = preventiveStates.getTimestamps();
    }

    // TODO : simplifier gestion des timeGap, timestamps

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        int numberOfTimestamps = timestamps.size();
        for (GeneratorConstraints individualGeneratorConstraints : generatorConstraints) {
            Optional<TemporalData<InjectionRangeAction>> associatedInjections = getInjectionRangeActionOfGenerator(individualGeneratorConstraints.getGeneratorId());
            if (associatedInjections.isPresent()) {
                double timeGap = computeTimeGap(timestamps.get(0), timestamps.get(1));
                boolean isStartUpDefined = isStartUpDefined(individualGeneratorConstraints, timeGap);
                boolean isShutDownDefined = isShutDownDefined(individualGeneratorConstraints, timeGap);

                for (int timestampIndex = 0; timestampIndex < numberOfTimestamps; timestampIndex++) {
                    addVariables(linearProblem, individualGeneratorConstraints, timestampIndex, timestamps, isStartUpDefined, isShutDownDefined);
                }
                for (int timestampIndex = 0; timestampIndex < numberOfTimestamps; timestampIndex++) {
                    addBasicConstraints(linearProblem, individualGeneratorConstraints, timestampIndex, timestamps, associatedInjections.get().getData(timestamps.get(timestampIndex)).orElseThrow(), isStartUpDefined, isShutDownDefined);
                }
                for (int timestampIndex = 0; timestampIndex < numberOfTimestamps; timestampIndex++) {
                    for (int laterTimestampIndex = timestampIndex + 1; laterTimestampIndex < numberOfTimestamps; laterTimestampIndex++) {
                        if (isStartUpDefined) {
                            addStartUpConstraint(linearProblem, individualGeneratorConstraints.getGeneratorId(), timestamps.get(timestampIndex), timestamps.get(laterTimestampIndex));
                        }
                    }
                    for (int earlierTimestampIndex = timestampIndex - 1; earlierTimestampIndex >= 0; earlierTimestampIndex--) {
                        if (isShutDownDefined) {
                            addShutDownConstraint(linearProblem, individualGeneratorConstraints.getGeneratorId(), timestamps.get(timestampIndex), timestamps.get(earlierTimestampIndex));
                        }
                    }
                }
                for (int timestampIndex = 0; timestampIndex < numberOfTimestamps - 1; timestampIndex++) {
                    addPowerVariationConstraints(linearProblem, individualGeneratorConstraints, timestampIndex, timestamps);
                }
            }
        }
    }

    private boolean isStartUpDefined(GeneratorConstraints generatorConstraints, double timestampDuration) {
        Optional<Double> leadTime = generatorConstraints.getLeadTime();
        return leadTime.isPresent() && leadTime.get() > timestampDuration;
    }

    private boolean isShutDownDefined(GeneratorConstraints generatorConstraints, double timestampDuration) {
        Optional<Double> lagTime = generatorConstraints.getLagTime();
        return lagTime.isPresent() && lagTime.get() > timestampDuration;
    }


    private void addVariables(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, int timestampIndex, List<OffsetDateTime> timestamps, boolean isStartUpDefined, boolean isShutDownDefined) {
        OffsetDateTime timestamp = timestamps.get(timestampIndex);
        OffsetDateTime nextTimestamp = timestamp == timestamps.getLast() ? null : timestamps.get(timestampIndex + 1);

        // create variables
        addPowerVariable(linearProblem, generatorConstraints, timestamp);
        addStateVariables(linearProblem, generatorConstraints.getGeneratorId(), timestamp, isStartUpDefined, isShutDownDefined);
        if (nextTimestamp != null) {
            addStateTransitionVariables(linearProblem, generatorConstraints.getGeneratorId(), timestamp, isStartUpDefined, isShutDownDefined);
        }
    }

    private void addBasicConstraints(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, int timestampIndex, List<OffsetDateTime> timestamps, InjectionRangeAction injectionRangeAction, boolean isStartUpDefined, boolean isShutDownDefined) {
        OffsetDateTime timestamp = timestamps.get(timestampIndex);
        OffsetDateTime nextTimestamp = timestamp == timestamps.getLast() ? null : timestamps.get(timestampIndex + 1);
        double timestampDuration = computeTimeGap(timestamp, nextTimestamp);

        // create and fill basic constraints on states
        addUniqueGeneratorStateConstraint(linearProblem, generatorConstraints.getGeneratorId(), timestamp, isStartUpDefined, isShutDownDefined);
        addOffPowerConstraint(linearProblem, generatorConstraints, timestamp);
        addOnPowerConstraints(linearProblem, generatorConstraints, timestamp);
        if (nextTimestamp != null) {
             // link transition to current state
            addStateFromTransitionConstraints(linearProblem, generatorConstraints.getGeneratorId(), timestamp, isStartUpDefined, isShutDownDefined);
            // link transition to next state
            addStateToTransitionConstraints(linearProblem, generatorConstraints.getGeneratorId(), timestamp, nextTimestamp, isStartUpDefined, isShutDownDefined);
        }
        addPowerToInjectionConstraint(linearProblem, generatorConstraints, timestamp, injectionRangeAction, preventiveStates.getData(timestamp).orElseThrow(), networks.getData(timestamp).orElseThrow());
    }

    // variables

    private static void addPowerVariable(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, OffsetDateTime timestamp) {
        linearProblem.addGeneratorPowerVariable(generatorConstraints.getGeneratorId(), generatorConstraints.getPMax().orElse(DEFAULT_P_MAX), timestamp);
    }

    private static void addStateVariables(LinearProblem linearProblem, String generatorId, OffsetDateTime timestamp, boolean isStartUpDefined, boolean isShutDownDefined) {
        linearProblem.addGeneratorStateVariable(generatorId, timestamp, LinearProblem.GeneratorState.ON);
        linearProblem.addGeneratorStateVariable(generatorId, timestamp, LinearProblem.GeneratorState.OFF);
        if (isStartUpDefined) {
            linearProblem.addGeneratorStateVariable(generatorId, timestamp, LinearProblem.GeneratorState.START_UP);
        }
        if (isShutDownDefined) {
            linearProblem.addGeneratorStateVariable(generatorId, timestamp, LinearProblem.GeneratorState.SHUT_DOWN);
        }
    }

    private static void addStateTransitionVariables(LinearProblem linearProblem, String generatorId, OffsetDateTime timestamp, boolean isStartUpDefined, boolean isShutDownDefined) {
        linearProblem.addGeneratorStateTransitionVariable(generatorId, timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.ON);
        linearProblem.addGeneratorStateTransitionVariable(generatorId, timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.OFF);

        if (isStartUpDefined) {
            linearProblem.addGeneratorStateTransitionVariable(generatorId, timestamp, LinearProblem.GeneratorState.START_UP, LinearProblem.GeneratorState.ON);
            linearProblem.addGeneratorStateTransitionVariable(generatorId, timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.START_UP);
            linearProblem.addGeneratorStateTransitionVariable(generatorId, timestamp, LinearProblem.GeneratorState.START_UP, LinearProblem.GeneratorState.START_UP);
        } else {
            linearProblem.addGeneratorStateTransitionVariable(generatorId, timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.ON);
        }

        if (isShutDownDefined) {
            linearProblem.addGeneratorStateTransitionVariable(generatorId, timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.SHUT_DOWN);
            linearProblem.addGeneratorStateTransitionVariable(generatorId, timestamp, LinearProblem.GeneratorState.SHUT_DOWN, LinearProblem.GeneratorState.SHUT_DOWN);
            linearProblem.addGeneratorStateTransitionVariable(generatorId, timestamp, LinearProblem.GeneratorState.SHUT_DOWN, LinearProblem.GeneratorState.OFF);
        } else {
            linearProblem.addGeneratorStateTransitionVariable(generatorId, timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.OFF);
        }
    }

    // constraints

    /**
     * C1 - The generator must and can only be in one state.
     * START_UP state is defined if LeadTime is present.
     * SHUT_DOWN state is defined if LagTime is present.
     * <br/>
     * ON + OFF (+ START_UP) (+ SHUT_DOWN) = 1
     */
    private static void addUniqueGeneratorStateConstraint(LinearProblem linearProblem, String generatorId, OffsetDateTime timestamp, boolean isStartUpDefined, boolean isShutDownDefined) {
        OpenRaoMPConstraint uniqueGeneratorStateConstraint = linearProblem.addUniqueGeneratorStateConstraint(generatorId, timestamp);
        uniqueGeneratorStateConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorId, timestamp, LinearProblem.GeneratorState.ON), 1);
        uniqueGeneratorStateConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorId, timestamp, LinearProblem.GeneratorState.OFF), 1);
        if (isStartUpDefined) {
            uniqueGeneratorStateConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorId, timestamp, LinearProblem.GeneratorState.START_UP), 1);
        }
        if (isShutDownDefined) {
            uniqueGeneratorStateConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorId, timestamp, LinearProblem.GeneratorState.SHUT_DOWN), 1);
        }

    }

    /**
     * C6 - The generator is OFF if and only if its power is null.
     * <br/>
     * P <= P_max (1 - OFF) + MINIMAL_P * OFF
     */
    private static void addOffPowerConstraint(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, OffsetDateTime timestamp) {
        OpenRaoMPConstraint offPowerConstraint = linearProblem.addGeneratorPowerOffConstraint(generatorConstraints.getGeneratorId(), generatorConstraints.getPMax().orElse(DEFAULT_P_MAX), timestamp);
        offPowerConstraint.setCoefficient(linearProblem.getGeneratorPowerVariable(generatorConstraints.getGeneratorId(), timestamp), 1);
        offPowerConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF), generatorConstraints.getPMax().orElse(DEFAULT_P_MAX) - MINIMAL_P);

    }

    /**
     * C7 - The generator is ON if and only if its power is in the range [P_min, P_max].
     * <br/>
     * P >= P_min ON
     * <br/>
     * P <= P_min (1 - ON) + P_max ON
     */
    private static void addOnPowerConstraints(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, OffsetDateTime timestamp) {
        OpenRaoMPVariable generatorPowerVariable = linearProblem.getGeneratorPowerVariable(generatorConstraints.getGeneratorId(), timestamp);
        OpenRaoMPVariable generatorOnVariable = linearProblem.getGeneratorStateVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON);
        OpenRaoMPConstraint onPowerConstraintInf = linearProblem.addGeneratorPowerOnConstraint(generatorConstraints.getGeneratorId(), timestamp, 0, linearProblem.infinity(), LinearProblem.AbsExtension.POSITIVE);
        onPowerConstraintInf.setCoefficient(generatorPowerVariable, 1);
        onPowerConstraintInf.setCoefficient(generatorOnVariable, -generatorConstraints.getPMin().orElse(0.0));
        OpenRaoMPConstraint onPowerConstraintSup = linearProblem.addGeneratorPowerOnConstraint(generatorConstraints.getGeneratorId(), timestamp, -linearProblem.infinity(), generatorConstraints.getPMin().orElse(0.0), LinearProblem.AbsExtension.NEGATIVE);
        onPowerConstraintSup.setCoefficient(generatorPowerVariable, 1);
        onPowerConstraintSup.setCoefficient(generatorOnVariable, generatorConstraints.getPMin().orElse(0.0) - generatorConstraints.getPMax().orElse(DEFAULT_P_MAX));
    }

    /**
     * C2 - The previous state of the generator must match the transition.
     * <br/>
     * state_j{t} = /Sigma T{state_i -> state_j}
     */
    private static void addStateFromTransitionConstraints(LinearProblem linearProblem, String generatorId, OffsetDateTime timestamp, boolean isStartUpDefined, boolean isShutDownDefined) {
        // ON
        // ON = Tr(ON->ON) + ...
        OpenRaoMPConstraint fromOnConstraint = linearProblem.addGeneratorStateFromTransitionConstraint(generatorId, timestamp, LinearProblem.GeneratorState.ON);
        fromOnConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorId, timestamp, LinearProblem.GeneratorState.ON), 1);
        fromOnConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorId, timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.ON), -1);

        // OFF
        // OFF = Tr(OFF->OFF) + ...
        OpenRaoMPConstraint fromOffConstraint = linearProblem.addGeneratorStateFromTransitionConstraint(generatorId, timestamp, LinearProblem.GeneratorState.OFF);
        fromOffConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorId, timestamp, LinearProblem.GeneratorState.OFF), 1);
        fromOffConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorId, timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.OFF), -1);

        // START-UP = Tr(START-UP->ON) + Tr(START-UP->START-UP)
        if (isStartUpDefined) {
            OpenRaoMPConstraint fromStartUpConstraint = linearProblem.addGeneratorStateFromTransitionConstraint(generatorId, timestamp, LinearProblem.GeneratorState.START_UP);
            fromStartUpConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorId, timestamp, LinearProblem.GeneratorState.START_UP), 1);
            fromStartUpConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorId, timestamp, LinearProblem.GeneratorState.START_UP, LinearProblem.GeneratorState.ON), -1);
            fromStartUpConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorId, timestamp, LinearProblem.GeneratorState.START_UP, LinearProblem.GeneratorState.START_UP), -1);

            // OFF = Tr(OFF->OFF) +  Tr(OFF->START-UP)
            fromOffConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorId, timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.START_UP), -1);
        } else {
            // OFF = Tr(OFF->OFF) +  Tr(OFF->ON)
            fromOffConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorId, timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.ON), -1);
        }

        if (isShutDownDefined) {
            // SHUT-DOWN = Tr(SHUT-DOWN->OFF) + Tr(SHUT-DOWN->SHUT-DOWN)
            OpenRaoMPConstraint fromShutDownConstraint = linearProblem.addGeneratorStateFromTransitionConstraint(generatorId, timestamp, LinearProblem.GeneratorState.SHUT_DOWN);
            fromShutDownConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorId, timestamp, LinearProblem.GeneratorState.SHUT_DOWN), 1);
            fromShutDownConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorId, timestamp, LinearProblem.GeneratorState.SHUT_DOWN, LinearProblem.GeneratorState.SHUT_DOWN), -1);
            fromShutDownConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorId, timestamp, LinearProblem.GeneratorState.SHUT_DOWN, LinearProblem.GeneratorState.OFF), -1);

            // SHUT-DOWN DEFINED : ON = Tr(ON->ON) + Tr(ON->SHUT-DOWN)
            fromOnConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorId, timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.SHUT_DOWN), -1);
        } else {
            // SHUT-DOWN NOT DEFINED : ON = Tr(ON->ON) + Tr(ON->OFF)
            fromOnConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorId, timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.OFF), -1);
        }
    }

    /**
     * C3 - The current state of the generator must match the transition.
     * <br/>
     * state_j{t+1} = /Sigma T{state_j -> state_i}
     */
    private static void addStateToTransitionConstraints(LinearProblem linearProblem, String generatorId, OffsetDateTime timestamp, OffsetDateTime nextTimestamp, boolean isStartUpDefined, boolean isShutDownDefined) {
        // ON
        // ON = Tr(ON->ON) + ...
        OpenRaoMPConstraint toOnConstraint = linearProblem.addGeneratorStateToTransitionConstraint(generatorId, timestamp, LinearProblem.GeneratorState.ON);
        toOnConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorId, nextTimestamp, LinearProblem.GeneratorState.ON), 1);
        toOnConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorId, timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.ON), -1);

        // OFF
        // OFF = Tr(OFF->OFF) + ...
        OpenRaoMPConstraint toOffConstraint = linearProblem.addGeneratorStateToTransitionConstraint(generatorId, timestamp, LinearProblem.GeneratorState.OFF);
        toOffConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorId, nextTimestamp, LinearProblem.GeneratorState.OFF), 1);
        toOffConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorId, timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.OFF), -1);

        if (isStartUpDefined) {
            // START-UP = Tr(OFF->START-UP) + Tr(START-UP->START-UP)
            OpenRaoMPConstraint toStartUpConstraint = linearProblem.addGeneratorStateToTransitionConstraint(generatorId, timestamp, LinearProblem.GeneratorState.START_UP);
            toStartUpConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorId, nextTimestamp, LinearProblem.GeneratorState.START_UP), 1);
            toStartUpConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorId, timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.START_UP), -1);
            toStartUpConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorId, timestamp, LinearProblem.GeneratorState.START_UP, LinearProblem.GeneratorState.START_UP), -1);

            // START-UP DEFINED : ON = Tr(ON->ON) + Tr(START-UP->ON)
            toOnConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorId, timestamp, LinearProblem.GeneratorState.START_UP, LinearProblem.GeneratorState.ON), -1);
        } else {
            // START-UP NOT DEFINED : ON = Tr(ON->ON) + Tr(OFF->ON)
            toOnConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorId, timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.ON), -1);
        }

        if (isShutDownDefined) {
            // SHUT-DOWN = Tr(ON->SHUT-DOWN) + Tr(SHUT-DOWN->SHUT-DOWN)
            OpenRaoMPConstraint toShutDownConstraint = linearProblem.addGeneratorStateToTransitionConstraint(generatorId, timestamp, LinearProblem.GeneratorState.SHUT_DOWN);
            toShutDownConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorId, nextTimestamp, LinearProblem.GeneratorState.SHUT_DOWN), 1);
            toShutDownConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorId, timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.SHUT_DOWN), -1);
            toShutDownConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorId, timestamp, LinearProblem.GeneratorState.SHUT_DOWN, LinearProblem.GeneratorState.SHUT_DOWN), -1);

            // SHUT-DOWN DEFINED : OFF = Tr(OFF->OFF) + Tr(SHUT-DOWN->OFF)
            toOffConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorId, timestamp, LinearProblem.GeneratorState.SHUT_DOWN, LinearProblem.GeneratorState.OFF), -1);
        } else {
            // SHUT-DOWN NOT DEFINED : OFF = Tr(OFF->OFF) + Tr(ON->OFF)
            toOffConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorId, timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.OFF), -1);
        }
    }

    // TODO reprendre ici
    // TODO ne peut on pas direct faire timestamp et nextTimestamp comme partout ?
    private static void addPowerVariationConstraints(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, int mainTimestampIndex, List<OffsetDateTime> allTimestamps, double timeGap) {
        double upwardPowerGradient = generatorConstraints.getUpwardPowerGradient().orElse(DEFAULT_POWER_GRADIENT);
        double downwardPowerGradient = generatorConstraints.getDownwardPowerGradient().orElse(-DEFAULT_POWER_GRADIENT);

        OpenRaoMPConstraint powerTransitionConstraintInf = linearProblem.addGeneratorPowerTransitionConstraint(generatorConstraints.getGeneratorId(), 0, linearProblem.infinity(), allTimestamps.get(mainTimestampIndex), LinearProblem.AbsExtension.POSITIVE);
        powerTransitionConstraintInf.setCoefficient(linearProblem.getGeneratorPowerVariable(generatorConstraints.getGeneratorId(), allTimestamps.get(mainTimestampIndex + 1)), 1.0);
        powerTransitionConstraintInf.setCoefficient(linearProblem.getGeneratorPowerVariable(generatorConstraints.getGeneratorId(), allTimestamps.get(mainTimestampIndex)), -1.0);

        OpenRaoMPConstraint powerTransitionConstraintSup = linearProblem.addGeneratorPowerTransitionConstraint(generatorConstraints.getGeneratorId(), -linearProblem.infinity(), 0, allTimestamps.get(mainTimestampIndex), LinearProblem.AbsExtension.NEGATIVE);
        powerTransitionConstraintSup.setCoefficient(linearProblem.getGeneratorPowerVariable(generatorConstraints.getGeneratorId(), allTimestamps.get(mainTimestampIndex + 1)), 1.0);
        powerTransitionConstraintSup.setCoefficient(linearProblem.getGeneratorPowerVariable(generatorConstraints.getGeneratorId(), allTimestamps.get(mainTimestampIndex)), -1.0);

        // TODO make this not optional
        Optional<Double> pMin = generatorConstraints.getPMin();
        Optional<Double> leadTime = generatorConstraints.getLeadTime();
        Optional<Double> lagTime = generatorConstraints.getLagTime();

        // ON -> ON
        OpenRaoMPVariable onOnTransitionVariable = linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), allTimestamps.get(mainTimestampIndex), LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.ON);
        powerTransitionConstraintInf.setCoefficient(onOnTransitionVariable, -downwardPowerGradient * timeGap);
        powerTransitionConstraintSup.setCoefficient(onOnTransitionVariable, -upwardPowerGradient * timeGap);

        //OFF -> OFF
        OpenRaoMPVariable offOffTransitionVariable = linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), allTimestamps.get(mainTimestampIndex), LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.OFF);
        powerTransitionConstraintInf.setCoefficient(offOffTransitionVariable, -MINIMAL_P);
        powerTransitionConstraintSup.setCoefficient(offOffTransitionVariable, -MINIMAL_P);

            if (leadTime.isPresent() && timeGap < leadTime.get()) {
                // OFF -> RU
                double upwardPowerRampFactor = pMin.orElse(0.0) / leadTime.get();
                OpenRaoMPVariable offRampUpTransitionVariable = linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), allTimestamps.get(mainTimestampIndex), LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.START_UP);
                powerTransitionConstraintInf.setCoefficient(offRampUpTransitionVariable, -upwardPowerRampFactor * timeGap);
                powerTransitionConstraintSup.setCoefficient(offRampUpTransitionVariable, -upwardPowerRampFactor * timeGap);
            } else {
                // OFF -> ON
                OpenRaoMPVariable offOnTransitionVariable = linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), allTimestamps.get(mainTimestampIndex), LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.ON);
                powerTransitionConstraintInf.setCoefficient(offOnTransitionVariable, -pMin.orElse(0.0));
                powerTransitionConstraintSup.setCoefficient(offOnTransitionVariable, -pMin.orElse(0.0) - (timeGap - leadTime.orElse(0.0)) * upwardPowerGradient);
            }

            if (leadTime.isPresent()) {
                if (timeGap < leadTime.get()) {
                    // RU -> RU
                    double upwardPowerRampFactor = pMin.orElse(0.0) / leadTime.get();
                    OpenRaoMPVariable rampUpRampUpTransitionVariable = linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), allTimestamps.get(mainTimestampIndex), LinearProblem.GeneratorState.START_UP, LinearProblem.GeneratorState.START_UP);
                    powerTransitionConstraintInf.setCoefficient(rampUpRampUpTransitionVariable, -upwardPowerRampFactor * timeGap);
                    powerTransitionConstraintSup.setCoefficient(rampUpRampUpTransitionVariable, -upwardPowerRampFactor * timeGap);
                }
                // RU -> ON
                for (int rampUpStartTimestampIndex = 0; rampUpStartTimestampIndex < mainTimestampIndex; rampUpStartTimestampIndex++) {
                    Optional<OffsetDateTime> projectedRampUpEnd = getRampUpEndTimestamp(rampUpStartTimestampIndex, allTimestamps, leadTime.get());
                    if (projectedRampUpEnd.isPresent() && projectedRampUpEnd.get().equals(allTimestamps.get(mainTimestampIndex + 1))) {
                        OpenRaoMPVariable offRampUpTransitionVariable = linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), allTimestamps.get(rampUpStartTimestampIndex), LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.START_UP);
                        double minimalPowerIncrease = (leadTime.get() - computeTimeGap(allTimestamps.get(rampUpStartTimestampIndex), allTimestamps.get(mainTimestampIndex))) * pMin.orElse(0.0) / leadTime.get();
                        powerTransitionConstraintInf.setCoefficient(offRampUpTransitionVariable, -minimalPowerIncrease);
                        powerTransitionConstraintSup.setCoefficient(offRampUpTransitionVariable, -minimalPowerIncrease - (computeTimeGap(allTimestamps.get(rampUpStartTimestampIndex), allTimestamps.get(mainTimestampIndex + 1)) - leadTime.get()) * upwardPowerGradient);
                    }
                }
            }

            if (lagTime.isPresent()) {
                // ON -> RD
                for (int rampDownEndTimestampIndex = mainTimestampIndex + 2; rampDownEndTimestampIndex < allTimestamps.size(); rampDownEndTimestampIndex++) {
                    Optional<OffsetDateTime> projectedRampDownStart = getRampDownStartTimestamp(rampDownEndTimestampIndex, allTimestamps, lagTime.get());
                    if (projectedRampDownStart.isPresent() && projectedRampDownStart.get().equals(allTimestamps.get(mainTimestampIndex))) {
                        OpenRaoMPVariable onRampDownTransitionVariable = linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), allTimestamps.get(rampDownEndTimestampIndex - 1), LinearProblem.GeneratorState.SHUT_DOWN, LinearProblem.GeneratorState.OFF);
                        double minimalPowerDecrease = -(lagTime.get() - computeTimeGap(allTimestamps.get(mainTimestampIndex + 1), allTimestamps.get(rampDownEndTimestampIndex))) * pMin.orElse(0.0) / lagTime.get();
                        powerTransitionConstraintInf.setCoefficient(onRampDownTransitionVariable, -minimalPowerDecrease - (computeTimeGap(allTimestamps.get(mainTimestampIndex), allTimestamps.get(rampDownEndTimestampIndex)) - lagTime.get()) * downwardPowerGradient);
                        powerTransitionConstraintSup.setCoefficient(onRampDownTransitionVariable, -minimalPowerDecrease);
                    }
                }
            }

            if (lagTime.isEmpty() || timeGap >= lagTime.get()) {
                // ON -> OFF
                OpenRaoMPVariable onOffTransitionVariable = linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), allTimestamps.get(mainTimestampIndex), LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.OFF);
                powerTransitionConstraintInf.setCoefficient(onOffTransitionVariable, pMin.orElse(0.0) - (timeGap - lagTime.orElse(0.0)) * downwardPowerGradient);
                powerTransitionConstraintSup.setCoefficient(onOffTransitionVariable, pMin.orElse(0.0));
            } else {
                double downwardPowerRampFactor = pMin.orElse(0.0) / lagTime.get();
                // RD -> RD
                OpenRaoMPVariable rampDownRampDownTransitionVariable = linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), allTimestamps.get(mainTimestampIndex), LinearProblem.GeneratorState.SHUT_DOWN, LinearProblem.GeneratorState.SHUT_DOWN);
                powerTransitionConstraintInf.setCoefficient(rampDownRampDownTransitionVariable, downwardPowerRampFactor * timeGap);
                powerTransitionConstraintSup.setCoefficient(rampDownRampDownTransitionVariable, downwardPowerRampFactor * timeGap);
                // RD -> OFF
                OpenRaoMPVariable rampDownOffTransitionVariable = linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), allTimestamps.get(mainTimestampIndex), LinearProblem.GeneratorState.SHUT_DOWN, LinearProblem.GeneratorState.OFF);
                powerTransitionConstraintInf.setCoefficient(rampDownOffTransitionVariable, downwardPowerRampFactor * timeGap);
                powerTransitionConstraintSup.setCoefficient(rampDownOffTransitionVariable, downwardPowerRampFactor * timeGap);
            }
    }

    private static void addPowerToInjectionConstraint(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, OffsetDateTime timestamp, InjectionRangeAction injectionRangeAction, State state, Network network) {
        OpenRaoMPConstraint powerToInjectionConstraint = linearProblem.addGeneratorToInjectionConstraint(generatorConstraints.getGeneratorId(), injectionRangeAction, timestamp);
        powerToInjectionConstraint.setCoefficient(linearProblem.getGeneratorPowerVariable(generatorConstraints.getGeneratorId(), timestamp), 1.0);
        powerToInjectionConstraint.setCoefficient(linearProblem.getRangeActionSetpointVariable(injectionRangeAction, state), -getDistributionKey(generatorConstraints.getGeneratorId(), injectionRangeAction, network));
    }

    /**
     * C4 - START-UP definition
     * <br/>
     * For t' between t and leadTime, T(OFF->START-UP)(t) < START-UP(t')
     */
    private static void addStartUpConstraint(LinearProblem linearProblem, String generatorId, OffsetDateTime startingUpTimestamp, OffsetDateTime nextTimestamp) {
        OpenRaoMPConstraint startUpConstraint = linearProblem.addGeneratorRampingConstraint(generatorId, startingUpTimestamp, nextTimestamp, LinearProblem.VariationDirectionExtension.UPWARD);
        startUpConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorId, startingUpTimestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.START_UP), 1.0);
        startUpConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorId, nextTimestamp, LinearProblem.GeneratorState.START_UP), -1.0);
    }

    /**
     * C5 - SHUT-DOWN definition
     * <br/>
     * For t' between t - lagTime and t, T(SHUT-DOWN->OFF)(t) < SHUT-DOWN(t')
     */
    // TODO vérifier pourquoi on avait 3 timestamps ici
    private static void addShutDownConstraint(LinearProblem linearProblem, String generatorId, OffsetDateTime shutDownTimestamp, OffsetDateTime previousTimestamp) {
        OpenRaoMPConstraint shutDownConstraint = linearProblem.addGeneratorRampingConstraint(generatorId, previousTimestamp, shutDownTimestamp, LinearProblem.VariationDirectionExtension.DOWNWARD);
        shutDownConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorId, shutDownTimestamp, LinearProblem.GeneratorState.SHUT_DOWN, LinearProblem.GeneratorState.OFF), 1.0);
        shutDownConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorId, previousTimestamp, LinearProblem.GeneratorState.SHUT_DOWN), -1.0);
    }

    // utility methods
// TODO simplify
    private static double computeTimeGap(OffsetDateTime timestamp1, OffsetDateTime timestamp2) {
        if (timestamp1 == null || timestamp2 == null) {
            return DEFAULT_TIMESTAMP_DURATION;
        } else if (timestamp1.isAfter(timestamp2)) {
            throw new OpenRaoException("timestamp1 is expected to come before timestamp2");
        }
        return timestamp1.until(timestamp2, ChronoUnit.SECONDS) / 3600.0;
    }

    private Optional<TemporalData<InjectionRangeAction>> getInjectionRangeActionOfGenerator(String generatorId) {
        Map<OffsetDateTime, InjectionRangeAction> injectionRangeActionPerTimestamp = new HashMap<>();
        for (OffsetDateTime timestamp : injectionRangeActionsPerTimestamp.getTimestamps()) {
            Optional<InjectionRangeAction> injectionRangeAction = getInjectionRangeActionOfGenerator(generatorId, injectionRangeActionsPerTimestamp.getData(timestamp).orElse(Set.of()));
            if (injectionRangeAction.isEmpty()) {
                OpenRaoLoggerProvider.TECHNICAL_LOGS.warn("Generator {} is not involved in any redispatching action for timestamp {} and will thus be ignored.", generatorId, timestamp);
                return Optional.empty();
            }
            injectionRangeActionPerTimestamp.put(timestamp, injectionRangeAction.get());
        }
        return Optional.of(new TemporalDataImpl<>(injectionRangeActionPerTimestamp));
    }

    private static Optional<InjectionRangeAction> getInjectionRangeActionOfGenerator(String generatorId, Set<InjectionRangeAction> allInjectionRangeActions) {
        return allInjectionRangeActions.stream().filter(injectionRangeAction -> injectionRangeAction.getNetworkElements().stream().map(NetworkElement::getId).anyMatch(generatorId::equals)).min(Comparator.comparing(Identifiable::getId));
    }

    private static double getDistributionKey(String generatorId, InjectionRangeAction injectionRangeAction, Network network) {
        return getGeneratorTypeCoefficient(generatorId, network) * injectionRangeAction.getInjectionDistributionKeys().entrySet().stream().filter(entry -> entry.getKey().getId().equals(generatorId)).map(Map.Entry::getValue).findFirst().orElse(0.0);
    }

    private static double getGeneratorTypeCoefficient(String generatorId, Network network) {
        if (network.getGenerator(generatorId) != null) {
            return 1.0;
        } else if (network.getLoad(generatorId) != null) {
            return -1.0;
        } else {
            throw new OpenRaoException("Network element %s is neither a generator nor a load.".formatted(generatorId));
        }
    }

    // TODO delete
    private static Optional<OffsetDateTime> getRampUpEndTimestamp(int rampUpStartTimestampIndex, List<OffsetDateTime> allTimestamps, double leadTime) {
        for (int possibleRampUpEndTimestamp = rampUpStartTimestampIndex; possibleRampUpEndTimestamp < allTimestamps.size(); possibleRampUpEndTimestamp++) {
            if (computeTimeGap(allTimestamps.get(rampUpStartTimestampIndex), allTimestamps.get(possibleRampUpEndTimestamp)) >= leadTime) {
                return Optional.of(allTimestamps.get(possibleRampUpEndTimestamp));
            }
        }
        return Optional.empty();
    }

    // TODO delete
    private static Optional<OffsetDateTime> getRampDownStartTimestamp(int rampDownEndTimestampIndex, List<OffsetDateTime> allTimestamps, double lagTime) {
        for (int possibleRampDownStartTimestamp = rampDownEndTimestampIndex - 1; possibleRampDownStartTimestamp >= 0; possibleRampDownStartTimestamp--) {
            if (computeTimeGap(allTimestamps.get(possibleRampDownStartTimestamp), allTimestamps.get(rampDownEndTimestampIndex)) >= lagTime) {
                return Optional.of(allTimestamps.get(possibleRampDownStartTimestamp));
            }
        }
        return Optional.empty();
    }

    @Override
    public void updateBetweenMipIteration(LinearProblem linearProblem, RangeActionActivationResult rangeActionActivationResult) {
        // nothing to do
    }
}
