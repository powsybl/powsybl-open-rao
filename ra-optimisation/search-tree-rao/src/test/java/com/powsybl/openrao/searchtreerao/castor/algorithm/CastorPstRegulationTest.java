/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class CastorPstRegulationTest {
    @Test
    void testPstRegulationWithSeveralContingencyScenarios() throws IOException {
        Network network = Network.read("4NodesSeries.uct", getClass().getResourceAsStream("/network/4NodesSeries.uct"));
        Crac crac = Crac.read("crac-for-regulation.json", getClass().getResourceAsStream("/crac/crac-for-regulation.json"), network);
        RaoResult raoResult = RaoResult.read(getClass().getResourceAsStream("/raoResult/raoResultPreRegulation.json"), crac);
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_ac_3pstsRegulation.json"));

        PstRangeAction pst34 = crac.getPstRangeAction("pstFr34");

        // TODO: set up log appender for logs check
        // TODO: if ran with new RaoParameters() instead => infinite loop (need to investigate which parameters are mandatory)
        List<String> pstsToRegulate = List.of("FFR1AA1  FFR2AA1  2", "FFR2AA1  FFR3AA1  2", "FFR3AA1  FFR4AA1  2");
        Set<PstRegulationResult> pstRegulationResults = CastorPstRegulation.regulatePsts(pstsToRegulate, network, crac, raoParameters, raoResult);

        assertEquals(3, pstRegulationResults.size());

        // Contingency FR1-FR2
        PstRegulationResult pstRegulationResultCoFr12 = pstRegulationResults.stream().filter(pstRegulationResult -> "Contingency FR 12".equals(pstRegulationResult.contingency().getId())).findFirst().get();
        assertTrue(pstRegulationResultCoFr12.regulatedTapPerPst().isEmpty());

        // Contingency FR2-FR3
        PstRegulationResult pstRegulationResultCoFr23 = pstRegulationResults.stream().filter(pstRegulationResult -> "Contingency FR 23".equals(pstRegulationResult.contingency().getId())).findFirst().get();
        assertTrue(pstRegulationResultCoFr23.regulatedTapPerPst().isEmpty());

        // Contingency FR3-FR4
        PstRegulationResult pstRegulationResultCoFr34 = pstRegulationResults.stream().filter(pstRegulationResult -> "Contingency FR 34".equals(pstRegulationResult.contingency().getId())).findFirst().get();
        assertEquals(Map.of(pst34, 3), pstRegulationResultCoFr34.regulatedTapPerPst());
    }
}
