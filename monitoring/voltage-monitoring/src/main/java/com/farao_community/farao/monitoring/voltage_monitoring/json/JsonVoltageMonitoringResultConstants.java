/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.monitoring.voltage_monitoring.json;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Instant;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class JsonVoltageMonitoringResultConstants {
    private JsonVoltageMonitoringResultConstants() {
    }

    static final String TYPE = "type";
    static final String STATUS = "status";
    static final String VOLTAGE_MONITORING_RESULT = "VOLTAGE_MONITORING_RESULT";
    static final String VOLTAGE_VALUES = "extreme-voltage-values-in-kilovolts";
    static final String APPLIED_RAS = "applied-ras";
    static final String CNEC_ID = "cnec-id";
    static final String INSTANT = "instant";
    static final String CONTINGENCY = "contingency";
    static final String REMEDIAL_ACTIONS = "remedial-actions";
    static final String MIN = "min";
    static final String MAX = "max";

    // instants
    public static final String PREVENTIVE_INSTANT = "preventive";
    public static final String OUTAGE_INSTANT = "outage";
    public static final String AUTO_INSTANT = "auto";
    public static final String CURATIVE_INSTANT = "curative";

    public static Instant deserializeInstant(String stringValue) {
        switch (stringValue) {
            case PREVENTIVE_INSTANT:
                return Instant.PREVENTIVE;
            case OUTAGE_INSTANT:
                return Instant.OUTAGE;
            case AUTO_INSTANT:
                return Instant.AUTO;
            case CURATIVE_INSTANT:
                return Instant.CURATIVE;
            default:
                throw new FaraoException(String.format("Unrecognized instant %s", stringValue));
        }
    }
}
