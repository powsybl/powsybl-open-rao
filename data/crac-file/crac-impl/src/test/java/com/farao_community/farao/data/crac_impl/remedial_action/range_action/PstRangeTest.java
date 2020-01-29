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

    private Network network;
    private PstRange pstRange1;

    private Range range1;

    @Before
    public void setUp() throws Exception {
        pstRange = new PstRange(
                "pst_range_id",
                "pst_range_name",
                "pst_range_operator",
                createUsageRules(),
                createRanges(),
                new NetworkElement(networkElementId, networkElementId));

        network = Mockito.mock(Network.class);
        range1 = Mockito.mock(Range.class);

        Mockito.when(range1.getRangeType()).thenReturn(RangeType.ABSOLUTE_FIXED);
        Mockito.when(range1.getMin()).thenReturn(5.);
        Mockito.when(range1.getMax()).thenReturn(13.);

        Range range2 = Mockito.mock(Range.class);
        Mockito.when(range2.getRangeType()).thenReturn(RangeType.RELATIVE_FIXED);
        Mockito.when(range2.getMin()).thenReturn(7.);

        NetworkElement networkElement = Mockito.mock(NetworkElement.class);

        pstRange1 = new PstRange("id", networkElement);
        pstRange1.addRange(range1);
        pstRange1.addRange(range2);

        TwoWindingsTransformer twoWindingsTransformer = Mockito.mock(TwoWindingsTransformer.class);
        PhaseTapChanger phaseTapChanger = Mockito.mock(PhaseTapChanger.class);
        Mockito.when(network.getTwoWindingsTransformer(pstRange1.getNetworkElement().getId())).thenReturn(twoWindingsTransformer);
        Mockito.when(twoWindingsTransformer.getPhaseTapChanger()).thenReturn(phaseTapChanger);
        Mockito.when(phaseTapChanger.getTapPosition()).thenReturn(10); // then getMinValueWithRange(network, range2) will be 3
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
        assertEquals(13, pstRange1.getMaxValueWithRange(network, range1), 0);
        assertEquals(5, pstRange1.getMinValueWithRange(network, range1), 0);
    }

    @Test
    public void getMinValue() {
        assertEquals(5, pstRange1.getMinValue(network), 0);

    }
}
