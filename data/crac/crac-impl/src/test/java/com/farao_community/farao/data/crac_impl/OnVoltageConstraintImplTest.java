/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.data.crac_api.usage_rule.OnVoltageConstraint;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Fabrice Buscaylet {@literal <fabrice.buscaylet at artelys.com>}
 */
class OnVoltageConstraintImplTest {
    VoltageCnec voltageCnec;
    State preventiveState;
    State curativeState;

    @BeforeEach
    public void setUp() {
        voltageCnec = Mockito.mock(VoltageCnec.class);
        preventiveState = Mockito.mock(State.class);
        Mockito.when(preventiveState.getInstant()).thenReturn(Instant.PREVENTIVE);
        Mockito.when(preventiveState.isPreventive()).thenReturn(true);
        curativeState = Mockito.mock(State.class);
        Mockito.when(curativeState.getInstant()).thenReturn(Instant.CURATIVE);
        Mockito.when(curativeState.isPreventive()).thenReturn(false);
    }

    @Test
    void testConstructor() {
        OnVoltageConstraint onVoltageConstraint = new OnVoltageConstraintImpl(Instant.PREVENTIVE, voltageCnec);

        assertEquals(Instant.PREVENTIVE, onVoltageConstraint.getInstant());
        assertSame(voltageCnec, onVoltageConstraint.getVoltageCnec());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onVoltageConstraint.getUsageMethod());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onVoltageConstraint.getUsageMethod(preventiveState));
        assertEquals(UsageMethod.UNDEFINED, onVoltageConstraint.getUsageMethod(curativeState));
    }

    @Test
    void testEquals() {
        OnVoltageConstraint onVoltageConstraint1 = new OnVoltageConstraintImpl(Instant.PREVENTIVE, voltageCnec);
        assertEquals(onVoltageConstraint1, onVoltageConstraint1);
        assertEquals(onVoltageConstraint1.hashCode(), onVoltageConstraint1.hashCode());

        assertNotNull(onVoltageConstraint1);
        assertNotEquals(onVoltageConstraint1, Mockito.mock(OnInstantImpl.class));

        OnVoltageConstraint onVoltageConstraint2 = new OnVoltageConstraintImpl(Instant.PREVENTIVE, voltageCnec);
        assertEquals(onVoltageConstraint1, onVoltageConstraint2);
        assertEquals(onVoltageConstraint1.hashCode(), onVoltageConstraint2.hashCode());

        onVoltageConstraint2 = new OnVoltageConstraintImpl(Instant.CURATIVE, voltageCnec);
        assertNotEquals(onVoltageConstraint1, onVoltageConstraint2);
        assertNotEquals(onVoltageConstraint1.hashCode(), onVoltageConstraint2.hashCode());

        onVoltageConstraint2 = new OnVoltageConstraintImpl(Instant.PREVENTIVE, Mockito.mock(VoltageCnec.class));
        assertNotEquals(onVoltageConstraint1, onVoltageConstraint2);
        assertNotEquals(onVoltageConstraint1.hashCode(), onVoltageConstraint2.hashCode());
    }

    @Test
    void testGetUsageMethod() {
        State curativeState2 = Mockito.mock(State.class);
        Mockito.when(curativeState2.getInstant()).thenReturn(Instant.CURATIVE);
        Mockito.when(curativeState2.isPreventive()).thenReturn(false);

        OnVoltageConstraint onVoltageConstraint = new OnVoltageConstraintImpl(Instant.PREVENTIVE, voltageCnec);
        assertEquals(UsageMethod.TO_BE_EVALUATED, onVoltageConstraint.getUsageMethod(preventiveState));
        assertEquals(UsageMethod.UNDEFINED, onVoltageConstraint.getUsageMethod(curativeState));
        assertEquals(UsageMethod.UNDEFINED, onVoltageConstraint.getUsageMethod(curativeState2));

        Mockito.when(voltageCnec.getState()).thenReturn(curativeState);
        onVoltageConstraint = new OnVoltageConstraintImpl(Instant.CURATIVE, voltageCnec);
        assertEquals(UsageMethod.UNDEFINED, onVoltageConstraint.getUsageMethod(preventiveState));
        assertEquals(UsageMethod.TO_BE_EVALUATED, onVoltageConstraint.getUsageMethod(curativeState));
        assertEquals(UsageMethod.UNDEFINED, onVoltageConstraint.getUsageMethod(curativeState2));
    }
}
