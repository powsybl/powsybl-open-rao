/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.logs.RaoBusinessLogs;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

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

        ListAppender<ILoggingEvent> listAppender = getBusinessLogs();
        List<ILoggingEvent> logsList = listAppender.list;

        // TODO: if ran with new RaoParameters() instead => infinite loop (need to investigate which parameters are mandatory)
        List<String> pstsToRegulate = List.of("FFR1AA1  FFR2AA1  2", "FFR2AA1  FFR3AA1  2", "FFR3AA1  FFR4AA1  2");
        Set<PstRegulationResult> pstRegulationResults = CastorPstRegulation.regulatePsts(pstsToRegulate, network, crac, raoParameters, raoResult);
        List<String> logMessages = logsList.stream().map(ILoggingEvent::getFormattedMessage).sorted().toList();

        // PST FR2-FR3 is only preventive so it cannot be regulated
        assertEquals("PST FFR2AA1  FFR3AA1  2 cannot be regulated as no curative PST range action was defined for it.", logMessages.get(3));
        assertEquals(3, pstRegulationResults.size());

        // Contingency FR1-FR2: both curative PSTs are in abutment so no regulation is performed
        PstRegulationResult pstRegulationResultCoFr12 = getPstRegulationResultForGivenContingency(pstRegulationResults, "Contingency FR 12");
        assertTrue(pstRegulationResultCoFr12.regulatedTapPerPst().isEmpty());
        assertEquals("PST FFR1AA1  FFR2AA1  2 will not be regulated for contingency scenario Contingency FR 12 as it is in abutment.", logMessages.get(0));
        assertEquals("PST FFR3AA1  FFR4AA1  2 will not be regulated for contingency scenario Contingency FR 12 as it is in abutment.", logMessages.get(4));

        // Contingency FR2-FR3: both curative PSTs are in abutment so no regulation is performed
        PstRegulationResult pstRegulationResultCoFr23 = getPstRegulationResultForGivenContingency(pstRegulationResults, "Contingency FR 23");
        assertTrue(pstRegulationResultCoFr23.regulatedTapPerPst().isEmpty());
        assertEquals("PST FFR1AA1  FFR2AA1  2 will not be regulated for contingency scenario Contingency FR 23 as it is in abutment.", logMessages.get(1));
        assertEquals("PST FFR3AA1  FFR4AA1  2 will not be regulated for contingency scenario Contingency FR 23 as it is in abutment.", logMessages.get(5));

        // Contingency FR3-FR4: PST FR1-FR2 in abutment, but not FR3-FR4 thanks to auto shift thus it is moved to tap 3
        PstRegulationResult pstRegulationResultCoFr34 = getPstRegulationResultForGivenContingency(pstRegulationResults, "Contingency FR 34");
        assertEquals(Map.of(pst34, 3), pstRegulationResultCoFr34.regulatedTapPerPst());
        assertEquals("PST FFR1AA1  FFR2AA1  2 will not be regulated for contingency scenario Contingency FR 34 as it is in abutment.", logMessages.get(2));
        assertEquals("PST regulation for contingency scenario Contingency FR 34: pstFr34 (9 -> 3)", logMessages.get(6));
    }

    private static ListAppender<ILoggingEvent> getBusinessLogs() {
        Logger logger = (Logger) LoggerFactory.getLogger(RaoBusinessLogs.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        return listAppender;
    }

    private static PstRegulationResult getPstRegulationResultForGivenContingency(Set<PstRegulationResult> pstRegulationResults, String contingencyId) {
        return pstRegulationResults.stream().filter(pstRegulationResult -> contingencyId.equals(pstRegulationResult.contingency().getId())).findFirst().orElseThrow();
    }
}
