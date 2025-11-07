/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.range;

/**
 * Interface dedicated to the definition of the so-called 'standard' range of
 * a StandardRangeAction
 *
 * StandardRange are defined with a min and a max value.
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface StandardRange extends Range {
    /**
     * Get the minimum of the range
     */
    double getMin();

    /**
     * Get the maximum of the range
     */
    double getMax();
}
