/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network;

import com.powsybl.iidm.network.*;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeActionAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.VariationDirection;
import com.powsybl.openrao.data.crac.io.commons.OpenRaoImportException;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import com.powsybl.openrao.data.crac.io.network.parameters.CountertradingRangeActions;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class CountertradingRangeActionsCreator {
    private final Crac crac;
    private final Network network;
    private final CountertradingRangeActions parameters;
    private final NetworkCracCreationContext creationContext;

    CountertradingRangeActionsCreator(Crac crac, Network network, CountertradingRangeActions parameters, NetworkCracCreationContext creationContext) {
        this.crac = crac;
        this.network = network;
        this.parameters = parameters;
        this.creationContext = creationContext;
    }

    void addCountertradingActions() {
        if (parameters.getCountries().isEmpty()) {
            throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA, "Cannot create counter-trading remedial actions without an explicit list of countries.");
        }
        parameters.getCountries().orElseThrow().forEach(country -> {

            if (parameters.getZonalData(country).isPresent()) {
                creationContext.getCreationReport().altered("Network CRAC importer does not yet support custom GLSKs for counter-trading actions. Proportional GLSK will be considered.");
            }

            crac.getSortedInstants().stream().filter(instant -> !instant.isOutage())
                .forEach(instant -> addCountertradingActionForInstant(country, instant));
        });
    }

    private void addCountertradingActionForInstant(Country country, Instant instant) {
        if (parameters.getRaRange(country, instant).getMin().isEmpty() || parameters.getRaRange(country, instant).getMax().isEmpty()) {
            throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA,
                String.format("Cannot create a counter-trading action for %s at instant %s without a defined min/max range.", country, instant));
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
                String.format("Cannot create a counter-trading action for %s at instant %s because initial production is almost zero (proportional GLSK is assumed). Maybe all generators were filtered out.", country, instant));
        }

        InjectionRangeActionAdder injectionRangeActionAdder = crac.newInjectionRangeAction()
            .withId("CT_" + country.getName())
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
        //  crac.getInjectionRangeAction("CT_" + country.getName()).apply(network, initialTotalP);
        // TODO is the above line necessary ?
    }
}
