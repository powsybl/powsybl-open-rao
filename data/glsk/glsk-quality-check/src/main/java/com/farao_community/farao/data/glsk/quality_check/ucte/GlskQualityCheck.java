/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.quality_check.ucte;

import com.farao_community.farao.data.glsk.import_.actors.TypeGlskFile;
import com.farao_community.farao.data.glsk.import_.GlskPoint;
import com.farao_community.farao.data.glsk.import_.GlskRegisteredResource;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Network;

import java.util.Map;

/**
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
class GlskQualityCheck {

    private static final String GENERATOR = "A04";

    private static final String LOAD = "A05";

    private QualityReport qualityReport = new QualityReport();

    public static QualityReport gskQualityCheck(GlskQualityCheckInput input) {
        return new GlskQualityCheck().generateReport(input);
    }

    private QualityReport generateReport(GlskQualityCheckInput input) {
        Map<String, GlskPoint> glskPointMap = input.getUcteGlskDocument().getGlskPointsForInstant(input.getInstant());
        glskPointMap.forEach((country, glskPoint) -> {
            checkGlskPoint(glskPoint, input.getNetwork(), country);
        });
        return qualityReport;
    }

    private void checkGlskPoint(GlskPoint glskPoint, Network network, String tso) {
        glskPoint.getGlskShiftKeys().forEach(glskShiftKey -> {
            if (glskShiftKey.getPsrType().equals(GENERATOR)) {
                glskShiftKey.getRegisteredResourceArrayList()
                        .forEach(resource -> checkResource(resource, network.getGenerator(resource.getGeneratorId(TypeGlskFile.UCTE)), "Generator", network, tso));
            } else if (glskShiftKey.getPsrType().equals(LOAD)) {
                glskShiftKey.getRegisteredResourceArrayList()
                        .forEach(resource -> checkResource(resource, network.getLoad(resource.getLoadId(TypeGlskFile.UCTE)), "Load", network, tso));
            }
        });
    }

    private void checkResource(GlskRegisteredResource registeredResource, Injection injection, String type, Network network, String tso) {
        if (injection == null) {

            if (network.getBusBreakerView().getBus(registeredResource.getmRID()) == null) {
                qualityReport.warn(registeredResource.getmRID(),
                        type,
                        tso,
                        "GLSK node is not found in CGM");
            } else {
                qualityReport.warn(registeredResource.getmRID(),
                        type,
                        tso,
                        "GLSK node is present but it's not representing a Generator or Load");
            }
        } else {
            if (!injection.getTerminal().isConnected()
                    || !injection.getTerminal().getBusBreakerView().getBus().isInMainSynchronousComponent()) {
                qualityReport.warn(registeredResource.getmRID(),
                        type,
                        tso,
                        "GLSK node is connected to an island");
            }
        }
    }
}
