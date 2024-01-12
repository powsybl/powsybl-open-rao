/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.voltagemonitoring.json;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class JsonVoltageMonitoringResultConstants {
    private JsonVoltageMonitoringResultConstants() {
    }

    static final String VOLTAGE_MONITORING_RESULT = "VOLTAGE_MONITORING_RESULT";
    static final String VOLTAGE_VALUES = "extreme-voltage-values-in-kilovolts";
    static final String APPLIED_RAS = "applied-ras";
    static final String MIN = "min";
    static final String MAX = "max";
}
