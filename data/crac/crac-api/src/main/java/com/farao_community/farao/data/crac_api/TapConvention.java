/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

/**
 * Convention to define range or setpoint of PSTs
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public enum TapConvention {
    CENTERED_ON_ZERO, // Taps from -x to x
    STARTS_AT_ONE // Taps from 1 to y
}
