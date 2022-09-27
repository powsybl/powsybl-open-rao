/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.monitoring.angle_monitoring;

import com.powsybl.glsk.api.GlskPoint;
import com.powsybl.glsk.api.GlskRegisteredResource;
import com.powsybl.glsk.api.GlskShiftKey;
import com.powsybl.glsk.api.util.converters.GlskPointScalableConverter;
import com.powsybl.iidm.network.Network;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.farao_community.farao.commons.logs.FaraoLoggerProvider.BUSINESS_WARNS;

/**
 * A redispatch action collects a quantity of power to be redispatched (powerToBeRedispatched) in country (countryName)
 * according to merit order glsks that exclude networkElementsToBeExcluded.
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class RedispatchAction {
    private final Network network;
    private final String countryName;
    private final double powerToBeRedispatched;
    private final Set<String> networkElementsToBeExcluded;
    private final GlskPoint glskPoint;

    RedispatchAction(Network network, String countryName, double powerToBeRedispatched, Set<String> networkElementsToBeExcluded, GlskPoint glskPoint) {
        this.network = network;
        this.countryName = countryName;
        this.powerToBeRedispatched = powerToBeRedispatched; // positive for generation, negative for load
        this.networkElementsToBeExcluded = networkElementsToBeExcluded;
        this.glskPoint = Objects.requireNonNull(glskPoint);

    }

    // TODO : integrate this filter on scalables in powsybl
    private void filterGlskPoint() {
        for (GlskShiftKey glskShiftKey : glskPoint.getGlskShiftKeys()) {
            List<GlskRegisteredResource> filteredRegisteredResourceList = new ArrayList<>(glskShiftKey.getRegisteredResourceArrayList());
            for (GlskRegisteredResource glskRegisteredResource : glskShiftKey.getRegisteredResourceArrayList()) {
                // PROBLEM : all contain only 1 registeredResource
                if (networkElementsToBeExcluded.contains(glskRegisteredResource.getmRID())) {
                    glskRegisteredResource.setmRID("UNKNOWN ID - TEMPORARY FILTERING");
                }
            }
            glskShiftKey.setRegisteredResourceArrayList(filteredRegisteredResourceList);
        }
    }

    /**
     * Scales powerToBeRedispatched on network.
     */
    public void apply() {
        filterGlskPoint();
        double redispatchedPower = GlskPointScalableConverter.convert(network, glskPoint).scale(network, powerToBeRedispatched);
        BUSINESS_WARNS.warn("Scaling for country {}: asked={}, done={}", countryName, powerToBeRedispatched, redispatchedPower);
    }
}
