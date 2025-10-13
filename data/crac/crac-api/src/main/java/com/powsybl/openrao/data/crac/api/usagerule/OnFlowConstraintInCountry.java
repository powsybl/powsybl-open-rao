/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.usagerule;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.iidm.network.Country;
import com.powsybl.openrao.data.crac.api.State;

import java.util.Optional;

/**
 * The OnFlowConstraint UsageRule is defined on a given country.
 * A remedial action with this usage rule shall be available when a FlowCnec of
 * the given country is constrained.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public interface OnFlowConstraintInCountry extends UsageRule {
    /**
     * Get the country of the FlowCnec that should be constrained
     */
    Country getCountry();

    /**
     * Get the Instant of the usage rule: the usage rule is only triggered when the RA is being used at the
     * given instant.
     */
    Instant getInstant();

    /**
     * Get the Contingency of the usage rule: the usage rule is only triggered if there is a CNEC
     * with a flow constraint after this given contingency.
     * If empty, then there is no Contingency condition.
     */
    Optional<Contingency> getContingency();

    default boolean isDefinedForState(State state) {
        if (!state.getInstant().equals(getInstant())) {
            return false;
        }
        return getContingency().isEmpty() || getContingency().equals(state.getContingency());
    }
}
