/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.ucte.quality_check;

import com.farao_community.farao.data.glsk.api.AbstractGlskPoint;
import com.farao_community.farao.data.glsk.api.AbstractGlskRegisteredResource;
import com.farao_community.farao.data.glsk.ucte.UcteGlskPoint;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Network;

import java.util.Map;

/**
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
class GlskQualityCheck {

    private static final String GENERATOR = "A04";

    private static final String LOAD = "A05";

    private final QualityReport qualityReport = new QualityReport();

    public static QualityReport gskQualityCheck(GlskQualityCheckInput input) {
        return new GlskQualityCheck().generateReport(input);
    }

    private QualityReport generateReport(GlskQualityCheckInput input) {
        Map<String, UcteGlskPoint> glskPointMap = input.getUcteGlskDocument().getGlskPointsForInstant(input.getInstant());
        glskPointMap.forEach((country, glskPoint) -> checkGlskPoint(glskPoint, input.getNetwork(), country));
        return qualityReport;
    }

    private void checkGlskPoint(AbstractGlskPoint glskPoint, Network network, String tso) {
        glskPoint.getGlskShiftKeys().forEach(glskShiftKey -> {
            if (glskShiftKey.getPsrType().equals(GENERATOR)) {
                glskShiftKey.getRegisteredResourceArrayList()
                        .forEach(resource -> checkResource(resource, network.getGenerator(resource.getGeneratorId()), "Generator", network, tso));
            } else if (glskShiftKey.getPsrType().equals(LOAD)) {
                glskShiftKey.getRegisteredResourceArrayList()
                        .forEach(resource -> checkResource(resource, network.getLoad(resource.getLoadId()), "Load", network, tso));
            }
        });
    }

    private void checkResource(AbstractGlskRegisteredResource registeredResource, Injection<?> injection, String type, Network network, String tso) {
        if (injection == null) {

            if (network.getBusBreakerView().getBus(registeredResource.getmRID()) == null) {
                qualityReport.warn("1",
                        registeredResource.getmRID(),
                        type,
                        tso,
                        "GLSK node is not found in CGM");
            } else {
                qualityReport.warn("2",
                        registeredResource.getmRID(),
                        type,
                        tso,
                        "GLSK node is present but has no running Generator or Load");
            }
        } else {
            if (!injection.getTerminal().isConnected()
                    || !injection.getTerminal().getBusBreakerView().getBus().isInMainSynchronousComponent()) {
                qualityReport.warn(
                        "3",
                        registeredResource.getmRID(),
                        type,
                        tso,
                        "GLSK node is connected to an island");
            }
        }
    }
}
