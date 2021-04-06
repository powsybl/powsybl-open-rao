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
import org.junit.Test;

import static com.farao_community.farao.data.crac_api.RangeDefinition.CENTERED_ON_ZERO;
import static com.farao_community.farao.data.crac_api.RangeDefinition.STARTS_AT_ONE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class PstSetpointImplTest extends AbstractRemedialActionTest {

    @Test
    public void basicMethods() {
        PstSetpointImpl pstSetpoint = new PstSetpointImpl(
            new NetworkElement("BBE2AA1  BBE3AA1  1"),
            12,
            STARTS_AT_ONE);

        assertEquals(12, pstSetpoint.getSetPoint(), 0);
        assertEquals(STARTS_AT_ONE, pstSetpoint.getRangeDefinition());
    }

    @Test
    public void applyStartsAtOne1() {
        PstSetpointImpl pstSetpoint = new PstSetpointImpl(
            new NetworkElement("BBE2AA1  BBE3AA1  1"),
            12,
            STARTS_AT_ONE);

        Network network = NetworkImportsUtil.import12NodesNetwork();
        network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().setLowTapPosition(1);
        pstSetpoint.apply(network);
        assertEquals(12, network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getTapPosition());
    }

    @Test
    public void applyStartsAtOne2() {
        PstSetpointImpl pstSetpoint = new PstSetpointImpl(
            new NetworkElement("BBE2AA1  BBE3AA1  1"),
            12,
            STARTS_AT_ONE);

        Network network = NetworkImportsUtil.import12NodesNetwork();
        pstSetpoint.apply(network);
        assertEquals(-5, network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getTapPosition());
    }

    @Test
    public void applyCenteredOnZero() {
        PstSetpointImpl pstSetpoint = new PstSetpointImpl(
            new NetworkElement("BBE2AA1  BBE3AA1  1"),
            -9,
            CENTERED_ON_ZERO);

        Network network = NetworkImportsUtil.import12NodesNetwork();
        pstSetpoint.apply(network);
        assertEquals(-9, network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getTapPosition());
    }

    @Test
    public void applyOutOfBoundStartsAtOne() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        PstSetpointImpl pstSetpoint = new PstSetpointImpl(
                new NetworkElement("BBE2AA1  BBE3AA1  1"),
                50,
                STARTS_AT_ONE);
        try {
            pstSetpoint.apply(network);
            fail();
        } catch (FaraoException e) {
            assertEquals("Tap value 33 not in the range of high and low tap positions [-16,16] of the phase tap changer BBE2AA1  BBE3AA1  1 steps", e.getMessage());
        }
    }

    @Test
    public void applyOutOfBoundCenteredOnZero() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        PstSetpointImpl pstSetpoint = new PstSetpointImpl(
                new NetworkElement("BBE2AA1  BBE3AA1  1"),
                50,
                CENTERED_ON_ZERO);
        try {
            pstSetpoint.apply(network);
            fail();
        } catch (FaraoException e) {
            assertEquals("Tap value 50 not in the range of high and low tap positions [-16,16] of the phase tap changer BBE2AA1  BBE3AA1  1 steps", e.getMessage());
        }
    }
}
