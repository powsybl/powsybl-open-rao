/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creator_api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Common methods used in CRAC creation reports
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class CracCreationReport {
    private static final Logger LOGGER = LoggerFactory.getLogger(CracCreationReport.class);
    private List<String> creationReport;

    public CracCreationReport() {
        creationReport = new ArrayList<>();
    }

    public void error(String errorReason) {
        String message = String.format("[ERROR] %s", errorReason);
        creationReport.add(message);
        LOGGER.info(message);
    }

    public void removed(String removedReason) {
        String message = String.format("[REMOVED] %s", removedReason);
        creationReport.add(message);
        LOGGER.info(message);
    }

    public void altered(String alteredReason) {
        String message = String.format("[ALTERED] %s", alteredReason);
        creationReport.add(message);
        LOGGER.info(message);
    }

    public void warn(String warnReason) {
        String message = String.format("[WARN] %s", warnReason);
        creationReport.add(message);
        LOGGER.info(message);
    }

    public void info(String infoReason) {
        String message = String.format("[INFO] %s", infoReason);
        creationReport.add(message);
        LOGGER.info(message);
    }

    public void printCreationReport() {
        creationReport.forEach(LOGGER::info);
    }

    public List<String> getReport() {
        return creationReport;
    }
}
