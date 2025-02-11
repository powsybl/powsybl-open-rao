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
import com.powsybl.openrao.data.intertemporalconstraint.PowerGradient;
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
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com}
 */
public class PowerGradientConstraintFiller implements ProblemFiller {
    private final TemporalData<State> preventiveStates;
    private final TemporalData<Network> networkPerTimestamp;
    private final TemporalData<Set<InjectionRangeAction>> injectionRangeActionsPerTimestamp;  //raoInput.getCrac().getRangeActions(raoInput.getCrac().getPreventiveState(), UsageMethod.AVAILABLE)
    private final Set<PowerGradient> powerGradients;

    public PowerGradientConstraintFiller(TemporalData<State> preventiveStates, TemporalData<Network> networkPerTimestamp, TemporalData<Set<InjectionRangeAction>> injectionRangeActionsPerTimestamp, Set<PowerGradient> powerGradients) {
        this.preventiveStates = preventiveStates;
        this.networkPerTimestamp = networkPerTimestamp;
        this.injectionRangeActionsPerTimestamp = injectionRangeActionsPerTimestamp;
        this.powerGradients = powerGradients;
    }

    //  TODO : only create generator variables when necessary (map injection range actions/generators)
    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        List<OffsetDateTime> timestamps = preventiveStates.getTimestamps();
        IntStream.range(0, timestamps.size()).forEach(timestampIndex -> {
            OffsetDateTime timestamp = timestamps.get(timestampIndex);
            powerGradients.forEach(powerGradient -> {
                String generatorId = powerGradient.getNetworkElementId();
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
    private static void addPowerGradientConstraint(LinearProblem linearProblem, PowerGradient constraint, OffsetDateTime currentTimestamp, OffsetDateTime previousTimestamp, OpenRaoMPVariable generatorPowerVariable) {
        double timeGap = previousTimestamp.until(currentTimestamp, ChronoUnit.HOURS);
        double lb = constraint.getMinValue().map(minValue -> minValue * timeGap).orElse(-linearProblem.infinity());
        double ub = constraint.getMaxValue().map(maxValue -> maxValue * timeGap).orElse(linearProblem.infinity());
        String generatorId = constraint.getNetworkElementId();
        OpenRaoMPConstraint generatorPowerGradientConstraint = linearProblem.addGeneratorPowerGradientConstraint(generatorId, currentTimestamp, previousTimestamp, lb, ub);
        OpenRaoMPVariable previousGeneratorPowerVariable = linearProblem.getGeneratorPowerVariable(constraint.getNetworkElementId(), previousTimestamp);
        generatorPowerGradientConstraint.setCoefficient(generatorPowerVariable, 1.0);
        generatorPowerGradientConstraint.setCoefficient(previousGeneratorPowerVariable, -1.0);
    }

    /** Build Power Constraint, for a generator g at timestamp t considering the set of preventive injection range action defined at timestamp t that act on g with distribution key d_i
     * P(g,t) = p0(g,t) + sum_{i \in injectionAction_prev(g,t)} d_i(g) * [delta^{+}(r,s,t) - delta^{-}(r,s,t)]
     * */
    private void addPowerConstraint(LinearProblem linearProblem, String generatorId, OpenRaoMPVariable generatorPowerVariable, OffsetDateTime timestamp) {
        OpenRaoMPConstraint generatorPowerConstraint = linearProblem.addGeneratorPowerConstraint(generatorId, getInitialPower(generatorId, networkPerTimestamp.getData(timestamp).orElseThrow()), timestamp);
        generatorPowerConstraint.setCoefficient(generatorPowerVariable, 1.0);

        // Find injection range actions related to generators with power gradients
        injectionRangeActionsPerTimestamp.getData(timestamp).orElseThrow().stream()
            .filter(injectionRangeAction -> injectionRangeAction.getInjectionDistributionKeys().keySet().stream().map(NetworkElement::getId).anyMatch(generatorId::equals))
            .forEach(injectionRangeAction -> {
                double injectionKey = injectionRangeAction.getInjectionDistributionKeys().entrySet().stream().filter(entry -> generatorId.equals(entry.getKey().getId())).map(Map.Entry::getValue).findFirst().get();
                OpenRaoMPVariable upwardVariationVariable = linearProblem.getRangeActionVariationVariable(injectionRangeAction, preventiveStates.getData(timestamp).orElseThrow(), LinearProblem.VariationDirectionExtension.UPWARD, Optional.ofNullable(timestamp));
                OpenRaoMPVariable downwardVariationVariable = linearProblem.getRangeActionVariationVariable(injectionRangeAction, preventiveStates.getData(timestamp).orElseThrow(), LinearProblem.VariationDirectionExtension.DOWNWARD, Optional.ofNullable(timestamp));
                generatorPowerConstraint.setCoefficient(upwardVariationVariable, -injectionKey);
                generatorPowerConstraint.setCoefficient(downwardVariationVariable, injectionKey);
            });
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
