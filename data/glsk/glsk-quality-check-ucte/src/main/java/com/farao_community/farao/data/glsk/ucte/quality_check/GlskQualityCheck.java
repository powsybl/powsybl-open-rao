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
import com.powsybl.iidm.network.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        List<String> manualGskGenerators =  glskPoint.getGlskShiftKeys().stream()
                .filter(gskShiftKey -> gskShiftKey.getPsrType().equals(GENERATOR) && gskShiftKey.getBusinessType().equals("B43"))
                .flatMap(gskShiftKey -> gskShiftKey.getRegisteredResourceArrayList().stream())
                .map(AbstractGlskRegisteredResource::getGeneratorId).collect(Collectors.toList());
        List<String> manualGskLoads =  glskPoint.getGlskShiftKeys().stream()
                .filter(gskShiftKey -> gskShiftKey.getPsrType().equals(LOAD) && gskShiftKey.getBusinessType().equals("B43"))
                .flatMap(gskShiftKey -> gskShiftKey.getRegisteredResourceArrayList().stream())
                .map(AbstractGlskRegisteredResource::getLoadId).collect(Collectors.toList());

        network.getVoltageLevelStream().forEach(voltageLevel -> voltageLevel.getBusBreakerView().getBuses().forEach(bus -> {
            if (manualGskGenerators.contains(bus.getId() + "_generator")) {
                createMissingGenerator(network, voltageLevel, bus.getId());
            }
            if (manualGskLoads.contains(bus.getId() + "_load")) {
                createMissingLoad(network, voltageLevel, bus.getId());
            }
        }));

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

    private void createMissingGenerator(Network network, VoltageLevel voltageLevel, String busId) {
        String generatorId = busId + "_generator";
        if (network.getGenerator(generatorId) == null) {
            voltageLevel.newGenerator()
                    .setBus(busId)
                    .setEnsureIdUnicity(true)
                    .setId(generatorId)
                    .setMaxP(9999)
                    .setMinP(0)
                    .setTargetP(0)
                    .setTargetQ(0)
                    .setTargetV(voltageLevel.getNominalV())
                    .setVoltageRegulatorOn(false)
                    .setFictitious(true)
                    .add()
                    .newMinMaxReactiveLimits().setMaxQ(99999).setMinQ(99999).add();
        }
    }

    private void createMissingLoad(Network network, VoltageLevel voltageLevel, String busId) {
        String loadId = busId + "_load";
        if (network.getLoad(loadId) == null) {
            voltageLevel.newLoad()
                    .setBus(busId)
                    .setEnsureIdUnicity(true)
                    .setId(loadId)
                    .setP0(0)
                    .setQ0(0)
                    .setLoadType(LoadType.FICTITIOUS)
                    .add();
        }
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
