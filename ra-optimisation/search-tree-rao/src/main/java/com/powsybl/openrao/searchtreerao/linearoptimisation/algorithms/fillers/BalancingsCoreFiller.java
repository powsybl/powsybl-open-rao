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
import java.util.*;

public class BalancingsCoreFiller implements ProblemFiller {
    private final TemporalData<State> preventiveStates;
    private final TemporalData<Set<InjectionRangeAction>> injectionRangeActionsPerTimestamp;
    private final Set<GeneratorConstraints> generatorConstraints;

    private static final double DEFAULT_TIMESTAMP_DURATION = Double.MAX_VALUE;

    public BalancingsCoreFiller(TemporalData<State> preventiveStates, TemporalData<Set<InjectionRangeAction>> injectionRangeActionsPerTimestamp, Set<GeneratorConstraints> generatorConstraints) {
        this.preventiveStates = preventiveStates;
        this.injectionRangeActionsPerTimestamp = injectionRangeActionsPerTimestamp;
        this.generatorConstraints = generatorConstraints;
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        List<OffsetDateTime> timestamps = preventiveStates.getTimestamps();
        int numberOfTimestamps = timestamps.size();
        for (GeneratorConstraints individualGeneratorConstraints : generatorConstraints) {
            Optional<TemporalData<InjectionRangeAction>> associatedInjections = getInjectionRangeActionOfGenerator(individualGeneratorConstraints.getGeneratorId());
            if (associatedInjections.isPresent()) {
                for (int timestampIndex = 0; timestampIndex < numberOfTimestamps; timestampIndex++) {
                    addVariables(linearProblem, individualGeneratorConstraints, timestampIndex, timestamps);
                }
                for (int timestampIndex = 0; timestampIndex < numberOfTimestamps; timestampIndex++) {
                    addBasicConstraints(linearProblem, individualGeneratorConstraints, timestampIndex, timestamps, associatedInjections.get().getData(timestamps.get(timestampIndex)).orElseThrow());
                }
            }
        }
    }

    private void addVariables(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, int timestampIndex, List<OffsetDateTime> timestamps) {
        OffsetDateTime timestamp = timestamps.get(timestampIndex);
        OffsetDateTime nextTimestamp = timestamp == timestamps.getLast() ? null : timestamps.get(timestampIndex + 1);

        // create variables
        addStateVariables(linearProblem, generatorConstraints, timestamp);
        if (nextTimestamp != null) {
            addStateTransitionVariables(linearProblem, generatorConstraints, timestamp);
        }
    }

    private void addBasicConstraints(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, int timestampIndex, List<OffsetDateTime> timestamps, InjectionRangeAction injectionRangeAction) {
        OffsetDateTime timestamp = timestamps.get(timestampIndex);
        OffsetDateTime nextTimestamp = timestamp == timestamps.getLast() ? null : timestamps.get(timestampIndex + 1);
        double timestampDuration = computeTimeGap(timestamp, nextTimestamp);

        // create and fill basic constraints on states
        if (nextTimestamp != null) {
            addStateTransitionConstraints(linearProblem, generatorConstraints, timestamp, nextTimestamp, timestampDuration);
        }
    }

    private static void addStateTransitionConstraints(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, OffsetDateTime timestamp, OffsetDateTime nextTimestamp, double timestampDuration) {
        // link transition to current state
        addStateFromTransitionConstraints(linearProblem, generatorConstraints, timestamp);
        // link transition to next state
        addStateToTransitionConstraints(linearProblem, generatorConstraints, timestamp, nextTimestamp);
    }

    private static void addStateVariables(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, OffsetDateTime timestamp) {
        // Define states UP, DOWN & FLAT, and that ON = UP + DOWN + FLAT
        String genId = generatorConstraints.getGeneratorId();
        OpenRaoMPVariable up = linearProblem.addGeneratorStateVariable(genId, timestamp, LinearProblem.GeneratorState.UP);
        OpenRaoMPVariable down = linearProblem.addGeneratorStateVariable(genId, timestamp, LinearProblem.GeneratorState.DOWN);
        OpenRaoMPVariable flat = linearProblem.addGeneratorStateVariable(genId, timestamp, LinearProblem.GeneratorState.FLAT);

        OpenRaoMPConstraint c = linearProblem.getSolver().makeConstraint(0, 0, String.format("detail_ON_state_%s_%s", genId, timestamp));
        c.setCoefficient(up, 1);
        c.setCoefficient(down, 1);
        c.setCoefficient(flat, 1);
        c.setCoefficient(linearProblem.getGeneratorStateVariable(genId, timestamp, LinearProblem.GeneratorState.ON), -1);
    }

