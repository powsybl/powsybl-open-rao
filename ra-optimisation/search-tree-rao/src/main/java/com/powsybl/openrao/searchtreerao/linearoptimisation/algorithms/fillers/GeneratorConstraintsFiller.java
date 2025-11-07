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

    private static final double DEFAULT_TIMESTAMP_DURATION = Double.MAX_VALUE;
    private static final double DEFAULT_P_MAX = 10000.0;
    private static final double DEFAULT_POWER_GRADIENT = 100000.0;

    public GeneratorConstraintsFiller(TemporalData<Network> networks, TemporalData<State> preventiveStates, TemporalData<Set<InjectionRangeAction>> injectionRangeActionsPerTimestamp, Set<GeneratorConstraints> generatorConstraints) {
        this.networks = networks;
        this.preventiveStates = preventiveStates;
        this.injectionRangeActionsPerTimestamp = injectionRangeActionsPerTimestamp;
        this.generatorConstraints = generatorConstraints;
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        List<OffsetDateTime> timestamps = preventiveStates.getTimestamps();
        for (GeneratorConstraints individualGeneratorConstraints : generatorConstraints) {
            Optional<TemporalData<InjectionRangeAction>> associatedInjections = getInjectionRangeActionOfGenerator(individualGeneratorConstraints.getGeneratorId());
            if (associatedInjections.isPresent()) {
                OffsetDateTime previousTimestamp = null;
                for (OffsetDateTime timestamp : timestamps) {
                    addVariablesAndBasicConstraints(linearProblem, individualGeneratorConstraints, timestamp, previousTimestamp, associatedInjections.get().getData(timestamp).orElseThrow());
                    previousTimestamp = timestamp;
                }
                int numberOfTimestamps = timestamps.size();
                for (int timestampIndex = 1; timestampIndex < numberOfTimestamps; timestampIndex++) {
                    for (int laterTimestampIndex = timestampIndex + 1; laterTimestampIndex < numberOfTimestamps; laterTimestampIndex++) {
                        double rampingDuration = computeTimeGap(timestamps.get(timestampIndex - 1), timestamps.get(laterTimestampIndex));
                        addRampUpConstraint(linearProblem, individualGeneratorConstraints, timestamps.get(timestampIndex), timestamps.get(laterTimestampIndex), rampingDuration);
                        addRampDownConstraint(linearProblem, individualGeneratorConstraints, timestamps.get(timestampIndex), timestamps.get(laterTimestampIndex), rampingDuration);
                    }
                    addPowerVariationConstraints(linearProblem, individualGeneratorConstraints, timestampIndex, timestamps);
                }
            }
        }
    }

    private void addVariablesAndBasicConstraints(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, OffsetDateTime timestamp, OffsetDateTime previousTimestamp, InjectionRangeAction injectionRangeAction) {
        double timestampDuration = computeTimeGap(previousTimestamp, timestamp);

        // create variables
        addPowerVariable(linearProblem, generatorConstraints, timestamp);
        addStateVariables(linearProblem, generatorConstraints, timestamp);
        addStateTransitionVariables(linearProblem, generatorConstraints, timestamp, timestampDuration);
        // create and fill basic constraints on states
        addUniqueGeneratorStateConstraint(linearProblem, generatorConstraints, timestamp);
        addOffPowerConstraint(linearProblem, generatorConstraints, timestamp);
        addOnPowerConstraints(linearProblem, generatorConstraints, timestamp);
        addStateTransitionConstraints(linearProblem, generatorConstraints, timestamp, previousTimestamp, timestampDuration);
        addPowerToInjectionConstraint(linearProblem, generatorConstraints, timestamp, injectionRangeAction, preventiveStates.getData(timestamp).orElseThrow(), networks.getData(timestamp).orElseThrow());
    }

    // variables

    private static void addPowerVariable(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, OffsetDateTime timestamp) {
        linearProblem.addGeneratorPowerVariable(generatorConstraints.getGeneratorId(), generatorConstraints.getPMax().orElse(DEFAULT_P_MAX), timestamp);
    }

    private static void addStateVariables(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, OffsetDateTime timestamp) {
        linearProblem.addGeneratorStateVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON);
        linearProblem.addGeneratorStateVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF);
        linearProblem.addGeneratorStateVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_UP);
        linearProblem.addGeneratorStateVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_DOWN);
    }

    private static void addStateTransitionVariables(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, OffsetDateTime timestamp, double timestampDuration) {
        Optional<Double> leadTime = generatorConstraints.getLeadTime();
        Optional<Double> lagTime = generatorConstraints.getLagTime();

        linearProblem.addGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.ON);
        linearProblem.addGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.OFF);
        linearProblem.addGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_UP, LinearProblem.GeneratorState.ON);
        linearProblem.addGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_DOWN, LinearProblem.GeneratorState.OFF);

        if (leadTime.isPresent() && timestampDuration < leadTime.get()) {
            linearProblem.addGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.RAMP_UP);
            linearProblem.addGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_UP, LinearProblem.GeneratorState.RAMP_UP);
        } else {
            linearProblem.addGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.ON);
        }

        if (lagTime.isPresent()) {
            // TODO: should check next timestamp duration
            linearProblem.addGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.RAMP_DOWN);
            if (timestampDuration < lagTime.get()) {
                linearProblem.addGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_DOWN, LinearProblem.GeneratorState.RAMP_DOWN);
            } else {
                linearProblem.addGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.OFF);
            }
        } else {
            linearProblem.addGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.OFF);
        }
    }

    // constraints

    /**
     * The generator must and can only be in one state.
     * <br/>
     * ON + OFF (+ RAMP_UP) (+ RAMP_DOWN) = 1
     */
    private static void addUniqueGeneratorStateConstraint(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, OffsetDateTime timestamp) {
        OpenRaoMPConstraint uniqueGeneratorStateConstraint = linearProblem.addUniqueGeneratorStateConstraint(generatorConstraints.getGeneratorId(), timestamp);
        uniqueGeneratorStateConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON), 1);
        uniqueGeneratorStateConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF), 1);
        uniqueGeneratorStateConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_UP), 1);
        uniqueGeneratorStateConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_DOWN), 1);
    }

    /**
     * The generator is off if and only if its power is null.
     * <br/>
     * P <= P_max (1 - OFF)
     */
    private static void addOffPowerConstraint(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, OffsetDateTime timestamp) {
        OpenRaoMPConstraint offPowerConstraint = linearProblem.addGeneratorPowerOffConstraint(generatorConstraints.getGeneratorId(), generatorConstraints.getPMax().orElse(DEFAULT_P_MAX), timestamp);
        offPowerConstraint.setCoefficient(linearProblem.getGeneratorPowerVariable(generatorConstraints.getGeneratorId(), timestamp), 1);
        offPowerConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF), generatorConstraints.getPMax().orElse(DEFAULT_P_MAX));
    }

    /**
     * The generator is on if and only if its power is in the range [P_min, P_max].
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

    private static void addStateTransitionConstraints(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, OffsetDateTime timestamp, OffsetDateTime previousTimestamp, double timestampDuration) {
        // link transition to previous state
        if (previousTimestamp != null) {
            addStateFromTransitionConstraints(linearProblem, generatorConstraints, timestamp, previousTimestamp, timestampDuration);
        }
        // link transition to current state
        addStateToTransitionConstraints(linearProblem, generatorConstraints, timestamp, timestampDuration);
    }

    /**
     * The previous state of the generator must match the transition.
     * <br/>
     * ON{t - 1} = T{ON -> ON} (+ T{ON -> OFF}) (+ T{ON -> RAMP_DOWN})
     * <br/>
     * OFF{t - 1} = T{OFF -> OFF} (+ T{OFF -> ON}) (+ T{OFF -> RAMP_UP})
     * <br/>
     * RAMP_DOWN{t - 1} = T{RAMP_DOWN -> RAMP_DOWN} + T{RAMP_DOWN -> OFF}
     * <br/>
     * RAMP_UP{t - 1} = T{RAMP_UP -> RAMP_UP} + T{RAMP_UP -> ON}
     */
    private static void addStateFromTransitionConstraints(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, OffsetDateTime timestamp, OffsetDateTime previousTimestamp, double timestampDuration) {
        OpenRaoMPConstraint fromOnConstraint = linearProblem.addGeneratorStateFromTransitionConstraint(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON);
        fromOnConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorConstraints.getGeneratorId(), previousTimestamp, LinearProblem.GeneratorState.ON), 1);
        fromOnConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.ON), -1);

        OpenRaoMPConstraint fromOffConstraint = linearProblem.addGeneratorStateFromTransitionConstraint(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF);
        fromOffConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorConstraints.getGeneratorId(), previousTimestamp, LinearProblem.GeneratorState.OFF), 1);
        fromOffConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.OFF), -1);

        Optional<Double> leadTime = generatorConstraints.getLeadTime();
        if (leadTime.isPresent()) {
            fromOffConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.RAMP_UP), -1);

            OpenRaoMPConstraint fromRampUpConstraint = linearProblem.addGeneratorStateFromTransitionConstraint(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_UP);
            fromRampUpConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorConstraints.getGeneratorId(), previousTimestamp, LinearProblem.GeneratorState.RAMP_UP), 1);
            fromRampUpConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_UP, LinearProblem.GeneratorState.ON), -1);

            if (timestampDuration < leadTime.get()) {
                fromRampUpConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_UP, LinearProblem.GeneratorState.RAMP_UP), -1);
            } else {
                fromOffConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.ON), -1);
            }
        } else {
            fromOffConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.ON), -1);
        }

        Optional<Double> lagTime = generatorConstraints.getLagTime();
        if (lagTime.isPresent()) {
            fromOnConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.RAMP_DOWN), -1);

            if (timestampDuration < lagTime.get()) {
                OpenRaoMPConstraint fromRampDownConstraint = linearProblem.addGeneratorStateFromTransitionConstraint(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_DOWN);
                fromRampDownConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorConstraints.getGeneratorId(), previousTimestamp, LinearProblem.GeneratorState.RAMP_DOWN), 1);
                fromRampDownConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_DOWN, LinearProblem.GeneratorState.OFF), -1);
                fromRampDownConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_DOWN, LinearProblem.GeneratorState.RAMP_DOWN), -1);
            } else {
                fromOnConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.OFF), -1);
            }
        } else {
            fromOnConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.OFF), -1);
        }
    }

    /**
     * The current state of the generator must match the transition.
     * <br/>
     * ON{t} = T{ON -> ON} (+ T{OFF -> ON}) (+ T{RAMP_UP -> ON})
     * <br/>
     * OFF{t} = T{OFF -> OFF} (+ T{ON -> OFF}) (+ T{RAMP_DOWN -> OFF})
     * <br/>
     * RAMP_DOWN{t} = T{RAMP_DOWN -> RAMP_DOWN} + T{ON -> RAMP_DOWN}
     * <br/>
     * RAMP_UP{t} = T{RAMP_UP -> RAMP_UP} + T{OFF -> RAMP_UP}
     */
    private static void addStateToTransitionConstraints(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, OffsetDateTime timestamp, double timestampDuration) {
        OpenRaoMPConstraint toOnConstraint = linearProblem.addGeneratorStateToTransitionConstraint(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON);
        toOnConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON), 1);
        toOnConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.ON), -1);

        OpenRaoMPConstraint toOffConstraint = linearProblem.addGeneratorStateToTransitionConstraint(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF);
        toOffConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF), 1);
        toOffConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.OFF), -1);

        Optional<Double> leadTime = generatorConstraints.getLeadTime();
        if (leadTime.isPresent()) {
            toOnConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_UP, LinearProblem.GeneratorState.ON), -1);

            if (timestampDuration < leadTime.get()) {
                OpenRaoMPConstraint toRampUpConstraint = linearProblem.addGeneratorStateToTransitionConstraint(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_UP);
                toRampUpConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_UP), 1);
                toRampUpConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.RAMP_UP), -1);
                toRampUpConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_UP, LinearProblem.GeneratorState.RAMP_UP), -1);
            } else {
                toOnConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.ON), -1);
            }
        } else {
            toOnConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.ON), -1);
        }

        Optional<Double> lagTime = generatorConstraints.getLagTime();
        if (lagTime.isPresent()) {
            toOffConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_DOWN, LinearProblem.GeneratorState.OFF), -1);

            OpenRaoMPConstraint toRampDownConstraint = linearProblem.addGeneratorStateToTransitionConstraint(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_DOWN);
            toRampDownConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_DOWN), 1);
            toRampDownConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.RAMP_DOWN), -1);

            if (timestampDuration < lagTime.get()) {
                toRampDownConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_DOWN, LinearProblem.GeneratorState.RAMP_DOWN), -1);
            } else {
                toOffConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.OFF), -1);
            }
        } else {
            toOffConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.OFF), -1);
        }
    }

    private static void addPowerVariationConstraints(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, int mainTimestampIndex, List<OffsetDateTime> allTimestamps) {
        double upwardPowerGradient = generatorConstraints.getUpwardPowerGradient().orElse(DEFAULT_POWER_GRADIENT);
        double downwardPowerGradient = generatorConstraints.getDownwardPowerGradient().orElse(-DEFAULT_POWER_GRADIENT);

        OpenRaoMPConstraint powerTransitionConstraintInf = linearProblem.addGeneratorPowerTransitionConstraint(generatorConstraints.getGeneratorId(), 0, linearProblem.infinity(), allTimestamps.get(mainTimestampIndex), LinearProblem.AbsExtension.POSITIVE);
        powerTransitionConstraintInf.setCoefficient(linearProblem.getGeneratorPowerVariable(generatorConstraints.getGeneratorId(), allTimestamps.get(mainTimestampIndex)), 1.0);
        powerTransitionConstraintInf.setCoefficient(linearProblem.getGeneratorPowerVariable(generatorConstraints.getGeneratorId(), allTimestamps.get(mainTimestampIndex - 1)), -1.0);

        OpenRaoMPConstraint powerTransitionConstraintSup = linearProblem.addGeneratorPowerTransitionConstraint(generatorConstraints.getGeneratorId(), -linearProblem.infinity(), 0, allTimestamps.get(mainTimestampIndex), LinearProblem.AbsExtension.NEGATIVE);
        powerTransitionConstraintSup.setCoefficient(linearProblem.getGeneratorPowerVariable(generatorConstraints.getGeneratorId(), allTimestamps.get(mainTimestampIndex)), 1.0);
        powerTransitionConstraintSup.setCoefficient(linearProblem.getGeneratorPowerVariable(generatorConstraints.getGeneratorId(), allTimestamps.get(mainTimestampIndex - 1)), -1.0);

        double pMin = generatorConstraints.getPMin().orElse(0.0);
        Optional<Double> leadTime = generatorConstraints.getLeadTime();
        Optional<Double> lagTime = generatorConstraints.getLagTime();

        double maximumPowerAmplitude = generatorConstraints.getPMax().orElse(DEFAULT_P_MAX) - generatorConstraints.getPMin().orElse(0.0);
        double mainTimestampDuration = computeTimeGap(allTimestamps.get(mainTimestampIndex - 1), allTimestamps.get(mainTimestampIndex));

        // OFF -> OFF: nothing to do
        // OFF -> RU
        if (leadTime.isPresent() && mainTimestampDuration < leadTime.get()) {
            double upwardPowerRampFactor = pMin / leadTime.get();
            OpenRaoMPVariable offRampUpTransitionVariable = linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), allTimestamps.get(mainTimestampIndex), LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.RAMP_UP);
            powerTransitionConstraintInf.setCoefficient(offRampUpTransitionVariable, -upwardPowerRampFactor * mainTimestampDuration);
            powerTransitionConstraintSup.setCoefficient(offRampUpTransitionVariable, -upwardPowerRampFactor * mainTimestampDuration);
        }
        // OFF -> ON
        if (leadTime.isEmpty() || mainTimestampDuration >= leadTime.get()) {
            OpenRaoMPVariable offOnTransitionVariable = linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), allTimestamps.get(mainTimestampIndex), LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.ON);
            powerTransitionConstraintInf.setCoefficient(offOnTransitionVariable, -pMin);
            powerTransitionConstraintSup.setCoefficient(offOnTransitionVariable, -pMin - Math.min(maximumPowerAmplitude, (mainTimestampDuration - leadTime.orElse(0.0)) * upwardPowerGradient));
        }
        // OFF -> RD: impossible

        // RU -> OFF: impossible
        // RU -> RU
        if (leadTime.isPresent() && mainTimestampDuration < leadTime.get()) {
            double upwardPowerRampFactor = pMin / leadTime.get();
            OpenRaoMPVariable rampUpRampUpTransitionVariable = linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), allTimestamps.get(mainTimestampIndex), LinearProblem.GeneratorState.RAMP_UP, LinearProblem.GeneratorState.RAMP_UP);
            powerTransitionConstraintInf.setCoefficient(rampUpRampUpTransitionVariable, -upwardPowerRampFactor * mainTimestampDuration);
            powerTransitionConstraintSup.setCoefficient(rampUpRampUpTransitionVariable, -upwardPowerRampFactor * mainTimestampDuration);
        }
        // RU -> ON
        if (leadTime.isPresent()) {
            for (int rampUpStartTimestampIndex = 1; rampUpStartTimestampIndex < mainTimestampIndex; rampUpStartTimestampIndex++) {
                Optional<OffsetDateTime> projectedRampUpEnd = getRampUpEndTimestamp(rampUpStartTimestampIndex, allTimestamps, leadTime.get());
                if (projectedRampUpEnd.isPresent() && projectedRampUpEnd.get().equals(allTimestamps.get(mainTimestampIndex))) {
                    OpenRaoMPVariable offRampUpTransitionVariable = linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), allTimestamps.get(rampUpStartTimestampIndex), LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.RAMP_UP);
                    double minimalPowerIncrease = (leadTime.get() - computeTimeGap(allTimestamps.get(rampUpStartTimestampIndex - 1), allTimestamps.get(mainTimestampIndex - 1))) * pMin / leadTime.get();
                    powerTransitionConstraintInf.setCoefficient(offRampUpTransitionVariable, -minimalPowerIncrease);
                    powerTransitionConstraintSup.setCoefficient(offRampUpTransitionVariable, -minimalPowerIncrease - Math.min(maximumPowerAmplitude, (computeTimeGap(allTimestamps.get(rampUpStartTimestampIndex - 1), allTimestamps.get(mainTimestampIndex)) - leadTime.get()) * upwardPowerGradient));
                }
            }
        }
        // RU -> RD: impossible

        // ON -> OFF
        if (lagTime.isEmpty() || mainTimestampDuration >= lagTime.get()) {
            OpenRaoMPVariable onOffTransitionVariable = linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), allTimestamps.get(mainTimestampIndex), LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.OFF);
            powerTransitionConstraintInf.setCoefficient(onOffTransitionVariable, pMin);
            powerTransitionConstraintSup.setCoefficient(onOffTransitionVariable, pMin - Math.max(-maximumPowerAmplitude, (mainTimestampDuration - lagTime.orElse(0.0)) * downwardPowerGradient));
        }
        // ON -> RU: impossible
        // ON -> ON
        OpenRaoMPVariable onOnTransitionVariable = linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), allTimestamps.get(mainTimestampIndex), LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.ON);
        powerTransitionConstraintInf.setCoefficient(onOnTransitionVariable, -downwardPowerGradient * mainTimestampDuration);
        powerTransitionConstraintSup.setCoefficient(onOnTransitionVariable, -upwardPowerGradient * mainTimestampDuration);
        // ON -> RD
        if (lagTime.isPresent()) {
            for (int rampDownEndTimestampIndex = mainTimestampIndex + 1; rampDownEndTimestampIndex < allTimestamps.size(); rampDownEndTimestampIndex++) {
                Optional<OffsetDateTime> projectedRampDownStartEnd = getRampDownStartTimestamp(rampDownEndTimestampIndex, allTimestamps, lagTime.get());
                if (projectedRampDownStartEnd.isPresent() && projectedRampDownStartEnd.get().equals(allTimestamps.get(mainTimestampIndex))) {
                    OpenRaoMPVariable onRampDownTransitionVariable = linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), allTimestamps.get(rampDownEndTimestampIndex), LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.RAMP_DOWN);
                    double minimalPowerDecrease = -(lagTime.get() - computeTimeGap(allTimestamps.get(mainTimestampIndex), allTimestamps.get(rampDownEndTimestampIndex))) * pMin / lagTime.get();
                    powerTransitionConstraintInf.setCoefficient(onRampDownTransitionVariable, -minimalPowerDecrease - Math.max(-maximumPowerAmplitude, (computeTimeGap(allTimestamps.get(mainTimestampIndex - 1), allTimestamps.get(rampDownEndTimestampIndex)) - lagTime.get()) * downwardPowerGradient));
                    powerTransitionConstraintSup.setCoefficient(onRampDownTransitionVariable, -minimalPowerDecrease);
                }
            }
        }

        // RD -> OFF
        if (lagTime.isPresent() && mainTimestampDuration < lagTime.get()) {
            double downwardPowerRampFactor = pMin / lagTime.get();
            OpenRaoMPVariable rampDownOffTransitionVariable = linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), allTimestamps.get(mainTimestampIndex), LinearProblem.GeneratorState.RAMP_DOWN, LinearProblem.GeneratorState.OFF);
            powerTransitionConstraintInf.setCoefficient(rampDownOffTransitionVariable, downwardPowerRampFactor * mainTimestampDuration);
            powerTransitionConstraintSup.setCoefficient(rampDownOffTransitionVariable, downwardPowerRampFactor * mainTimestampDuration);
        }
        // RD -> RU: impossible
        // RD -> ON: impossible
        // RD -> RD
        if (lagTime.isPresent() && mainTimestampDuration < lagTime.get()) {
            double downwardPowerRampFactor = pMin / lagTime.get();
            OpenRaoMPVariable rampDownRampDownTransitionVariable = linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), allTimestamps.get(mainTimestampIndex), LinearProblem.GeneratorState.RAMP_DOWN, LinearProblem.GeneratorState.RAMP_DOWN);
            powerTransitionConstraintInf.setCoefficient(rampDownRampDownTransitionVariable, downwardPowerRampFactor * mainTimestampDuration);
            powerTransitionConstraintSup.setCoefficient(rampDownRampDownTransitionVariable, downwardPowerRampFactor * mainTimestampDuration);
        }
    }

    private static void addPowerToInjectionConstraint(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, OffsetDateTime timestamp, InjectionRangeAction injectionRangeAction, State state, Network network) {
        OpenRaoMPConstraint powerToInjectionConstraint = linearProblem.addGeneratorToInjectionConstraint(generatorConstraints.getGeneratorId(), injectionRangeAction, timestamp);
        powerToInjectionConstraint.setCoefficient(linearProblem.getGeneratorPowerVariable(generatorConstraints.getGeneratorId(), timestamp), 1.0);
        powerToInjectionConstraint.setCoefficient(linearProblem.getRangeActionSetpointVariable(injectionRangeAction, state), -getDistributionKey(generatorConstraints.getGeneratorId(), injectionRangeAction, network));
    }

    private static void addRampUpConstraint(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, OffsetDateTime rampingStartTimestamp, OffsetDateTime otherRampingTimestamp, double rampingDuration) {
        Optional<Double> leadTime = generatorConstraints.getLeadTime();
        if (generatorConstraints.getPMin().isPresent() && leadTime.isPresent() && rampingDuration <= leadTime.get()) {
            OpenRaoMPConstraint rampUpConstraint = linearProblem.addGeneratorRampingConstraint(generatorConstraints.getGeneratorId(), rampingStartTimestamp, otherRampingTimestamp, LinearProblem.VariationDirectionExtension.UPWARD);
            rampUpConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), rampingStartTimestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.RAMP_UP), 1.0);
            rampUpConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorConstraints.getGeneratorId(), otherRampingTimestamp, LinearProblem.GeneratorState.RAMP_UP), -1.0);
        }
    }

    private static void addRampDownConstraint(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, OffsetDateTime rampingStartTimestamp, OffsetDateTime otherRampingTimestamp, double rampingDuration) {
        Optional<Double> lagTime = generatorConstraints.getLagTime();
        if (generatorConstraints.getPMin().isPresent() && lagTime.isPresent() && rampingDuration <= lagTime.get()) {
            OpenRaoMPConstraint rampUpConstraint = linearProblem.addGeneratorRampingConstraint(generatorConstraints.getGeneratorId(), rampingStartTimestamp, otherRampingTimestamp, LinearProblem.VariationDirectionExtension.DOWNWARD);
            rampUpConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), rampingStartTimestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.RAMP_DOWN), 1.0);
            rampUpConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorConstraints.getGeneratorId(), otherRampingTimestamp, LinearProblem.GeneratorState.RAMP_DOWN), -1.0);
        }
    }

    // utility methods

    private static double computeTimeGap(OffsetDateTime timestamp1, OffsetDateTime timestamp2) {
        if (timestamp1 == null) {
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
        return allInjectionRangeActions.stream().filter(
                injectionRangeAction -> injectionRangeAction.getNetworkElements().stream().map(NetworkElement::getId).sorted().toList().contains(generatorId))
            .findFirst();
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

    private static Optional<OffsetDateTime> getRampUpEndTimestamp(int rampUpStartTimestampIndex, List<OffsetDateTime> allTimestamps, double leadTime) {
        OffsetDateTime previousTimestamp = allTimestamps.get(rampUpStartTimestampIndex - 1);
        for (int possibleRampUpEndTimestamp = rampUpStartTimestampIndex; possibleRampUpEndTimestamp < allTimestamps.size(); possibleRampUpEndTimestamp++) {
            if (computeTimeGap(previousTimestamp, allTimestamps.get(possibleRampUpEndTimestamp)) > leadTime) {
                return Optional.of(allTimestamps.get(possibleRampUpEndTimestamp));
            }
        }
        return Optional.empty();
    }

    private static Optional<OffsetDateTime> getRampDownStartTimestamp(int rampDownEndTimestampIndex, List<OffsetDateTime> allTimestamps, double lagTime) {
        for (int possibleRampDownStartTimestamp = rampDownEndTimestampIndex - 1; possibleRampDownStartTimestamp >= 1; possibleRampDownStartTimestamp--) {
            if (computeTimeGap(allTimestamps.get(possibleRampDownStartTimestamp - 1), allTimestamps.get(rampDownEndTimestampIndex)) > lagTime) {
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
