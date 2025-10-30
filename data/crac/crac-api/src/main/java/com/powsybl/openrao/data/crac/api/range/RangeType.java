/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.range;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public enum RangeType {
    ABSOLUTE,
    RELATIVE_TO_PREVIOUS_INSTANT,
    RELATIVE_TO_INITIAL_NETWORK,
    RELATIVE_TO_PREVIOUS_TIME_STEP
}
