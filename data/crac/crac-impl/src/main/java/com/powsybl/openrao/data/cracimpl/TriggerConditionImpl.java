/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.triggercondition.TriggerCondition;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;

import java.util.Optional;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class TriggerConditionImpl implements TriggerCondition {
    private final Instant instant;
    private final Contingency contingency;
    private final Cnec<?> cnec;
    private final String country;
    private final UsageMethod usageMethod;

    public TriggerConditionImpl(Instant instant, Contingency contingency, Cnec<?> cnec, String country, UsageMethod usageMethod) {
        this.instant = instant;
        this.contingency = contingency;
        this.cnec = cnec;
        this.country = country;
        this.usageMethod = usageMethod;
    }

    @Override
    public Instant getInstant() {
        return instant;
    }

    @Override
    public Optional<Contingency> getContingency() {
        return contingency == null ? Optional.empty() : Optional.of(contingency);
    }

    @Override
    public Optional<Cnec<?>> getCnec() {
        return cnec == null ? Optional.empty() : Optional.of(cnec);
    }

    @Override
    public Optional<String> getCountry() {
        return country == null ? Optional.empty() : Optional.of(country);
    }

    @Override
    public UsageMethod getUsageMethod() {
        return usageMethod;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TriggerConditionImpl otherTriggerCondition = (TriggerConditionImpl) o;
        return getInstant().equals(otherTriggerCondition.getInstant())
            && getContingency().equals(otherTriggerCondition.getContingency())
            && getCnec().equals(otherTriggerCondition.getCnec())
            && getCountry().equals(otherTriggerCondition.getCountry())
            && getUsageMethod().equals(otherTriggerCondition.getUsageMethod());
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
