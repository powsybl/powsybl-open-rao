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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
public class GlskQualityCheck {

    private static final String GENERATOR = "A04";

    private static final String LOAD = "A05";

    private List<QualityReport> qualityReports;

    public List<QualityReport> getQualityReports() {
        return qualityReports;
    }

    public GlskQualityCheck() {
        this.qualityReports = new ArrayList<>();
    }

    public List<QualityReport> gskQualityCheck(GlskQualityCheckImporter data) {
        Map<String, GlskPoint> glskPointMap = data.getUcteGlskDocument().getGlskPointForInstant(data.getInstant());
        glskPointMap.forEach((country, glskPoint) -> {
            checkGlskPoint(glskPoint, data.getNetwork(), country);
        });
        return this.qualityReports;
    }

    private void checkGlskPoint(GlskPoint glskPoint, Network network, String tso) {
        glskPoint.getGlskShiftKeys().stream().forEach(glskShiftKey -> {
            if (glskShiftKey.getPsrType().equals(GENERATOR)) {
                glskShiftKey.getRegisteredResourceArrayList().stream()
                        .forEach(resource -> checkResource(resource, network.getGenerator(resource.getGeneratorId(TypeGlskFile.UCTE)), "Generator", network, tso));
            } else if (glskShiftKey.getPsrType().equals(LOAD)) {
                glskShiftKey.getRegisteredResourceArrayList().stream()
                        .forEach(resource -> checkResource(resource, network.getLoad(resource.getLoadId(TypeGlskFile.UCTE)), "Load", network, tso));
            }
        });
    }

    private void checkResource(GlskRegisteredResource registeredResource, Injection injection, String type, Network network, String tso) {
        if (injection == null) {

            if (network.getBusBreakerView().getBus(registeredResource.getmRID()) == null) { //todo valid the use of BusBreakerView not BusView
                setRapport(registeredResource.getmRID(),
                        type,
                        tso,
                        SeverityEnum.WARNING,
                        "GSK node is not found in CGM");
            } else {
                setRapport(registeredResource.getmRID(),
                        type,
                        tso,
                        SeverityEnum.WARNING,
                        "The GSK node is present but it's not representing a Generator or Load");
            }
        } else {
            if (!injection.getTerminal().getBusBreakerView().getBus().isInMainSynchronousComponent()) { //todo valid the use of BusBreakerView not BusView
                setRapport(registeredResource.getmRID(),
                        type,
                        tso,
                        SeverityEnum.WARNING,
                        "GLSK node is connected to an island");
            }
        }
    }

    private void setRapport(String nodeId, String type, String tso, SeverityEnum severity, String message) {
        this.qualityReports.add(QualityReport.setQualityReport(nodeId, type, tso, severity, message));
    }
}
