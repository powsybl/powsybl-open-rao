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
 * Condition that must be met for a remedial action to be triggered.
 * A trigger condition is a global AND gate that gathers up to 4 sub-conditions:
 *
 * <ol>
 *     <li>
 *          (mandatory) an {@link Instant} at which the remedial action can be applied;
 *     </li>
 *     <li>
 *          (optional) a {@link Contingency} after which the remedial action becomes applicable;
 *     </li>
 *     <li>
 *          (optional) a {@link Cnec} which, if constrained, makes the remedial action applicable;
 *     </li>
 *     <li>
 *          (optional) and a {@link Country} in which any overloaded FlowCNEC makes the remedial action applicable.
 *     </li>
 * </ol>
 *
 * A trigger condition also has a {@link UsageMethod} that describes how the remedial should be used if triggered.
 * <p>
 * A remedial action can have several trigger conditions, and they will be considered as a global OR gate among trigger conditions.
 *
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
