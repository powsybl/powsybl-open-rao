/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class PstRegulationInputTest {
    @Test
    void testRetrieveLimitingThresholdInCrac() throws IOException {
        Network network = Network.read("2Nodes4ParallelLines3PSTs.uct", getClass().getResourceAsStream("/network/2Nodes4ParallelLines3PSTs.uct"));
        Crac crac = Crac.read("crac-3-psts.json", getClass().getResourceAsStream("/crac/crac-3-psts.json"), network);

        // thresholds are properly defined on both sides
        PstRangeAction pstBeFr2 = crac.getPstRangeAction("pstBeFr2");
        PstRegulationInput pst1RegulationInput = PstRegulationInput.of(pstBeFr2, "BBE1AA1  FFR1AA1  2", crac);
        assertNotNull(pst1RegulationInput);
        assertEquals(TwoSides.TWO, pst1RegulationInput.limitingSide());
        assertEquals(500.0, pst1RegulationInput.limitingThreshold());

        // thresholds are defined only one side 1
        PstRangeAction pstBeFr3 = crac.getPstRangeAction("pstBeFr3");
        PstRegulationInput pst2RegulationInput = PstRegulationInput.of(pstBeFr3, "BBE1AA1  FFR1AA1  3", crac);
        assertNotNull(pst2RegulationInput);
        assertEquals(TwoSides.ONE, pst2RegulationInput.limitingSide());
        assertEquals(250.0, pst2RegulationInput.limitingThreshold());

        // no FlowCNEC defined
        PstRangeAction pstBeFr4 = crac.getPstRangeAction("pstBeFr4");
        assertNull(PstRegulationInput.of(pstBeFr4, "BBE1AA1  FFR1AA1  4", crac));
    }
}
