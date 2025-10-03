/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.crac.impl;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.usagerule.OnFlowConstraintInCountry;
import com.powsybl.openrao.data.crac.api.usagerule.OnFlowConstraintInCountryAdder;
import com.powsybl.iidm.network.Country;

import java.util.Optional;

import static com.powsybl.openrao.data.crac.impl.AdderUtils.assertAttributeNotNull;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class OnFlowConstraintInCountryAdderImpl<T extends AbstractRemedialActionAdder<T>> implements OnFlowConstraintInCountryAdder<T> {

    public static final String ON_FLOW_CONSTRAINT_IN_COUNTRY = "OnFlowConstraintInCountry";
    private T owner;
    private String instantId;
    private String contingencyId;
    private Country country;

    OnFlowConstraintInCountryAdderImpl(AbstractRemedialActionAdder<T> owner) {
        this.owner = (T) owner;
    }

    @Override
    public OnFlowConstraintInCountryAdder<T> withInstant(String instantId) {
        this.instantId = instantId;
        return this;
    }

    @Override
    public OnFlowConstraintInCountryAdder<T> withContingency(String contingencyId) {
        this.contingencyId = contingencyId;
        return this;
    }

    @Override
    public OnFlowConstraintInCountryAdder<T> withCountry(Country country) {
        this.country = country;
        return this;
    }

    @Override
    public T add() {
        assertAttributeNotNull(instantId, ON_FLOW_CONSTRAINT_IN_COUNTRY, "instant", "withInstant()");
        assertAttributeNotNull(country, ON_FLOW_CONSTRAINT_IN_COUNTRY, "country", "withCountry()");

        Instant instant = owner.getCrac().getInstant(instantId);
        if (instant.isOutage()) {
            throw new OpenRaoException("OnFlowConstraintInCountry usage rules are not allowed for OUTAGE instant.");
        }
        if (instant.isPreventive()) {
            owner.getCrac().addPreventiveState();
        }

        Optional<Contingency> optionalContingency = Optional.empty();
        if (contingencyId != null) {
            Contingency contingency = owner.getCrac().getContingency(contingencyId);
            if (contingency == null) {
                throw new OpenRaoException(String.format("Contingency %s of OnFlowConstraintInCountry usage rule does not exist in the crac. Use crac.newContingency() first.", contingencyId));
            }
            optionalContingency = Optional.of(contingency);
        }

        OnFlowConstraintInCountry onFlowConstraint = new OnFlowConstraintInCountryImpl(instant, optionalContingency, country);
        owner.addUsageRule(onFlowConstraint);
        return owner;
    }
}
