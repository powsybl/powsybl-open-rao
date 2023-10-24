/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.api.parameters;

import com.farao_community.farao.commons.FaraoException;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class JsonCracCreationParametersConstants {

    static final String CRAC_FACTORY = "crac-factory";

    static final String DEFAULT_MONITORED_LINE_SIDE = "default-monitored-line-side";
    private static final String MONITOR_LINES_ON_LEFT_SIDE_TEXT = "monitor-lines-on-left-side";
    private static final String MONITOR_LINES_ON_RIGHT_SIDE_TEXT = "monitor-lines-on-right-side";
    private static final String MONITOR_LINES_ON_BOTH_SIDES_TEXT = "monitor-lines-on-both-sides";

    private JsonCracCreationParametersConstants() {
        // should not be instantiated
    }

    static String serializeMonitoredLineSide(CracCreationParameters.MonitoredLineSide monitoredLineSide) {
        return switch (monitoredLineSide) {
            case MONITOR_LINES_ON_LEFT_SIDE -> MONITOR_LINES_ON_LEFT_SIDE_TEXT;
            case MONITOR_LINES_ON_RIGHT_SIDE -> MONITOR_LINES_ON_RIGHT_SIDE_TEXT;
            case MONITOR_LINES_ON_BOTH_SIDES -> MONITOR_LINES_ON_BOTH_SIDES_TEXT;
            default -> throw new FaraoException(String.format("Unknown monitored line side: %s", monitoredLineSide));
        };
    }

    static CracCreationParameters.MonitoredLineSide deserializeMonitoredLineSide(String monitoredLineSide) {
        return switch (monitoredLineSide) {
            case MONITOR_LINES_ON_LEFT_SIDE_TEXT -> CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_LEFT_SIDE;
            case MONITOR_LINES_ON_RIGHT_SIDE_TEXT ->
                CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_RIGHT_SIDE;
            case MONITOR_LINES_ON_BOTH_SIDES_TEXT ->
                CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_BOTH_SIDES;
            default -> throw new FaraoException(String.format("Unknown monitored line side: %s", monitoredLineSide));
        };
    }
}
