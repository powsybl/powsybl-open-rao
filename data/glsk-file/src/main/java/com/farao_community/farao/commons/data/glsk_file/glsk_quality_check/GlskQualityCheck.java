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

    public void gskQualityCheck(GlskQualityCheckImporter data) {
        Map<String, GlskPoint> glskPointMap = data.getUcteGlskDocument().getGlskPointForInstant(data.getInstant());
        glskPointMap.forEach((country, glskPoint) -> {
            checkShiftKey(glskPoint, data.getNetwork());
        });
    }

    private void checkShiftKey(GlskPoint glskPoint, Network network) {
        glskPoint.getGlskShiftKeys().stream().forEach(glskShiftKey -> {
            if (glskShiftKey.getPsrType().equals(GENERATOR)) {
                glskShiftKey.getRegisteredResourceArrayList().stream()
                        .forEach(resource -> checkId(resource, resource.getGeneratorId(TypeGlskFile.UCTE), network));
            } else {
                glskShiftKey.getRegisteredResourceArrayList().stream()
                        .forEach(resource -> checkId(resource, resource.getLoadId(TypeGlskFile.UCTE), network));
            }
        });
    }

    private void checkId(GlskRegisteredResource registeredResource, String injectionId, Network network) {
        if (injectionId == null) {
            if (network.getBusView().getBus(registeredResource.getmRID()) != null &&
                    !network.getBusView().getBus(registeredResource.getmRID()).isInMainSynchronousComponent()) {
                setRapport(network.getId(),
                        "Error 3",
                        registeredResource.getmRID(),
                        SeverityEnum.WARNING,
                        "Check whether a generator or load is connected to the main island -> node exist and is connected to busbar But that busbar is isolated");
            } else if (network.getBusView().getBus(registeredResource.getmRID()) == null) {
                setRapport(network.getId(),
                        "Error 1",
                        registeredResource.getmRID(),
                        SeverityEnum.WARNING,
                        "log in the data quality check");
            } else {
                setRapport(network.getId(),
                        "Error 2",
                        registeredResource.getmRID(),
                        SeverityEnum.WARNING,
                        "The GSK node is mapped into cgm with a production unit witch is not running");
            }
        } else {
        }
    }

    private void setRapport(String nodeId, String type, String tso, SeverityEnum severity, String message) {
        this.qualityReports.add(QualityReport.setQualityReport(nodeId, type, tso, severity, message));
    }
}
