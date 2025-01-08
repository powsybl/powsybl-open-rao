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
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
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

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class PowerGradientConstraintFiller implements ProblemFiller {
    private final InterTemporalRaoInput input;

    public PowerGradientConstraintFiller(InterTemporalRaoInput input) {
        this.input = input;
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        input.getPowerGradientConstraints().forEach(
            powerGradientConstraint -> input.getRaoInputs().getTimestamps().forEach(
                timestamp -> linearProblem.addGeneratorPowerVariable(powerGradientConstraint.getNetworkElementId(), timestamp)
            )
        );

        List<OffsetDateTime> timestampsToRun = input.getTimestampsToRun().stream().sorted().toList();
        int numberOfTimestamps = input.getTimestampsToRun().size();

        for (int timestampIndex = 1; timestampIndex < numberOfTimestamps; timestampIndex++) {
            OffsetDateTime currentTimestamp = timestampsToRun.get(timestampIndex);
            OffsetDateTime previousTimestamp = timestampsToRun.get(timestampIndex - 1);
            RaoInput raoInput = input.getRaoInputs().getData(currentTimestamp).orElseThrow();

            input.getPowerGradientConstraints().forEach(powerGradientConstraint -> {
                OpenRaoMPVariable currentGeneratorPowerVariable = linearProblem.getGeneratorPowerVariable(powerGradientConstraint.getNetworkElementId(), currentTimestamp);
                OpenRaoMPVariable previousGeneratorPowerVariable = linearProblem.getGeneratorPowerVariable(powerGradientConstraint.getNetworkElementId(), previousTimestamp);

                OpenRaoMPConstraint generatorPowerConstraint = linearProblem.addGeneratorPowerConstraint(powerGradientConstraint.getNetworkElementId(), getP0(powerGradientConstraint.getNetworkElementId(), raoInput.getNetwork()), currentTimestamp);
                generatorPowerConstraint.setCoefficient(currentGeneratorPowerVariable, 1.0);
                generatorPowerConstraint.setCoefficient(previousGeneratorPowerVariable, -1.0);

                // TODO: optimize this
                raoInput.getCrac().getRangeActions(raoInput.getCrac().getPreventiveState(), UsageMethod.AVAILABLE)
                    .stream()
                    .filter(InjectionRangeAction.class::isInstance)
                    .map(InjectionRangeAction.class::cast)
                    .filter(injectionRangeAction -> injectionRangeAction.getNetworkElements().stream().anyMatch(networkElement -> powerGradientConstraint.getNetworkElementId().equals(networkElement.getId())))
                    .forEach(injectionRangeAction -> {
                        OpenRaoMPVariable upwardSetPointVariationVariable = linearProblem.getRangeActionVariationVariable(injectionRangeAction, raoInput.getCrac().getPreventiveState(), LinearProblem.VariationDirectionExtension.UPWARD); // TODO: add timestamp
                        OpenRaoMPVariable downwardSetPointVariationVariable = linearProblem.getRangeActionVariationVariable(injectionRangeAction, raoInput.getCrac().getPreventiveState(), LinearProblem.VariationDirectionExtension.DOWNWARD); // TODO: add timestamp
                        double distributionKey = 0;
                        generatorPowerConstraint.setCoefficient(upwardSetPointVariationVariable, -distributionKey);
                        generatorPowerConstraint.setCoefficient(downwardSetPointVariationVariable, distributionKey);
                    });

                OpenRaoMPConstraint interTemporalPowerGradientConstraint = linearProblem.addGeneratorPowerGradientConstraint(
                    powerGradientConstraint.getNetworkElementId(),
                    powerGradientConstraint.getMinPowerGradient().orElse(-linearProblem.infinity()),
                    powerGradientConstraint.getMaxPowerGradient().orElse(linearProblem.infinity()),
                    currentTimestamp,
                    previousTimestamp);
                interTemporalPowerGradientConstraint.setCoefficient(currentGeneratorPowerVariable, 1.0);
                interTemporalPowerGradientConstraint.setCoefficient(previousGeneratorPowerVariable, -1.0);
            });
        }
    }

    private static double getP0(String generatorId, Network network) {
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
