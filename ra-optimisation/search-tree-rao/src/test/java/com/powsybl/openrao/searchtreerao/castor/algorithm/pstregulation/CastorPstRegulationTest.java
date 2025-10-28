/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm.pstregulation;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.commons.logs.RaoBusinessLogs;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.impl.PostPerimeterResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        Instant curativeInstant = crac.getLastInstant();

        PstRangeAction pst12 = crac.getPstRangeAction("pstFr12");
        PstRangeAction pst34 = crac.getPstRangeAction("pstFr34");

        // mock optimization result to give the same values as RAO result
        Map<State, PostPerimeterResult> postContingencyResults = new HashMap<>();
        crac.getStates(curativeInstant).forEach(
            curativeState -> {
                OptimizationResult optimizationResult = Mockito.mock(OptimizationResult.class);
                crac.getFlowCnecs(curativeState).forEach(
                    flowCnec -> Mockito.when(optimizationResult.getMargin(flowCnec, Unit.AMPERE)).thenReturn(raoResult.getMargin(curativeInstant, flowCnec, Unit.AMPERE))
                );

                PostPerimeterResult postPerimeterResult = Mockito.mock(PostPerimeterResult.class);
                Mockito.when(postPerimeterResult.getOptimizationResult()).thenReturn(optimizationResult);

                postContingencyResults.put(curativeState, postPerimeterResult);
            }
        );

        ListAppender<ILoggingEvent> listAppender = getBusinessLogs();
        List<ILoggingEvent> logsList = listAppender.list;

        Map<String, String> pstsToRegulate = Map.of("FFR1AA1  FFR2AA1  2", "FFR1AA1  FFR2AA1  2", "FFR2AA1  FFR3AA1  2", "FFR2AA1  FFR3AA1  2", "FFR3AA1  FFR4AA1  2", "FFR3AA1  FFR4AA1  2");
        Set<PstRegulationResult> pstRegulationResults = CastorPstRegulation.regulatePsts(pstsToRegulate, postContingencyResults, network, crac, raoParameters, raoResult);
        List<String> logMessages = logsList.stream().map(ILoggingEvent::getFormattedMessage).sorted().toList();

        assertEquals("2 PST(s) to regulate: pstFr12, pstFr34", logMessages.get(0));
        assertEquals("3 contingency scenario(s) to regulate: Contingency FR 12, Contingency FR 23, Contingency FR 34", logMessages.get(1));

        // PST FR2-FR3 is only preventive so it cannot be regulated
        assertEquals("PST FFR2AA1  FFR3AA1  2 cannot be regulated as no curative PST range action was defined for it.", logMessages.get(5));

        assertEquals(3, pstRegulationResults.size());

        // Contingency FR1-FR2
        PstRegulationResult pstRegulationResultCoFr12 = getPstRegulationResultForGivenContingency(pstRegulationResults, "Contingency FR 12");
        assertEquals(Map.of(pst12, -15, pst34, -5), pstRegulationResultCoFr12.regulatedTapPerPst());
        assertEquals("FlowCNEC 'cnecFr34PstCurative - Co12' of contingency scenario 'Contingency FR 12' is overloaded and is the most limiting element, PST regulation has been triggered: pstFr12 (-10 -> -15), pstFr34 (0 -> -5)", logMessages.get(4));

        // Contingency FR2-FR3
        PstRegulationResult pstRegulationResultCoFr23 = getPstRegulationResultForGivenContingency(pstRegulationResults, "Contingency FR 23");
        assertEquals(Map.of(pst12, -5, pst34, -5), pstRegulationResultCoFr23.regulatedTapPerPst());
        assertEquals("FlowCNEC 'cnecFr23PstCurative - Co23' of contingency scenario 'Contingency FR 23' is overloaded and is the most limiting element, PST regulation has been triggered: pstFr12 (0 -> -5), pstFr34 (0 -> -5)", logMessages.get(3));

        // Contingency FR3-FR4
        PstRegulationResult pstRegulationResultCoFr34 = getPstRegulationResultForGivenContingency(pstRegulationResults, "Contingency FR 34");
        assertEquals(Map.of(pst12, -5, pst34, -15), pstRegulationResultCoFr34.regulatedTapPerPst());
        assertEquals("FlowCNEC 'cnecFr12PstCurative - Co34' of contingency scenario 'Contingency FR 34' is overloaded and is the most limiting element, PST regulation has been triggered: pstFr12 (0 -> -5)", logMessages.get(2));
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
