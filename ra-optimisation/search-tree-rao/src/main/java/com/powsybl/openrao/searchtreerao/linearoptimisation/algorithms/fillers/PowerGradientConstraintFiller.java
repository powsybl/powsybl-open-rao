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
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import com.powsybl.openrao.data.intertemporalconstraint.PowerGradient;
import com.powsybl.openrao.raoapi.InterTemporalRaoInput;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPVariable;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com}
 */
public class PowerGradientConstraintFiller implements ProblemFiller {
    private final InterTemporalRaoInput input;

    public PowerGradientConstraintFiller(InterTemporalRaoInput input) {
        this.input = input;
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        List<OffsetDateTime> timestamps = input.getRaoInputs().getTimestamps();
        for (int timestampIndex = 0; timestampIndex < timestamps.size(); timestampIndex++) {
            OffsetDateTime timestamp = timestamps.get(timestampIndex);
            RaoInput raoInput = input.getRaoInputs().getData(timestamp).orElseThrow();
            Set<InjectionRangeAction> preventiveInjectionRangeActions = raoInput.getCrac().getRangeActions(raoInput.getCrac().getPreventiveState(), UsageMethod.AVAILABLE)
                .stream().filter(InjectionRangeAction.class::isInstance).map(InjectionRangeAction.class::cast).collect(Collectors.toSet());
            int finalTimestampIndex = timestampIndex;
            input.getPowerGradientConstraints().forEach(constraint -> {
                String generatorId = constraint.getNetworkElementId();
                OpenRaoMPVariable generatorPowerVariable = linearProblem.addGeneratorPowerVariable(generatorId, timestamp);
                addPowerConstraint(linearProblem, preventiveInjectionRangeActions, raoInput, generatorId, generatorPowerVariable, timestamp);
                if (finalTimestampIndex > 0) {
                    addPowerGradientConstraint(linearProblem, constraint, timestamp, timestamps.get(finalTimestampIndex - 1), generatorPowerVariable);
                }
            });
        }
    }

    private static void addPowerGradientConstraint(LinearProblem linearProblem, PowerGradient constraint, OffsetDateTime currentTimestamp, OffsetDateTime previousTimestamp, OpenRaoMPVariable generatorPowerVariable) {
        OpenRaoMPVariable previousGeneratorPowerVariable = linearProblem.getGeneratorPowerVariable(constraint.getNetworkElementId(), previousTimestamp);
        OpenRaoMPConstraint generatorPowerGradientConstraint = linearProblem.addGeneratorPowerGradientConstraint(constraint, currentTimestamp, previousTimestamp);
        generatorPowerGradientConstraint.setCoefficient(generatorPowerVariable, 1.0);
        generatorPowerGradientConstraint.setCoefficient(previousGeneratorPowerVariable, -1.0);
    }

    private static void addPowerConstraint(LinearProblem linearProblem, Set<InjectionRangeAction> preventiveInjectionRangeActions, RaoInput raoInput, String generatorId, OpenRaoMPVariable generatorPowerVariable, OffsetDateTime timestamp) {
        OpenRaoMPConstraint generatorPowerConstraint = linearProblem.addGeneratorPowerConstraint(generatorId, getInitialPower(generatorId, raoInput.getNetwork()), timestamp);
        generatorPowerConstraint.setCoefficient(generatorPowerVariable, 1.0);
        preventiveInjectionRangeActions.stream()
            .filter(injectionRangeAction -> injectionRangeAction.getInjectionDistributionKeys().keySet().stream().map(NetworkElement::getId).anyMatch(generatorId::equals))
            .forEach(injectionRangeAction -> {
                double injectionKey = injectionRangeAction.getInjectionDistributionKeys().entrySet().stream().filter(entry -> generatorId.equals(entry.getKey().getId())).map(Map.Entry::getValue).findFirst().get();
                // TODO: Handle timestamp !
                OpenRaoMPVariable upwardVariationVariable = linearProblem.getRangeActionVariationVariable(injectionRangeAction, raoInput.getCrac().getPreventiveState(), LinearProblem.VariationDirectionExtension.UPWARD, Optional.ofNullable(timestamp));
                OpenRaoMPVariable downwardVariationVariable = linearProblem.getRangeActionVariationVariable(injectionRangeAction, raoInput.getCrac().getPreventiveState(), LinearProblem.VariationDirectionExtension.DOWNWARD, Optional.ofNullable(timestamp));
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
