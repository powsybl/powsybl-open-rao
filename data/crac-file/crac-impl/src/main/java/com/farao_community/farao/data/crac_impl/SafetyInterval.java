/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.Unit;

/**
 * Limits for angles or voltage.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */

public class SafetyInterval extends AbstractThreshold {

    private double minValue;
    private double maxValue;

    public SafetyInterval(Unit unit, double minValue, double maxValue) {
        super(unit);
        this.minValue = minValue;
        this.maxValue = maxValue;
    }
}
