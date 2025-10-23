/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.range;

/**
 * Interface dedicated to the definition of the ranges of PSTs
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface TapRange extends Range {
    /**
     * Get the minimum tap of the range
     */
    int getMinTap();

    /**
     * Get the maximum tap of the range
     */
    int getMaxTap();

}
