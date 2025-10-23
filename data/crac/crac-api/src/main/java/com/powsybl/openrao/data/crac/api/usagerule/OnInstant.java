/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.usagerule;

import com.powsybl.openrao.data.crac.api.State;

/**
 * The OnInstant UsageRule is defined at a given Instant. For instance, if a RemedialAction
 * has an OnInstant UsageRule with Instant "curative", this RemedialAction will be available
 * after all the contingencies at Instant "curative". If the instant is "auto" it will be forced.
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface OnInstant extends UsageRule {
    default boolean isDefinedForState(State state) {
        return state.getInstant().equals(getInstant());
    }
}
