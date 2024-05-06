/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.anglemonitoring;

import com.powsybl.glsk.api.GlskPoint;
import com.powsybl.glsk.api.GlskRegisteredResource;
import com.powsybl.glsk.api.GlskShiftKey;
import com.powsybl.glsk.api.util.converters.GlskPointScalableConverter;
import com.powsybl.iidm.network.Network;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Thi implementation uses a user-define GLSK point
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class RedispatchActionWithGlskPoint extends AbstractRedispatchAction {
    private final GlskPoint glskPoint;

    RedispatchActionWithGlskPoint(Set<String> networkElementsToBeExcluded, GlskPoint glskPoint) {
        this.glskPoint = filterGlskPoint(Objects.requireNonNull(glskPoint), networkElementsToBeExcluded);
    }

    // TODO : integrate this filter on scalables in powsybl
    private static GlskPoint filterGlskPoint(GlskPoint glskPoint, Set<String> networkElementsToBeExcluded) {
        for (GlskShiftKey glskShiftKey : glskPoint.getGlskShiftKeys()) {
            List<GlskRegisteredResource> filteredRegisteredResourceList = new ArrayList<>(glskShiftKey.getRegisteredResourceArrayList());
            for (GlskRegisteredResource glskRegisteredResource : glskShiftKey.getRegisteredResourceArrayList()) {
                if (networkElementsToBeExcluded.contains(glskRegisteredResource.getmRID())) {
                    glskRegisteredResource.setmRID("UNKNOWN ID - TEMPORARY FILTERING");
                }
            }
            glskShiftKey.setRegisteredResourceArrayList(filteredRegisteredResourceList);
        }
        return glskPoint;
    }

    @Override
    public void apply(Network network, double powerToRedispatch) {
        super.apply(network, powerToRedispatch, GlskPointScalableConverter.convert(network, glskPoint));
    }
}
