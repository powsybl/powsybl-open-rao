/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craccreation.creator.api;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.*;

/**
 * Common methods used in CRAC creation reports
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class CracCreationReport {
    private CracCreationReport() {
        // utility class
    }

    public static void error(String errorReason, ReportNode reportNode) {
        reportNode.newReportNode()
                .withMessageTemplate("cracCreationError", "[ERROR] ${reason}")
                .withUntypedValue("reason", errorReason)
                .withSeverity(TypedValue.ERROR_SEVERITY)
                .add();
        BUSINESS_LOGS.error("[ERROR] {}", errorReason);
    }

    public static void removed(String removedReason, ReportNode reportNode) {
        reportNode.newReportNode()
                .withMessageTemplate("cracCreationRemoved", "[REMOVED] ${removedReason}")
                .withUntypedValue("removedReason", removedReason)
                .withSeverity(TypedValue.WARN_SEVERITY)
                .add();
        BUSINESS_WARNS.warn("[REMOVED] {}", removedReason);
    }

    public static void added(String addedReason, ReportNode reportNode) {
        reportNode.newReportNode()
                .withMessageTemplate("cracCreationAdded", "[ADDED] ${addedReason}")
                .withUntypedValue("addedReason", addedReason)
                .withSeverity(TypedValue.WARN_SEVERITY)
                .add();
        BUSINESS_WARNS.warn("[ADDED] {}", addedReason);
    }

    public static void altered(String alteredReason, ReportNode reportNode) {
        reportNode.newReportNode()
                .withMessageTemplate("cracCreationAltered", "[ALTERED] ${alteredReason}")
                .withUntypedValue("alteredReason", alteredReason)
                .withSeverity(TypedValue.WARN_SEVERITY)
                .add();
        BUSINESS_WARNS.warn("[ALTERED] {}", alteredReason);
    }

    public static void warn(String warnReason, ReportNode reportNode) {
        reportNode.newReportNode()
                .withMessageTemplate("cracCreationWarn", "[WARN] ${warnReason}")
                .withUntypedValue("warnReason", warnReason)
                .withSeverity(TypedValue.WARN_SEVERITY)
                .add();
        BUSINESS_WARNS.warn("[WARN] {}", warnReason);
    }

    public static void info(String infoReason, ReportNode reportNode) {
        reportNode.newReportNode()
                .withMessageTemplate("cracCreationInfo", "[INFO] ${infoReason}")
                .withUntypedValue("infoReason", infoReason)
                .withSeverity(TypedValue.WARN_SEVERITY)
                .add();
        TECHNICAL_LOGS.info("[INFO] {}", infoReason);
    }
}
