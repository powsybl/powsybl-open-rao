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
        OnConstraint<AngleCnec> onAngleConstraint = new OnConstraintImpl<>(PREVENTIVE_INSTANT, angleCnec);

        assertEquals(PREVENTIVE_INSTANT, onAngleConstraint.getInstant());
        assertSame(angleCnec, onAngleConstraint.getCnec());
    }

    @Test
    void testConstructorFlow() {
        OnConstraint<FlowCnec> onFlowConstraint = new OnConstraintImpl<>(PREVENTIVE_INSTANT, flowCnec);

        assertEquals(PREVENTIVE_INSTANT, onFlowConstraint.getInstant());
        assertSame(flowCnec, onFlowConstraint.getCnec());
    }

    @Test
    void testConstructorVoltage() {
        OnConstraint<VoltageCnec> onVoltageConstraint = new OnConstraintImpl<>(PREVENTIVE_INSTANT, voltageCnec);

        assertEquals(PREVENTIVE_INSTANT, onVoltageConstraint.getInstant());
        assertSame(voltageCnec, onVoltageConstraint.getCnec());
    }

    @Test
    void testEqualsAngle() {
        OnConstraint<AngleCnec> onAngleConstraint1 = new OnConstraintImpl<>(PREVENTIVE_INSTANT, angleCnec);
        assertEquals(onAngleConstraint1, onAngleConstraint1);
        assertEquals(onAngleConstraint1.hashCode(), onAngleConstraint1.hashCode());

        assertNotNull(onAngleConstraint1);
        assertNotEquals(onAngleConstraint1, Mockito.mock(OnInstantImpl.class));

        OnConstraint<AngleCnec> onAngleConstraint2 = new OnConstraintImpl<>(PREVENTIVE_INSTANT, angleCnec);
        assertEquals(onAngleConstraint1, onAngleConstraint2);
        assertEquals(onAngleConstraint1.hashCode(), onAngleConstraint2.hashCode());

        onAngleConstraint2 = new OnConstraintImpl<>(CURATIVE_INSTANT, angleCnec);
        assertNotEquals(onAngleConstraint1, onAngleConstraint2);
        assertNotEquals(onAngleConstraint1.hashCode(), onAngleConstraint2.hashCode());

        onAngleConstraint2 = new OnConstraintImpl<>(PREVENTIVE_INSTANT, Mockito.mock(AngleCnec.class));
        assertNotEquals(onAngleConstraint1, onAngleConstraint2);
        assertNotEquals(onAngleConstraint1.hashCode(), onAngleConstraint2.hashCode());
    }

    @Test
    void testEqualsFlow() {
        OnConstraint<FlowCnec> onFlowConstraint1 = new OnConstraintImpl<>(PREVENTIVE_INSTANT, flowCnec);
        assertEquals(onFlowConstraint1, onFlowConstraint1);
        assertEquals(onFlowConstraint1.hashCode(), onFlowConstraint1.hashCode());

        assertNotNull(onFlowConstraint1);
        assertNotEquals(onFlowConstraint1, Mockito.mock(OnInstantImpl.class));

        OnConstraint<FlowCnec> onFlowConstraint2 = new OnConstraintImpl<>(PREVENTIVE_INSTANT, flowCnec);
        assertEquals(onFlowConstraint1, onFlowConstraint2);
        assertEquals(onFlowConstraint1.hashCode(), onFlowConstraint2.hashCode());

        onFlowConstraint2 = new OnConstraintImpl<>(CURATIVE_INSTANT, flowCnec);
        assertNotEquals(onFlowConstraint1, onFlowConstraint2);
        assertNotEquals(onFlowConstraint1.hashCode(), onFlowConstraint2.hashCode());

        onFlowConstraint2 = new OnConstraintImpl<>(PREVENTIVE_INSTANT, Mockito.mock(FlowCnec.class));
        assertNotEquals(onFlowConstraint1, onFlowConstraint2);
        assertNotEquals(onFlowConstraint1.hashCode(), onFlowConstraint2.hashCode());
    }

    @Test
    void testEqualsVoltage() {
        OnConstraint<VoltageCnec> onVoltageConstraint1 = new OnConstraintImpl<>(PREVENTIVE_INSTANT, voltageCnec);
        assertEquals(onVoltageConstraint1, onVoltageConstraint1);
        assertEquals(onVoltageConstraint1.hashCode(), onVoltageConstraint1.hashCode());

        assertNotNull(onVoltageConstraint1);
        assertNotEquals(onVoltageConstraint1, Mockito.mock(OnInstantImpl.class));

        OnConstraint<VoltageCnec> onVoltageConstraint2 = new OnConstraintImpl<>(PREVENTIVE_INSTANT, voltageCnec);
        assertEquals(onVoltageConstraint1, onVoltageConstraint2);
        assertEquals(onVoltageConstraint1.hashCode(), onVoltageConstraint2.hashCode());

        onVoltageConstraint2 = new OnConstraintImpl<>(CURATIVE_INSTANT, voltageCnec);
        assertNotEquals(onVoltageConstraint1, onVoltageConstraint2);
        assertNotEquals(onVoltageConstraint1.hashCode(), onVoltageConstraint2.hashCode());

        onVoltageConstraint2 = new OnConstraintImpl<>(PREVENTIVE_INSTANT, Mockito.mock(VoltageCnec.class));
        assertNotEquals(onVoltageConstraint1, onVoltageConstraint2);
        assertNotEquals(onVoltageConstraint1.hashCode(), onVoltageConstraint2.hashCode());
    }

    @Test
    void testEquals() {
        OnConstraint<AngleCnec> onAngleConstraint = new OnConstraintImpl<>(PREVENTIVE_INSTANT, angleCnec);
        OnConstraint<FlowCnec> onFlowConstraint = new OnConstraintImpl<>(PREVENTIVE_INSTANT, flowCnec);
        OnConstraint<VoltageCnec> onVoltageConstraint = new OnConstraintImpl<>(PREVENTIVE_INSTANT, voltageCnec);

        assertNotEquals(onAngleConstraint, onFlowConstraint);
        assertNotEquals(onAngleConstraint, onVoltageConstraint);
        assertNotEquals(onFlowConstraint, onVoltageConstraint);
    }
}
