/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracapi.triggercondition;

import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Country;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;

import java.util.Optional;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public interface TriggerCondition {
    /**
     * Get the {@link Instant} of the trigger condition
     */
    Instant getInstant();

    /**
     * Get the {@link Contingency} of the trigger condition
     */
    Optional<Contingency> getContingency();

    /**
     * Get the {@link Cnec} of the trigger condition
     */
    Optional<Cnec<?>> getCnec();

    /**
     * Get the {@link Country} of the trigger condition
     */
    Optional<Country> getCountry();

    /**
     * Get the {@link UsageMethod} of the trigger condition
     */
    UsageMethod getUsageMethod();

    /**
     * Get the {@link UsageMethod} of the trigger condition on a given state
     */
    UsageMethod getUsageMethod(State state);
}
