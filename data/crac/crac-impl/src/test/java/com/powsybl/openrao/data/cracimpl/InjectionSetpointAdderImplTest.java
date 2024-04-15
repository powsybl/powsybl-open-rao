/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.networkaction.GeneratorActionAdder;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkActionAdder;
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
        GeneratorActionAdder injectionSetpointAdder = networkActionAdder.newInjectionSetPoint()
            .withSetpoint(100.).withUnit(Unit.MEGAWATT);
        Exception e = assertThrows(OpenRaoException.class, injectionSetpointAdder::add);
        assertEquals("Cannot add InjectionSetPoint without a network element. Please use withNetworkElement() with a non null value", e.getMessage());
    }

    @Test
    void testNoSetpoint() {
        GeneratorActionAdder injectionSetpointAdder = networkActionAdder.newInjectionSetPoint()
            .withNetworkElement("groupNetworkElementId").withUnit(Unit.MEGAWATT);
        Exception e = assertThrows(OpenRaoException.class, injectionSetpointAdder::add);
        assertEquals("Cannot add InjectionSetPoint without a setpoint. Please use withSetPoint() with a non null value", e.getMessage());
    }

    @Test
    void testNoUnit() {
        GeneratorActionAdder injectionSetpointAdder = networkActionAdder.newInjectionSetPoint()
                .withNetworkElement("groupNetworkElementId").withSetpoint(100.);
        Exception e = assertThrows(OpenRaoException.class, injectionSetpointAdder::add);
        assertEquals("Cannot add InjectionSetPoint without a unit. Please use withUnit() with a non null value", e.getMessage());
    }

    @Test
    void testNegativeSetPointWithSectionCount() {
        GeneratorActionAdder injectionSetpointAdder = networkActionAdder.newInjectionSetPoint()
                .withNetworkElement("groupNetworkElementId").withSetpoint(-100.).withUnit(Unit.SECTION_COUNT);
        Exception e = assertThrows(OpenRaoException.class, injectionSetpointAdder::add);
        assertEquals("With a SECTION_COUNT unit, setpoint should be a positive integer", e.getMessage());
    }

    @Test
    void testNonIntegerSetPointWithSectionCount() {
        GeneratorActionAdder injectionSetpointAdder = networkActionAdder.newInjectionSetPoint()
                .withNetworkElement("groupNetworkElementId").withSetpoint(1.3).withUnit(Unit.SECTION_COUNT);
        Exception e = assertThrows(OpenRaoException.class, injectionSetpointAdder::add);
        assertEquals("With a SECTION_COUNT unit, setpoint should be a positive integer", e.getMessage());
    }
}
