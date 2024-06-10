/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracimpl;

import com.powsybl.action.GeneratorAction;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.networkaction.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class GeneratorActionAdderImplTest {

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
        NetworkAction networkAction = networkActionAdder.newGeneratorAction()
            .withNetworkElement("groupNetworkElementId")
            .withActivePowerValue(100.)
            .add()
            .add();

        GeneratorAction generatorAction = (GeneratorAction) networkAction.getElementaryActions().iterator().next();
        assertEquals("groupNetworkElementId", generatorAction.getGeneratorId());
        assertEquals(100., generatorAction.getActivePowerValue().getAsDouble(), 1e-3);

        // check that network element have been added to CracImpl
        assertEquals(1, ((CracImpl) crac).getNetworkElements().size());
        assertNotNull(((CracImpl) crac).getNetworkElement("groupNetworkElementId"));
    }

    @Test
    void testNoNetworkElement() {
        GeneratorActionAdder generatorActionAdder = networkActionAdder.newGeneratorAction()
            .withActivePowerValue(100.);
        Exception e = assertThrows(OpenRaoException.class, generatorActionAdder::add);
        assertEquals("Cannot add GeneratorAction without a network element. Please use withNetworkElement() with a non null value", e.getMessage());
    }

    @Test
    void testNoSetpoint() {
        GeneratorActionAdder generatorActionAdder = networkActionAdder.newGeneratorAction()
            .withNetworkElement("groupNetworkElementId");
        Exception e = assertThrows(OpenRaoException.class, generatorActionAdder::add);
        assertEquals("Cannot add GeneratorAction without a activePowerValue. Please use withActivePowerValue() with a non null value", e.getMessage());
    }

}
