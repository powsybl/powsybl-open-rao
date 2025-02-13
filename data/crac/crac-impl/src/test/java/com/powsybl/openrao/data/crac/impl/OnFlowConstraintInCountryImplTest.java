/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.crac.impl;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.usagerule.OnFlowConstraintInCountry;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import com.powsybl.iidm.network.Country;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class OnFlowConstraintInCountryImplTest {
    private static final Instant PREVENTIVE_INSTANT = new InstantImpl("preventive", InstantKind.PREVENTIVE, null);
    private static final Instant OUTAGE_INSTANT = new InstantImpl("outage", InstantKind.OUTAGE, PREVENTIVE_INSTANT);
    private static final Instant AUTO_INSTANT = new InstantImpl("auto", InstantKind.AUTO, OUTAGE_INSTANT);
    private static final Instant CURATIVE_INSTANT = new InstantImpl("curative", InstantKind.CURATIVE, AUTO_INSTANT);

    State preventiveState;
    State curativeState;

    @BeforeEach
    public void setUp() {
        preventiveState = Mockito.mock(State.class);
        Mockito.when(preventiveState.getInstant()).thenReturn(PREVENTIVE_INSTANT);
        curativeState = Mockito.mock(State.class);
        Mockito.when(curativeState.getInstant()).thenReturn(CURATIVE_INSTANT);
    }

    @Test
    void testConstructor() {
        OnFlowConstraintInCountry onFlowConstraint = new OnFlowConstraintInCountryImpl(UsageMethod.AVAILABLE, PREVENTIVE_INSTANT, Optional.empty(), Country.EC);

        assertEquals(PREVENTIVE_INSTANT, onFlowConstraint.getInstant());
        assertEquals(Country.EC, onFlowConstraint.getCountry());
        assertEquals(UsageMethod.AVAILABLE, onFlowConstraint.getUsageMethod());
        assertEquals(UsageMethod.AVAILABLE, onFlowConstraint.getUsageMethod(preventiveState));
        assertEquals(UsageMethod.UNDEFINED, onFlowConstraint.getUsageMethod(curativeState));
        assertFalse(onFlowConstraint.getContingency().isPresent());
    }

    @Test
    void testEquals() {
        OnFlowConstraintInCountry onFlowConstraint1 = new OnFlowConstraintInCountryImpl(UsageMethod.AVAILABLE, PREVENTIVE_INSTANT, Optional.empty(), Country.ES);
        assertEquals(onFlowConstraint1, onFlowConstraint1);
        assertEquals(onFlowConstraint1.hashCode(), onFlowConstraint1.hashCode());

        assertNotNull(onFlowConstraint1);
        assertNotEquals(onFlowConstraint1, Mockito.mock(OnInstantImpl.class));

        OnFlowConstraintInCountry onFlowConstraint2 = new OnFlowConstraintInCountryImpl(UsageMethod.AVAILABLE, PREVENTIVE_INSTANT, Optional.empty(), Country.ES);
        assertEquals(onFlowConstraint1, onFlowConstraint2);
        assertEquals(onFlowConstraint1.hashCode(), onFlowConstraint2.hashCode());

        onFlowConstraint2 = new OnFlowConstraintInCountryImpl(UsageMethod.AVAILABLE, CURATIVE_INSTANT, Optional.empty(), Country.ES);
        assertNotEquals(onFlowConstraint1, onFlowConstraint2);
        assertNotEquals(onFlowConstraint1.hashCode(), onFlowConstraint2.hashCode());

        onFlowConstraint2 = new OnFlowConstraintInCountryImpl(UsageMethod.AVAILABLE, PREVENTIVE_INSTANT, Optional.empty(), Country.FR);
        assertNotEquals(onFlowConstraint1, onFlowConstraint2);
        assertNotEquals(onFlowConstraint1.hashCode(), onFlowConstraint2.hashCode());
    }

    @Test
    void testGetUsageMethodWithContringency() {
        Contingency contingency1 = Mockito.mock(Contingency.class);
        Contingency contingency2 = Mockito.mock(Contingency.class);

        State stateAuto1 = new PostContingencyState(contingency1, AUTO_INSTANT, null);
        State stateCur1 = new PostContingencyState(contingency1, CURATIVE_INSTANT, null);
        State stateAuto2 = new PostContingencyState(contingency2, AUTO_INSTANT, null);
        State stateCur2 = new PostContingencyState(contingency2, CURATIVE_INSTANT, null);

        OnFlowConstraintInCountry ur;

        ur = new OnFlowConstraintInCountryImpl(UsageMethod.AVAILABLE, AUTO_INSTANT, Optional.of(contingency1), Country.ES);
        assertEquals(UsageMethod.AVAILABLE, ur.getUsageMethod(stateAuto1));
        assertEquals(UsageMethod.UNDEFINED, ur.getUsageMethod(stateCur1));
        assertEquals(UsageMethod.UNDEFINED, ur.getUsageMethod(stateAuto2));
        assertEquals(UsageMethod.UNDEFINED, ur.getUsageMethod(stateCur2));

        ur = new OnFlowConstraintInCountryImpl(UsageMethod.AVAILABLE, AUTO_INSTANT, Optional.empty(), Country.ES);
        assertEquals(UsageMethod.AVAILABLE, ur.getUsageMethod(stateAuto1));
        assertEquals(UsageMethod.UNDEFINED, ur.getUsageMethod(stateCur1));
        assertEquals(UsageMethod.AVAILABLE, ur.getUsageMethod(stateAuto2));
        assertEquals(UsageMethod.UNDEFINED, ur.getUsageMethod(stateCur2));

        ur = new OnFlowConstraintInCountryImpl(UsageMethod.AVAILABLE, CURATIVE_INSTANT, Optional.of(contingency1), Country.ES);
        assertEquals(UsageMethod.UNDEFINED, ur.getUsageMethod(stateAuto1));
        assertEquals(UsageMethod.AVAILABLE, ur.getUsageMethod(stateCur1));
        assertEquals(UsageMethod.UNDEFINED, ur.getUsageMethod(stateAuto2));
        assertEquals(UsageMethod.UNDEFINED, ur.getUsageMethod(stateCur2));

        ur = new OnFlowConstraintInCountryImpl(UsageMethod.AVAILABLE, CURATIVE_INSTANT, Optional.empty(), Country.ES);
        assertEquals(UsageMethod.UNDEFINED, ur.getUsageMethod(stateAuto1));
        assertEquals(UsageMethod.AVAILABLE, ur.getUsageMethod(stateCur1));
        assertEquals(UsageMethod.UNDEFINED, ur.getUsageMethod(stateAuto2));
        assertEquals(UsageMethod.AVAILABLE, ur.getUsageMethod(stateCur2));
    }
}
