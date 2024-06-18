/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracapi.triggercondition;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;

import java.util.Optional;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public interface TriggerCondition {
    Instant getInstant();

    Optional<Contingency> getContingency();

    Optional<Cnec<?>> getCnec();

    Optional<String> getCountry();

    UsageMethod getUsageMethod();

    default boolean isAvailable(Instant instant, Contingency contingency, Cnec<?> cnec, String country) {
        return this.getInstant().equals(instant)
            && getContingency().equals(Optional.of(contingency))
            && getCnec().equals(Optional.of(cnec))
            && getCountry().equals(Optional.of(country));
    }
}
