/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.commons.logs;

import com.powsybl.openrao.commons.opentelemetry.OpenTelemetryReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public abstract class AbstractOpenRaoLogger implements OpenRaoLogger {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void trace(String format, Object... arguments) {
        OpenTelemetryReporter.trace(logger, format, arguments);
    }

    @Override
    public void info(String format, Object... arguments) {
        OpenTelemetryReporter.info(logger, format, arguments);
    }

    @Override
    public void warn(String format, Object... arguments) {
        OpenTelemetryReporter.warn(logger, format, arguments);
    }

    @Override
    public void error(String format, Object... arguments) {
        OpenTelemetryReporter.error(logger, format, arguments);
    }

    @Override
    public void debug(String format, Object... arguments) {
        OpenTelemetryReporter.debug(logger, format, arguments);
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }
}
