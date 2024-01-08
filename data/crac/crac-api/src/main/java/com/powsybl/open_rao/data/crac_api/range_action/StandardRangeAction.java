/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.data.crac_api.range_action;

import com.powsybl.open_rao.data.crac_api.range.StandardRange;

import java.util.List;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface StandardRangeAction<T extends StandardRangeAction<T>> extends RangeAction<T> {

    List<StandardRange> getRanges();

    /**
     * Get the setpoint of the remedial action before RAO.
     */
    double getInitialSetpoint();
}
