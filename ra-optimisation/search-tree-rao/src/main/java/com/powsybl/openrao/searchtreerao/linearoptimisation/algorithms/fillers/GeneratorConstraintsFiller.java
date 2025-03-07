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
import com.powsybl.openrao.data.crac.api.NetworkElement;
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
import java.util.Map;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
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
            OffsetDateTime previousTimestamp = null;
            double initialGeneratorPower = getInitialPower(individualGeneratorConstraints.getGeneratorId(), networkPerTimestamp.getData(timestamps.get(0)).orElseThrow());
            for (OffsetDateTime timestamp : timestamps) {
                addVariablesAndBasicConstraints(linearProblem, individualGeneratorConstraints, timestamp, previousTimestamp, timestampDuration, initialGeneratorPower);
                previousTimestamp = timestamp;
            }
            addInterTemporalConstraints(linearProblem, individualGeneratorConstraints, timestamps, timestampDuration);
        }
    }

    private void addVariablesAndBasicConstraints(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, OffsetDateTime timestamp, OffsetDateTime previousTimestamp, double timestampDuration, double initialGeneratorPower) {
        // create variables
        addPowerVariable(linearProblem, generatorConstraints, timestamp);
        addStateVariables(linearProblem, generatorConstraints, timestamp, timestampDuration);
        addStateTransitionVariables(linearProblem, generatorConstraints, timestamp, timestampDuration);

        // create and fill basic constraints
        addUniqueGeneratorStateConstraint(linearProblem, generatorConstraints, timestamp, timestampDuration);
        addOffPowerConstraint(linearProblem, generatorConstraints, timestamp);
        addOnPowerConstraints(linearProblem, generatorConstraints, timestamp);
        addStateTransitionConstraints(linearProblem, generatorConstraints, initialGeneratorPower, timestamp, previousTimestamp, timestampDuration);
        addPowerTransitionConstraints(linearProblem, generatorConstraints, initialGeneratorPower, timestamp, previousTimestamp, timestampDuration);
        addRedispatchingImpactConstraint(linearProblem, generatorConstraints, timestamp);
    }

    private static void addInterTemporalConstraints(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, List<OffsetDateTime> timestamps, double timestampDuration) {
        // TODO: rampUp, rampDown, minUp, maxUp, minOff
    }

    // variables

    private static void addPowerVariable(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, OffsetDateTime timestamp) {
        linearProblem.addGeneratorPowerVariable(generatorConstraints.getGeneratorId(), generatorConstraints.getPMax(), timestamp);
    }

    private static void addStateVariables(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, OffsetDateTime timestamp, double timestampDuration) {
        linearProblem.addGeneratorStateVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON);
        linearProblem.addGeneratorStateVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF);
        if (generatorConstraints.getLeadTime() > timestampDuration) {
            linearProblem.addGeneratorStateVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_UP);
        }
        if (generatorConstraints.getLagTime() > timestampDuration) {
            linearProblem.addGeneratorStateVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_DOWN);
        }
    }

    private static void addStateTransitionVariables(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, OffsetDateTime timestamp, double timestampDuration) {
        linearProblem.addGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.OFF);
        linearProblem.addGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.ON);

        if (generatorConstraints.getLeadTime() <= timestampDuration) {
            linearProblem.addGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.ON);
        } else {
            linearProblem.addGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.RAMP_UP);
            linearProblem.addGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_UP, LinearProblem.GeneratorState.RAMP_UP);
            linearProblem.addGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_UP, LinearProblem.GeneratorState.ON);
        }

        if (generatorConstraints.getLagTime() <= timestampDuration) {
            linearProblem.addGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.OFF);
        } else {
            linearProblem.addGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.RAMP_DOWN);
            linearProblem.addGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_DOWN, LinearProblem.GeneratorState.RAMP_DOWN);
            linearProblem.addGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_DOWN, LinearProblem.GeneratorState.OFF);
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
        if (generatorConstraints.getLeadTime() > timestampDuration) {
            uniqueGeneratorStateConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_UP), 1);
        }
        if (generatorConstraints.getLagTime() > timestampDuration) {
            uniqueGeneratorStateConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_DOWN), 1);
        }
    }

    /**
     * The generator is off if and only if its power is null.
     * <br/>
     * P <= P_max (1 - OFF)
     */
    private static void addOffPowerConstraint(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, OffsetDateTime timestamp) {
        OpenRaoMPConstraint offPowerConstraint = linearProblem.addGeneratorPowerOffConstraint(generatorConstraints.getGeneratorId(), generatorConstraints.getPMax(), timestamp);
        offPowerConstraint.setCoefficient(linearProblem.getGeneratorPowerVariable(generatorConstraints.getGeneratorId(), timestamp), 1);
        offPowerConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF), generatorConstraints.getPMax());
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
        onPowerConstraintInf.setCoefficient(generatorOnVariable, -generatorConstraints.getPMin());

        OpenRaoMPConstraint onPowerConstraintSup = linearProblem.addGeneratorPowerOnConstraint(generatorConstraints.getGeneratorId(), timestamp, -linearProblem.infinity(), generatorConstraints.getPMin(), LinearProblem.AbsExtension.NEGATIVE);
        onPowerConstraintSup.setCoefficient(generatorPowerVariable, 1);
        onPowerConstraintSup.setCoefficient(generatorOnVariable, generatorConstraints.getPMin() - generatorConstraints.getPMax());
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
            if (generatorConstraints.getLeadTime() <= timestampDuration) {
                initialStateFromConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.OFF), 1);
            } else {
                initialStateFromConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.RAMP_DOWN), 1);
            }
        } else {
            initialStateFromConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.OFF), 1);
            if (generatorConstraints.getLeadTime() <= timestampDuration) {
                initialStateFromConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.ON), 1);
            } else {
                initialStateFromConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.RAMP_UP), 1);
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

        if (generatorConstraints.getLeadTime() <= timestampDuration) {
            fromOffConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.ON), -1);
        } else {
            fromOffConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.RAMP_UP), -1);

            OpenRaoMPConstraint fromRampUpConstraint = linearProblem.addGeneratorStateFromTransitionConstraint(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_UP);
            fromRampUpConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_UP, LinearProblem.GeneratorState.RAMP_UP), -1);
            fromRampUpConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_UP, LinearProblem.GeneratorState.ON), -1);
        }

        if (generatorConstraints.getLagTime() <= timestampDuration) {
            fromOnConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.OFF), -1);
        } else {
            fromOnConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.RAMP_DOWN), -1);

            OpenRaoMPConstraint fromRampDownConstraint = linearProblem.addGeneratorStateFromTransitionConstraint(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_DOWN);
            fromRampDownConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_DOWN, LinearProblem.GeneratorState.RAMP_DOWN), -1);
            fromRampDownConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_DOWN, LinearProblem.GeneratorState.OFF), -1);
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

        if (generatorConstraints.getLeadTime() <= timestampDuration) {
            toOnConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.ON), -1);
        } else {
            toOnConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_DOWN, LinearProblem.GeneratorState.RAMP_UP), -1);

            OpenRaoMPConstraint toRampUpConstraint = linearProblem.addGeneratorStateToTransitionConstraint(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_UP);
            toRampUpConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_UP, LinearProblem.GeneratorState.RAMP_UP), -1);
            toRampUpConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.RAMP_UP), -1);
        }

        if (generatorConstraints.getLagTime() <= timestampDuration) {
            toOffConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.OFF), -1);
        } else {
            toOffConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_DOWN, LinearProblem.GeneratorState.OFF), -1);

            OpenRaoMPConstraint toRampDownConstraint = linearProblem.addGeneratorStateToTransitionConstraint(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_DOWN);
            toRampDownConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_DOWN, LinearProblem.GeneratorState.RAMP_DOWN), -1);
            toRampDownConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.RAMP_DOWN), -1);
        }
    }

    private static void addPowerTransitionConstraints(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, double initialGeneratorPower, OffsetDateTime timestamp, OffsetDateTime previousTimestamp, double timestampDuration) {
        double reducedLeadTime = computeReducedCharacteristicTime(generatorConstraints.getLeadTime(), timestampDuration);
        double reducedLagTime = computeReducedCharacteristicTime(generatorConstraints.getLagTime(), timestampDuration);

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

        if (generatorConstraints.getLeadTime() <= timestampDuration) {
            OpenRaoMPVariable offOnTransitionVariable = linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.ON);

            powerTransitionConstraintInf.setCoefficient(offOnTransitionVariable, -generatorConstraints.getPMin());
            powerTransitionConstraintSup.setCoefficient(offOnTransitionVariable, -generatorConstraints.getPMin() - (timestampDuration - reducedLeadTime) * upwardPowerGradient);
        } else {
            OpenRaoMPVariable offRampUpTransitionVariable = linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.RAMP_UP);
            OpenRaoMPVariable rampUpRampUpTransitionVariable = linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_UP, LinearProblem.GeneratorState.RAMP_UP);
            OpenRaoMPVariable rampUpOnTransitionVariable = linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_UP, LinearProblem.GeneratorState.ON);

            powerTransitionConstraintInf.setCoefficient(offRampUpTransitionVariable, -timestampDuration * generatorConstraints.getPMin() / generatorConstraints.getLeadTime());
            powerTransitionConstraintInf.setCoefficient(rampUpRampUpTransitionVariable, -timestampDuration * generatorConstraints.getPMin() / generatorConstraints.getLeadTime());
            powerTransitionConstraintInf.setCoefficient(rampUpOnTransitionVariable, -reducedLeadTime * generatorConstraints.getPMin() / generatorConstraints.getLeadTime());

            powerTransitionConstraintSup.setCoefficient(offRampUpTransitionVariable, -timestampDuration * generatorConstraints.getPMin() / generatorConstraints.getLeadTime());
            powerTransitionConstraintSup.setCoefficient(rampUpRampUpTransitionVariable, -timestampDuration * generatorConstraints.getPMin() / generatorConstraints.getLeadTime());
            powerTransitionConstraintSup.setCoefficient(rampUpOnTransitionVariable, -reducedLeadTime * generatorConstraints.getPMin() / generatorConstraints.getLeadTime() - (timestampDuration - reducedLeadTime) * upwardPowerGradient);
        }

        if (generatorConstraints.getLagTime() <= timestampDuration) {
            OpenRaoMPVariable onOffTransitionVariable = linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.OFF);

            powerTransitionConstraintInf.setCoefficient(onOffTransitionVariable, generatorConstraints.getPMin() - (timestampDuration - reducedLagTime) * downwardPowerGradient);
            powerTransitionConstraintSup.setCoefficient(onOffTransitionVariable, generatorConstraints.getPMin());
        } else {
            OpenRaoMPVariable onRampDownTransitionVariable = linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.RAMP_DOWN);
            OpenRaoMPVariable rampDownRampDownTransitionVariable = linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_DOWN, LinearProblem.GeneratorState.RAMP_DOWN);
            OpenRaoMPVariable rampDownOffTransitionVariable = linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_DOWN, LinearProblem.GeneratorState.OFF);

            powerTransitionConstraintInf.setCoefficient(onRampDownTransitionVariable, reducedLagTime * generatorConstraints.getPMin() / generatorConstraints.getLagTime() - (timestampDuration - reducedLagTime) * downwardPowerGradient);
            powerTransitionConstraintInf.setCoefficient(rampDownRampDownTransitionVariable, timestampDuration * generatorConstraints.getPMin() / generatorConstraints.getLagTime());
            powerTransitionConstraintInf.setCoefficient(rampDownOffTransitionVariable, timestampDuration * generatorConstraints.getPMin() / generatorConstraints.getLagTime());

            powerTransitionConstraintSup.setCoefficient(onRampDownTransitionVariable, reducedLagTime * generatorConstraints.getPMin() / generatorConstraints.getLagTime());
            powerTransitionConstraintSup.setCoefficient(rampDownRampDownTransitionVariable, timestampDuration * generatorConstraints.getPMin() / generatorConstraints.getLagTime());
            powerTransitionConstraintSup.setCoefficient(rampDownOffTransitionVariable, timestampDuration * generatorConstraints.getPMin() / generatorConstraints.getLagTime());
        }
    }

    /**
     * Build Power Constraint, for a generator g at timestamp t considering the set of preventive injection range action defined at timestamp t that act on g with distribution key d_i
     * P(g,t) = p0(g,t) + sum_{i \in injectionAction_prev(g,t)} d_i(g) * [delta^{+}(r,s,t) - delta^{-}(r,s,t)]
     */
    private void addRedispatchingImpactConstraint(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, OffsetDateTime timestamp) {
        OpenRaoMPConstraint generatorPowerConstraint = linearProblem.addGeneratorRedispatchingConstraint(generatorConstraints.getGeneratorId(), getInitialPower(generatorConstraints.getGeneratorId(), networkPerTimestamp.getData(timestamp).orElseThrow()), timestamp);
        generatorPowerConstraint.setCoefficient(linearProblem.getGeneratorPowerVariable(generatorConstraints.getGeneratorId(), timestamp), 1.0);

        // Find injection range actions related to generators with power gradients
        injectionRangeActionsPerTimestamp.getData(timestamp).orElseThrow().stream()
            .filter(injectionRangeAction -> injectionRangeAction.getInjectionDistributionKeys().keySet().stream().map(NetworkElement::getId).anyMatch(generatorConstraints.getGeneratorId()::equals))
            .forEach(injectionRangeAction -> {
                double injectionKey = injectionRangeAction.getInjectionDistributionKeys().entrySet().stream().filter(entry -> generatorConstraints.getGeneratorId().equals(entry.getKey().getId())).map(Map.Entry::getValue).findFirst().get();
                OpenRaoMPVariable upwardVariationVariable = linearProblem.getRangeActionVariationVariable(injectionRangeAction, preventiveStates.getData(timestamp).orElseThrow(), LinearProblem.VariationDirectionExtension.UPWARD);
                OpenRaoMPVariable downwardVariationVariable = linearProblem.getRangeActionVariationVariable(injectionRangeAction, preventiveStates.getData(timestamp).orElseThrow(), LinearProblem.VariationDirectionExtension.DOWNWARD);
                generatorPowerConstraint.setCoefficient(upwardVariationVariable, -injectionKey);
                generatorPowerConstraint.setCoefficient(downwardVariationVariable, injectionKey);
            });
    }

    // utility methods

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
        } else if (initialGeneratorPower >= generatorConstraints.getPMin() && initialGeneratorPower <= generatorConstraints.getPMax()) {
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
