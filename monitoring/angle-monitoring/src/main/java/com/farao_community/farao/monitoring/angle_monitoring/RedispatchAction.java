/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.monitoring.angle_monitoring;

import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Network;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.farao_community.farao.commons.logs.FaraoLoggerProvider.BUSINESS_WARNS;

/**
 * A redispatch action collects a quantity of power to be redispatched (powerToBeRedispatched) in country (countryName)
 * according to proportional glsks that exclude networkElementsToBeExcluded.
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class RedispatchAction {

    private final String countryName;
    private final double powerToBeRedispatched;
    private final Set<String> networkElementsToBeExcluded;
    private final Set<ScalableNetworkElement> glsks;

    RedispatchAction(String countryName, double powerToBeRedispatched, Set<String> networkElementsToBeExcluded, Set<ScalableNetworkElement> glsks) {
        this.countryName = countryName;
        this.powerToBeRedispatched = powerToBeRedispatched; // positive for generation, negative for load
        this.networkElementsToBeExcluded = networkElementsToBeExcluded;
        this.glsks = glsks;

    }

    /**
     * Returns a proportional scalable defined from ScalableNetworkElements (glsks), after filtering networkElementsToBeExcluded
     * and reproportionalized percentages when necessary
     */
    private Scalable defineProportionalGlsk() {
        // Filter out network elements that have to be excluded
        Set<ScalableNetworkElement> filteredGlsks = glsks.stream()
                .filter(glsk -> !networkElementsToBeExcluded.contains(glsk.getId())).collect(Collectors.toSet());

        if (filteredGlsks.size() != glsks.size()) {
            BUSINESS_WARNS.warn("%s scalable network elements have been filtered.", glsks.size() - filteredGlsks.size());
            Double sumPercentages = filteredGlsks.stream().mapToDouble(ScalableNetworkElement::getPercentage).sum();
            filteredGlsks.forEach(glsk -> glsk.setPercentage((float) (glsk.getPercentage() / sumPercentages * 100.)));
        }

        // Define proportionalGlsk
        List<Scalable> scalables = new ArrayList<>();
        List<Float> percentages = new ArrayList<>();
        for (ScalableNetworkElement glsk : filteredGlsks) {
            if (glsk.getScalableType().equals(ScalableNetworkElement.ScalableType.GENERATOR)) {
                scalables.add(Scalable.onGenerator(glsk.getId()));
                percentages.add(glsk.getPercentage());
            } else if (glsk.getScalableType().equals(ScalableNetworkElement.ScalableType.LOAD)) {
                scalables.add(Scalable.onLoad(glsk.getId()));
                percentages.add(glsk.getPercentage());
            } else {
                BUSINESS_WARNS.warn("Unhandled type of scalable for scalable {}.", glsk.getId());
            }
        }
        return Scalable.proportional(percentages, scalables);
    }

    /**
     * Scales powerToBeRedispatched on network.
     */
    public void apply(Network network) {
        double redispatchedPower = defineProportionalGlsk().scale(network, powerToBeRedispatched);
        BUSINESS_WARNS.warn("Scaling for country {}: asked={}, done={}", countryName, powerToBeRedispatched, redispatchedPower);
    }
}
