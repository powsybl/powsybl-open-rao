/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.data.crac_api.threshold;

import com.powsybl.open_rao.commons.Unit;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public interface ThresholdAdder<I extends ThresholdAdder<I>> {

    /**
     * if unit is PERCENT_IMAX, the min/max value should be between -1 and 1, where 1 = 100%.
     */
    I withUnit(Unit unit);

    I withMax(Double max);

    I withMin(Double min);
}
