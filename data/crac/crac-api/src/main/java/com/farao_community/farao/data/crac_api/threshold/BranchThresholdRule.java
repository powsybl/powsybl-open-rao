/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api.threshold;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public enum BranchThresholdRule {
    ON_LOW_VOLTAGE_LEVEL,
    ON_HIGH_VOLTAGE_LEVEL,
    ON_NON_REGULATED_SIDE,
    ON_REGULATED_SIDE,
    ON_LEFT_SIDE,
    ON_RIGHT_SIDE
}
