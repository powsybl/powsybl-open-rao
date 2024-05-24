/*
 *  Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.glsk.virtual.hubs;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.virtualhubs.MarketArea;
import com.powsybl.openrao.virtualhubs.VirtualHub;
import com.powsybl.openrao.virtualhubs.VirtualHubsConfiguration;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class GlskVirtualHubsTest {

    private static final double DOUBLE_TOLERANCE = 1e-3;
    private Network network;
    private VirtualHubsConfiguration virtualHubsConfiguration;

    private static ReportNode buildNewRootNode() {
        return ReportNode.newRootReportNode().withMessageTemplate("Test report node", "This is a parent report node for report tests").build();
    }

    @BeforeEach
    private void setUp() {
        String networkFileName = "network_with_virtual_hubs.xiidm";
        network = Network.read(networkFileName, getClass().getResourceAsStream("/" + networkFileName));

        virtualHubsConfiguration = new VirtualHubsConfiguration();
        MarketArea frMarketArea = new MarketArea("FR", "10YFR-RTE------C", true, false);
        virtualHubsConfiguration.addMarketArea(frMarketArea);
        virtualHubsConfiguration.addVirtualHub(new VirtualHub("code1", "17YXTYUDHGKAAAAS", true, false, "X_GBFR1 ", frMarketArea, null));
        virtualHubsConfiguration.addVirtualHub(new VirtualHub("code2", "15XGDYRHKLKAAAAS", false, false, "NNL3AA1 ", frMarketArea, null));
    }

    @Test
    void testGetVirtualHubsOk() throws IOException, URISyntaxException {
        List<String> virtualHubEiCodes = Arrays.asList("17YXTYUDHGKAAAAS", "15XGDYRHKLKAAAAS");
        ReportNode reportNode = buildNewRootNode();
        ZonalData<SensitivityVariableSet> glsks = GlskVirtualHubs.getVirtualHubGlsks(virtualHubsConfiguration, network, virtualHubEiCodes, reportNode);

        assertEquals(2, glsks.getDataPerZone().size());

        // check data for virtual hub on generator
        assertNotNull(glsks.getData("15XGDYRHKLKAAAAS"));
        assertEquals(1, glsks.getData("15XGDYRHKLKAAAAS").getVariables().size());
        assertTrue(glsks.getData("15XGDYRHKLKAAAAS").getVariablesById().containsKey("NNL3AA1 _load"));
        assertEquals(1., glsks.getData("15XGDYRHKLKAAAAS").getVariablesById().get("NNL3AA1 _load").getWeight(), DOUBLE_TOLERANCE);

        // check data for virtual hub on dangling line
        assertNotNull(glsks.getData("17YXTYUDHGKAAAAS"));
        assertEquals(1, glsks.getData("17YXTYUDHGKAAAAS").getVariables().size());
        assertTrue(glsks.getData("17YXTYUDHGKAAAAS").getVariablesById().containsKey("FFR1AA1  X_GBFR1  1"));
        assertEquals(1., glsks.getData("17YXTYUDHGKAAAAS").getVariablesById().get("FFR1AA1  X_GBFR1  1").getWeight(), DOUBLE_TOLERANCE);

        String expected = Files.readString(Path.of(getClass().getResource("/reports/expectedReportNodeContentVirtualHubsOk.txt").toURI()));
        try (StringWriter writer = new StringWriter()) {
            reportNode.print(writer);
            String actual = writer.toString();
            assertEquals(expected, actual);
        }
    }

    @Test
    void testGetVirtualHubsNotFound() throws IOException, URISyntaxException {
        ReportNode reportNode = buildNewRootNode();

        List<String> virtualHubEiCodes = Collections.singletonList("UNKNOWN_EICODE");
        ZonalData<SensitivityVariableSet> glsks = GlskVirtualHubs.getVirtualHubGlsks(virtualHubsConfiguration, network, virtualHubEiCodes, reportNode);
        assertEquals(0, glsks.getDataPerZone().size());

        String expected = Files.readString(Path.of(getClass().getResource("/reports/expectedReportNodeContentVirtualHubsNotFound.txt").toURI()));
        try (StringWriter writer = new StringWriter()) {
            reportNode.print(writer);
            String actual = writer.toString();
            assertEquals(expected, actual);
        }
    }
}
