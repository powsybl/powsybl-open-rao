/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracapi.usagerule;

import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.iidm.network.Country;

/**
 * The OnFlowConstraint UsageRule is defined on a given country.
 * A remedial action with this usage rule shall be available when a FlowCnec of
 * the given country is constrained.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public interface OnFlowConstraintInCountry extends UsageRule {
    /**
     * Get the FlowCnec that should be constrained
     */
    Country getCountry();

    /**
     * Get the Instant of the free to use
     */
    Instant getInstant();
}
