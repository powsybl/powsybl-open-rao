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
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static com.farao_community.farao.data.crac_api.RangeDefinition.CENTERED_ON_ZERO;
import static com.farao_community.farao.data.crac_api.RangeDefinition.STARTS_AT_ONE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class PstSetpointTest extends AbstractRemedialActionTest {

    private String networkElementId;
    private PstSetpoint pstSetpointStartsAtOne;
    private PstSetpoint pstSetpointCenteredOnZero;

    @Before
    public void setUp() {
        networkElementId = "BBE2AA1  BBE3AA1  1";
        pstSetpointStartsAtOne = new PstSetpoint(
                "pstsetpoint_id",
                new NetworkElement(networkElementId),
                12,
                STARTS_AT_ONE);
        pstSetpointCenteredOnZero = new PstSetpoint(
                "pstsetpoint_id",
                new NetworkElement(networkElementId),
                0,
                CENTERED_ON_ZERO);
    }

    @Test
    public void basicMethods() {
        assertEquals(12, pstSetpointStartsAtOne.getSetpoint(), 0);
        pstSetpointStartsAtOne.setSetpoint(0);
        assertEquals(0, pstSetpointStartsAtOne.getSetpoint(), 0);
    }

    @Test
    public void getNetworkElements() {
        Set<NetworkElement> pstNetworkElements = pstSetpointStartsAtOne.getNetworkElements();
        assertEquals(networkElementId, pstNetworkElements.iterator().next().getId());
    }

    @Test
    public void applyStartsAtOne() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        network.getTwoWindingsTransformer(networkElementId).getPhaseTapChanger().setLowTapPosition(1);
        pstSetpointStartsAtOne.apply(network);
        assertEquals(12, network.getTwoWindingsTransformer(networkElementId).getPhaseTapChanger().getTapPosition());
    }

    @Test
    public void applycenteredOnZero() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        network.getTwoWindingsTransformer(networkElementId).getPhaseTapChanger().setLowTapPosition(-16);
        pstSetpointCenteredOnZero.apply(network);
        assertEquals(0, network.getTwoWindingsTransformer(networkElementId).getPhaseTapChanger().getTapPosition());
    }

    @Test
    public void applyOutOfBoundStartsAtOne() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        PstSetpoint pstSetpoint = new PstSetpoint(
                "out_of_bound",
                new NetworkElement(networkElementId),
                50,
                STARTS_AT_ONE);
        try {
            pstSetpoint.apply(network);
            fail();
        } catch (FaraoException e) {
            assertEquals(String.format("Tap value 33 not in the range of high and low tap positions [-16,16] of the phase tap changer %s steps", networkElementId), e.getMessage());
        }
    }

    @Test
    public void applyOutOfBoundCenteredOnZero() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        PstSetpoint pstSetpoint = new PstSetpoint(
                "out_of_bound",
                new NetworkElement(networkElementId),
                50,
                CENTERED_ON_ZERO);
        try {
            pstSetpoint.apply(network);
            fail();
        } catch (FaraoException e) {
            assertEquals(String.format("Tap value 50 not in the range of high and low tap positions [-16,16] of the phase tap changer %s steps", networkElementId), e.getMessage());
        }
    }
}
