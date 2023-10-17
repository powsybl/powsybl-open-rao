/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.usage_rule.OnFlowConstraintInCountry;
import com.farao_community.farao.data.crac_api.usage_rule.OnFlowConstraintInCountryAdder;
import com.powsybl.iidm.network.Country;

import static com.farao_community.farao.data.crac_impl.AdderUtils.assertAttributeNotNull;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class OnFlowConstraintInCountryAdderImpl<T extends AbstractRemedialActionAdder<T>> implements OnFlowConstraintInCountryAdder<T> {

    private final T owner;
    private Instant instant;
    private Country country;

    OnFlowConstraintInCountryAdderImpl(AbstractRemedialActionAdder<T> owner) {
        this.owner = (T) owner;
    }

    @Override
    public OnFlowConstraintInCountryAdder<T> withInstant(Instant instant) {
        this.instant = instant;
        return this;
    }

    @Override
    public OnFlowConstraintInCountryAdder<T> withCountry(Country country) {
        this.country = country;
        return this;
    }

    @Override
    public T add() {
        assertAttributeNotNull(instant, "OnInstant", "instant", "withInstant()");
        assertAttributeNotNull(country, "OnFlowConstraintInCountry", "country", "withCountry()");

        if (instant.getInstantKind().equals(InstantKind.OUTAGE)) {
            throw new FaraoException("OnFlowConstraintInCountry usage rules are not allowed for OUTAGE instant.");
        }
        if (instant.getInstantKind().equals(InstantKind.PREVENTIVE)) {
            owner.getCrac().addPreventiveState(instant);
        }

        OnFlowConstraintInCountry onFlowConstraint = new OnFlowConstraintInCountryImpl(instant, country);
        owner.addUsageRule(onFlowConstraint);
        return owner;
    }
}
