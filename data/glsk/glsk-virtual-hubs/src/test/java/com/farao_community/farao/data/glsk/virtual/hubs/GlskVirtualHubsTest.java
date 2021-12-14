/*
 *  Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.virtual.hubs;

import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class GlskVirtualHubsTest {

    private static final double DOUBLE_TOLERANCE = 1e-3;

    @Test
    public void testGetVirtualHubsOk() {
        String networkFileName = "network_with_virtual_hubs.xiidm";
        Network network = Importers.loadNetwork(networkFileName, getClass().getResourceAsStream("/" + networkFileName));
        List<String> virtualHubEiCodes = Arrays.asList("17YXTYUDHGKAAAAS", "15XGDYRHKLKAAAAS");

        ZonalData<LinearGlsk> glsks = GlskVirtualHubs.getVirtualHubGlsks(network, virtualHubEiCodes);

        assertEquals(2, glsks.getDataPerZone().size());

        // check data for virtual hub on generator
        assertNotNull(glsks.getData("15XGDYRHKLKAAAAS"));
        assertEquals(1, glsks.getData("15XGDYRHKLKAAAAS").getGLSKs().size());
        assertTrue(glsks.getData("15XGDYRHKLKAAAAS").getGLSKs().containsKey("NNL3AA1 _load"));
        assertEquals(1., glsks.getData("15XGDYRHKLKAAAAS").getGLSKs().get("NNL3AA1 _load").doubleValue(), DOUBLE_TOLERANCE);

        // check data for virtual hub on dangling line
        assertNotNull(glsks.getData("17YXTYUDHGKAAAAS"));
        assertEquals(1, glsks.getData("17YXTYUDHGKAAAAS").getGLSKs().size());
        assertTrue(glsks.getData("17YXTYUDHGKAAAAS").getGLSKs().containsKey("FFR1AA1 _load"));
        assertEquals(1., glsks.getData("17YXTYUDHGKAAAAS").getGLSKs().get("FFR1AA1 _load").doubleValue(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetVirtualHubsNotFound() {
        String networkFileName = "network_with_virtual_hubs.xiidm";
        Network network = Importers.loadNetwork(networkFileName, getClass().getResourceAsStream("/" + networkFileName));
        List<String> virtualHubEiCodes = Collections.singletonList("UNKNOWN_EICODE");

        ZonalData<LinearGlsk> glsks = GlskVirtualHubs.getVirtualHubGlsks(network, virtualHubEiCodes);

        assertEquals(0, glsks.getDataPerZone().size());
    }
}
