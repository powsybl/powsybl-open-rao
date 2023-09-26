/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.usage_rule.OnFlowConstraintInCountry;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.powsybl.iidm.network.Country;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class OnFlowConstraintInCountryImplTest {
    State preventiveState;
    State curativeState;

    @BeforeEach
    public void setUp() {
        preventiveState = Mockito.mock(State.class);
        Mockito.when(preventiveState.getInstant()).thenReturn(Instant.PREVENTIVE);
        curativeState = Mockito.mock(State.class);
        Mockito.when(curativeState.getInstant()).thenReturn(Instant.CURATIVE);
    }

    @Test
    void testConstructor() {
        OnFlowConstraintInCountry onFlowConstraint = new OnFlowConstraintInCountryImpl(UsageMethod.AVAILABLE, Instant.PREVENTIVE, Country.EC);

        assertEquals(Instant.PREVENTIVE, onFlowConstraint.getInstant());
        assertEquals(Country.EC, onFlowConstraint.getCountry());
        assertEquals(UsageMethod.AVAILABLE, onFlowConstraint.getUsageMethod());
        assertEquals(UsageMethod.AVAILABLE, onFlowConstraint.getUsageMethod(preventiveState));
        assertEquals(UsageMethod.UNDEFINED, onFlowConstraint.getUsageMethod(curativeState));
    }

    @Test
    void testEquals() {
        OnFlowConstraintInCountry onFlowConstraint1 = new OnFlowConstraintInCountryImpl(UsageMethod.AVAILABLE, Instant.PREVENTIVE, Country.ES);
        assertEquals(onFlowConstraint1, onFlowConstraint1);
        assertEquals(onFlowConstraint1.hashCode(), onFlowConstraint1.hashCode());

        assertNotNull(onFlowConstraint1);
        assertNotEquals(onFlowConstraint1, Mockito.mock(OnInstantImpl.class));

        OnFlowConstraintInCountry onFlowConstraint2 = new OnFlowConstraintInCountryImpl(UsageMethod.AVAILABLE, Instant.PREVENTIVE, Country.ES);
        assertEquals(onFlowConstraint1, onFlowConstraint2);
        assertEquals(onFlowConstraint1.hashCode(), onFlowConstraint2.hashCode());

        onFlowConstraint2 = new OnFlowConstraintInCountryImpl(UsageMethod.AVAILABLE, Instant.CURATIVE, Country.ES);
        assertNotEquals(onFlowConstraint1, onFlowConstraint2);
        assertNotEquals(onFlowConstraint1.hashCode(), onFlowConstraint2.hashCode());

        onFlowConstraint2 = new OnFlowConstraintInCountryImpl(UsageMethod.AVAILABLE, Instant.PREVENTIVE, Country.FR);
        assertNotEquals(onFlowConstraint1, onFlowConstraint2);
        assertNotEquals(onFlowConstraint1.hashCode(), onFlowConstraint2.hashCode());
    }
}
