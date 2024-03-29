/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.commons.logs;

/**
 * RAO business logs, containing important information understandable by the end user.
 * Only INFO and ERROR levels are allowed:
 * - INFO: high-level information about the RAO steps
 * - ERROR: information about fatal errors leading the RAO to interruption
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class RaoBusinessLogs extends AbstractOpenRaoLogger {
    private static final String LOG_LEVEL_NOT_ALLOWED = "Log level not allowed in this logger";

    public RaoBusinessLogs() {
        // nothing to do
    }

    @Override
    public void trace(String format, Object... arguments) {
        throw new IllegalCallerException(LOG_LEVEL_NOT_ALLOWED);
    }

    @Override
    public void warn(String format, Object... arguments) {
        throw new IllegalCallerException(LOG_LEVEL_NOT_ALLOWED);
    }

    @Override
    public void debug(String format, Object... arguments) {
        throw new IllegalCallerException(LOG_LEVEL_NOT_ALLOWED);
    }
}
