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

    CountertradingRangeActionsCreator(NetworkCracCreationContext creationContext, Network network, CountertradingRangeActions parameters) {
        this.creationContext = creationContext;
        this.crac = creationContext.getCrac();
        this.network = network;
        this.parameters = parameters;
    }

    void addCountertradingActions() {
        if (parameters.getCountries().isEmpty()) {
            creationContext.getCreationReport().removed("Cannot create counter-trading remedial actions without an explicit list of countries.");
            return;
        }
        if (parameters.getZonalData().isPresent()) {
            creationContext.getCreationReport().altered("Network CRAC importer does not yet support custom GLSKs for counter-trading actions. Proportional GLSK will be considered.");
        }
        parameters.getCountries().orElseThrow().forEach(country -> crac.getSortedInstants().stream().filter(instant -> !instant.isOutage())
            .forEach(instant -> {
                try {
                    addCountertradingActionForInstant(country, instant);
                } catch (OpenRaoImportException e) {
                    creationContext.getCreationReport().removed(e.getMessage());
                }
            }));
    }

    private void addCountertradingActionForInstant(Country country, Instant instant) {
        if (parameters.getRaRange(country, instant).getMin().isEmpty() || parameters.getRaRange(country, instant).getMax().isEmpty()) {
            throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA,
                String.format("Cannot create a counter-trading action for %s at instant %s without a defined min/max range.", country, instant));
        }
        if (parameters.getRaRange(country, instant).getMin().orElseThrow() > -0.1 && parameters.getRaRange(country, instant).getMax().orElseThrow() < 0.1) {
            // range is zero, no need to create the RA
            return;
        }

        Set<Generator> consideredGenerators = network.getGeneratorStream()
            .filter(generator -> Utils.injectionIsInCountries(generator, Set.of(country)))
            .filter(generator -> parameters.shouldIncludeInjection(generator, instant))
            .filter(generator -> !creationContext.isInjectionUsedInAction(instant, generator.getId()))
            .collect(Collectors.toSet());

        Utils.addInjectionRangeAction(
            creationContext,
            consideredGenerators,
            "CT_" + country.getName(),
            instant,
            parameters.getRaRange(country, instant),
            true,
            parameters.getRaCosts(country, instant));
    }
}
