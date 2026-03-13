/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network;

import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.io.commons.OpenRaoImportException;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import com.powsybl.openrao.data.crac.io.network.parameters.BalancingRangeAction;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class BalancingRangeActionCreator {
    private final Crac crac;
    private final Network network;
    private final BalancingRangeAction parameters;
    private final NetworkCracCreationContext creationContext;

    BalancingRangeActionCreator(NetworkCracCreationContext creationContext, Network network, BalancingRangeAction parameters) {
        this.creationContext = creationContext;
        this.crac = creationContext.getCrac();
        this.network = network;
        this.parameters = parameters;
    }

    void addBalancingRangeAction() {
        crac.getSortedInstants().stream().filter(instant -> !instant.isOutage())
            .forEach(instant -> {
                try {
                    addBalancingRangeActionForInstant(instant);
                } catch (OpenRaoImportException e) {
                    creationContext.getCreationReport().removed(e.getMessage());
                }
            });
    }

    private void addBalancingRangeActionForInstant(Instant instant) {
        if (parameters.getRaRange(instant).getMin().isEmpty() || parameters.getRaRange(instant).getMax().isEmpty()) {
            throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA,
                String.format("Cannot create a balancing action at instant %s without a defined min/max range.", instant));
        }
        if (parameters.getRaRange(instant).getMin().orElseThrow() > -0.1 && parameters.getRaRange(instant).getMax().orElseThrow() < 0.1) {
            // range is zero, no need to create the RA
            return;
        }

        Set<Generator> consideredGenerators = network.getGeneratorStream()
            .filter(generator -> parameters.shouldIncludeInjection(generator, instant))
            .filter(generator -> !creationContext.isInjectionUsedInAction(instant, generator.getId()))
            .collect(Collectors.toSet());

        Utils.addInjectionRangeAction(
            creationContext,
            consideredGenerators,
            "BALANCING",
            instant,
            parameters.getRaRange(instant),
            true,
            parameters.getRaCosts(instant));
    }
}
