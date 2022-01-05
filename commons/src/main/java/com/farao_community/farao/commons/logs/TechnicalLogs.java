/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.commons.logs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Technical logs, containing information understandable by the advanced users or maintainers.
 * All log levels are allowed.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class TechnicalLogs implements FaraoLogger {
    private final Logger logger = LoggerFactory.getLogger(TechnicalLogs.class);

    public TechnicalLogs() {
        // nothing to do
    }

    @Override
    public void trace(String format, Object... arguments) {
        logger.trace(format, arguments);
    }

    @Override
    public void info(String format, Object... arguments) {
        logger.info(format, arguments);
    }

    @Override
    public void warn(String format, Object... arguments) {
        logger.warn(format, arguments);
    }

    @Override
    public void error(String format, Object... arguments) {
        logger.error(format, arguments);
    }

    @Override
    public void debug(String format, Object... arguments) {
        logger.debug(format, arguments);
    }
}
