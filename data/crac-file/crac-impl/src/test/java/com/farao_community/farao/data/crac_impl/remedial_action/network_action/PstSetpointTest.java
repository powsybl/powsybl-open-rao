/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.network_action;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_impl.AbstractRemedialActionTest;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class PstSetpointTest extends AbstractRemedialActionTest {

    private String networkElementId;
    private PstSetpoint pstSetpoint;

    @Before
    public void setUp() {
        networkElementId = "BBE2AA1  BBE3AA1  1";
        pstSetpoint = new PstSetpoint(
                "pstsetpoint_id",
                new NetworkElement(networkElementId),
                12);
    }

    @Test
    public void basicMethods() {
        assertEquals(12, pstSetpoint.getSetpoint(), 0);
        pstSetpoint.setSetpoint(0);
        assertEquals(0, pstSetpoint.getSetpoint(), 0);
    }

    @Test
    public void apply() {
        Network network = Importers.loadNetwork(
                "TestCase12Nodes.uct",
                getClass().getResourceAsStream("/TestCase12Nodes.uct")
        );
        assertEquals(0, network.getTwoWindingsTransformer(networkElementId).getPhaseTapChanger().getTapPosition());
        pstSetpoint.apply(network);
        assertEquals(-5, network.getTwoWindingsTransformer(networkElementId).getPhaseTapChanger().getTapPosition());
    }

    @Test
    public void applyOutOfBound() {
        Network network = Importers.loadNetwork(
                "TestCase12Nodes.uct",
                getClass().getResourceAsStream("/TestCase12Nodes.uct")
        );
        PstSetpoint pstSetpoint = new PstSetpoint(
                "out_of_bound",
                new NetworkElement(networkElementId),
                50);

        try {
            pstSetpoint.apply(network);
            fail();
        } catch (FaraoException e) {
            assertEquals(String.format("Tap value 33 not in the range of high and low tap positions [-16,16] of the phase tap changer %s steps", networkElementId), e.getMessage());
        }
    }

    @Test
    public void getNetworkElements() {
        Set<NetworkElement> pstNetworkElements = pstSetpoint.getNetworkElements();
        assertEquals(networkElementId, pstNetworkElements.iterator().next().getId());
    }

}
