/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network;

import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.util.SwitchPredicates;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.rangeaction.VariationDirection;
import com.powsybl.openrao.data.crac.io.commons.OpenRaoImportException;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import com.powsybl.openrao.data.crac.io.network.parameters.InjectionRangeActionCosts;
import com.powsybl.openrao.data.crac.io.network.parameters.RedispatchingRangeActions;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class RedispatchingCreator {
    private final Crac crac;
    private final Network network;
    private final RedispatchingRangeActions parameters;

    RedispatchingCreator(Crac crac, Network network, RedispatchingRangeActions parameters) {
        this.crac = crac;
        this.network = network;
        this.parameters = parameters;
    }

    void addRedispatchRangeActions() {
        Set<Instant> instants = crac.getSortedInstants().stream().filter(instant -> !instant.isOutage()).collect(Collectors.toSet());
        network.getGeneratorStream()
            .filter(generator -> Utils.injectionIsInCountries(generator, parameters.getCountries().orElse(null)))
            .forEach(generator ->
                instants.stream().filter(instant -> parameters.shouldCreateRedispatchingAction(generator, instant))
                    .forEach(instant -> addGeneratorActionForInstant(generator, instant)));
        network.getLoadStream()
            .filter(load -> Utils.injectionIsInCountries(load, parameters.getCountries().orElse(null)))
            .forEach(load ->
                instants.stream().filter(instant -> parameters.shouldCreateRedispatchingAction(load, instant))
                    .forEach(instant -> addLoadActionForInstant(load, instant)));
        // TODO add other injections (batteries...)
    }

    private void addGeneratorActionForInstant(Generator generator, Instant instant) {
        // TODO merge preventive & curative RA if ranges are the same?
        // advantage : simpler crac
        // disadvantage : more complex code + we might not see bugs in "detailed" version
        double initialP = Math.round(generator.getTargetP());
        // TODO round it in network too ?
        double minP = Math.min(generator.getMinP(), generator.getTargetP());
        if (parameters.getRaRange(generator, instant).getMin().isPresent()) {
            minP = Math.min(minP, parameters.getRaRange(generator, instant).getMin().get());
        }
        double maxP = Math.max(generator.getMaxP(), generator.getTargetP());
        if (parameters.getRaRange(generator, instant).getMax().isPresent()) {
            maxP = Math.max(maxP, parameters.getRaRange(generator, instant).getMax().get());
        }
        InjectionRangeActionCosts costs = parameters.getRaCosts(generator, instant);
        crac.newInjectionRangeAction()
            .withId("RD_GEN_" + generator.getId() + "_" + instant.getId())
            .withNetworkElementAndKey(1.0, generator.getId())
            .newRange()
            .withMin(minP)
            .withMax(maxP).add()
            .newOnInstantUsageRule().withInstant(instant.getId()).add()
            .withInitialSetpoint(initialP)
            .withVariationCost(costs.downVariationCost(), VariationDirection.DOWN)
            .withVariationCost(costs.upVariationCost(), VariationDirection.UP)
            .withActivationCost(costs.activationCost())
            .add();

        // connect the generator
        generator.connect(SwitchPredicates.IS_OPEN);
    }

    private void addLoadActionForInstant(Load load, Instant instant) {
        double initialP = Math.round(load.getP0());
        // TODO round it in network too ?
        if (parameters.getRaRange(load, instant).getMin().isEmpty() || parameters.getRaRange(load, instant).getMax().isEmpty()) {
            throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA, String.format("Could not create range action for load %s at instant %s, because you did not define its min or max value in the parameters.", load.getId(), instant.getId()));
        }
        double minP = Math.min(initialP, parameters.getRaRange(load, instant).getMin().get());
        double maxP = Math.max(initialP, parameters.getRaRange(load, instant).getMax().get());
        InjectionRangeActionCosts costs = parameters.getRaCosts(load, instant);
        crac.newInjectionRangeAction()
            .withId("RD_LOAD_" + load.getId() + "_" + instant.getId())
            .withNetworkElementAndKey(1.0, load.getId())
            .newRange()
            .withMin(minP)
            .withMax(maxP).add()
            .newOnInstantUsageRule().withInstant(instant.getId()).add()
            .withInitialSetpoint(initialP)
            .withVariationCost(costs.downVariationCost(), VariationDirection.DOWN)
            .withVariationCost(costs.upVariationCost(), VariationDirection.UP)
            .withActivationCost(costs.activationCost())
            .add();
    }
}
