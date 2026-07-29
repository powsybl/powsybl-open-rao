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
import com.powsybl.openrao.searchtreerao.marmot.Marmot;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
class MarmotReportsTest {

    @Test
    void testLogMarmotCnecs() {
        final ReportNode reportNode = ReportsTestUtils.getTestRootNode();
        final List<ILoggingEvent> technicalLogs = ReportsTestUtils.getTechnicalLogs().list;

        MarmotReports.reportMarmotCnecs(reportNode, List.of(
            new Marmot.LoggingAddedCnecs(
                OffsetDateTime.parse("2026-06-10T11:50:30Z"),
                "min-margin-violation-evaluator",
                List.of("cnec1", "cnec2"),
                Map.of("cnec1", 1., "cnec2", 2.)
            ),
            new Marmot.LoggingAddedCnecs(
                OffsetDateTime.parse("2026-06-10T18:00:00Z"),
                "random-vc-name",
                List.of("cnec3"),
                Map.of("cnec3", 3.)
            )
        ));

        final List<ReportNode> traceReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.TRACE_SEVERITY);
        assertEquals("[MARMOT] Proceeding to next iteration by adding 3 cnecs across 2 timestamps", traceReports.getFirst().getMessage());
        assertEquals("for timestamp 2026-06-10T11:50:30Z and virtual cost min-margin-violation-evaluator: cnec1(1.0),cnec2(2.0)", traceReports.getFirst().getChildren().get(0).getMessage());
        assertEquals("for timestamp 2026-06-10T18:00Z and virtual cost random-vc-name: cnec3", traceReports.getFirst().getChildren().get(1).getMessage());
        assertEquals("[INFO] [MARMOT] Proceeding to next iteration by adding: for timestamp 2026-06-10T11:50:30Z and virtual cost min-margin-violation-evaluator cnec1(1.0),cnec2(2.0), for timestamp 2026-06-10T18:00Z and virtual cost random-vc-name cnec3,", technicalLogs.getFirst().toString());
    }
}
