/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craccreation.creator.api;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;

import java.util.ArrayList;
import java.util.List;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.*;

/**
 * Common methods used in CRAC creation reports
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class CracCreationReport {
    private final List<String> creationReport;

    public CracCreationReport() {
        creationReport = new ArrayList<>();
    }

    public CracCreationReport(CracCreationReport toCopy) {
        this.creationReport = new ArrayList<>(toCopy.creationReport);
    }

    public void error(String errorReason, ReportNode reportNode) {
        String message = String.format("[ERROR] %s", errorReason);
        creationReport.add(message);
        reportNode.newReportNode()
                .withMessageTemplate("cracCreationError", "[ERROR] ${reason}")
                .withUntypedValue("reason", errorReason)
                .withSeverity(TypedValue.ERROR_SEVERITY)
                .add();
        BUSINESS_LOGS.error(message);
    }

    public void removed(String removedReason, ReportNode reportNode) {
        String message = String.format("[REMOVED] %s", removedReason);
        creationReport.add(message);
        reportNode.newReportNode()
                .withMessageTemplate("cracCreationRemoved", "[REMOVED] ${removedReason}")
                .withUntypedValue("removedReason", removedReason)
                .withSeverity(TypedValue.WARN_SEVERITY)
                .add();
        BUSINESS_WARNS.warn(message);
    }

    public void added(String addedReason, ReportNode reportNode) {
        String message = String.format("[ADDED] %s", addedReason);
        creationReport.add(message);
        reportNode.newReportNode()
                .withMessageTemplate("cracCreationAdded", "[ADDED] ${addedReason}")
                .withUntypedValue("addedReason", addedReason)
                .withSeverity(TypedValue.WARN_SEVERITY)
                .add();
        BUSINESS_WARNS.warn(message);
    }

    public void altered(String alteredReason, ReportNode reportNode) {
        String message = String.format("[ALTERED] %s", alteredReason);
        creationReport.add(message);
        reportNode.newReportNode()
                .withMessageTemplate("cracCreationAltered", "[ALTERED] ${alteredReason}")
                .withUntypedValue("alteredReason", alteredReason)
                .withSeverity(TypedValue.WARN_SEVERITY)
                .add();
        BUSINESS_WARNS.warn(message);
    }

    public void warn(String warnReason, ReportNode reportNode) {
        String message = String.format("[WARN] %s", warnReason);
        creationReport.add(message);
        reportNode.newReportNode()
                .withMessageTemplate("cracCreationWarn", "[WARN] ${warnReason}")
                .withUntypedValue("warnReason", warnReason)
                .withSeverity(TypedValue.WARN_SEVERITY)
                .add();
        BUSINESS_WARNS.warn(message);
    }

    public void info(String infoReason, ReportNode reportNode) {
        String message = String.format("[INFO] %s", infoReason);
        creationReport.add(message);
        reportNode.newReportNode()
                .withMessageTemplate("cracCreationInfo", "[INFO] ${infoReason}")
                .withUntypedValue("infoReason", infoReason)
                .withSeverity(TypedValue.WARN_SEVERITY)
                .add();
        TECHNICAL_LOGS.info(message);
    }

    public void printCreationReport() {
        creationReport.forEach(format -> BUSINESS_LOGS.info(format));
    }

    // TODO remove this :)
    public List<String> getReport() {
        return creationReport;
    }

    // TODO remove this :)
    public String toString() {
        return String.join("\n", creationReport);
    }
}
