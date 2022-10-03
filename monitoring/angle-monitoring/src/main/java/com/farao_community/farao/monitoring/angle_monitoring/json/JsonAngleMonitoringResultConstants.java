/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.monitoring.angle_monitoring.json;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Instant;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class JsonAngleMonitoringResultConstants {
    private JsonAngleMonitoringResultConstants() {
    }

    static final String TYPE = "type";
    static final String STATUS = "status";
    static final String ANGLE_MONITORING_RESULT = "ANGLE_MONITORING_RESULT";
    static final String ANGLE_VALUES = "angle-cnec-quantities-in-degrees";
    static final String CNEC_ID = "cnec-id";
    static final String QUANTITY = "quantity";
    static final String APPLIED_CRAS = "applied-cras";
    static final String CONTINGENCY = "contingency";
    static final String INSTANT = "instant";
    static final String REMEDIAL_ACTIONS = "remedial-actions";

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
