/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.action.PhaseTapChangerTapPositionAction;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkActionAdder;
import com.powsybl.openrao.data.crac.api.networkaction.PhaseTapChangerTapPositionActionAdder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class PhaseTapChangerTapPositionActionAdderImplTest {

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

        NetworkAction networkAction = networkActionAdder.newPhaseTapChangerTapPositionAction()
            .withNetworkElement("pstNetworkElementId")
            .withTapPosition(0)
            .add()
            .add();

        PhaseTapChangerTapPositionAction phaseTapChangerTapPositionAction = (PhaseTapChangerTapPositionAction) networkAction.getElementaryActions().iterator().next();
        assertEquals("pstNetworkElementId", phaseTapChangerTapPositionAction.getTransformerId());
        assertEquals(0, phaseTapChangerTapPositionAction.getTapPosition(), 1e-3);

        // check that network element has been created in CracImpl
        assertEquals(1, ((CracImpl) crac).getNetworkElements().size());
        assertNotNull(((CracImpl) crac).getNetworkElement("pstNetworkElementId"));
    }

    @Test
    void testNoNetworkElement() {
        PhaseTapChangerTapPositionActionAdder pstSetpointAdder = networkActionAdder.newPhaseTapChangerTapPositionAction()
            .withTapPosition(0);
        assertThrows(OpenRaoException.class, pstSetpointAdder::add);
    }

    @Test
    void testNoSetpoint() {
        PhaseTapChangerTapPositionActionAdder pstSetpointAdder = networkActionAdder.newPhaseTapChangerTapPositionAction()
            .withNetworkElement("pstNetworkElementId");
        assertThrows(OpenRaoException.class, pstSetpointAdder::add);
    }
}
