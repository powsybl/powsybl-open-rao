/*
 *  Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.data.glsk.virtual.hubs;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.commons.ZonalDataImpl;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgramArea;
import com.farao_community.farao.virtual_hubs.network_extension.AssignedVirtualHub;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public final class GlskVirtualHubs {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlskVirtualHubs.class);

    private GlskVirtualHubs() {
    }

    public ZonalData<LinearGlsk> getGlskFromVirtualHubs(Network network, ReferenceProgram referenceProgram) {
        List<String> countryCodes = referenceProgram.getListOfAreas().stream()
            .filter(ReferenceProgramArea::isVirtualHub)
            .map(ReferenceProgramArea::getAreaCode)
            .collect(Collectors.toList());
        return getGlskFromVirtualHubs(network, countryCodes);
    }

    public ZonalData<LinearGlsk> getGlskFromVirtualHubs(Network network, List<String> eiCodes) {
        //List<LinearGlsk> virtualHubGlsks = new ArrayList<>();
        // Extract from the referenceExchangeDataList the ones that are described in the virtualhubs
        // List<ReferenceExchangeData> referenceExchangesFromVirtualHubs;
        Map<String, LinearGlsk> glsks = new HashMap<>();
        List<Injection<?>> injectionsWithVirtualHubs = getInjectionsWithVirtualHubs(network);
        eiCodes.forEach(eiCode -> {
            Optional<Injection<?>> virtualHubInjection = identifyVirtualHub(eiCode, injectionsWithVirtualHubs);
            if (virtualHubInjection.isPresent()) {
                Optional<LinearGlsk> virtualHubGlsk = createGlskFromVirtualHub(virtualHubInjection.get());
                if (virtualHubGlsk.isPresent()) {
                    glsks.put(eiCode, virtualHubGlsk.get()
                    );
                }
            }
            }
        );
        //danglingLinesWithVirtualHubs.forEach(danglingLine -> handleVirtualHubGlsk(virtualHubGlsks, referenceExchangesFromVirtualHubs, danglingLine));
        //generatorsWithVirtualHubs.forEach(generator -> handleVirtualHubGlsk(virtualHubGlsks, referenceExchangesFromVirtualHubs, generator));
        return new ZonalDataImpl<>(glsks);
    }

    private List<Injection<?>> getInjectionsWithVirtualHubs(Network network) {
        List<Injection<?>> danglingLinesWithVirtualHubs = network.getDanglingLineStream()
            .filter(danglingLine -> danglingLine.getExtension(AssignedVirtualHub.class) != null)
            .collect(Collectors.toList());
        List<Injection<?>> generatorsWithVirtualHubs = network.getGeneratorStream()
            .filter(generator -> generator.getExtension(AssignedVirtualHub.class) != null)
            .collect(Collectors.toList());
        danglingLinesWithVirtualHubs.addAll(generatorsWithVirtualHubs);
        return danglingLinesWithVirtualHubs;
    }

    private Optional<Injection<?>> identifyVirtualHub(String eiCode, List<Injection<?>> injectionsWithVirtualHubs) {
        for (Injection<?> injection : injectionsWithVirtualHubs) {
            if (injection.getExtension(AssignedVirtualHub.class).getEic().equals(eiCode)) {
                return Optional.of(injection);
            }
        }
        LOGGER.warn("No injection found for virtual hub {}", eiCode);
        return Optional.empty();
    }

    private Optional<LinearGlsk> createGlskFromVirtualHub(Injection<?> injection) {
        Map<String, Float> glskMap = new HashMap<>();
        try {
            String glskId= getGlskId(injection);
            glskMap.put(glskId, 1.0F);
            String eiCode = injection.getExtension(AssignedVirtualHub.class).getEic();
            return Optional.of(new LinearGlsk(eiCode, eiCode, glskMap));
        } catch (FaraoException e) {
            return Optional.empty();
        }
    }

    private String getGlskId(Injection<?> virtualHub) {
        if (virtualHub instanceof Generator) {
            LOGGER.debug("Generator found for virtual hub {}", virtualHub.getId());
            return virtualHub.getId();
        } else {
            Optional<Generator> generator = virtualHub.getTerminal().getVoltageLevel().getGeneratorStream().findFirst();
            if (generator.isEmpty()) {
                String message = String.format("No generator found for virtual hub %s", virtualHub.getId());
                LOGGER.warn(message);
                throw new FaraoException(message);
            }
            return generator.get().getId();
        }
    }
}