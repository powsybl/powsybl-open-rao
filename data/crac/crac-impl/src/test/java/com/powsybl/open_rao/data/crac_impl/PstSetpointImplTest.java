/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.data.crac_impl;

import com.powsybl.open_rao.commons.FaraoException;
import com.powsybl.open_rao.data.crac_api.NetworkElement;
import com.powsybl.open_rao.data.crac_api.network_action.PstSetpoint;
import com.powsybl.open_rao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class PstSetpointImplTest {

    @Test
    void basicMethods() {
        NetworkElement ne = new NetworkElementImpl("BBE2AA1  BBE3AA1  1");
        PstSetpointImpl pstSetpoint = new PstSetpointImpl(ne, 12);

        assertEquals(12, pstSetpoint.getSetpoint(), 0);
        assertEquals(ne, pstSetpoint.getNetworkElement());
        assertEquals(Set.of(ne), pstSetpoint.getNetworkElements());
        assertTrue(pstSetpoint.canBeApplied(Mockito.mock(Network.class)));
    }

    @Test
    void hasImpactOnNetwork() {
        PstSetpointImpl pstSetpoint = new PstSetpointImpl(
            new NetworkElementImpl("BBE2AA1  BBE3AA1  1"),
            -9);
        Network network = NetworkImportsUtil.import12NodesNetwork();

        assertTrue(pstSetpoint.hasImpactOnNetwork(network));
    }

    @Test
    void hasNoImpactOnNetwork() {
        PstSetpointImpl pstSetpoint = new PstSetpointImpl(
            new NetworkElementImpl("BBE2AA1  BBE3AA1  1"),
            0);
        Network network = NetworkImportsUtil.import12NodesNetwork();

        assertFalse(pstSetpoint.hasImpactOnNetwork(network));
    }

    @Test
    void applyCenteredOnZero() {
        PstSetpointImpl pstSetpoint = new PstSetpointImpl(
            new NetworkElementImpl("BBE2AA1  BBE3AA1  1"),
            -9);

        Network network = NetworkImportsUtil.import12NodesNetwork();
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
        } catch (FaraoException e) {
            assertEquals("Tap value 17 not in the range of high and low tap positions [-16,16] of the phase tap changer BBE2AA1  BBE3AA1  1 steps", e.getMessage());
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
        } catch (FaraoException e) {
            assertEquals("Tap value 50 not in the range of high and low tap positions [-16,16] of the phase tap changer BBE2AA1  BBE3AA1  1 steps", e.getMessage());
        }
    }

    @Test
    void equals() {
        PstSetpoint pstSetpoint = new PstSetpointImpl(
            new NetworkElementImpl("BBE2AA1  BBE3AA1  1"),
            -9);
        assertEquals(pstSetpoint, pstSetpoint);

        PstSetpoint samePstSetpoint = new PstSetpointImpl(
            new NetworkElementImpl("BBE2AA1  BBE3AA1  1"),
            -9);
        assertEquals(samePstSetpoint, samePstSetpoint);

        PstSetpoint differentPstSetpoint = new PstSetpointImpl(
            new NetworkElementImpl("BBE2AA1  BBE3AA1  1"),
            -10);
        assertNotEquals(pstSetpoint, differentPstSetpoint);
    }
}
