/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.monitoring.angle_monitoring;

import com.powsybl.glsk.api.GlskPoint;
import com.powsybl.glsk.api.GlskRegisteredResource;
import com.powsybl.glsk.api.GlskShiftKey;
import com.powsybl.glsk.api.util.converters.GlskPointScalableConverter;
import com.powsybl.iidm.network.Network;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.powsybl.open_rao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;

/**
 * A redispatch action collects a quantity of power to be redispatched (powerToBeRedispatched) in country (countryName)
 * according to glsks that exclude networkElementsToBeExcluded.
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class RedispatchAction {
    private final double powerToBeRedispatched;
    private final Set<String> networkElementsToBeExcluded;
    private final GlskPoint glskPoint;

    RedispatchAction(double powerToBeRedispatched, Set<String> networkElementsToBeExcluded, GlskPoint glskPoint) {
        this.powerToBeRedispatched = powerToBeRedispatched; // positive for generation, negative for load
        this.networkElementsToBeExcluded = networkElementsToBeExcluded;
        this.glskPoint = Objects.requireNonNull(glskPoint);

    }

    // TODO : integrate this filter on scalables in powsybl
    private void filterGlskPoint() {
        for (GlskShiftKey glskShiftKey : glskPoint.getGlskShiftKeys()) {
            List<GlskRegisteredResource> filteredRegisteredResourceList = new ArrayList<>(glskShiftKey.getRegisteredResourceArrayList());
            for (GlskRegisteredResource glskRegisteredResource : glskShiftKey.getRegisteredResourceArrayList()) {
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
    public void apply(Network network) {
        filterGlskPoint();
        double redispatchedPower = GlskPointScalableConverter.convert(network, glskPoint).scale(network, powerToBeRedispatched);
        if (Math.abs(redispatchedPower - powerToBeRedispatched) > 1) {
            BUSINESS_WARNS.warn("Redispatching failed: asked={} MW, applied={} MW", powerToBeRedispatched, redispatchedPower);
        }
    }
}
