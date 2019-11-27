/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.commons.data.glsk_file.glsk_quality_check;

import com.farao_community.farao.commons.data.glsk_file.GlskPoint;
import com.farao_community.farao.commons.data.glsk_file.GlskRegisteredResource;
import com.farao_community.farao.commons.data.glsk_file.actors.TypeGlskFile;
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

    private QualityReport generateReport(GlskQualityCheckInput data) {
        Map<String, GlskPoint> glskPointMap = data.getUcteGlskDocument().getGlskPointsForInstant(data.getInstant());
        glskPointMap.forEach((country, glskPoint) -> {
            checkGlskPoint(glskPoint, data.getNetwork(), country);
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
                qualityReport.log(registeredResource.getmRID(),
                        type,
                        tso,
                        SeverityEnum.WARNING,
                        "GSK node is not found in CGM");
            } else {
                qualityReport.log(registeredResource.getmRID(),
                        type,
                        tso,
                        SeverityEnum.WARNING,
                        "The GSK node is present but it's not representing a Generator or Load");
            }
        } else {
            if (!injection.getTerminal().getBusBreakerView().getBus().isInMainSynchronousComponent()) {
                qualityReport.log(registeredResource.getmRID(),
                        type,
                        tso,
                        SeverityEnum.WARNING,
                        "GLSK node is connected to an island");
            }
        }
    }
}
