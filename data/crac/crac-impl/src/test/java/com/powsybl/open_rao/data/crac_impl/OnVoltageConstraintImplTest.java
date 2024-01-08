/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.data.crac_impl;

import com.powsybl.open_rao.data.crac_api.Instant;
import com.powsybl.open_rao.data.crac_api.InstantKind;
import com.powsybl.open_rao.data.crac_api.State;
import com.powsybl.open_rao.data.crac_api.cnec.VoltageCnec;
import com.powsybl.open_rao.data.crac_api.usage_rule.OnVoltageConstraint;
import com.powsybl.open_rao.data.crac_api.usage_rule.UsageMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Fabrice Buscaylet {@literal <fabrice.buscaylet at artelys.com>}
 */
class OnVoltageConstraintImplTest {
    private static final Instant PREVENTIVE_INSTANT = new InstantImpl("preventive", InstantKind.PREVENTIVE, null);
    private static final Instant OUTAGE_INSTANT = new InstantImpl("outage", InstantKind.OUTAGE, PREVENTIVE_INSTANT);
    private static final Instant AUTO_INSTANT = new InstantImpl("auto", InstantKind.AUTO, OUTAGE_INSTANT);
    private static final Instant CURATIVE_INSTANT = new InstantImpl("curative", InstantKind.CURATIVE, AUTO_INSTANT);

    VoltageCnec voltageCnec;
    State preventiveState;
    State curativeState;

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
        OnVoltageConstraint onVoltageConstraint = new OnVoltageConstraintImpl(UsageMethod.AVAILABLE, PREVENTIVE_INSTANT, voltageCnec);

        assertEquals(PREVENTIVE_INSTANT, onVoltageConstraint.getInstant());
        assertSame(voltageCnec, onVoltageConstraint.getVoltageCnec());
        assertEquals(UsageMethod.AVAILABLE, onVoltageConstraint.getUsageMethod());
        assertEquals(UsageMethod.AVAILABLE, onVoltageConstraint.getUsageMethod(preventiveState));
        assertEquals(UsageMethod.UNDEFINED, onVoltageConstraint.getUsageMethod(curativeState));
    }

    @Test
    void testEquals() {
        OnVoltageConstraint onVoltageConstraint1 = new OnVoltageConstraintImpl(UsageMethod.AVAILABLE, PREVENTIVE_INSTANT, voltageCnec);
        assertEquals(onVoltageConstraint1, onVoltageConstraint1);
        assertEquals(onVoltageConstraint1.hashCode(), onVoltageConstraint1.hashCode());

        assertNotNull(onVoltageConstraint1);
        assertNotEquals(onVoltageConstraint1, Mockito.mock(OnInstantImpl.class));

        OnVoltageConstraint onVoltageConstraint2 = new OnVoltageConstraintImpl(UsageMethod.AVAILABLE, PREVENTIVE_INSTANT, voltageCnec);
        assertEquals(onVoltageConstraint1, onVoltageConstraint2);
        assertEquals(onVoltageConstraint1.hashCode(), onVoltageConstraint2.hashCode());

        onVoltageConstraint2 = new OnVoltageConstraintImpl(UsageMethod.AVAILABLE, CURATIVE_INSTANT, voltageCnec);
        assertNotEquals(onVoltageConstraint1, onVoltageConstraint2);
        assertNotEquals(onVoltageConstraint1.hashCode(), onVoltageConstraint2.hashCode());

        onVoltageConstraint2 = new OnVoltageConstraintImpl(UsageMethod.AVAILABLE, PREVENTIVE_INSTANT, Mockito.mock(VoltageCnec.class));
        assertNotEquals(onVoltageConstraint1, onVoltageConstraint2);
        assertNotEquals(onVoltageConstraint1.hashCode(), onVoltageConstraint2.hashCode());
    }

    @Test
    void testGetUsageMethod() {
        State curativeState2 = Mockito.mock(State.class);
        Mockito.when(curativeState2.getInstant()).thenReturn(CURATIVE_INSTANT);
        Mockito.when(curativeState2.isPreventive()).thenReturn(false);

        OnVoltageConstraint onVoltageConstraint = new OnVoltageConstraintImpl(UsageMethod.AVAILABLE, PREVENTIVE_INSTANT, voltageCnec);
        assertEquals(UsageMethod.AVAILABLE, onVoltageConstraint.getUsageMethod(preventiveState));
        assertEquals(UsageMethod.UNDEFINED, onVoltageConstraint.getUsageMethod(curativeState));
        assertEquals(UsageMethod.UNDEFINED, onVoltageConstraint.getUsageMethod(curativeState2));

        Mockito.when(voltageCnec.getState()).thenReturn(curativeState);
        onVoltageConstraint = new OnVoltageConstraintImpl(UsageMethod.AVAILABLE, CURATIVE_INSTANT, voltageCnec);
        assertEquals(UsageMethod.UNDEFINED, onVoltageConstraint.getUsageMethod(preventiveState));
        assertEquals(UsageMethod.AVAILABLE, onVoltageConstraint.getUsageMethod(curativeState));
        assertEquals(UsageMethod.UNDEFINED, onVoltageConstraint.getUsageMethod(curativeState2));
    }
}
