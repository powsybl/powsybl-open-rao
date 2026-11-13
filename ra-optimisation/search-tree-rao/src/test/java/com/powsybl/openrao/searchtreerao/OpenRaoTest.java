/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.TimeCoupledRaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.FastRaoParameters;
import com.powsybl.openrao.searchtreerao.castor.algorithm.Castor;
import com.powsybl.openrao.searchtreerao.fastrao.FastRao;
import com.powsybl.openrao.searchtreerao.marmot.Marmot;
import com.powsybl.openrao.searchtreerao.reports.ReportsTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class OpenRaoTest {
    private ListAppender<ILoggingEvent> technicalLogs;
    private final RaoResult raoResult = mock(RaoResult.class);

    @BeforeEach
    void setUp() {
        technicalLogs = ReportsTestUtils.getTechnicalLogs();
    }

    @AfterEach
    void tearDown() {
        technicalLogs.stop();
    }

    private RaoInput mockRaoInput(int numberOfCurativeInstants) {
        Crac crac = mock(Crac.class);
        SortedSet<Instant> curativeInstants = mock(SortedSet.class);
        when(curativeInstants.size()).thenReturn(numberOfCurativeInstants);
        when(crac.getInstants(InstantKind.CURATIVE)).thenReturn(curativeInstants);
        RaoInput raoInput = mock(RaoInput.class);
        when(raoInput.getCrac()).thenReturn(crac);
        return raoInput;
    }

    private void assertLastLog(String expectedMessage) {
        List<ILoggingEvent> logs = technicalLogs.list;
        assertTrue(logs.stream().anyMatch(log -> log.getFormattedMessage().equals(expectedMessage)));
    }

    @Test
    void testTimeCoupledInputUsesMarmot() {
        RaoResult timeCoupledRaoResult = mock(RaoResult.class);
        try (MockedConstruction<Marmot> marmot = mockConstruction(Marmot.class,
            (m, context) -> when(m.run(any(TimeCoupledRaoInput.class), any(), any())).thenReturn(CompletableFuture.completedFuture(timeCoupledRaoResult)))) {
            RaoResult result = new OpenRao().run(mock(TimeCoupledRaoInput.class), new RaoParameters(ReportNode.NO_OP), ReportNode.NO_OP).join();
            assertSame(timeCoupledRaoResult, result);
            assertEquals(1, marmot.constructed().size());
        }
        assertLastLog("Time-coupled situation detected; using MARMOT algorithm as the computation engine");
    }

    @Test
    void testMultiCurativeUsesCastor() {
        RaoParameters parameters = new RaoParameters(ReportNode.NO_OP);
        parameters.addExtension(FastRaoParameters.class, new FastRaoParameters());
        try (MockedConstruction<Castor> castor = mockConstruction(Castor.class,
                (m, context) -> when(m.run(any(RaoInput.class), any(), any(), any())).thenReturn(CompletableFuture.completedFuture(raoResult)));
             MockedConstruction<FastRao> fastRao = mockConstruction(FastRao.class)) {
            assertSame(raoResult, new OpenRao().run(mockRaoInput(2), parameters, ReportNode.NO_OP).join());
            assertEquals(1, castor.constructed().size());
            assertEquals(0, fastRao.constructed().size());
        }
        assertLastLog("Multi-curative situation detected; using CASTOR algorithm as the computation engine");
    }

    @Test
    void testFastRaoParametersUsesFastRao() {
        RaoParameters parameters = new RaoParameters(ReportNode.NO_OP);
        parameters.addExtension(FastRaoParameters.class, new FastRaoParameters());
        try (MockedConstruction<FastRao> fastRao = mockConstruction(FastRao.class,
                (m, context) -> when(m.run(any(RaoInput.class), any(), any(), any())).thenReturn(CompletableFuture.completedFuture(raoResult)));
             MockedConstruction<Castor> castor = mockConstruction(Castor.class)) {
            assertSame(raoResult, new OpenRao().run(mockRaoInput(1), parameters, ReportNode.NO_OP).join());
            assertEquals(1, fastRao.constructed().size());
            assertEquals(0, castor.constructed().size());
        }
        assertLastLog("FastRAO parameters extension found; using FastRAO algorithm as the computation engine");
    }

    @Test
    void testDefaultUsesCastor() {
        try (MockedConstruction<Castor> castor = mockConstruction(Castor.class,
                (m, context) -> when(m.run(any(RaoInput.class), any(), any(), any())).thenReturn(CompletableFuture.completedFuture(raoResult)));
             MockedConstruction<FastRao> fastRao = mockConstruction(FastRao.class)) {
            assertSame(raoResult, new OpenRao().run(mockRaoInput(1), new RaoParameters(ReportNode.NO_OP), ReportNode.NO_OP).join());
            assertEquals(1, castor.constructed().size());
            assertEquals(0, fastRao.constructed().size());
        }
        assertLastLog("Using CASTOR (search-tree RAO) algorithm as the computation engine");
    }
}
