/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.commons.PowsyblException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.networkaction.PstSetpoint;
import com.powsybl.openrao.data.cracimpl.utils.NetworkImportsUtil;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class PstSetpointImplTest {

    @Test
    void basicMethods() {
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction pstSetpoint = crac.newNetworkAction()
            .withId("pstSetpoint")
            .newPstSetPoint()
                .withNetworkElement("BBE2AA1  BBE3AA1  1")
                .withSetpoint(12)
                .add()
            .add();
        assertEquals(1, pstSetpoint.getNetworkElements().size());
        assertEquals("BBE2AA1  BBE3AA1  1", pstSetpoint.getNetworkElements().iterator().next().getId());
        assertTrue(pstSetpoint.canBeApplied(Mockito.mock(Network.class)));
    }

    @Test
    void hasImpactOnNetwork() {
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction pstSetpoint = crac.newNetworkAction()
            .withId("pstSetpoint")
            .newPstSetPoint()
            .withNetworkElement("BBE2AA1  BBE3AA1  1")
            .withSetpoint(-9)
            .add()
            .add();
        Network network = NetworkImportsUtil.import12NodesNetwork();
        assertTrue(pstSetpoint.hasImpactOnNetwork(network));
    }

    @Test
    void hasNoImpactOnNetwork() {
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction pstSetpoint = crac.newNetworkAction()
            .withId("pstSetpoint")
            .newPstSetPoint()
            .withNetworkElement("BBE2AA1  BBE3AA1  1")
            .withSetpoint(0)
            .add()
            .add();
        Network network = NetworkImportsUtil.import12NodesNetwork();
        assertFalse(pstSetpoint.hasImpactOnNetwork(network));

    }

    @Test
    void applyCenteredOnZero() {
        PstSetpointImpl pstSetpoint = new PstSetpointImpl(
            new NetworkElementImpl("BBE2AA1  BBE3AA1  1"),
            -9);

        Network network = NetworkImportsUtil.import12NodesNetwork();
        assertEquals(0, network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getTapPosition());
        pstSetpoint.apply(network);
        assertEquals(-9, network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getTapPosition());
    }

    @Test
    void applyOutOfBoundStartsAtOne() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        PstSetpointImpl pstSetpoint = new PstSetpointImpl(
            new NetworkElementImpl("BBE2AA1  BBE3AA1  1"),
            17);
        try {
            pstSetpoint.apply(network);
            fail();
        } catch (PowsyblException e) {
            assertEquals("2 windings transformer 'BBE2AA1  BBE3AA1  1': incorrect tap position 17 [-16, 16]", e.getMessage());
        }
    }

    @Test
    void applyOutOfBoundCenteredOnZero() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        PstSetpointImpl pstSetpoint = new PstSetpointImpl(
            new NetworkElementImpl("BBE2AA1  BBE3AA1  1"),
            50);
        try {
            pstSetpoint.apply(network);
            fail();
        } catch (PowsyblException e) {
            assertEquals("2 windings transformer 'BBE2AA1  BBE3AA1  1': incorrect tap position 50 [-16, 16]", e.getMessage());
        }
    }

    @Test
    void equals() {
        PstSetpoint pstSetpoint = new PstSetpointImpl(new NetworkElementImpl("BBE2AA1  BBE3AA1  1"), -9);
        assertEquals(pstSetpoint, new PstSetpointImpl(new NetworkElementImpl("BBE2AA1  BBE3AA1  1"), -9));

        PstSetpoint differentPstSetpointOnSetPoint = new PstSetpointImpl(new NetworkElementImpl("BBE2AA1  BBE3AA1  1"), -10);
        assertNotEquals(pstSetpoint, differentPstSetpointOnSetPoint);

        PstSetpoint differentPstSetpointOnNetworkEl = new PstSetpointImpl(new NetworkElementImpl("BBE2AA1  BBE3AA1  2"), -9);
        assertNotEquals(pstSetpoint, differentPstSetpointOnNetworkEl);
    }
}
