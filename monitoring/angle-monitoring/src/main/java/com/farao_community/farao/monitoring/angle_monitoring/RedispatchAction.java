/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.monitoring.angle_monitoring;

import com.powsybl.iidm.modification.scalable.CompoundScalable;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Network;

import java.util.Objects;
import java.util.Set;

import static com.farao_community.farao.commons.logs.FaraoLoggerProvider.BUSINESS_WARNS;

/**
 * A redispatch action collects a quantity of power to be redispatched (powerToBeRedispatched) in country (countryName)
 * according to glsks that exclude networkElementsToBeExcluded.
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class RedispatchAction {
    private final double powerToBeRedispatched;
    private final Set<String> networkElementsToBeExcluded;
    private final CompoundScalable compoundScalable;

    RedispatchAction(double powerToBeRedispatched, Set<String> networkElementsToBeExcluded, CompoundScalable compoundScalable) {
        this.powerToBeRedispatched = powerToBeRedispatched; // positive for generation, negative for load
        this.networkElementsToBeExcluded = networkElementsToBeExcluded;
        this.compoundScalable = Objects.requireNonNull(compoundScalable);

    }

    private void filterGlskPoint(Network network) {
        compoundScalable.getScalables().stream().filter(scalable -> !(scalable instanceof CompoundScalable)).forEach(s -> {
            for (Injection injection : s.filterInjections(network)) {
                if (networkElementsToBeExcluded.contains(injection.getId())) {
                    compoundScalable.deactivateScalables(Set.of(s));
                    break;
                }
            }
        });
    }

    /**
     * Scales powerToBeRedispatched on network.
     */
    public void apply(Network network) {
        filterGlskPoint(network);
        double redispatchedPower = compoundScalable.scale(network, powerToBeRedispatched);
        BUSINESS_WARNS.warn("Scaling : asked={}, done={}", powerToBeRedispatched, redispatchedPower);
    }
}
