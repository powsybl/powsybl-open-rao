/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.iidm.network.Generator;
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
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class GeneratorConstraintsFiller implements ProblemFiller {
    private final TemporalData<Network> networks;
    private final TemporalData<State> preventiveStates;
    private final TemporalData<Set<InjectionRangeAction>> injectionRangeActionsPerTimestamp;
    private final Set<GeneratorConstraints> generatorConstraints;
    private final List<OffsetDateTime> timestamps;
    private final double timestampDuration;

    private static final double DEFAULT_POWER_GRADIENT = 100000.0;
    private static final double DEFAULT_P_MAX = 10000.0;
    private static final double OFF_POWER_THRESHOLD = 1.0;

    // TODO: check that all temporal data are correctly filled with the same timestamps
    public GeneratorConstraintsFiller(TemporalData<Network> networks, TemporalData<State> preventiveStates, TemporalData<Set<InjectionRangeAction>> injectionRangeActionsPerTimestamp, Set<GeneratorConstraints> generatorConstraints) {
        this.networks = networks;
        this.preventiveStates = preventiveStates;
        this.injectionRangeActionsPerTimestamp = injectionRangeActionsPerTimestamp;
        this.generatorConstraints = generatorConstraints;
        this.timestampDuration = computeTimestampDuration(networks.getTimestamps());
        this.timestamps = networks.getTimestamps();
    }

    // TODO: reflect upon how to deal with loads constraints-wise (i.e. does it make sense to define lead/lag times or p min/max?)
    // TODO: move this check at a prior moment
    private static double computeTimestampDuration(List<OffsetDateTime> timestamps) {
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
        int numberOfTimestamps = networks.getTimestamps().size();
        for (GeneratorConstraints individualGeneratorConstraints : generatorConstraints) {
            String generatorId = individualGeneratorConstraints.getGeneratorId();
            Optional<Double> leadTime = individualGeneratorConstraints.getLeadTime();
            Optional<Double> lagTime = individualGeneratorConstraints.getLagTime();
            Optional<TemporalData<InjectionRangeAction>> associatedInjections = getInjectionRangeActionOfGenerator(individualGeneratorConstraints.getGeneratorId());
            if (associatedInjections.isPresent()) {
                // Add variables
                for (int timestampIndex = 0; timestampIndex < numberOfTimestamps; timestampIndex++) {
                    OffsetDateTime timestamp = timestamps.get(timestampIndex);
                    addPowerVariable(linearProblem, generatorId, timestamp);
                    addStateVariables(linearProblem, generatorId, timestamp);
                    if (timestampIndex < numberOfTimestamps - 1) {
                        addStateTransitionVariables(linearProblem, generatorId, timestamp);
                    }
                }
                // Add constraints
                for (int timestampIndex = 0; timestampIndex < numberOfTimestamps; timestampIndex++) {
                    OffsetDateTime timestamp = timestamps.get(timestampIndex);
                    // Constraints not involving state transition variables
                    addUniqueGeneratorStateConstraint(linearProblem, generatorId, timestamp);
                    addOnOffPowerConstraints(linearProblem, generatorId, timestamp);

                    // Constraints involving state transition variables, defined on indexes [0, numberOfTimestamps - 2]
                    if (timestampIndex < numberOfTimestamps - 1) {
                        OffsetDateTime nextTimestamp = timestamps.get(timestampIndex + 1);
                        // link transition to current state
                        addStateFromTransitionConstraints(linearProblem, generatorId, timestamp);
                        // link transition to next state
                        addStateToTransitionConstraints(linearProblem, generatorId, timestamp, nextTimestamp);

                        // For t' between ceil(t + 1 - leadTime) and t, T(OFF->ON)(t) <= OFF(t')
                        if (leadTime.isPresent() && leadTime.get() > timestampDuration) {
                            int firstTimestampIndex = Math.max(0, timestampIndex + 1 - (int) Math.ceil(leadTime.get() / timestampDuration));
                            for (int earlierTimestampIndex = timestampIndex; earlierTimestampIndex >= firstTimestampIndex; earlierTimestampIndex--) {
                                addLeadTimeConstraint(linearProblem, individualGeneratorConstraints.getGeneratorId(), timestamps.get(timestampIndex), timestamps.get(earlierTimestampIndex));
                            }
                        }

                        // For t' between t+1 and ceil(t + lagTime) and t, T(ON->OFF)(t) <= OFF(t')
                        if (lagTime.isPresent() && lagTime.get() > timestampDuration) {
                            int lastTimestampIndex = Math.min(numberOfTimestamps - 1, timestampIndex + (int) Math.ceil(lagTime.get() / timestampDuration));
                            for (int laterTimestampIndex = timestampIndex + 1; laterTimestampIndex <= lastTimestampIndex; laterTimestampIndex++) {
                                addLagTimeConstraint(linearProblem, individualGeneratorConstraints.getGeneratorId(), timestamps.get(timestampIndex), timestamps.get(laterTimestampIndex));
                            }
                        }
                        addPowerVariationConstraints(linearProblem, individualGeneratorConstraints, timestamps.get(timestampIndex), timestamps.get(timestampIndex + 1));
                    }
                    addPowerToInjectionConstraint(linearProblem, generatorId, timestamp, associatedInjections.get().getData(timestamps.get(timestampIndex)).orElseThrow(), preventiveStates.getData(timestamp).orElseThrow(), networks.getData(timestamp).orElseThrow());
                }
            }
        }
    }

    // ---- Variables
    private void addPowerVariable(LinearProblem linearProblem, String generatorId, OffsetDateTime timestamp) {
        double pMax = getMaxP(generatorId, networks.getData(timestamp).orElseThrow());
        linearProblem.addGeneratorPowerVariable(generatorId, pMax, timestamp);
    }

    private static void addStateVariables(LinearProblem linearProblem, String generatorId, OffsetDateTime timestamp) {
        linearProblem.addGeneratorStateVariable(generatorId, timestamp, LinearProblem.GeneratorState.ON);
        linearProblem.addGeneratorStateVariable(generatorId, timestamp, LinearProblem.GeneratorState.OFF);
    }

    private static void addStateTransitionVariables(LinearProblem linearProblem, String generatorId, OffsetDateTime timestamp) {
        linearProblem.addGeneratorStateTransitionVariable(generatorId, timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.ON);
        linearProblem.addGeneratorStateTransitionVariable(generatorId, timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.OFF);
        linearProblem.addGeneratorStateTransitionVariable(generatorId, timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.ON);
        linearProblem.addGeneratorStateTransitionVariable(generatorId, timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.OFF);
    }

    // ---- Constraints

    /**
     * C1 - The generator must and can only be in one state.
     * <br/>
     * ON + OFF = 1
     */
    private static void addUniqueGeneratorStateConstraint(LinearProblem linearProblem, String generatorId, OffsetDateTime timestamp) {
        OpenRaoMPConstraint uniqueGeneratorStateConstraint = linearProblem.addUniqueGeneratorStateConstraint(generatorId, timestamp);
        uniqueGeneratorStateConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorId, timestamp, LinearProblem.GeneratorState.ON), 1);
        uniqueGeneratorStateConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorId, timestamp, LinearProblem.GeneratorState.OFF), 1);
    }

    /**
     * C2 - The previous state of the generator must match the transition.
     * <br/>
     * state_j{t} = /Sigma T{state_i -> state_j}
     */
    private static void addStateFromTransitionConstraints(LinearProblem linearProblem, String generatorId, OffsetDateTime timestamp) {
        // ON = ON = Tr(ON->ON) + Tr(ON->OFF)
        OpenRaoMPConstraint fromOnConstraint = linearProblem.addGeneratorStateFromTransitionConstraint(generatorId, timestamp, LinearProblem.GeneratorState.ON);
        fromOnConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorId, timestamp, LinearProblem.GeneratorState.ON), 1);
        fromOnConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorId, timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.ON), -1);
        fromOnConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorId, timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.OFF), -1);

        // OFF = Tr(OFF->OFF) +  Tr(OFF->ON)
        OpenRaoMPConstraint fromOffConstraint = linearProblem.addGeneratorStateFromTransitionConstraint(generatorId, timestamp, LinearProblem.GeneratorState.OFF);
        fromOffConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorId, timestamp, LinearProblem.GeneratorState.OFF), 1);
        fromOffConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorId, timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.OFF), -1);
        fromOffConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorId, timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.ON), -1);
    }

    /**
     * C3 - The current state of the generator must match the transition.
     * <br/>
     * state_j{t+1} = /Sigma T{state_j -> state_i}
     */
    private static void addStateToTransitionConstraints(LinearProblem linearProblem, String generatorId, OffsetDateTime timestamp, OffsetDateTime nextTimestamp) {
        // ON
        // ON = ON = Tr(ON->ON) + Tr(OFF->ON)
        OpenRaoMPConstraint toOnConstraint = linearProblem.addGeneratorStateToTransitionConstraint(generatorId, timestamp, LinearProblem.GeneratorState.ON);
        toOnConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorId, nextTimestamp, LinearProblem.GeneratorState.ON), 1);
        toOnConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorId, timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.ON), -1);
        toOnConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorId, timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.ON), -1);

        // OFF
        // OFF = OFF = Tr(OFF->OFF) + Tr(ON->OFF)
        OpenRaoMPConstraint toOffConstraint = linearProblem.addGeneratorStateToTransitionConstraint(generatorId, timestamp, LinearProblem.GeneratorState.OFF);
        toOffConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorId, nextTimestamp, LinearProblem.GeneratorState.OFF), 1);
        toOffConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorId, timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.OFF), -1);
        toOffConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorId, timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.OFF), -1);
    }

    /**
     * C4 - Lead time
     * <br/>
     * For t' between floor(t+1 - leadTime) and t, T(OFF->ON)(t) <= OFF(t')
     */
    private static void addLeadTimeConstraint(LinearProblem linearProblem, String generatorId, OffsetDateTime startingUpTimestamp, OffsetDateTime previousTimestamp) {
        OpenRaoMPConstraint leadTimeConstraint = linearProblem.addGeneratorStartingUpConstraint(generatorId, startingUpTimestamp, previousTimestamp);
        leadTimeConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorId, startingUpTimestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.ON), 1.0);
        leadTimeConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorId, previousTimestamp, LinearProblem.GeneratorState.OFF), -1.0);
    }

    /**
     * C5 - Lag time
     * <br/>
     * For t' between t+1 and ceil(t + lagTime) and t, T(ON->OFF)(t) <= OFF(t')
     */
    private static void addLagTimeConstraint(LinearProblem linearProblem, String generatorId, OffsetDateTime shuttingDownTimestamp, OffsetDateTime nextTimestamp) {
        OpenRaoMPConstraint lagTimeConstraint = linearProblem.addGeneratorShuttingDownConstraint(generatorId, shuttingDownTimestamp, nextTimestamp);
        lagTimeConstraint.setCoefficient(linearProblem.getGeneratorStateTransitionVariable(generatorId, shuttingDownTimestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.OFF), 1.0);
        lagTimeConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorId, nextTimestamp, LinearProblem.GeneratorState.OFF), -1.0);
    }

    /**
     * C6 - The generator is ON if and only if its power is in the range [P_min, P_max]
     * and OFF if and only if its power is in the range [0, OFF_POWER_THRESHOLD]
     * <br/>
     * P >= P_min ON
     * <br/>
     * P <= P_max ON + OFF_POWER_THRESHOLD OFF
     */
    private void addOnOffPowerConstraints(LinearProblem linearProblem, String generatorId, OffsetDateTime timestamp) {
        double pMin = getMinP(generatorId, networks.getData(timestamp).orElseThrow());
        double pMax = getMaxP(generatorId, networks.getData(timestamp).orElseThrow());
        OpenRaoMPVariable generatorPowerVariable = linearProblem.getGeneratorPowerVariable(generatorId, timestamp);
        OpenRaoMPVariable generatorOnVariable = linearProblem.getGeneratorStateVariable(generatorId, timestamp, LinearProblem.GeneratorState.ON);
        OpenRaoMPVariable generatorOffVariable = linearProblem.getGeneratorStateVariable(generatorId, timestamp, LinearProblem.GeneratorState.OFF);

        OpenRaoMPConstraint onOffPowerConstraintInf = linearProblem.addGeneratorPowerOnOffConstraint(generatorId, timestamp, 0, linearProblem.infinity(), LinearProblem.AbsExtension.POSITIVE);
        onOffPowerConstraintInf.setCoefficient(generatorPowerVariable, 1);
        onOffPowerConstraintInf.setCoefficient(generatorOnVariable, -pMin);

        OpenRaoMPConstraint onOffPowerConstraintSup = linearProblem.addGeneratorPowerOnOffConstraint(generatorId, timestamp, -linearProblem.infinity(), 0, LinearProblem.AbsExtension.NEGATIVE);
        onOffPowerConstraintSup.setCoefficient(generatorPowerVariable, 1);
        onOffPowerConstraintSup.setCoefficient(generatorOnVariable, -pMax);
        onOffPowerConstraintSup.setCoefficient(generatorOffVariable, -OFF_POWER_THRESHOLD);
    }

    /**
     * C7 - Constraints linking power variations to state transitions
     * <br/>
     */
    private void addPowerVariationConstraints(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, OffsetDateTime timestamp, OffsetDateTime nextTimestamp) {
        double upwardPowerGradient = generatorConstraints.getUpwardPowerGradient().orElse(DEFAULT_POWER_GRADIENT);
        double downwardPowerGradient = generatorConstraints.getDownwardPowerGradient().orElse(-DEFAULT_POWER_GRADIENT);
        double pMin = getMinP(generatorConstraints.getGeneratorId(), networks.getData(timestamp).orElseThrow());

        OpenRaoMPConstraint powerTransitionConstraintInf = linearProblem.addGeneratorPowerTransitionConstraint(generatorConstraints.getGeneratorId(), 0, linearProblem.infinity(), timestamp, LinearProblem.AbsExtension.POSITIVE);
        powerTransitionConstraintInf.setCoefficient(linearProblem.getGeneratorPowerVariable(generatorConstraints.getGeneratorId(), nextTimestamp), 1.0);
        powerTransitionConstraintInf.setCoefficient(linearProblem.getGeneratorPowerVariable(generatorConstraints.getGeneratorId(), timestamp), -1.0);

        OpenRaoMPConstraint powerTransitionConstraintSup = linearProblem.addGeneratorPowerTransitionConstraint(generatorConstraints.getGeneratorId(), -linearProblem.infinity(), 0, timestamp, LinearProblem.AbsExtension.NEGATIVE);
        powerTransitionConstraintSup.setCoefficient(linearProblem.getGeneratorPowerVariable(generatorConstraints.getGeneratorId(), nextTimestamp), 1.0);
        powerTransitionConstraintSup.setCoefficient(linearProblem.getGeneratorPowerVariable(generatorConstraints.getGeneratorId(), timestamp), -1.0);

        // ON -> ON
        OpenRaoMPVariable onOnTransitionVariable = linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.ON);
        powerTransitionConstraintInf.setCoefficient(onOnTransitionVariable, -downwardPowerGradient * timestampDuration);
        powerTransitionConstraintSup.setCoefficient(onOnTransitionVariable, -upwardPowerGradient * timestampDuration);

        // OFF -> OFF
        OpenRaoMPVariable offOffTransitionVariable = linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.OFF);
        powerTransitionConstraintInf.setCoefficient(offOffTransitionVariable, OFF_POWER_THRESHOLD);
        powerTransitionConstraintSup.setCoefficient(offOffTransitionVariable, -OFF_POWER_THRESHOLD);

        // OFF -> ON
        double nextPMin = getMinP(generatorConstraints.getGeneratorId(), networks.getData(nextTimestamp).orElseThrow());
        OpenRaoMPVariable offOnTransitionVariable = linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.ON);
        powerTransitionConstraintInf.setCoefficient(offOnTransitionVariable, -(nextPMin - OFF_POWER_THRESHOLD));
        if (generatorConstraints.getLeadTime().isPresent()) {
            // if the generator has a lead time, ON state starts at Pmin on a timestamp before power increases
            powerTransitionConstraintSup.setCoefficient(offOnTransitionVariable, -nextPMin);
        } else {
            // otherwise the power is simply constrained by the power gradient
            powerTransitionConstraintSup.setCoefficient(offOnTransitionVariable, -nextPMin - upwardPowerGradient * timestampDuration);
        }

        // ON -> OFF
        OpenRaoMPVariable onOffTransitionVariable = linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.ON, LinearProblem.GeneratorState.OFF);
        if (generatorConstraints.getLagTime().isPresent()) {
            // if the generator has a lag time, ON state finishes at Pmin on a timestamp before power decreases
            powerTransitionConstraintInf.setCoefficient(onOffTransitionVariable, pMin);
        } else {
            // otherwise the power is simply constrained by the power gradient
            powerTransitionConstraintInf.setCoefficient(onOffTransitionVariable, pMin - downwardPowerGradient * timestampDuration);
        }
        powerTransitionConstraintSup.setCoefficient(onOffTransitionVariable, pMin - OFF_POWER_THRESHOLD);
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

    private static void addPowerToInjectionConstraint(LinearProblem linearProblem, String generatorId, OffsetDateTime timestamp, InjectionRangeAction injectionRangeAction, State state, Network network) {
        OpenRaoMPConstraint powerToInjectionConstraint = linearProblem.addGeneratorToInjectionConstraint(generatorId, injectionRangeAction, timestamp);
        powerToInjectionConstraint.setCoefficient(linearProblem.getGeneratorPowerVariable(generatorId, timestamp), 1.0);
        powerToInjectionConstraint.setCoefficient(linearProblem.getRangeActionSetpointVariable(injectionRangeAction, state), -getDistributionKey(generatorId, injectionRangeAction));
    }

    private static Optional<InjectionRangeAction> getInjectionRangeActionOfGenerator(String generatorId, Set<InjectionRangeAction> allInjectionRangeActions) {
        return allInjectionRangeActions.stream().filter(injectionRangeAction -> injectionRangeAction.getNetworkElements().stream().map(NetworkElement::getId).anyMatch(generatorId::equals)).min(Comparator.comparing(Identifiable::getId));
    }

    private static double getDistributionKey(String generatorId, InjectionRangeAction injectionRangeAction) {
        NetworkElement networkElement = injectionRangeAction.getNetworkElements().stream().filter(element -> element.getId().equals(generatorId)).findFirst().orElseThrow();
        return injectionRangeAction.getInjectionDistributionKeys().get(networkElement);
    }

    private static double getMinP(String generatorId, Network network) {
        return getGenerator(generatorId, network).getMinP();
    }

    private static double getMaxP(String generatorId, Network network) {
        return getGenerator(generatorId, network).getMaxP();
    }

    // TODO: import generator data in the GeneratorConstraint directly
    private static Generator getGenerator(String generatorId, Network network) {
        Generator generator = network.getGenerator(generatorId);
        if (generator == null) {
            throw new OpenRaoException("Network element %s is not a generator.".formatted(generatorId));
        }
        return generator;
    }

    @Override
    public void updateBetweenMipIteration(LinearProblem linearProblem, RangeActionActivationResult rangeActionActivationResult) {
        // nothing to do
    }
}
