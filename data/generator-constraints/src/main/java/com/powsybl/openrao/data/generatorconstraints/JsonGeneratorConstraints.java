/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.generatorconstraints;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class JsonGeneratorConstraints {

    private JsonGeneratorConstraints() {
    }

    public static final String ID = "id";
    public static final String P_MIN = "pMin";
    public static final String P_MAX = "pMax";
    public static final String LEAD_TIME = "leadTime";
    public static final String LAG_TIME = "lagTime";
    public static final String UPWARD_POWER_GRADIENT = "upwardPowerGradient";
    public static final String DOWNWARD_POWER_GRADIENT = "downwardPowerGradient";
    public static final String MIN_UP_TIME = "minUpTime";
    public static final String MAX_UP_TIME = "maxUpTime";
    public static final String MIN_OFF_TIME = "minOffTime";

}
