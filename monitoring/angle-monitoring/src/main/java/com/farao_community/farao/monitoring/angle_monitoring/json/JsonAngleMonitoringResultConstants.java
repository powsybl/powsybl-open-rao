/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.monitoring.angle_monitoring.json;

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
}
