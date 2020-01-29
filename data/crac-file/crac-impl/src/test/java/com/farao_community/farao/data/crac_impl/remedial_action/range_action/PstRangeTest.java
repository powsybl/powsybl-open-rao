/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.range_action;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_impl.range_domain.Range;
import com.farao_community.farao.data.crac_impl.range_domain.RangeType;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;


import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class PstRangeTest extends AbstractNetworkElementRangeActionTest {

    private String networkElementId = "BBE2AA1  BBE3AA1  1";
    private PstRange pstRange;

    @Before
    public void setUp() throws Exception {
        PstRange pstRange = new PstRange(
                "pst_range_id",
                "pst_range_name",
                "pst_range_operator",
                createUsageRules(),
                createRanges(),
                new NetworkElement(networkElementId, networkElementId));
        this.pstRange = pstRange;
    }

    @Test
    public void apply() {
        Network network = Importers.loadNetwork(
            "TestCase12Nodes.uct",
            getClass().getResourceAsStream("/TestCase12Nodes.uct")
        );
        assertEquals(0, network.getTwoWindingsTransformer(networkElementId).getPhaseTapChanger().getTapPosition());
        pstRange.apply(network, 12);
        assertEquals(-5, network.getTwoWindingsTransformer(networkElementId).getPhaseTapChanger().getTapPosition());
    }

    @Test
    public void applyOutOfBound() {
        Network network = Importers.loadNetwork(
            "TestCase12Nodes.uct",
            getClass().getResourceAsStream("/TestCase12Nodes.uct")
        );
        try {
            pstRange.apply(network, 50);
            fail();
        } catch (FaraoException e) {
            assertEquals("PST cannot be set because setpoint is out of PST boundaries", e.getMessage());
        }
    }

    @Test
    public void applyOnUnknownPst() {
        Network network = Importers.loadNetwork(
            "TestCase12Nodes.uct",
            getClass().getResourceAsStream("/TestCase12Nodes.uct")
        );
        PstRange unknownPstRange = new PstRange(
                "unknown_pstrange_id",
                "unknown_pstrange_name",
                "unknown_pstrange_operator",
                createUsageRules(),
                createRanges(),
                new NetworkElement("unknown pst", "unknown pst"));
        try {
            unknownPstRange.apply(network, 50);
            fail();
        } catch (FaraoException e) {
            assertEquals("PST unknown pst does not exist in the current network", e.getMessage());
        }
    }

    @Test
    public void applyOnTransformerWithNoPhaseShifter() {
        Network network = Importers.loadNetwork(
            "TestCase12Nodes_no_pst.uct",
            getClass().getResourceAsStream("/TestCase12Nodes_no_pst.uct"));
        String notPstRangeElementId = "BBE2AA1  BBE3AA1  1";
        PstRange notAPstRange = new PstRange(
                "not_pstrange_id",
                "not_pstrange_name",
                "not_pstrange_operator",
                createUsageRules(),
                createRanges(),
                new NetworkElement(notPstRangeElementId, notPstRangeElementId));
        try {
            notAPstRange.apply(network, 50);
            fail();
        } catch (FaraoException e) {
            assertEquals(String.format("Transformer %s is not a PST but is defined as a PstRange", notPstRangeElementId), e.getMessage());
        }
    }

    @Test
    public void getMinAndMaxValueWithRange() {
        Network network = Mockito.mock(Network.class);
        Range range = Mockito.mock(Range.class);
        TwoWindingsTransformer twoWindingsTransformer = Mockito.mock(TwoWindingsTransformer.class);
        PhaseTapChanger phaseTapChanger = Mockito.mock(PhaseTapChanger.class);
        Mockito.when(range.getMin()).thenReturn(3.0);
        Mockito.when(range.getMax()).thenReturn(3.0);
        Mockito.when(range.getRangeType()).thenReturn(RangeType.RELATIVE_FIXED);
        Mockito.when(network.getTwoWindingsTransformer(pstRange.getNetworkElement().getId())).thenReturn(twoWindingsTransformer);
        Mockito.when(twoWindingsTransformer.getPhaseTapChanger()).thenReturn(phaseTapChanger);
        Mockito.when(phaseTapChanger.getTapPosition()).thenReturn(10);
        assertEquals(13, pstRange.getMaxValueWithRange(network, range), 0);
        assertEquals(7, pstRange.getMinValueWithRange(network, range), 0);
    }
}
