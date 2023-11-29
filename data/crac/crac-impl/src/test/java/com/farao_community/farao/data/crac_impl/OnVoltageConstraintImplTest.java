/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.InstantKind;
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
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";
    private static final Instant PREVENTIVE_INSTANT = new InstantImpl(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE, null);
    private static final Instant OUTAGE_INSTANT = new InstantImpl(OUTAGE_INSTANT_ID, InstantKind.OUTAGE, PREVENTIVE_INSTANT);
    private static final Instant AUTO_INSTANT = new InstantImpl(AUTO_INSTANT_ID, InstantKind.AUTO, OUTAGE_INSTANT);
    private static final Instant CURATIVE_INSTANT = new InstantImpl(CURATIVE_INSTANT_ID, InstantKind.CURATIVE, AUTO_INSTANT);

    private VoltageCnec voltageCnec;
    private State preventiveState;
    private State curativeState;

    @BeforeEach
    public void setUp() {
        voltageCnec = Mockito.mock(VoltageCnec.class);
        preventiveState = Mockito.mock(State.class);
        Mockito.when(preventiveState.getInstant()).thenReturn(PREVENTIVE_INSTANT);
        Mockito.when(preventiveState.isPreventive()).thenReturn(true);
        curativeState = Mockito.mock(State.class);
        Mockito.when(curativeState.getInstant()).thenReturn(CURATIVE_INSTANT);
        Mockito.when(curativeState.isPreventive()).thenReturn(false);
    }

    @Test
    void testConstructor() {
        OnVoltageConstraint onVoltageConstraint = new OnVoltageConstraintImpl(PREVENTIVE_INSTANT, voltageCnec);

        assertEquals(PREVENTIVE_INSTANT, onVoltageConstraint.getInstant());
        assertSame(voltageCnec, onVoltageConstraint.getVoltageCnec());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onVoltageConstraint.getUsageMethod());
        assertEquals(UsageMethod.TO_BE_EVALUATED, onVoltageConstraint.getUsageMethod(preventiveState));
        assertEquals(UsageMethod.UNDEFINED, onVoltageConstraint.getUsageMethod(curativeState));
    }

    @Test
    void testEquals() {
        OnVoltageConstraint onVoltageConstraint1 = new OnVoltageConstraintImpl(PREVENTIVE_INSTANT, voltageCnec);
        assertEquals(onVoltageConstraint1, onVoltageConstraint1);
        assertEquals(onVoltageConstraint1.hashCode(), onVoltageConstraint1.hashCode());

        assertNotNull(onVoltageConstraint1);
        assertNotEquals(onVoltageConstraint1, Mockito.mock(OnInstantImpl.class));

        OnVoltageConstraint onVoltageConstraint2 = new OnVoltageConstraintImpl(PREVENTIVE_INSTANT, voltageCnec);
        assertEquals(onVoltageConstraint1, onVoltageConstraint2);
        assertEquals(onVoltageConstraint1.hashCode(), onVoltageConstraint2.hashCode());

        onVoltageConstraint2 = new OnVoltageConstraintImpl(CURATIVE_INSTANT, voltageCnec);
        assertNotEquals(onVoltageConstraint1, onVoltageConstraint2);
        assertNotEquals(onVoltageConstraint1.hashCode(), onVoltageConstraint2.hashCode());

        onVoltageConstraint2 = new OnVoltageConstraintImpl(PREVENTIVE_INSTANT, Mockito.mock(VoltageCnec.class));
        assertNotEquals(onVoltageConstraint1, onVoltageConstraint2);
        assertNotEquals(onVoltageConstraint1.hashCode(), onVoltageConstraint2.hashCode());
    }

    @Test
    void testGetUsageMethod() {
        State curativeState2 = Mockito.mock(State.class);
        Mockito.when(curativeState2.getInstant()).thenReturn(CURATIVE_INSTANT);
        Mockito.when(curativeState2.isPreventive()).thenReturn(false);

        OnVoltageConstraint onVoltageConstraint = new OnVoltageConstraintImpl(PREVENTIVE_INSTANT, voltageCnec);
        assertEquals(UsageMethod.TO_BE_EVALUATED, onVoltageConstraint.getUsageMethod(preventiveState));
        assertEquals(UsageMethod.UNDEFINED, onVoltageConstraint.getUsageMethod(curativeState));
        assertEquals(UsageMethod.UNDEFINED, onVoltageConstraint.getUsageMethod(curativeState2));

        Mockito.when(voltageCnec.getState()).thenReturn(curativeState);
        onVoltageConstraint = new OnVoltageConstraintImpl(CURATIVE_INSTANT, voltageCnec);
        assertEquals(UsageMethod.UNDEFINED, onVoltageConstraint.getUsageMethod(preventiveState));
        assertEquals(UsageMethod.TO_BE_EVALUATED, onVoltageConstraint.getUsageMethod(curativeState));
        assertEquals(UsageMethod.UNDEFINED, onVoltageConstraint.getUsageMethod(curativeState2));
    }
}
