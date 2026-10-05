/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.optimization.timecoupled.reports;

import com.powsybl.commons.report.ReportNode;

import java.time.OffsetDateTime;

import static com.powsybl.commons.report.TypedValue.INFO_SEVERITY;
import static com.powsybl.commons.report.TypedValue.WARN_SEVERITY;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_LOGS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;

/**
 * Reports of the {@code TimeCoupledLinearRao} provider.
 *
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class TimeCoupledLinearRaoReports {

    private TimeCoupledLinearRaoReports() {
        // Utility class should not be instantiated
    }

    public static void reportTimeCoupledLinearRaoNetworkActionsIgnored(final ReportNode parentNode, final OffsetDateTime timestamp, final int nbNetworkActions) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportTimeCoupledLinearRaoNetworkActionsIgnored")
            .withUntypedValue("timestamp", timestamp.toString())
            .withUntypedValue("nbNetworkActions", nbNetworkActions)
            .withSeverity(WARN_SEVERITY)
            .add();

        BUSINESS_WARNS.warn("[TIME-COUPLED LINEAR RAO] The CRAC of timestamp {} contains {} network action(s) which will not be taken into account: only range actions are optimized",
            timestamp, nbNetworkActions);
    }

    public static ReportNode reportTimeCoupledLinearRaoRunningInitialSensiAnalyses(final ReportNode parentNode) {
        final ReportNode addedNode = parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportTimeCoupledLinearRaoRunningInitialSensiAnalyses")
            .withSeverity(INFO_SEVERITY)
            .add();

        BUSINESS_LOGS.info("[TIME-COUPLED LINEAR RAO] Running initial sensitivity analyses");

        return addedNode;
    }

    public static ReportNode reportTimeCoupledLinearRaoGlobalOptimization(final ReportNode parentNode) {
        final ReportNode addedNode = parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportTimeCoupledLinearRaoGlobalOptimization")
            .withSeverity(INFO_SEVERITY)
            .add();

        BUSINESS_LOGS.info("[TIME-COUPLED LINEAR RAO] Global time-coupled linear optimization of all range actions");

        return addedNode;
    }
}
