/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.generatorconstraints.GeneratorConstraints;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPVariable;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class GeneratorConstraintsFiller implements ProblemFiller {
    private final TemporalData<State> preventiveStates;
    private final TemporalData<Network> networkPerTimestamp;
    private final TemporalData<Set<InjectionRangeAction>> injectionRangeActionsPerTimestamp;
    private final Set<GeneratorConstraints> generatorConstraints;

    private static final double TIME_EPSILON = 1e-8; // used to ensure that lead and lag times are not multiples of the timestamp duration (which would create side effects in some cases)

    public GeneratorConstraintsFiller(TemporalData<State> preventiveStates, TemporalData<Network> networkPerTimestamp, TemporalData<Set<InjectionRangeAction>> injectionRangeActionsPerTimestamp, Set<GeneratorConstraints> generatorConstraints) {
        this.preventiveStates = preventiveStates;
        this.networkPerTimestamp = networkPerTimestamp;
        this.injectionRangeActionsPerTimestamp = injectionRangeActionsPerTimestamp;
        this.generatorConstraints = generatorConstraints;
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        List<OffsetDateTime> timestamps = preventiveStates.getTimestamps();
        double timestampDuration = computeTimestampDuration(timestamps);
        // TODO : only create generator variables when necessary (i.e. if impacted by RD or CT)
        for (GeneratorConstraints individualGeneratorConstraints : generatorConstraints) {
            double reducedLeadTime = computeReducedCharacteristicTime(individualGeneratorConstraints.getLeadTime().orElse(0.0), timestampDuration);
            double reducedLagTime = computeReducedCharacteristicTime(individualGeneratorConstraints.getLagTime().orElse(0.0), timestampDuration);
            OffsetDateTime previousTimestamp = null;
            double initialGeneratorPower = getInitialPower(individualGeneratorConstraints.getGeneratorId(), networkPerTimestamp.getData(timestamps.get(0)).orElseThrow());
            for (OffsetDateTime timestamp : timestamps) {
                addVariablesAndBasicConstraints(linearProblem, individualGeneratorConstraints, timestamp, previousTimestamp, timestampDuration, initialGeneratorPower, reducedLeadTime, reducedLagTime);
                previousTimestamp = timestamp;
            }
            addInterTemporalConstraints(linearProblem, individualGeneratorConstraints, timestamps, timestampDuration, reducedLeadTime, reducedLagTime);
        }
    }

    private void addVariablesAndBasicConstraints(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, OffsetDateTime timestamp, OffsetDateTime previousTimestamp, double timestampDuration, double initialGeneratorPower, double reducedLeadTime, double reducedLagTime) {
        // create variables
        addPowerVariable(linearProblem, generatorConstraints, timestamp);
        addStateVariables(linearProblem, generatorConstraints, timestamp, timestampDuration);
        addStateTransitionVariables(linearProblem, generatorConstraints, timestamp, timestampDuration);
        // create and fill basic constraints on states
        addUniqueGeneratorStateConstraint(linearProblem, generatorConstraints, timestamp, timestampDuration);
        addOffPowerConstraint(linearProblem, generatorConstraints, timestamp);
        addOnPowerConstraints(linearProblem, generatorConstraints, timestamp);
        addStateTransitionConstraints(linearProblem, generatorConstraints, initialGeneratorPower, timestamp, previousTimestamp, timestampDuration);
        addPowerTransitionConstraints(linearProblem, generatorConstraints, initialGeneratorPower, timestamp, previousTimestamp, timestampDuration, reducedLeadTime, reducedLagTime);
    }

    private static void addInterTemporalConstraints(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, List<OffsetDateTime> timestamps, double timestampDuration, double reducedLeadTime, double reducedLagTime) {
        addRampUpTimeConstraints(linearProblem, generatorConstraints, timestamps, timestampDuration);
        addRampDownTimeConstraints(linearProblem, generatorConstraints, timestamps, timestampDuration);
    }

    // variables

    private static void addPowerVariable(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, OffsetDateTime timestamp) {
        linearProblem.addGeneratorPowerVariable(generatorConstraints.getGeneratorId(), generatorConstraints.getPMax().orElse(linearProblem.infinity()), timestamp);
    }

    private static void addStateVariables(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, OffsetDateTime timestamp, double timestampDuration) {
        linearProblem.addGeneratorStateVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON);
        linearProblem.addGeneratorStateVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF);
        if (leadTimeLongerThanTimestamp(generatorConstraints, timestampDuration)) {
            linearProblem.addGeneratorStateVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_UP);
        }
        if (lagTimeLongerThanTimestamp(generatorConstraints, timestampDuration)) {
            linearProblem.addGeneratorStateVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_DOWN);
        }
    }

    private static void addStateTransitionVariables(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, OffsetDateTime timestamp, double timestampDuration) {
        linearProblem.addGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.OFF);
        linearProblem.addGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.ON);
        if (leadTimeLongerThanTimestamp(generatorConstraints, timestampDuration)) {
            linearProblem.addGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.RAMP_UP);
            linearProblem.addGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_UP, LinearProblem.GeneratorState.RAMP_UP);
            linearProblem.addGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_UP, LinearProblem.GeneratorState.ON);
        } else {
            linearProblem.addGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.ON);
        }
        if (lagTimeLongerThanTimestamp(generatorConstraints, timestampDuration)) {
            linearProblem.addGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.RAMP_DOWN);
            linearProblem.addGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_DOWN, LinearProblem.GeneratorState.RAMP_DOWN);
            linearProblem.addGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_DOWN, LinearProblem.GeneratorState.OFF);
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
    private static void addUniqueGeneratorStateConstraint(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, OffsetDateTime timestamp, double timestampDuration) {
        OpenRaoMPConstraint uniqueGeneratorStateConstraint = linearProblem.addUniqueGeneratorStateConstraint(generatorConstraints.getGeneratorId(), timestamp);
        uniqueGeneratorStateConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON), 1);
        uniqueGeneratorStateConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF), 1);
        if (leadTimeLongerThanTimestamp(generatorConstraints, timestampDuration)) {
            uniqueGeneratorStateConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_UP), 1);
        }
        if (lagTimeLongerThanTimestamp(generatorConstraints, timestampDuration)) {
            uniqueGeneratorStateConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_DOWN), 1);
        }
    }

    /**
     * The generator is off if and only if its power is null.
     * <br/>
     * P <= P_max (1 - OFF)
     */
    private static void addOffPowerConstraint(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, OffsetDateTime timestamp) {
        OpenRaoMPConstraint offPowerConstraint = linearProblem.addGeneratorPowerOffConstraint(generatorConstraints.getGeneratorId(), generatorConstraints.getPMax().orElse(linearProblem.infinity()), timestamp);
        offPowerConstraint.setCoefficient(linearProblem.getGeneratorPowerVariable(generatorConstraints.getGeneratorId(), timestamp), 1);
        offPowerConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF), generatorConstraints.getPMax().orElse(linearProblem.infinity()));
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
        onPowerConstraintSup.setCoefficient(generatorOnVariable, generatorConstraints.getPMin().orElse(0.0) - generatorConstraints.getPMax().orElse(linearProblem.infinity()));
    }

    private static void addStateTransitionConstraints(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, double initialGeneratorPower, OffsetDateTime timestamp, OffsetDateTime previousTimestamp, double timestampDuration) {
        // link transition to previous state
        if (previousTimestamp == null) {
            // handle initial timestamp separately
            addInitialStateFromTransitionConstraint(linearProblem, generatorConstraints, timestamp, timestampDuration, initialGeneratorPower);
        } else {
            addStateFromTransitionConstraints(linearProblem, generatorConstraints, timestamp, previousTimestamp, timestampDuration);
        }
        // link transition to current state
        addStateToTransitionConstraints(linearProblem, generatorConstraints, timestamp, timestampDuration);
    }

    private static void addInitialStateFromTransitionConstraint(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, OffsetDateTime timestamp, double timestampDuration, double initialGeneratorPower) {
        LinearProblem.GeneratorState initialGeneratorState = getInitialGeneratorState(generatorConstraints, initialGeneratorPower);
        OpenRaoMPConstraint initialStateFromConstraint = linearProblem.addGeneratorInitialStateFromTransitionConstraint(generatorConstraints.getGeneratorId(), timestamp);
        if (initialGeneratorState == LinearProblem.GeneratorState.ON) {
            initialStateFromConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.ON), 1);
            if (leadTimeLongerThanTimestamp(generatorConstraints, timestampDuration)) {
                initialStateFromConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.RAMP_DOWN), 1);
            } else {
                initialStateFromConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.OFF), 1);
            }
        } else {
            initialStateFromConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.OFF), 1);
            if (lagTimeLongerThanTimestamp(generatorConstraints, timestampDuration)) {
                initialStateFromConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.RAMP_UP), 1);
            } else {
                initialStateFromConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.ON), 1);
            }
        }
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
        fromOnConstraint.setCoefficient(linearProblem.addGeneratorStateVariable(generatorConstraints.getGeneratorId(), previousTimestamp, LinearProblem.GeneratorState.ON), 1);
        fromOnConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.ON), -1);
        OpenRaoMPConstraint fromOffConstraint = linearProblem.addGeneratorStateFromTransitionConstraint(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF);
        fromOffConstraint.setCoefficient(linearProblem.addGeneratorStateVariable(generatorConstraints.getGeneratorId(), previousTimestamp, LinearProblem.GeneratorState.OFF), 1);
        fromOffConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.OFF), -1);
        if (leadTimeLongerThanTimestamp(generatorConstraints, timestampDuration)) {
            fromOffConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.RAMP_UP), -1);
            OpenRaoMPConstraint fromRampUpConstraint = linearProblem.addGeneratorStateFromTransitionConstraint(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_UP);
            fromRampUpConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_UP, LinearProblem.GeneratorState.RAMP_UP), -1);
            fromRampUpConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_UP, LinearProblem.GeneratorState.ON), -1);
        } else {
            fromOffConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.ON), -1);
        }
        if (lagTimeLongerThanTimestamp(generatorConstraints, timestampDuration)) {
            fromOnConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.RAMP_DOWN), -1);
            OpenRaoMPConstraint fromRampDownConstraint = linearProblem.addGeneratorStateFromTransitionConstraint(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_DOWN);
            fromRampDownConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_DOWN, LinearProblem.GeneratorState.RAMP_DOWN), -1);
            fromRampDownConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_DOWN, LinearProblem.GeneratorState.OFF), -1);
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
        toOnConstraint.setCoefficient(linearProblem.addGeneratorStateVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON), 1);
        toOnConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.ON), -1);
        OpenRaoMPConstraint toOffConstraint = linearProblem.addGeneratorStateToTransitionConstraint(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF);
        toOffConstraint.setCoefficient(linearProblem.addGeneratorStateVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF), 1);
        toOffConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.OFF), -1);
        if (leadTimeLongerThanTimestamp(generatorConstraints, timestampDuration)) {
            toOnConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_DOWN, LinearProblem.GeneratorState.RAMP_UP), -1);
            OpenRaoMPConstraint toRampUpConstraint = linearProblem.addGeneratorStateToTransitionConstraint(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_UP);
            toRampUpConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_UP, LinearProblem.GeneratorState.RAMP_UP), -1);
            toRampUpConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.RAMP_UP), -1);
        } else {
            toOnConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.ON), -1);
        }
        if (lagTimeLongerThanTimestamp(generatorConstraints, timestampDuration)) {
            toOffConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_DOWN, LinearProblem.GeneratorState.OFF), -1);
            OpenRaoMPConstraint toRampDownConstraint = linearProblem.addGeneratorStateToTransitionConstraint(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_DOWN);
            toRampDownConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_DOWN, LinearProblem.GeneratorState.RAMP_DOWN), -1);
            toRampDownConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.RAMP_DOWN), -1);
        } else {
            toOffConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.OFF), -1);
        }
    }

    private static void addRampUpTimeConstraints(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, List<OffsetDateTime> timestamps, double timestampDuration) {
        if (leadTimeLongerThanTimestamp(generatorConstraints, timestampDuration)) {
            int consecutiveRampUpSteps = ((int) Math.floor(generatorConstraints.getLeadTime().orElseThrow() / timestampDuration)) - 1;
            int numberOfTimestamps = timestamps.size();
            for (int timestampIndex = 0; timestampIndex <= numberOfTimestamps; timestampIndex++) {
                OffsetDateTime timestamp = timestamps.get(timestampIndex);
                OpenRaoMPConstraint rampUpTimeConstraint = linearProblem.addGeneratorStateTimeConstraint(generatorConstraints.getGeneratorId(), 0, linearProblem.infinity(), timestamp, LinearProblem.GeneratorState.RAMP_UP, LinearProblem.MinOrMax.MIN);
                rampUpTimeConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.RAMP_UP), -consecutiveRampUpSteps);
                for (int offset = 1; offset <= Math.min(consecutiveRampUpSteps, numberOfTimestamps - timestampIndex); offset++) {
                    rampUpTimeConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamps.get(timestampIndex + offset), LinearProblem.GeneratorState.RAMP_UP, LinearProblem.GeneratorState.RAMP_UP), 1);
                }
            }
        }
    }

    private static void addRampDownTimeConstraints(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, List<OffsetDateTime> timestamps, double timestampDuration) {
        if (lagTimeLongerThanTimestamp(generatorConstraints, timestampDuration)) {
            int consecutiveRampDownSteps = ((int) Math.floor(generatorConstraints.getLagTime().orElseThrow() / timestampDuration)) - 1;
            int numberOfTimestamps = timestamps.size();
            for (int timestampIndex = 0; timestampIndex <= numberOfTimestamps; timestampIndex++) {
                OffsetDateTime timestamp = timestamps.get(timestampIndex);
                OpenRaoMPConstraint rampDownTimeConstraint = linearProblem.addGeneratorStateTimeConstraint(generatorConstraints.getGeneratorId(), 0, linearProblem.infinity(), timestamp, LinearProblem.GeneratorState.RAMP_DOWN, LinearProblem.MinOrMax.MIN);
                rampDownTimeConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.RAMP_DOWN), -consecutiveRampDownSteps);
                for (int offset = 1; offset <= Math.min(consecutiveRampDownSteps, numberOfTimestamps - timestampIndex); offset++) {
                    rampDownTimeConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamps.get(timestampIndex + offset), LinearProblem.GeneratorState.RAMP_DOWN, LinearProblem.GeneratorState.RAMP_DOWN), 1);
                }
            }
        }
    }

    private static void addPowerTransitionConstraints(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, double initialGeneratorPower, OffsetDateTime timestamp, OffsetDateTime previousTimestamp, double timestampDuration, double reducedLeadTime, double reducedLagTime) {
        double upwardPowerGradient = generatorConstraints.getUpwardPowerGradient().orElse(linearProblem.infinity());
        double downwardPowerGradient = generatorConstraints.getDownwardPowerGradient().orElse(-linearProblem.infinity());
        OpenRaoMPVariable powerVariable = linearProblem.getGeneratorPowerVariable(generatorConstraints.getGeneratorId(), timestamp);
        OpenRaoMPVariable onOnTransitionVariable = linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.ON);
        OpenRaoMPConstraint powerTransitionConstraintInf = linearProblem.addGeneratorPowerTransitionConstraint(generatorConstraints.getGeneratorId(), 0, linearProblem.infinity(), timestamp, LinearProblem.AbsExtension.POSITIVE);
        powerTransitionConstraintInf.setCoefficient(powerVariable, 1);
        powerTransitionConstraintInf.setCoefficient(onOnTransitionVariable, -timestampDuration * downwardPowerGradient);
        OpenRaoMPConstraint powerTransitionConstraintSup = linearProblem.addGeneratorPowerTransitionConstraint(generatorConstraints.getGeneratorId(), -linearProblem.infinity(), 0, timestamp, LinearProblem.AbsExtension.NEGATIVE);
        powerTransitionConstraintSup.setCoefficient(powerVariable, 1);
        powerTransitionConstraintSup.setCoefficient(onOnTransitionVariable, -timestampDuration * upwardPowerGradient);

        // use power of previous timestamp, or initial power if first timestamp
        if (previousTimestamp != null) {
            OpenRaoMPVariable previousPowerVariable = linearProblem.getGeneratorPowerVariable(generatorConstraints.getGeneratorId(), timestamp);
            powerTransitionConstraintInf.setCoefficient(previousPowerVariable, -1);
            powerTransitionConstraintSup.setCoefficient(previousPowerVariable, -1);
        } else {
            powerTransitionConstraintInf.setLb(initialGeneratorPower);
            powerTransitionConstraintSup.setUb(initialGeneratorPower);
        }
        if (leadTimeLongerThanTimestamp(generatorConstraints, timestampDuration)) {
            OpenRaoMPVariable offRampUpTransitionVariable = linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.RAMP_UP);
            OpenRaoMPVariable rampUpRampUpTransitionVariable = linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_UP, LinearProblem.GeneratorState.RAMP_UP);
            OpenRaoMPVariable rampUpOnTransitionVariable = linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_UP, LinearProblem.GeneratorState.ON);
            double leadTime = generatorConstraints.getLeadTime().orElseThrow();
            powerTransitionConstraintInf.setCoefficient(offRampUpTransitionVariable, -timestampDuration * generatorConstraints.getPMin().orElse(0.0) / leadTime);
            powerTransitionConstraintInf.setCoefficient(rampUpRampUpTransitionVariable, -timestampDuration * generatorConstraints.getPMin().orElse(0.0) / leadTime);
            powerTransitionConstraintInf.setCoefficient(rampUpOnTransitionVariable, -reducedLeadTime * generatorConstraints.getPMin().orElse(0.0) / leadTime);
            powerTransitionConstraintSup.setCoefficient(offRampUpTransitionVariable, -timestampDuration * generatorConstraints.getPMin().orElse(0.0) / leadTime);
            powerTransitionConstraintSup.setCoefficient(rampUpRampUpTransitionVariable, -timestampDuration * generatorConstraints.getPMin().orElse(0.0) / leadTime);
            powerTransitionConstraintSup.setCoefficient(rampUpOnTransitionVariable, -reducedLeadTime * generatorConstraints.getPMin().orElse(0.0) / leadTime - (timestampDuration - reducedLeadTime) * upwardPowerGradient);
        } else {
            OpenRaoMPVariable offOnTransitionVariable = linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.ON);
            powerTransitionConstraintInf.setCoefficient(offOnTransitionVariable, -generatorConstraints.getPMin().orElse(0.0));
            powerTransitionConstraintSup.setCoefficient(offOnTransitionVariable, -generatorConstraints.getPMin().orElse(0.0) - (timestampDuration - reducedLeadTime) * upwardPowerGradient);
        }

        if (lagTimeLongerThanTimestamp(generatorConstraints, timestampDuration)) {
            OpenRaoMPVariable onRampDownTransitionVariable = linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.RAMP_DOWN);
            OpenRaoMPVariable rampDownRampDownTransitionVariable = linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_DOWN, LinearProblem.GeneratorState.RAMP_DOWN);
            OpenRaoMPVariable rampDownOffTransitionVariable = linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_DOWN, LinearProblem.GeneratorState.OFF);
            double lagTime = generatorConstraints.getLagTime().orElseThrow();
            powerTransitionConstraintInf.setCoefficient(onRampDownTransitionVariable, reducedLagTime * generatorConstraints.getPMin().orElse(0.0) / lagTime - (timestampDuration - reducedLagTime) * downwardPowerGradient);
            powerTransitionConstraintInf.setCoefficient(rampDownRampDownTransitionVariable, timestampDuration * generatorConstraints.getPMin().orElse(0.0) / lagTime);
            powerTransitionConstraintInf.setCoefficient(rampDownOffTransitionVariable, timestampDuration * generatorConstraints.getPMin().orElse(0.0) / lagTime);
            powerTransitionConstraintSup.setCoefficient(onRampDownTransitionVariable, reducedLagTime * generatorConstraints.getPMin().orElse(0.0) / lagTime);
            powerTransitionConstraintSup.setCoefficient(rampDownRampDownTransitionVariable, timestampDuration * generatorConstraints.getPMin().orElse(0.0) / lagTime);
            powerTransitionConstraintSup.setCoefficient(rampDownOffTransitionVariable, timestampDuration * generatorConstraints.getPMin().orElse(0.0) / lagTime);
        } else {
            OpenRaoMPVariable onOffTransitionVariable = linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.OFF);
            powerTransitionConstraintInf.setCoefficient(onOffTransitionVariable, generatorConstraints.getPMin().orElse(0.0) - (timestampDuration - reducedLagTime) * downwardPowerGradient);
            powerTransitionConstraintSup.setCoefficient(onOffTransitionVariable, generatorConstraints.getPMin().orElse(0.0));
        }
    }

    // utility methods

    private static boolean leadTimeLongerThanTimestamp(GeneratorConstraints generatorConstraints, double timestampDuration) {
        Optional<Double> leadTime = generatorConstraints.getLeadTime();
        return leadTime.isPresent() && leadTime.get() > timestampDuration;
    }

    private static boolean lagTimeLongerThanTimestamp(GeneratorConstraints generatorConstraints, double timestampDuration) {
        Optional<Double> lagTime = generatorConstraints.getLeadTime();
        return lagTime.isPresent() && lagTime.get() > timestampDuration;
    }

    private static double computeTimestampDuration(List<OffsetDateTime> timestamps) {
        if (timestamps.size() < 2) {
            throw new OpenRaoException("At least two timestamps are required for inter-temporal computations.");
        }
        double timestampDuration = timestamps.get(1).until(timestamps.get(0), ChronoUnit.HOURS);
        for (int index = 1; index < timestamps.size() - 1; index++) {
            if (timestamps.get(index + 1).until(timestamps.get(index), ChronoUnit.HOURS) != timestampDuration) {
                // TODO: shall this check be performed elsewhere?
                throw new OpenRaoException("The timestamps must be evenly spaced in time.");
            }
        }
        return timestampDuration;
    }

    private static LinearProblem.GeneratorState getInitialGeneratorState(GeneratorConstraints generatorConstraints, double initialGeneratorPower) {
        if (initialGeneratorPower == 0) {
            return LinearProblem.GeneratorState.OFF;
        } else if (initialGeneratorPower >= generatorConstraints.getPMin().orElse(0.0) && initialGeneratorPower <= generatorConstraints.getPMax().orElse(Double.MAX_VALUE)) {
            return LinearProblem.GeneratorState.ON;
        } else {
            throw new OpenRaoException("Could not determine the initial state of generator '%s'.".formatted(generatorConstraints.getGeneratorId()));
        }
    }

    private static double computeReducedCharacteristicTime(double time, double timestampDuration) {
        return Math.max(0, time - TIME_EPSILON) % timestampDuration;
    }

    private static double getInitialPower(String generatorId, Network network) {
        Identifiable<?> networkElement = network.getIdentifiable(generatorId);
        if (networkElement instanceof Generator generator) {
            return generator.getTargetP();
        } else if (networkElement instanceof Load load) {
            return load.getP0();
        } else {
            throw new OpenRaoException("Network element `%s` is neither a generator nor a load.".formatted(generatorId));
        }
    }

    @Override
    public void updateBetweenMipIteration(LinearProblem linearProblem, RangeActionActivationResult rangeActionActivationResult) {
        // nothing to do
    }
}
