/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoimpl.reports;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class OpenRaoReportsTest {

    @Test
    void reportTimeCoupledSituationDetected() {
        final ReportNode reportNode = ReportsTestUtils.getTestRootNode();
        final List<ILoggingEvent> technicalLogs = ReportsTestUtils.getTechnicalLogs().list;

        OpenRaoReports.reportTimeCoupledSituationDetected(reportNode);

        final List<ReportNode> traceReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.INFO_SEVERITY);
        assertEquals("Time-coupled situation detected; using MARMOT algorithm as the computation engine", traceReports.getFirst().getMessage());
        assertEquals("Time-coupled situation detected; using MARMOT algorithm as the computation engine", technicalLogs.getFirst().getFormattedMessage());
    }

    @Test
    void reportMultiCurativeSituationDetected() {
        final ReportNode reportNode = ReportsTestUtils.getTestRootNode();
        final List<ILoggingEvent> technicalLogs = ReportsTestUtils.getTechnicalLogs().list;

        OpenRaoReports.reportMultiCurativeSituationDetected(reportNode);

        final List<ReportNode> traceReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.INFO_SEVERITY);
        assertEquals("Multi-curative situation detected; using CASTOR algorithm as the computation engine", traceReports.getFirst().getMessage());
        assertEquals("Multi-curative situation detected; using CASTOR algorithm as the computation engine", technicalLogs.getFirst().getFormattedMessage());
    }

    @Test
    void reportCastorAlgorithmSelected() {
        final ReportNode reportNode = ReportsTestUtils.getTestRootNode();
        final List<ILoggingEvent> technicalLogs = ReportsTestUtils.getTechnicalLogs().list;

        OpenRaoReports.reportCastorAlgorithmSelected(reportNode);

        final List<ReportNode> traceReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.INFO_SEVERITY);
        assertEquals("Using CASTOR (search-tree RAO) algorithm as the computation engine", traceReports.getFirst().getMessage());
        assertEquals("Using CASTOR (search-tree RAO) algorithm as the computation engine", technicalLogs.getFirst().getFormattedMessage());
    }

    @Test
    void reportFastRaoAlgorithmSelected() {
        final ReportNode reportNode = ReportsTestUtils.getTestRootNode();
        final List<ILoggingEvent> technicalLogs = ReportsTestUtils.getTechnicalLogs().list;

        OpenRaoReports.reportFastRaoAlgorithmSelected(reportNode);

        final List<ReportNode> traceReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.INFO_SEVERITY);
        assertEquals("FastRAO parameters extension found; using FastRAO algorithm as the computation engine", traceReports.getFirst().getMessage());
        assertEquals("FastRAO parameters extension found; using FastRAO algorithm as the computation engine", technicalLogs.getFirst().getFormattedMessage());
    }
}
