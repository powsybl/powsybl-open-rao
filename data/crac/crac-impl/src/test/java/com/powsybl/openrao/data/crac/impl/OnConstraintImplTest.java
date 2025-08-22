/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.crac.api.usagerule.OnConstraint;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class OnConstraintImplTest {
    private static final Instant PREVENTIVE_INSTANT = new InstantImpl("preventive", InstantKind.PREVENTIVE, null);
    private static final Instant OUTAGE_INSTANT = new InstantImpl("outage", InstantKind.OUTAGE, PREVENTIVE_INSTANT);
    private static final Instant AUTO_INSTANT = new InstantImpl("auto", InstantKind.AUTO, OUTAGE_INSTANT);
    private static final Instant CURATIVE_INSTANT = new InstantImpl("curative", InstantKind.CURATIVE, AUTO_INSTANT);

    AngleCnec angleCnec;
    FlowCnec flowCnec;
    VoltageCnec voltageCnec;
    State preventiveState;
    State curativeState;

    @BeforeEach
    public void setUp() {
        angleCnec = Mockito.mock(AngleCnec.class);
        flowCnec = Mockito.mock(FlowCnec.class);
        voltageCnec = Mockito.mock(VoltageCnec.class);
        preventiveState = Mockito.mock(State.class);
        Mockito.when(preventiveState.getInstant()).thenReturn(PREVENTIVE_INSTANT);
        Mockito.when(preventiveState.isPreventive()).thenReturn(true);
        curativeState = Mockito.mock(State.class);
        Mockito.when(curativeState.getInstant()).thenReturn(CURATIVE_INSTANT);
        Mockito.when(curativeState.isPreventive()).thenReturn(false);
    }

    @Test
    void testConstructorAngle() {
        OnConstraint<AngleCnec> onAngleConstraint = new OnConstraintImpl<>(UsageMethod.AVAILABLE, PREVENTIVE_INSTANT, angleCnec);

        assertEquals(PREVENTIVE_INSTANT, onAngleConstraint.getInstant());
        assertSame(angleCnec, onAngleConstraint.getCnec());
        assertEquals(UsageMethod.AVAILABLE, onAngleConstraint.getUsageMethod());
        assertEquals(UsageMethod.AVAILABLE, onAngleConstraint.getUsageMethod(preventiveState));
        assertEquals(UsageMethod.UNDEFINED, onAngleConstraint.getUsageMethod(curativeState));
    }

    @Test
    void testConstructorFlow() {
        OnConstraint<FlowCnec> onFlowConstraint = new OnConstraintImpl<>(UsageMethod.AVAILABLE, PREVENTIVE_INSTANT, flowCnec);

        assertEquals(PREVENTIVE_INSTANT, onFlowConstraint.getInstant());
        assertSame(flowCnec, onFlowConstraint.getCnec());
        assertEquals(UsageMethod.AVAILABLE, onFlowConstraint.getUsageMethod());
        assertEquals(UsageMethod.AVAILABLE, onFlowConstraint.getUsageMethod(preventiveState));
        assertEquals(UsageMethod.UNDEFINED, onFlowConstraint.getUsageMethod(curativeState));
    }

    @Test
    void testConstructorVoltage() {
        OnConstraint<VoltageCnec> onVoltageConstraint = new OnConstraintImpl<>(UsageMethod.AVAILABLE, PREVENTIVE_INSTANT, voltageCnec);

        assertEquals(PREVENTIVE_INSTANT, onVoltageConstraint.getInstant());
        assertSame(voltageCnec, onVoltageConstraint.getCnec());
        assertEquals(UsageMethod.AVAILABLE, onVoltageConstraint.getUsageMethod());
        assertEquals(UsageMethod.AVAILABLE, onVoltageConstraint.getUsageMethod(preventiveState));
        assertEquals(UsageMethod.UNDEFINED, onVoltageConstraint.getUsageMethod(curativeState));
    }

    @Test
    void testEqualsAngle() {
        OnConstraint<AngleCnec> onAngleConstraint1 = new OnConstraintImpl<>(UsageMethod.AVAILABLE, PREVENTIVE_INSTANT, angleCnec);
        assertEquals(onAngleConstraint1, onAngleConstraint1);
        assertEquals(onAngleConstraint1.hashCode(), onAngleConstraint1.hashCode());

        assertNotNull(onAngleConstraint1);
        assertNotEquals(onAngleConstraint1, Mockito.mock(OnInstantImpl.class));

        OnConstraint<AngleCnec> onAngleConstraint2 = new OnConstraintImpl<>(UsageMethod.AVAILABLE, PREVENTIVE_INSTANT, angleCnec);
        assertEquals(onAngleConstraint1, onAngleConstraint2);
        assertEquals(onAngleConstraint1.hashCode(), onAngleConstraint2.hashCode());

        onAngleConstraint2 = new OnConstraintImpl<>(UsageMethod.AVAILABLE, CURATIVE_INSTANT, angleCnec);
        assertNotEquals(onAngleConstraint1, onAngleConstraint2);
        assertNotEquals(onAngleConstraint1.hashCode(), onAngleConstraint2.hashCode());

        onAngleConstraint2 = new OnConstraintImpl<>(UsageMethod.AVAILABLE, PREVENTIVE_INSTANT, Mockito.mock(AngleCnec.class));
        assertNotEquals(onAngleConstraint1, onAngleConstraint2);
        assertNotEquals(onAngleConstraint1.hashCode(), onAngleConstraint2.hashCode());
    }

    @Test
    void testEqualsFlow() {
        OnConstraint<FlowCnec> onFlowConstraint1 = new OnConstraintImpl<>(UsageMethod.AVAILABLE, PREVENTIVE_INSTANT, flowCnec);
        assertEquals(onFlowConstraint1, onFlowConstraint1);
        assertEquals(onFlowConstraint1.hashCode(), onFlowConstraint1.hashCode());

        assertNotNull(onFlowConstraint1);
        assertNotEquals(onFlowConstraint1, Mockito.mock(OnInstantImpl.class));

        OnConstraint<FlowCnec> onFlowConstraint2 = new OnConstraintImpl<>(UsageMethod.AVAILABLE, PREVENTIVE_INSTANT, flowCnec);
        assertEquals(onFlowConstraint1, onFlowConstraint2);
        assertEquals(onFlowConstraint1.hashCode(), onFlowConstraint2.hashCode());

        onFlowConstraint2 = new OnConstraintImpl<>(UsageMethod.AVAILABLE, CURATIVE_INSTANT, flowCnec);
        assertNotEquals(onFlowConstraint1, onFlowConstraint2);
        assertNotEquals(onFlowConstraint1.hashCode(), onFlowConstraint2.hashCode());

        onFlowConstraint2 = new OnConstraintImpl<>(UsageMethod.AVAILABLE, PREVENTIVE_INSTANT, Mockito.mock(FlowCnec.class));
        assertNotEquals(onFlowConstraint1, onFlowConstraint2);
        assertNotEquals(onFlowConstraint1.hashCode(), onFlowConstraint2.hashCode());
    }

    @Test
    void testEqualsVoltage() {
        OnConstraint<VoltageCnec> onVoltageConstraint1 = new OnConstraintImpl<>(UsageMethod.AVAILABLE, PREVENTIVE_INSTANT, voltageCnec);
        assertEquals(onVoltageConstraint1, onVoltageConstraint1);
        assertEquals(onVoltageConstraint1.hashCode(), onVoltageConstraint1.hashCode());

        assertNotNull(onVoltageConstraint1);
        assertNotEquals(onVoltageConstraint1, Mockito.mock(OnInstantImpl.class));

        OnConstraint<VoltageCnec> onVoltageConstraint2 = new OnConstraintImpl<>(UsageMethod.AVAILABLE, PREVENTIVE_INSTANT, voltageCnec);
        assertEquals(onVoltageConstraint1, onVoltageConstraint2);
        assertEquals(onVoltageConstraint1.hashCode(), onVoltageConstraint2.hashCode());

        onVoltageConstraint2 = new OnConstraintImpl<>(UsageMethod.AVAILABLE, CURATIVE_INSTANT, voltageCnec);
        assertNotEquals(onVoltageConstraint1, onVoltageConstraint2);
        assertNotEquals(onVoltageConstraint1.hashCode(), onVoltageConstraint2.hashCode());

        onVoltageConstraint2 = new OnConstraintImpl<>(UsageMethod.AVAILABLE, PREVENTIVE_INSTANT, Mockito.mock(VoltageCnec.class));
        assertNotEquals(onVoltageConstraint1, onVoltageConstraint2);
        assertNotEquals(onVoltageConstraint1.hashCode(), onVoltageConstraint2.hashCode());
    }

    @Test
    void testEquals() {
        OnConstraint<AngleCnec> onAngleConstraint = new OnConstraintImpl<>(UsageMethod.AVAILABLE, PREVENTIVE_INSTANT, angleCnec);
        OnConstraint<FlowCnec> onFlowConstraint = new OnConstraintImpl<>(UsageMethod.AVAILABLE, PREVENTIVE_INSTANT, flowCnec);
        OnConstraint<VoltageCnec> onVoltageConstraint = new OnConstraintImpl<>(UsageMethod.AVAILABLE, PREVENTIVE_INSTANT, voltageCnec);

        assertNotEquals(onAngleConstraint, onFlowConstraint);
        assertNotEquals(onAngleConstraint, onVoltageConstraint);
        assertNotEquals(onFlowConstraint, onVoltageConstraint);
    }

    @Test
    void testGetUsageMethodAngle() {
        State curativeState2 = Mockito.mock(State.class);
        Mockito.when(curativeState2.getInstant()).thenReturn(CURATIVE_INSTANT);
        Mockito.when(curativeState2.isPreventive()).thenReturn(false);

        OnConstraint<AngleCnec> onAngleConstraint = new OnConstraintImpl<>(UsageMethod.AVAILABLE, PREVENTIVE_INSTANT, angleCnec);
        assertEquals(UsageMethod.AVAILABLE, onAngleConstraint.getUsageMethod(preventiveState));
        assertEquals(UsageMethod.UNDEFINED, onAngleConstraint.getUsageMethod(curativeState));
        assertEquals(UsageMethod.UNDEFINED, onAngleConstraint.getUsageMethod(curativeState2));

        Mockito.when(angleCnec.getState()).thenReturn(curativeState);
        onAngleConstraint = new OnConstraintImpl<>(UsageMethod.AVAILABLE, CURATIVE_INSTANT, angleCnec);
        assertEquals(UsageMethod.UNDEFINED, onAngleConstraint.getUsageMethod(preventiveState));
        assertEquals(UsageMethod.AVAILABLE, onAngleConstraint.getUsageMethod(curativeState));
        assertEquals(UsageMethod.UNDEFINED, onAngleConstraint.getUsageMethod(curativeState2));
    }

    @Test
    void testGetUsageMethodFlow() {
        State curativeState2 = Mockito.mock(State.class);
        Mockito.when(curativeState2.getInstant()).thenReturn(CURATIVE_INSTANT);
        Mockito.when(curativeState2.isPreventive()).thenReturn(false);

        OnConstraint<FlowCnec> onFlowConstraint = new OnConstraintImpl<>(UsageMethod.AVAILABLE, PREVENTIVE_INSTANT, flowCnec);
        assertEquals(UsageMethod.AVAILABLE, onFlowConstraint.getUsageMethod(preventiveState));
        assertEquals(UsageMethod.UNDEFINED, onFlowConstraint.getUsageMethod(curativeState));
        assertEquals(UsageMethod.UNDEFINED, onFlowConstraint.getUsageMethod(curativeState2));

        Mockito.when(flowCnec.getState()).thenReturn(curativeState);
        onFlowConstraint = new OnConstraintImpl<>(UsageMethod.AVAILABLE, CURATIVE_INSTANT, flowCnec);
        assertEquals(UsageMethod.UNDEFINED, onFlowConstraint.getUsageMethod(preventiveState));
        assertEquals(UsageMethod.AVAILABLE, onFlowConstraint.getUsageMethod(curativeState));
        assertEquals(UsageMethod.UNDEFINED, onFlowConstraint.getUsageMethod(curativeState2));
    }

    @Test
    void testGetUsageMethodVoltage() {
        State curativeState2 = Mockito.mock(State.class);
        Mockito.when(curativeState2.getInstant()).thenReturn(CURATIVE_INSTANT);
        Mockito.when(curativeState2.isPreventive()).thenReturn(false);

        OnConstraint<VoltageCnec> onVoltageConstraint = new OnConstraintImpl<>(UsageMethod.AVAILABLE, PREVENTIVE_INSTANT, voltageCnec);
        assertEquals(UsageMethod.AVAILABLE, onVoltageConstraint.getUsageMethod(preventiveState));
        assertEquals(UsageMethod.UNDEFINED, onVoltageConstraint.getUsageMethod(curativeState));
        assertEquals(UsageMethod.UNDEFINED, onVoltageConstraint.getUsageMethod(curativeState2));

        Mockito.when(voltageCnec.getState()).thenReturn(curativeState);
        onVoltageConstraint = new OnConstraintImpl<>(UsageMethod.AVAILABLE, CURATIVE_INSTANT, voltageCnec);
        assertEquals(UsageMethod.UNDEFINED, onVoltageConstraint.getUsageMethod(preventiveState));
        assertEquals(UsageMethod.AVAILABLE, onVoltageConstraint.getUsageMethod(curativeState));
        assertEquals(UsageMethod.UNDEFINED, onVoltageConstraint.getUsageMethod(curativeState2));
    }
}
