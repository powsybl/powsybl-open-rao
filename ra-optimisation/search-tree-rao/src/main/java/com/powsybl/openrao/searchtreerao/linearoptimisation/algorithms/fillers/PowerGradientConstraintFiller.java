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
import java.util.stream.IntStream;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com}
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
            generatorConstraints.forEach(powerGradient -> {
                String generatorId = powerGradient.getGeneratorId();
                OpenRaoMPVariable generatorPowerVariable = linearProblem.addGeneratorPowerVariable(generatorId, timestamp);
                addPowerConstraint(linearProblem, generatorId, generatorPowerVariable, timestamp);
                if (timestampIndex > 0) {
                    addPowerGradientConstraint(linearProblem, powerGradient, timestamp, timestamps.get(timestampIndex - 1), generatorPowerVariable);
                }
            });
        });
    }

    /** Build a Generator Power Gradient Constraint for a generator g at timestamp t
     * p^{-}(g) * delta(t, t + 1) <= P(g, t + 1) - P(g, t) <= p^{+}(g) * delta_t(t, t + 1)
     * */
    private static void addPowerGradientConstraint(LinearProblem linearProblem, GeneratorConstraints generatorConstraints, OffsetDateTime currentTimestamp, OffsetDateTime previousTimestamp, OpenRaoMPVariable generatorPowerVariable) {
        double timeGap = previousTimestamp.until(currentTimestamp, ChronoUnit.HOURS);
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
        // Initial power cannot be read from modified network (because of multiple iterations without applying on network?). It is fetched from rangeActionSetPointVariationConstraint's upper bound
        OpenRaoMPConstraint generatorPowerConstraint = linearProblem.addGeneratorPowerConstraint(generatorId, 0., timestamp);
        generatorPowerConstraint.setCoefficient(generatorPowerVariable, 1.0);
        final double[] bound = {0};

        // Find injection range actions related to generators with power gradients
        injectionRangeActionsPerTimestamp.getData(timestamp).orElseThrow().stream()
            .filter(injectionRangeAction -> injectionRangeAction.getInjectionDistributionKeys().keySet().stream().map(NetworkElement::getId).anyMatch(generatorId::equals))
            .forEach(injectionRangeAction -> {
                double injectionKey = injectionRangeAction.getInjectionDistributionKeys().entrySet().stream().filter(entry -> generatorId.equals(entry.getKey().getId())).map(Map.Entry::getValue).findFirst().get();
                OpenRaoMPVariable upwardVariationVariable = linearProblem.getRangeActionVariationVariable(injectionRangeAction, preventiveStates.getData(timestamp).orElseThrow(), LinearProblem.VariationDirectionExtension.UPWARD);
                OpenRaoMPVariable downwardVariationVariable = linearProblem.getRangeActionVariationVariable(injectionRangeAction, preventiveStates.getData(timestamp).orElseThrow(), LinearProblem.VariationDirectionExtension.DOWNWARD);
                generatorPowerConstraint.setCoefficient(upwardVariationVariable, -injectionKey);
                generatorPowerConstraint.setCoefficient(downwardVariationVariable, injectionKey);

                OpenRaoMPConstraint setPointVariationConstraint = linearProblem.getRangeActionSetPointVariationConstraint(injectionRangeAction, preventiveStates.getData(timestamp).orElseThrow());
                bound[0] = bound[0] + setPointVariationConstraint.ub() * injectionKey;
            });
        generatorPowerConstraint.setBounds(bound[0], bound[0]);
    }

    @Override
    public void updateBetweenMipIteration(LinearProblem linearProblem, RangeActionActivationResult rangeActionActivationResult) {
        // nothing to do
    }
}
