/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.glsk.virtual.hubs;

import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.commons.ZonalDataImpl;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.EICode;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceProgram;
import com.powsybl.openrao.virtualhubs.VirtualHub;
import com.powsybl.openrao.virtualhubs.VirtualHubsConfiguration;
import com.powsybl.sensitivity.SensitivityVariableSet;
import com.powsybl.sensitivity.WeightedSensitivityVariable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
     * @return one LinearGlsk for each virtual hub given in the referenceProgram and found
     * in the network
     */
    public static ZonalData<SensitivityVariableSet> getVirtualHubGlsks(VirtualHubsConfiguration virtualHubsConfiguration, Network network, ReferenceProgram referenceProgram) {
        List<String> countryCodes = referenceProgram.getListOfAreas().stream()
            .filter(eiCode -> !eiCode.isCountryCode())
            .map(EICode::getAreaCode)
            .toList();
        return getVirtualHubGlsks(virtualHubsConfiguration, network, countryCodes);
    }

    /**
     * Build GLSKs of virtual hubs
     *
     * @param network : Network object, which contains AssignedVirtualHub extensions on
     *                Loads which are virtual hubs
     * @param eiCodes : list of EI Codes of virtual hubs
     *
     * @return one LinearGlsk for each virtual hub given in eiCodes and found in the network
     */
    public static ZonalData<SensitivityVariableSet> getVirtualHubGlsks(VirtualHubsConfiguration virtualHubsConfiguration, Network network, List<String> eiCodes) {
        Map<String, SensitivityVariableSet> glsks = new HashMap<>();
        Map<String, Injection<?>> injections = buildInjectionsMap(virtualHubsConfiguration, network);

        eiCodes.forEach(eiCode -> {

            if (!injections.containsKey(eiCode)) {
                OpenRaoLoggerProvider.BUSINESS_WARNS.warn("No load found for virtual hub {}", eiCode);
            } else {
                OpenRaoLoggerProvider.TECHNICAL_LOGS.debug("Load {} found for virtual hub {}", injections.get(eiCode).getId(), eiCode);
                Optional<SensitivityVariableSet> virtualHubGlsk = createGlskFromVirtualHub(eiCode, injections.get(eiCode));
                virtualHubGlsk.ifPresent(linearGlsk -> glsks.put(eiCode, linearGlsk));
            }
        });
        return new ZonalDataImpl<>(glsks);
    }

    private static Map<String, Injection<?>> buildInjectionsMap(VirtualHubsConfiguration virtualHubsConfiguration, Network network) {
        Map<String, Injection<?>> injections = new HashMap<>();
        virtualHubsConfiguration.getVirtualHubs()
                .forEach(virtualHub -> {
                    Injection<?> injection = getInjection(network, virtualHub);
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

    private static Injection<?> getInjection(Network network, VirtualHub virtualHub) {

        Optional<Bus> bus = findBusById(network, virtualHub.nodeName());
        if (bus.isPresent()) {
            // virtual hub is on a real network node
            Optional<Load> busLoad = bus.get().getLoadStream().findFirst();
            if (busLoad.isEmpty()) {
                OpenRaoLoggerProvider.BUSINESS_WARNS.warn("Virtual hub {} cannot be assigned on node {} as it has no load in the network", virtualHub.eic(), virtualHub.nodeName());
                return null;
            }
            return busLoad.get();
        }

        Optional<DanglingLine> danglingLine = findDanglingLineWithXNode(network, virtualHub.nodeName());
        if (danglingLine.isPresent() && !danglingLine.get().isPaired()) {
            return danglingLine.get();
        }

        OpenRaoLoggerProvider.BUSINESS_WARNS.warn("Virtual hub {} cannot be assigned on node {} as it was not found in the network", virtualHub.eic(), virtualHub.nodeName());
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
