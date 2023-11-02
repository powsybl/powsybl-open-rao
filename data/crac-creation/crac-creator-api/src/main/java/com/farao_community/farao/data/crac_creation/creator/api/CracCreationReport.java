/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.api;

import java.util.ArrayList;
import java.util.List;

import static com.farao_community.farao.commons.logs.FaraoLoggerProvider.*;

/**
 * Common methods used in CRAC creation reports
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class CracCreationReport {
    private List<String> creationReport;

    public CracCreationReport() {
        creationReport = new ArrayList<>();
    }

    public CracCreationReport(CracCreationReport toCopy) {
        this.creationReport = new ArrayList<>(toCopy.creationReport);
    }

    public void error(String errorReason) {
        String message = String.format("[ERROR] %s", errorReason);
        creationReport.add(message);
        BUSINESS_LOGS.error(message);
    }

    public void removed(String removedReason) {
        String message = String.format("[REMOVED] %s", removedReason);
        creationReport.add(message);
        BUSINESS_WARNS.warn(message);
    }

    public void added(String addedReason) {
        String message = String.format("[ADDED] %s", addedReason);
        creationReport.add(message);
        BUSINESS_WARNS.warn(message);
    }

    public void altered(String alteredReason) {
        String message = String.format("[ALTERED] %s", alteredReason);
        creationReport.add(message);
        BUSINESS_WARNS.warn(message);
    }

    public void warn(String warnReason) {
        String message = String.format("[WARN] %s", warnReason);
        creationReport.add(message);
        BUSINESS_WARNS.warn(message);
    }

    public void info(String infoReason) {
        String message = String.format("[INFO] %s", infoReason);
        creationReport.add(message);
        TECHNICAL_LOGS.info(message);
    }

    public void printCreationReport() {
        creationReport.forEach(BUSINESS_LOGS::info);
    }

    public List<String> getReport() {
        return creationReport;
    }

    public String toString() {
        return String.join("\n", creationReport);
    }
}
