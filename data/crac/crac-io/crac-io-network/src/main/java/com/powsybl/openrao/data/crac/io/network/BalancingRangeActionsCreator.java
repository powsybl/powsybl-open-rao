/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeActionAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.VariationDirection;
import com.powsybl.openrao.data.crac.io.commons.OpenRaoImportException;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import com.powsybl.openrao.data.crac.io.network.parameters.BalancingRangeAction;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class BalancingRangeActionsCreator {
    private final Crac crac;
    private final Network network;
    private final BalancingRangeAction parameters;

    BalancingRangeActionsCreator(Crac crac, Network network, BalancingRangeAction parameters) {
        this.crac = crac;
        this.network = network;
        this.parameters = parameters;
    }

    void addBalancingRangeActions() {
        Set<Country> countries = parameters.getCountries().orElse(network.getCountries());
        countries.forEach(country -> {
            crac.getSortedInstants().stream().filter(instant -> !instant.isOutage())
                .forEach(instant -> addBalancingRangeActionForInstant(country, instant));
        });
    }

    private void addBalancingRangeActionForInstant(Country country, Instant instant) {
        // TODO reduce code duplication with counter-trading creator
        if (parameters.getRaRange(country, instant).getMin().isEmpty() || parameters.getRaRange(country, instant).getMax().isEmpty()) {
            throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA,
                String.format("Cannot create a balancing action for %s at instant %s without a defined min/max range.", country, instant));
        }

        Set<Generator> consideredGenerators = network.getGeneratorStream()
            .filter(generator -> Utils.injectionIsInCountries(generator, Set.of(country)))
            .filter(generator -> parameters.shouldIncludeInjection(generator, instant))
            .filter(generator -> Utils.injectionIsNotUsedInAnyInjectionRangeAction(crac, generator, instant))
            .collect(Collectors.toSet());

        double initialTotalP = Math.round(consideredGenerators.stream()
            .mapToDouble(Generator::getTargetP).sum());

        if (initialTotalP < 1.) {
            throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA,
                String.format("Cannot create a balancing action for %s at instant %s because initial production is almost zero (proportional GLSK is assumed). Maybe all generators were filtered out.", country, instant));
        }

        InjectionRangeActionAdder injectionRangeActionAdder = crac.newInjectionRangeAction()
            .withId("BALANCING_" + country.getName())
            .newRange()
            .withMin(initialTotalP + parameters.getRaRange(country, instant).getMin().orElseThrow())
            .withMax(initialTotalP + parameters.getRaRange(country, instant).getMax().orElseThrow())
            .add()
            .withInitialSetpoint(initialTotalP)
            .withVariationCost(parameters.getRaCosts(country, instant).downVariationCost(), VariationDirection.DOWN)
            .withVariationCost(parameters.getRaCosts(country, instant).upVariationCost(), VariationDirection.UP)
            .withActivationCost(parameters.getRaCosts(country, instant).activationCost())
            .newOnInstantUsageRule().withInstant(instant.getId()).add();

        consideredGenerators.forEach(generator -> injectionRangeActionAdder.withNetworkElementAndKey(generator.getTargetP() / initialTotalP, generator.getId()));

        injectionRangeActionAdder.add();
    }
}
