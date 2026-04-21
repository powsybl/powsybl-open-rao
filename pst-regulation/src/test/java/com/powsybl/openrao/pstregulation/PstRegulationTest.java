/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.pstregulation;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.commons.logs.RaoBusinessLogs;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoPstRegulationParameters;
import com.powsybl.openrao.searchtreerao.castor.algorithm.CastorFullOptimization;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class PstRegulationTest {
    @Test
    void testPstRegulationWithSeveralContingencyScenarios() throws IOException {
        Network network = Network.read("4NodesSeries.uct", getClass().getResourceAsStream("/network/4NodesSeries.uct"));
        network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), "InitialScenario");
        network.getVariantManager().setWorkingVariant("InitialScenario");

        Crac crac = Crac.read("crac-for-regulation.json", getClass().getResourceAsStream("/crac/crac-for-regulation.json"), network);
        RaoResult raoResult = RaoResult.read(getClass().getResourceAsStream("/raoResult/raoResultPreRegulation.json"), crac);
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_ac_3pstsRegulation.json"));

        PstRangeAction pst12 = crac.getPstRangeAction("pstFr12");
        PstRangeAction pst34 = crac.getPstRangeAction("pstFr34");

        ListAppender<ILoggingEvent> listAppender = getBusinessLogs();
        List<ILoggingEvent> logsList = listAppender.list;

        RaoResult raoResultWithPstRegulation = PstRegulation.regulatePsts(network, crac, raoResult, raoParameters);
        List<String> logMessages = logsList.stream().map(ILoggingEvent::getFormattedMessage).sorted().toList();

        assertEquals("2 PST(s) to regulate: pstFr12, pstFr34", logMessages.get(0));
        assertEquals("3 contingency scenario(s) to regulate: Contingency FR 12, Contingency FR 23, Contingency FR 34", logMessages.get(1));

        // PST FR2-FR3 is only preventive so it cannot be regulated
        assertEquals("PST FFR2AA1  FFR3AA1  2 cannot be regulated as no curative PST range action was defined for it.", logMessages.get(5));

        // Contingency FR1-FR2
        assertEquals(-15, raoResultWithPstRegulation.getOptimizedTapOnState(crac.getState("Contingency FR 12", crac.getLastInstant()), pst12));
        assertEquals(-5, raoResultWithPstRegulation.getOptimizedTapOnState(crac.getState("Contingency FR 12", crac.getLastInstant()), pst34));
        assertEquals(
            "FlowCNEC 'cnecFr34PstCurative - Co12' of contingency scenario 'Contingency FR 12' is overloaded and is the most limiting element, " +
                "PST regulation has been triggered: pstFr12 (-10 -> -15), pstFr34 (0 -> -5)",
            logMessages.get(4)
        );

        // Contingency FR2-FR3
        assertEquals(-5, raoResultWithPstRegulation.getOptimizedTapOnState(crac.getState("Contingency FR 23", crac.getLastInstant()), pst12));
        assertEquals(-5, raoResultWithPstRegulation.getOptimizedTapOnState(crac.getState("Contingency FR 23", crac.getLastInstant()), pst34));
        assertEquals(
            "FlowCNEC 'cnecFr23PstCurative - Co23' of contingency scenario 'Contingency FR 23' is overloaded and is the most limiting element, " +
                "PST regulation has been triggered: pstFr12 (0 -> -5), pstFr34 (0 -> -5)",
            logMessages.get(3)
        );

        // Contingency FR3-FR4
        assertEquals(-5, raoResultWithPstRegulation.getOptimizedTapOnState(crac.getState("Contingency FR 34", crac.getLastInstant()), pst12));
        assertEquals(-15, raoResultWithPstRegulation.getOptimizedTapOnState(crac.getState("Contingency FR 34", crac.getLastInstant()), pst34));
        assertEquals(
            "FlowCNEC 'cnecFr12PstCurative - Co34' of contingency scenario 'Contingency FR 34' is overloaded and is the most limiting element, " +
                "PST regulation has been triggered: pstFr12 (0 -> -5)",
            logMessages.get(2)
        );
    }

    private static ListAppender<ILoggingEvent> getBusinessLogs() {
        Logger logger = (Logger) LoggerFactory.getLogger(RaoBusinessLogs.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        return listAppender;
    }

    @Test
    void testPstRegulationAtTheEndOfRao() throws IOException {
        final Network network = Network.read("2Nodes3ParallelLinesPST.uct", getClass().getResourceAsStream("/network/2Nodes3ParallelLinesPST.uct"));
        final Crac crac = Crac.read("crac-regulation-1-PST.json", getClass().getResourceAsStream("/crac/crac-regulation-1-PST.json"), network);
        final RaoInput raoInput = RaoInput.build(network, crac).build();
        final RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_minMargin_ac.json"));

        final Instant curativeInstant = crac.getInstant(InstantKind.CURATIVE);
        final State curativeState = crac.getState("Contingency BE1 FR1 3", curativeInstant);

        final PstRangeAction pstRangeAction = crac.getPstRangeAction("pstBeFr2");
        final FlowCnec curativeCnecOnLine = crac.getFlowCnec("cnecBeFr1Curative");
        final FlowCnec curativeCnecOnPst = crac.getFlowCnec("cnecBeFr2Curative");

        // first run without regulation: min margin is maximized by setting PST on tap -2 even though PSt is overloaded
        // but not seen by the RAO because it has no associated FlowCNEC
        final RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals(690.23, raoResult.getCost(crac.getLastInstant()), 1e-2);
        assertEquals(-2, raoResult.getOptimizedTapOnState(curativeState, pstRangeAction));
        assertEquals(-676.38, raoResult.getMargin(curativeInstant, curativeCnecOnLine, Unit.AMPERE), 1e-2);
        assertEquals(-690.23, raoResult.getMargin(curativeInstant, curativeCnecOnPst, Unit.AMPERE), 1e-2);

        // second run with regulation: regulation shifts PST's tap to position 7 to remove the overload but worsens min margin
        final SearchTreeRaoPstRegulationParameters pstRegulationParameters = new SearchTreeRaoPstRegulationParameters();
        pstRegulationParameters.setPstsToRegulate(Map.of("BBE1AA1  FFR1AA1  2", "BBE1AA1  FFR1AA1  2"));
        raoParameters.getExtension(OpenRaoSearchTreeParameters.class).setPstRegulationParameters(pstRegulationParameters);

        final RaoResult raoResultWithRegulation = PstRegulation.regulatePsts(network, crac, raoResult, raoParameters);
        assertEquals(1382.77, raoResultWithRegulation.getCost(crac.getLastInstant()), 1e-2);
        assertEquals(7, raoResultWithRegulation.getOptimizedTapOnState(curativeState, pstRangeAction));
        assertEquals(-1382.77, raoResultWithRegulation.getMargin(curativeInstant, curativeCnecOnLine, Unit.AMPERE), 1e-2);
        assertEquals(15.49, raoResultWithRegulation.getMargin(curativeInstant, curativeCnecOnPst, Unit.AMPERE), 1e-2);
    }
}
