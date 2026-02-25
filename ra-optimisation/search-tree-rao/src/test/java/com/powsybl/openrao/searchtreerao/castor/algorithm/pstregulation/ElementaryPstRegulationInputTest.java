/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm.pstregulation;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class ElementaryPstRegulationInputTest {
    @Test
    void testRetrieveLimitingThresholdInCrac() throws IOException {
        Network network = Network.read("2Nodes4ParallelLines3PSTs.uct", getClass().getResourceAsStream("/network/2Nodes4ParallelLines3PSTs.uct"));
        Crac crac = Crac.read("crac-3-psts.json", getClass().getResourceAsStream("/crac/crac-3-psts.json"), network);
        State state = crac.getState("Contingency BE1 FR1 1", crac.getInstant(InstantKind.CURATIVE));

        // thresholds are properly defined on both sides
        PstRangeAction pstBeFr2 = crac.getPstRangeAction("pstBeFr2");
        ElementaryPstRegulationInput pst1RegulationInput = ElementaryPstRegulationInput.of(pstBeFr2, "BBE1AA1  FFR1AA1  2", state, crac, network);
        assertNotNull(pst1RegulationInput);
        assertEquals(TwoSides.TWO, pst1RegulationInput.limitingSide());
        assertEquals(500.0, pst1RegulationInput.limitingThreshold());

        // thresholds are defined only one side 1
        PstRangeAction pstBeFr3 = crac.getPstRangeAction("pstBeFr3");
        ElementaryPstRegulationInput pst2RegulationInput = ElementaryPstRegulationInput.of(pstBeFr3, "BBE1AA1  FFR1AA1  3", state, crac, network);
        assertNotNull(pst2RegulationInput);
        assertEquals(TwoSides.ONE, pst2RegulationInput.limitingSide());
        assertEquals(250.0, pst2RegulationInput.limitingThreshold());

        // no FlowCNEC defined
        PstRangeAction pstBeFr4 = crac.getPstRangeAction("pstBeFr4");
        assertNull(ElementaryPstRegulationInput.of(pstBeFr4, "BBE1AA1  FFR1AA1  4", state, crac, network));
    }

    @Test
    void testRetrieveLimitingThresholdInCracForLineInSeries() throws IOException {
        Network network = Network.read("2Nodes4ParallelLines3PSTs.uct", getClass().getResourceAsStream("/network/2Nodes4ParallelLines3PSTs.uct"));
        Crac crac = Crac.read("crac-3-psts-monitored-line-in-series.json", getClass().getResourceAsStream("/crac/crac-3-psts-monitored-line-in-series.json"), network);
        State state = crac.getState("Contingency BE1 FR1 2", crac.getInstant(InstantKind.CURATIVE));

        // common terminal is on side 1 of both the line and the PST so only the thresholds on this side are used
        PstRangeAction pstBeFr2 = crac.getPstRangeAction("pstBeFr2");
        ElementaryPstRegulationInput pst1RegulationInput = ElementaryPstRegulationInput.of(pstBeFr2, "BBE1AA1  FFR1AA1  1", state, crac, network);
        assertNotNull(pst1RegulationInput);
        assertEquals(TwoSides.ONE, pst1RegulationInput.limitingSide());
        assertEquals(800.0, pst1RegulationInput.limitingThreshold());

        // common terminal is on side 1 of both the line and the PST so only the thresholds on this side are used
        PstRangeAction pstBeFr3 = crac.getPstRangeAction("pstBeFr3");
        ElementaryPstRegulationInput pst2RegulationInput = ElementaryPstRegulationInput.of(pstBeFr3, "BBE1AA1  FFR1AA1  1", state, crac, network);
        assertNotNull(pst2RegulationInput);
        assertEquals(TwoSides.ONE, pst2RegulationInput.limitingSide());
        assertEquals(800.0, pst2RegulationInput.limitingThreshold());
    }
}
