/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.range;

import com.powsybl.openrao.commons.Unit;

/**
 * Generic interface for the definition of ranges
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface Range {

    /**
     * Get the {@link RangeType} of the range
     */
    RangeType getRangeType();

    /**
     * Get the {@link Unit} in which the bounds of the range are defined
     */
    Unit getUnit();

}
