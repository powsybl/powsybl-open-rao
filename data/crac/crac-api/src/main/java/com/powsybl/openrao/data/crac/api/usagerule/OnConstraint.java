/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.crac.api.usagerule;

import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public interface OnConstraint<T extends Cnec<?>> extends UsageRule {
    /**
     * Get the Cnec that should be constrained
     */
    T getCnec();

    default boolean isDefinedForState(State state) {
        if (!state.getInstant().equals(getInstant())) {
            return false;
        }
        return getInstant().isPreventive() || getCnec().getState().getContingency().equals(state.getContingency());
    }
}
