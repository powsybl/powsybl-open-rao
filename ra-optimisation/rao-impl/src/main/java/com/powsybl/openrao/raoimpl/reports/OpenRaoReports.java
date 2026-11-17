/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoimpl.reports;

import com.powsybl.commons.report.ReportNode;
import static com.powsybl.commons.report.TypedValue.INFO_SEVERITY;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

/**
 * Reports of the {@code OpenRao} provider.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class OpenRaoReports {
    private OpenRaoReports() {
        // Utility class should not be instantiated
    }

    public static void reportTimeCoupledSituationDetected(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.raoimpl.reportTimeCoupledSituationDetected")
            .withSeverity(INFO_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("Time-coupled situation detected; using MARMOT algorithm as the computation engine");
    }

    public static void reportMultiCurativeSituationDetected(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.raoimpl.reportMultiCurativeSituationDetected")
            .withSeverity(INFO_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("Multi-curative situation detected; using CASTOR algorithm as the computation engine");
    }

    public static void reportCastorAlgorithmSelected(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.raoimpl.reportCastorAlgorithmSelected")
            .withSeverity(INFO_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("Using CASTOR (search-tree RAO) algorithm as the computation engine");
    }

    public static void reportFastRaoAlgorithmSelected(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.raoimpl.reportFastRaoAlgorithmSelected")
            .withSeverity(INFO_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("FastRAO parameters extension found; using FastRAO algorithm as the computation engine");
    }
}
