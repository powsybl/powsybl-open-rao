/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.anglemonitoring;

import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Network;

import java.util.Objects;
import java.util.Set;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;

/**
 * A redispatch action collects a quantity of power to be redispatched (powerToBeRedispatched) in country (countryName)
 * according to glsks that exclude networkElementsToBeExcluded.
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class RedispatchAction {
    private final double powerToBeRedispatched;
    private final Scalable scalable;

    RedispatchAction(double powerToBeRedispatched, Set<String> networkElementsToBeExcluded, Scalable scalable) {
        this.powerToBeRedispatched = powerToBeRedispatched; // positive for generation, negative for load
        // TODO : filter scalable when possible in powsybl-core (see PR #2346)
        // this.scalable = scalable.filter(networkElementsToBeExcluded)
        this.scalable = Objects.requireNonNull(scalable);
    }

    /**
     * Scales powerToBeRedispatched on network.
     */
    public void apply(Network network) {
        double redispatchedPower = scalable.scale(network, powerToBeRedispatched);
        if (Math.abs(redispatchedPower - powerToBeRedispatched) > 1) {
            BUSINESS_WARNS.warn("Redispatching failed: asked={} MW, applied={} MW", powerToBeRedispatched, redispatchedPower);
        }
    }
}
