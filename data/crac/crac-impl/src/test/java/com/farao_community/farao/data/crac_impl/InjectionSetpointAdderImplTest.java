/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.network_action.InjectionSetpoint;
import com.farao_community.farao.data.crac_api.network_action.InjectionSetpointAdder;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.network_action.NetworkActionAdder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class InjectionSetpointAdderImplTest {

    private Crac crac;
    private NetworkActionAdder networkActionAdder;

    @BeforeEach
    public void setUp() {
        crac = new CracImplFactory().create("cracId");
        networkActionAdder = crac.newNetworkAction()
            .withId("networkActionId")
            .withName("networkActionName")
            .withOperator("operator");
    }

    @Test
    void testOk() {
        NetworkAction networkAction = networkActionAdder.newInjectionSetPoint()
            .withNetworkElement("groupNetworkElementId")
            .withSetpoint(100.)
            .withUnit(Unit.MEGAWATT)
            .add()
            .add();

        InjectionSetpoint injectionSetpoint = (InjectionSetpoint) networkAction.getElementaryActions().iterator().next();
        assertEquals("groupNetworkElementId", injectionSetpoint.getNetworkElement().getId());
        assertEquals(100., injectionSetpoint.getSetpoint(), 1e-3);

        // check that network element have been added to CracImpl
        assertEquals(1, ((CracImpl) crac).getNetworkElements().size());
        assertNotNull(((CracImpl) crac).getNetworkElement("groupNetworkElementId"));
    }

    @Test
    void testNoNetworkElement() {
        InjectionSetpointAdder injectionSetpointAdder = networkActionAdder.newInjectionSetPoint()
            .withSetpoint(100.).withUnit(Unit.MEGAWATT);
        Exception e = assertThrows(FaraoException.class, injectionSetpointAdder::add);
        assertEquals("Cannot add InjectionSetPoint without a network element. Please use withNetworkElement() with a non null value", e.getMessage());
    }

    @Test
    void testNoSetpoint() {
        InjectionSetpointAdder injectionSetpointAdder = networkActionAdder.newInjectionSetPoint()
            .withNetworkElement("groupNetworkElementId").withUnit(Unit.MEGAWATT);
        Exception e = assertThrows(FaraoException.class, injectionSetpointAdder::add);
        assertEquals("Cannot add InjectionSetPoint without a setpoint. Please use withSetPoint() with a non null value", e.getMessage());
    }

    @Test
    void testNoUnit() {
        InjectionSetpointAdder injectionSetpointAdder = networkActionAdder.newInjectionSetPoint()
                .withNetworkElement("groupNetworkElementId").withSetpoint(100.);
        Exception e = assertThrows(FaraoException.class, injectionSetpointAdder::add);
        assertEquals("Cannot add InjectionSetPoint without a unit. Please use withUnit() with a non null value", e.getMessage());
    }

    @Test
    void testNegativeSetPointWithSectionCount() {
        InjectionSetpointAdder injectionSetpointAdder = networkActionAdder.newInjectionSetPoint()
                .withNetworkElement("groupNetworkElementId").withSetpoint(-100.).withUnit(Unit.SECTION_COUNT);
        Exception e = assertThrows(FaraoException.class, injectionSetpointAdder::add);
        assertEquals("With a SECTION_COUNT unit, setpoint should be a positive integer", e.getMessage());
    }

    @Test
    void testNonIntegerSetPointWithSectionCount() {
        InjectionSetpointAdder injectionSetpointAdder = networkActionAdder.newInjectionSetPoint()
                .withNetworkElement("groupNetworkElementId").withSetpoint(1.3).withUnit(Unit.SECTION_COUNT);
        Exception e = assertThrows(FaraoException.class, injectionSetpointAdder::add);
        assertEquals("With a SECTION_COUNT unit, setpoint should be a positive integer", e.getMessage());
    }
}
