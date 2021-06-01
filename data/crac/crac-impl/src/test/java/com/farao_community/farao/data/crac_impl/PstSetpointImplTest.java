/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.network_action.PstSetpoint;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.iidm.network.Network;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class PstSetpointImplTest {

    @Test
    public void basicMethods() {
        PstSetpointImpl pstSetpoint = new PstSetpointImpl(
            new NetworkElementImpl("BBE2AA1  BBE3AA1  1"),
            12);

        assertEquals(12, pstSetpoint.getSetpoint(), 0);
    }

    @Test
    public void applyCenteredOnZero() {
        PstSetpointImpl pstSetpoint = new PstSetpointImpl(
            new NetworkElementImpl("BBE2AA1  BBE3AA1  1"),
            -9);

        Network network = NetworkImportsUtil.import12NodesNetwork();
        pstSetpoint.apply(network);
        assertEquals(-9, network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getTapPosition());
    }

    @Test
    public void applyOutOfBoundStartsAtOne() {
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
    public void applyOutOfBoundCenteredOnZero() {
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
    public void equals() {
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
