/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.commons.logs;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class OpenRaoLoggerProvider {
    public static final OpenRaoLogger BUSINESS_LOGS = new RaoBusinessLogs();
    public static final OpenRaoLogger BUSINESS_WARNS = new RaoBusinessWarns();
    public static final OpenRaoLogger TECHNICAL_LOGS = new TechnicalLogs();
    public static final CracImporterLogs CRAC_IMPORTER_LOGS = new CracImporterLogs();

    private OpenRaoLoggerProvider() {
        // utility class
    }
}
