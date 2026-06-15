/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.reports;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
class LinearOptimizerReportsTest {

    @Test
    void testLogFastRaoInitialSensitivityAnalysisResults() {
        final ReportNode reportNode = ReportsTestUtils.getTestRootNode();
        final List<ILoggingEvent> technicalLogs = ReportsTestUtils.getTechnicalLogs().list;

        LinearOptimizerReports.reportLinearOptimFoundWorseResult(reportNode, 12, 1., 2., 3., 4.);

        final List<ReportNode> traceReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.TRACE_SEVERITY);
        assertEquals("Iteration 12: linear optimization found a worse result than best iteration, with a cost increasing from 1.00 to 2.00 (functional: from 3.00 to 4.00)", traceReports.getFirst().getMessage());
        assertEquals("[INFO] Iteration 12: linear optimization found a worse result than best iteration, with a cost increasing from 1.00 to 2.00 (functional: from 3.00 to 4.00)", technicalLogs.getFirst().toString());
    }
}
