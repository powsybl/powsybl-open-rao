/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Country;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.triggercondition.TriggerCondition;
import com.powsybl.openrao.data.cracapi.triggercondition.UsageMethod;

import java.util.Optional;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class TriggerConditionImpl implements TriggerCondition {
    private final Instant instant;
    private final Contingency contingency;
    private final Cnec<?> cnec;
    private final Country country;
    private final UsageMethod usageMethod;

    public TriggerConditionImpl(Instant instant, Contingency contingency, Cnec<?> cnec, Country country, UsageMethod usageMethod) {
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
    public Optional<Country> getCountry() {
        return country == null ? Optional.empty() : Optional.of(country);
    }

    @Override
    public UsageMethod getUsageMethod() {
        return usageMethod;
    }

    @Override
    public UsageMethod getUsageMethod(State state) {
        if (state == null) {
            return UsageMethod.UNDEFINED;
        }
        if (state.isPreventive()) {
            return state.getInstant().equals(instant) ? usageMethod : UsageMethod.UNDEFINED;
        } else {
            if (cnec == null) {
                if (contingency == null) {
                    return state.getInstant().equals(instant) ? usageMethod : UsageMethod.UNDEFINED;
                }
                return state.getInstant().equals(instant) && state.getContingency().equals(Optional.ofNullable(contingency)) ? usageMethod : UsageMethod.UNDEFINED;
            }
            return state.getInstant().equals(instant) && state.equals(cnec.getState()) ? usageMethod : UsageMethod.UNDEFINED;
        }
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
        return instant.hashCode() * 47
            + (contingency == null ? 0 : contingency.hashCode() * 59)
            + (cnec == null ? 0 : cnec.hashCode() * 19)
            + (country == null ? 0 : country.hashCode() * 19)
            + usageMethod.hashCode() * 19;
    }
}
