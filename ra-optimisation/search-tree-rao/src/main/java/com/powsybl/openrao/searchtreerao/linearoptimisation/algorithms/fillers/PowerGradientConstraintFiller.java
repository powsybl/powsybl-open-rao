/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.openrao.commons.TemporalData;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static java.lang.Math.min;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class PowerGradientConstraintFiller implements ProblemFiller {
    private final TemporalData<State> preventiveStates;
    private final TemporalData<Set<InjectionRangeAction>> injectionRangeActionsPerTimestamp;
    private final Set<GeneratorConstraints> generatorConstraints;

    public PowerGradientConstraintFiller(TemporalData<State> preventiveStates, TemporalData<Set<InjectionRangeAction>> injectionRangeActionsPerTimestamp, Set<GeneratorConstraints> generatorConstraints) {
        this.preventiveStates = preventiveStates;
        this.injectionRangeActionsPerTimestamp = injectionRangeActionsPerTimestamp;
        this.generatorConstraints = generatorConstraints;
    }

    //  TODO : only create generator variables when necessary (map injection range actions/generators)
    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        List<OffsetDateTime> timestamps = preventiveStates.getTimestamps();

        IntStream.range(0, timestamps.size()).forEach(timestampIndex -> {
            OffsetDateTime timestamp = timestamps.get(timestampIndex);
            generatorConstraints.forEach(generatorConstraint -> {
                String generatorId = generatorConstraint.getGeneratorId();
                OpenRaoMPVariable generatorPowerVariable = linearProblem.addGeneratorPowerVariable(generatorId, timestamp);
                addPowerConstraint(linearProblem, generatorId, generatorPowerVariable, timestamp);
                if (timestampIndex > 0) {
                    addPowerGradientConstraint(linearProblem, generatorConstraint, timestamp, timestamps.get(timestampIndex - 1), generatorPowerVariable);
                }
            });
        });

        generatorConstraints.stream()
            .filter(generatorConstraint -> generatorConstraint.getMaxChanges().isPresent())
            .forEach(generatorConstraint -> {
                OpenRaoMPConstraint maxChangesConstraint = linearProblem.addGeneratorMaxChangesConstraint(generatorConstraint.getGeneratorId(), generatorConstraint.getMaxChanges().get());
                IntStream.range(0, timestamps.size()).forEach(timestampIndex -> {
                    addChangeOnTimestamp(linearProblem, generatorConstraint, timestampIndex, timestamps, maxChangesConstraint);
                });
            });
    }

    /**
     * diff_to_prec(g,t) >= (p(g,t) - p(g,t-1)) / 1000 (and -)
     * diff_to_init(g,t) >= (p(g,t) - P0(g,t)) / 1000 (and -)
     * changed(g,t) >= diff_to_init(g,t) + diff_to_prec - 1 (diff_to_init if first timestamp)
     * sum(t) changed(g,t) <= max_changes(g)
     */
    private void addChangeOnTimestamp(LinearProblem linearProblem, GeneratorConstraints generatorConstraint, int timestampIndex, List<OffsetDateTime> timestamps, OpenRaoMPConstraint maxChangesConstraint) {
        OffsetDateTime timestamp = timestamps.get(timestampIndex);
        double p0 = getInitialP(linearProblem, generatorConstraint.getGeneratorId(), timestamp);
        OpenRaoMPVariable generatorPower = linearProblem.getGeneratorPowerVariable(generatorConstraint.getGeneratorId(), timestamp);
        OpenRaoMPVariable generatorDiffToInitial = linearProblem.addGeneratorDiffToInitialVariable(generatorConstraint.getGeneratorId(), timestamp);
        OpenRaoMPConstraint generatorDiffToInitialConstraintPos = linearProblem.addGeneratorDiffToInitialConstraint(generatorConstraint.getGeneratorId(), timestamp, LinearProblem.AbsExtension.POSITIVE);
        //diff > p0 - p <=> diff + p > p0
        generatorDiffToInitialConstraintPos.setLb(p0);
        generatorDiffToInitialConstraintPos.setCoefficient(generatorDiffToInitial, 1000);
        generatorDiffToInitialConstraintPos.setCoefficient(generatorPower, 1);
        OpenRaoMPConstraint generatorDiffToInitialConstraintNeg = linearProblem.addGeneratorDiffToInitialConstraint(generatorConstraint.getGeneratorId(), timestamp, LinearProblem.AbsExtension.NEGATIVE);
        //diff > p - p0 <=> diff - p > -p0
        generatorDiffToInitialConstraintNeg.setLb(-p0);
        generatorDiffToInitialConstraintNeg.setCoefficient(generatorDiffToInitial, 1000);
        generatorDiffToInitialConstraintNeg.setCoefficient(generatorPower, -1);

        OpenRaoMPVariable generatorChangedVariable = linearProblem.addGeneratorChangedVariable(generatorConstraint.getGeneratorId(), timestamp);
        OpenRaoMPConstraint generatorChangedConstraint = linearProblem.addGeneratorChangedConstraint(generatorConstraint.getGeneratorId(), timestamp);
        //changed > diff + diff - 1
        generatorChangedConstraint.setLb(0.);
        generatorChangedConstraint.setCoefficient(generatorChangedVariable, 1.);
        generatorChangedConstraint.setCoefficient(generatorDiffToInitial, -1.);

        if (timestampIndex > 0) {
            OffsetDateTime previousTimestamp = timestamps.get(timestampIndex - 1);
            OpenRaoMPVariable generatorPowerPrevious = linearProblem.getGeneratorPowerVariable(generatorConstraint.getGeneratorId(), previousTimestamp);
            OpenRaoMPVariable generatorDiffToPreviousTs = linearProblem.getOrAddGeneratorDiffToPreviousTsVariable(generatorConstraint.getGeneratorId(), timestamp);
            OpenRaoMPConstraint generatorDiffToPreviousConstraintPos = linearProblem.addGeneratorDiffToPreviousConstraint(generatorConstraint.getGeneratorId(), timestamp, LinearProblem.AbsExtension.POSITIVE);
            generatorDiffToPreviousConstraintPos.setLb(0.);
            //diff > p' - p <=> diff + p - p'> 0
            generatorDiffToPreviousConstraintPos.setCoefficient(generatorDiffToPreviousTs, 1000);
            generatorDiffToPreviousConstraintPos.setCoefficient(generatorPower, 1);
            generatorDiffToPreviousConstraintPos.setCoefficient(generatorPowerPrevious, -1);
            OpenRaoMPConstraint generatorDiffToPreviousConstraintNeg = linearProblem.addGeneratorDiffToPreviousConstraint(generatorConstraint.getGeneratorId(), timestamp, LinearProblem.AbsExtension.NEGATIVE);
            generatorDiffToPreviousConstraintNeg.setLb(0.);
            //diff > p - p' <=> diff - p' + p> 0
            generatorDiffToPreviousConstraintNeg.setCoefficient(generatorDiffToPreviousTs, 1000);
            generatorDiffToPreviousConstraintNeg.setCoefficient(generatorPower, -1);
            generatorDiffToPreviousConstraintNeg.setCoefficient(generatorPowerPrevious, 1);

            generatorChangedConstraint.setLb(-1.);
            generatorChangedConstraint.setCoefficient(generatorDiffToPreviousTs, -1.);
        }

        if (generatorConstraint.getNoChangesForNTimestamps().isPresent() && generatorConstraint.getNoChangesForNTimestamps().get() > 0 && timestampIndex < timestamps.size() - 1) {
            // dont change for an extra n ts
            // diffToPrevious(t+1) + diffToPrevious(t+2) ... + diffToPrevious(t+n) + changed(t)  <= 1
            // ^ doesn't work because if initial P is not constant then we have diffToPrevious = 1 for t+1, t+2 ... t+n so we're "forced" to use the action
            // diffToPrevious(t+1) + diffToPrevious(t+2) ... + diffToPrevious(t+n) <= M * (1 - changed(t))
            // we need multiple constraints : changed(t) + diffToPrevious(t+k) <= 1 for k from 1 to n
            OpenRaoMPConstraint generatorConstantAfterChange = linearProblem.addGeneratorConstantAfterChangeConstraint(generatorConstraint.getGeneratorId(), timestamp);
            generatorConstantAfterChange.setUb(timestamps.size());
            for (int i = timestampIndex + 1; i < min(timestamps.size(), timestampIndex + 1 + generatorConstraint.getNoChangesForNTimestamps().orElseThrow()); i++) {
                OffsetDateTime ts = timestamps.get(i);
                generatorConstantAfterChange.setCoefficient(linearProblem.getOrAddGeneratorDiffToPreviousTsVariable(generatorConstraint.getGeneratorId(), ts), 1.);
            }
            generatorConstantAfterChange.setCoefficient(linearProblem.getGeneratorChangedVariable(generatorConstraint.getGeneratorId(), timestamp), timestamps.size());
        }

        maxChangesConstraint.setCoefficient(generatorChangedVariable, 1.);

        //min change is X MW
        double minChange = 100;
        OpenRaoMPVariable generatorChangeIsPositive = linearProblem.addGeneratorChangeIsPositiveVariable(generatorConstraint.getGeneratorId(), timestamp);
        OpenRaoMPConstraint minGeneratorChangeUp = linearProblem.addMinGeneratorChangeConstraint(generatorConstraint.getGeneratorId(), timestamp, LinearProblem.AbsExtension.POSITIVE);
        minGeneratorChangeUp.setCoefficient(generatorPower, 1);
        minGeneratorChangeUp.setCoefficient(generatorDiffToInitial, -minChange);
        minGeneratorChangeUp.setCoefficient(generatorChangeIsPositive, -1000);
        minGeneratorChangeUp.setLb(p0 - 1000);

        OpenRaoMPConstraint minGeneratorChangeDown = linearProblem.addMinGeneratorChangeConstraint(generatorConstraint.getGeneratorId(), timestamp, LinearProblem.AbsExtension.NEGATIVE);
        minGeneratorChangeDown.setCoefficient(generatorPower, -1);
        minGeneratorChangeDown.setCoefficient(generatorDiffToInitial, -minChange);
        minGeneratorChangeDown.setCoefficient(generatorChangeIsPositive, 1000);
        minGeneratorChangeDown.setLb(-p0);
    }

    /** Build a Generator Power Gradient Constraint for a generator g at timestamp t
     * p^{-}(g) * delta(t, t + 1) <= P(g, t + 1) - P(g, t) <= p^{+}(g) * delta_t(t, t + 1)
     * */
    private static void addPowerGradientConstraint(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, OffsetDateTime currentTimestamp, OffsetDateTime previousTimestamp, OpenRaoMPVariable generatorPowerVariable) {
        double timeGap = previousTimestamp.until(currentTimestamp, ChronoUnit.SECONDS) / 3600.;
        double lb = generatorConstraints.getDownwardPowerGradient().map(minValue -> minValue * timeGap).orElse(-linearProblem.infinity());
        double ub = generatorConstraints.getUpwardPowerGradient().map(maxValue -> maxValue * timeGap).orElse(linearProblem.infinity());
        String generatorId = generatorConstraints.getGeneratorId();
        OpenRaoMPConstraint generatorPowerGradientConstraint = linearProblem.addGeneratorPowerGradientConstraint(generatorId, currentTimestamp, previousTimestamp, lb, ub);
        OpenRaoMPVariable previousGeneratorPowerVariable = linearProblem.getGeneratorPowerVariable(generatorConstraints.getGeneratorId(), previousTimestamp);
        generatorPowerGradientConstraint.setCoefficient(generatorPowerVariable, 1.0);
        generatorPowerGradientConstraint.setCoefficient(previousGeneratorPowerVariable, -1.0);
    }

    /** Build Power Constraint, for a generator g at timestamp t considering the set of preventive injection range action defined at timestamp t that act on g with distribution key d_i
     * P(g,t) = p0(g,t) + sum_{i \in injectionAction_prev(g,t)} d_i(g) * [delta^{+}(r,s,t) - delta^{-}(r,s,t)]
     * */
    private void addPowerConstraint(LinearProblem linearProblem, String generatorId, OpenRaoMPVariable generatorPowerVariable, OffsetDateTime timestamp) {
        // Initial power cannot be read from network because power may have been modified in network since the beginning of the RAO. That's why initial power is fetched from rangeActionSetPointVariationConstraint's upper bound
        OpenRaoMPConstraint generatorPowerConstraint = linearProblem.addGeneratorPowerConstraint(generatorId, 0., timestamp);
        generatorPowerConstraint.setCoefficient(generatorPowerVariable, 1.0);
        final double bound = getInitialP(linearProblem, generatorId, timestamp, generatorPowerConstraint);
        generatorPowerConstraint.setBounds(bound, bound);
    }

    private double getInitialP(LinearProblem linearProblem, String generatorId, OffsetDateTime timestamp, OpenRaoMPConstraint generatorPowerConstraint) {
        final double[] bound = {0};

        // Find injection range actions related to generators with power gradients
        injectionRangeActionsPerTimestamp.getData(timestamp).orElseThrow().stream()
            .filter(injectionRangeAction -> injectionRangeAction.getInjectionDistributionKeys().keySet().stream().map(NetworkElement::getId).anyMatch(generatorId::equals))
            .forEach(injectionRangeAction -> {
                double injectionKey = injectionRangeAction.getInjectionDistributionKeys().entrySet().stream()
                    .filter(entry -> generatorId.equals(entry.getKey().getId()))
                    .map(Map.Entry::getValue)
                    .findFirst().get();
                OpenRaoMPVariable upwardVariationVariable = linearProblem.getRangeActionVariationVariable(injectionRangeAction, preventiveStates.getData(timestamp).orElseThrow(), LinearProblem.VariationDirectionExtension.UPWARD);
                OpenRaoMPVariable downwardVariationVariable = linearProblem.getRangeActionVariationVariable(injectionRangeAction, preventiveStates.getData(timestamp).orElseThrow(), LinearProblem.VariationDirectionExtension.DOWNWARD);
                generatorPowerConstraint.setCoefficient(upwardVariationVariable, -injectionKey);
                generatorPowerConstraint.setCoefficient(downwardVariationVariable, injectionKey);

                OpenRaoMPConstraint setPointVariationConstraint = linearProblem.getRangeActionSetPointVariationConstraint(injectionRangeAction, preventiveStates.getData(timestamp).orElseThrow());
                bound[0] = bound[0] + setPointVariationConstraint.ub() * injectionKey;
            });
        return bound[0];
    }

    private double getInitialP(LinearProblem linearProblem, String generatorId, OffsetDateTime timestamp) {
        final double[] bound = {0};

        // Find injection range actions related to generators with power gradients
        injectionRangeActionsPerTimestamp.getData(timestamp).orElseThrow().stream()
            .filter(injectionRangeAction -> injectionRangeAction.getInjectionDistributionKeys().keySet().stream().map(NetworkElement::getId).anyMatch(generatorId::equals))
            .forEach(injectionRangeAction -> {
                double injectionKey = injectionRangeAction.getInjectionDistributionKeys().entrySet().stream()
                    .filter(entry -> generatorId.equals(entry.getKey().getId()))
                    .map(Map.Entry::getValue)
                    .findFirst().get();

                OpenRaoMPConstraint setPointVariationConstraint = linearProblem.getRangeActionSetPointVariationConstraint(injectionRangeAction, preventiveStates.getData(timestamp).orElseThrow());
                bound[0] = bound[0] + setPointVariationConstraint.ub() * injectionKey;
            });
        return bound[0];
    }

    @Override
    public void updateBetweenMipIteration(LinearProblem linearProblem, RangeActionActivationResult rangeActionActivationResult) {
        // nothing to do
    }
}