    private static void addStateTransitionVariables(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, OffsetDateTime timestamp) {
        // Define transitions for states UP, DOWN & FLAT
        Optional<Double> leadTime = generatorConstraints.getLeadTime();
        Optional<Double> lagTime = generatorConstraints.getLagTime();

        for (LinearProblem.GeneratorState stateFrom : List.of(LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.UP, LinearProblem.GeneratorState.DOWN, LinearProblem.GeneratorState.FLAT)) {
            for (LinearProblem.GeneratorState stateTo : List.of(LinearProblem.GeneratorState.OFF, LinearProblem.GeneratorState.UP, LinearProblem.GeneratorState.DOWN, LinearProblem.GeneratorState.FLAT)) {
                if (linearProblem.getGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, stateFrom, stateTo) != null) {
                    continue;
                }
                linearProblem.addGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, stateFrom, stateTo);
            }
        }

        if (leadTime.isPresent()) {

            for (LinearProblem.GeneratorState state : List.of(LinearProblem.GeneratorState.UP, LinearProblem.GeneratorState.DOWN, LinearProblem.GeneratorState.FLAT)) {
                linearProblem.addGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, LinearProblem.GeneratorState.RAMP_UP, state);
            }
        }
        if (lagTime.isPresent()) {

            for (LinearProblem.GeneratorState state : List.of(LinearProblem.GeneratorState.UP, LinearProblem.GeneratorState.DOWN, LinearProblem.GeneratorState.FLAT)) {
                linearProblem.addGeneratorStateTransitionVariable(generatorConstraints.getGeneratorId(), timestamp, state, LinearProblem.GeneratorState.RAMP_DOWN);
            }
        }

    }

    private static void addStateFromTransitionConstraints(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, OffsetDateTime timestamp) {
        for (LinearProblem.GeneratorState stateFrom : List.of(LinearProblem.GeneratorState.UP, LinearProblem.GeneratorState.DOWN, LinearProblem.GeneratorState.FLAT)) {
            OpenRaoMPConstraint fromOnConstraint = linearProblem.addGeneratorStateFromTransitionConstraint(generatorConstraints.getGeneratorId(), timestamp, stateFrom);
            fromOnConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorConstraints.getGeneratorId(), timestamp, stateFrom), 1);
            for (LinearProblem.GeneratorState stateTo : LinearProblem.GeneratorState.values()) {
                OpenRaoMPVariable variable = linearProblem.getGeneratorStateVariable(generatorConstraints.getGeneratorId(), timestamp, stateTo);
                if (variable == null) {
                    continue;
                }
                fromOnConstraint.setCoefficient(variable, -1);
            }
        }
    }

    private static void addStateToTransitionConstraints(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, OffsetDateTime timestamp, OffsetDateTime nextTimestamp) {

        for (LinearProblem.GeneratorState stateTo : List.of(LinearProblem.GeneratorState.UP, LinearProblem.GeneratorState.DOWN, LinearProblem.GeneratorState.FLAT)) {
            OpenRaoMPConstraint toOnConstraint = linearProblem.addGeneratorStateToTransitionConstraint(generatorConstraints.getGeneratorId(), timestamp, stateTo);
            toOnConstraint.setCoefficient(linearProblem.getGeneratorStateVariable(generatorConstraints.getGeneratorId(), nextTimestamp, stateTo), 1);
            for (LinearProblem.GeneratorState stateFrom : LinearProblem.GeneratorState.values()) {
                OpenRaoMPVariable variable = linearProblem.getGeneratorStateVariable(generatorConstraints.getGeneratorId(), timestamp, stateFrom);
                if (variable == null) {
                    continue;
                }
                toOnConstraint.setCoefficient(variable, -1);
            }
        }
    }

    @Override
    public void updateBetweenMipIteration(LinearProblem linearProblem, RangeActionActivationResult rangeActionActivationResult) {

    }

    // utility methods

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


}
