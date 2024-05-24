/*
 *  Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.glsk.virtual.hubs;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.commons.EICode;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceProgram;
import com.powsybl.openrao.virtualhubs.VirtualHub;
import com.powsybl.openrao.virtualhubs.VirtualHubsConfiguration;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.commons.ZonalDataImpl;
import com.powsybl.iidm.network.*;
import com.powsybl.sensitivity.SensitivityVariableSet;
import com.powsybl.sensitivity.WeightedSensitivityVariable;

import java.util.*;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public final class GlskVirtualHubs {
    private GlskVirtualHubs() {
    }

    /**
     * Build GLSKs of virtual hubs
     *
     * @param network : Network object, which contains AssignedVirtualHub extensions on
     *                Loads which are virtual hubs
     * @param referenceProgram : Reference Program object
     *
     * @param reportNode
     * @return one LinearGlsk for each virtual hub given in the referenceProgram and found
     * in the network
     */
    public static ZonalData<SensitivityVariableSet> getVirtualHubGlsks(VirtualHubsConfiguration virtualHubsConfiguration, Network network, ReferenceProgram referenceProgram, ReportNode reportNode) {
        List<String> countryCodes = referenceProgram.getListOfAreas().stream()
            .filter(eiCode -> !eiCode.isCountryCode())
            .map(EICode::getAreaCode)
            .toList();
        return getVirtualHubGlsks(virtualHubsConfiguration, network, countryCodes, reportNode);
    }

    /**
     * Build GLSKs of virtual hubs
     *
     * @param network : Network object, which contains AssignedVirtualHub extensions on
     *                Loads which are virtual hubs
     * @param eiCodes : list of EI Codes of virtual hubs
     *
     * @param rootReportNode
     * @return one LinearGlsk for each virtual hub given in eiCodes and found in the network
     */
    public static ZonalData<SensitivityVariableSet> getVirtualHubGlsks(VirtualHubsConfiguration virtualHubsConfiguration, Network network, List<String> eiCodes, ReportNode rootReportNode) {
        ReportNode reportNode = GlskVirtualHubsReports.reportGlskVirtualHubsBuild(rootReportNode);
        Map<String, SensitivityVariableSet> glsks = new HashMap<>();
        Map<String, Injection<?>> injections = buildInjectionsMap(virtualHubsConfiguration, network, reportNode);

        eiCodes.forEach(eiCode -> {

            if (!injections.containsKey(eiCode)) {
                GlskVirtualHubsReports.reportGlskVirtualHubsNoLoadFound(reportNode, eiCode);
            } else {
                GlskVirtualHubsReports.reportGlskVirtualHubsLoadFound(reportNode, injections.get(eiCode).getId(), eiCode);
                Optional<SensitivityVariableSet> virtualHubGlsk = createGlskFromVirtualHub(eiCode, injections.get(eiCode));
                virtualHubGlsk.ifPresent(linearGlsk -> glsks.put(eiCode, linearGlsk));
            }
        });
        return new ZonalDataImpl<>(glsks);
    }

    private static Map<String, Injection<?>> buildInjectionsMap(VirtualHubsConfiguration virtualHubsConfiguration, Network network, ReportNode reportnode) {
        Map<String, Injection<?>> injections = new HashMap<>();
        virtualHubsConfiguration.getVirtualHubs()
                .forEach(virtualHub -> {
                    Injection<?> injection = getInjection(network, virtualHub, reportnode);
                    if (injection != null) {
                        injections.put(virtualHub.eic(), injection);
                    }
                });
        return injections;
    }

    private static Optional<SensitivityVariableSet> createGlskFromVirtualHub(String eiCode, Injection<?> injection) {
        List<WeightedSensitivityVariable> glskList = new ArrayList<>();
        try {
            glskList.add(new WeightedSensitivityVariable(injection.getId(), 1.0F));
            return Optional.of(new SensitivityVariableSet(eiCode, glskList));
        } catch (OpenRaoException e) {
            return Optional.empty();
        }
    }

    private static Injection<?> getInjection(Network network, VirtualHub virtualHub, ReportNode reportnode) {

        Optional<Bus> bus = findBusById(network, virtualHub.nodeName());
        if (bus.isPresent()) {
            // virtual hub is on a real network node
            Optional<Load> busLoad = bus.get().getLoadStream().findFirst();
            if (busLoad.isEmpty()) {
                GlskVirtualHubsReports.reportGlskVirtualHubsAssigmentErrorNoLoad(reportnode, virtualHub.eic(), virtualHub.nodeName());
                return null;
            }
            return busLoad.get();
        }

        Optional<DanglingLine> danglingLine = findDanglingLineWithXNode(network, virtualHub.nodeName());
        if (danglingLine.isPresent() && !danglingLine.get().isPaired()) {
            return danglingLine.get();
        }

        GlskVirtualHubsReports.reportGlskVirtualHubsAssigmentErrorNoNode(reportnode, virtualHub.eic(), virtualHub.nodeName());
        return null;
    }

    private static Optional<Bus> findBusById(Network network, String id) {
        return network.getVoltageLevelStream()
            .flatMap(vl -> vl.getBusBreakerView().getBusStream())
            .filter(bus -> bus.getId().equals(id))
            .findFirst();
    }

    private static Optional<DanglingLine> findDanglingLineWithXNode(Network network, String xNodeId) {
        return network.getDanglingLineStream()
            .filter(danglingLine -> danglingLine.getPairingKey().equals(xNodeId))
            .findFirst();
    }
}
