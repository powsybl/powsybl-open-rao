/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.range_action;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.RangeDefinition;
import com.farao_community.farao.data.crac_impl.mocks.TwoWindingsTransformerMock;
import com.farao_community.farao.data.crac_impl.range_domain.Range;
import com.farao_community.farao.data.crac_impl.range_domain.RangeType;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class PstWithRangeTest extends AbstractElementaryRangeActionTest {

    private int pstLowTapPosition = -6;
    private int pstHighTapPosition = 12;

    private String networkElementId = "BBE2AA1  BBE3AA1  1";
    private PstWithRange pstWithRange;

    private Network network;
    private PstWithRange pstWithRange1;

    private Range range1;
    private Range range2;

    @Before
    public void setUp() throws Exception {
        pstWithRange = new PstWithRange(
                "pst_range_id",
                "pst_range_name",
                "pst_range_operator",
                createUsageRules(),
                createRanges(),
                new NetworkElement(networkElementId, networkElementId));

        network = Mockito.mock(Network.class);
        range1 = Mockito.mock(Range.class);

        Mockito.when(range1.getRangeType()).thenReturn(RangeType.ABSOLUTE_FIXED);
        Mockito.when(range1.getRangeDefinition()).thenReturn(RangeDefinition.STARTS_AT_ONE);
        Mockito.when(range1.getMin()).thenReturn(5.);
        Mockito.when(range1.getMax()).thenReturn(13.);

        range2 = Mockito.mock(Range.class);
        Mockito.when(range2.getRangeType()).thenReturn(RangeType.RELATIVE_FIXED);
        Mockito.when(range2.getRangeDefinition()).thenReturn(RangeDefinition.STARTS_AT_ONE);
        Mockito.when(range2.getMin()).thenReturn(7.);

        NetworkElement networkElement = Mockito.mock(NetworkElement.class);

        pstWithRange1 = new PstWithRange("id", networkElement);
        pstWithRange1.addRange(range1);
        pstWithRange1.addRange(range2);

        TwoWindingsTransformer twoWindingsTransformer = new TwoWindingsTransformerMock(pstLowTapPosition, pstHighTapPosition, 10);  // then getMinValueWithRange(network, range2) will be 3
        Mockito.when(network.getTwoWindingsTransformer(pstWithRange1.getNetworkElement().getId())).thenReturn(twoWindingsTransformer);
    }

    @Test
    public void apply() {
        Network network = Importers.loadNetwork(
            "TestCase12Nodes.uct",
            getClass().getResourceAsStream("/TestCase12Nodes.uct")
        );
        assertEquals(0, network.getTwoWindingsTransformer(networkElementId).getPhaseTapChanger().getTapPosition());
        pstWithRange.apply(network, 12);
        assertEquals(-5, network.getTwoWindingsTransformer(networkElementId).getPhaseTapChanger().getTapPosition());
    }

    @Test
    public void applyOutOfBound() {
        Network network = Importers.loadNetwork(
            "TestCase12Nodes.uct",
            getClass().getResourceAsStream("/TestCase12Nodes.uct")
        );
        try {
            pstWithRange.apply(network, 50);
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
        PstWithRange unknownPstWithRange = new PstWithRange(
                "unknown_pstrange_id",
                "unknown_pstrange_name",
                "unknown_pstrange_operator",
                createUsageRules(),
                createRanges(),
                new NetworkElement("unknown pst", "unknown pst"));
        try {
            unknownPstWithRange.apply(network, 50);
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
        PstWithRange notAPstWithRange = new PstWithRange(
                "not_pstrange_id",
                "not_pstrange_name",
                "not_pstrange_operator",
                createUsageRules(),
                createRanges(),
                new NetworkElement(notPstRangeElementId, notPstRangeElementId));
        try {
            notAPstWithRange.apply(network, 50);
            fail();
        } catch (FaraoException e) {
            assertEquals(String.format("Transformer %s is not a PST but is defined as a PstRange", notPstRangeElementId), e.getMessage());
        }
    }

    @Test
    public void getMinAndMaxValueWithRange() {
        assertEquals(0.13, pstWithRange1.getMaxValueWithRange(network, range1), 0);
    }

    @Test
    public void getMinValue() {
        assertEquals(0.03, pstWithRange1.getMinValue(network), 0);
    }

    @Test
    public void synchronize() {
        pstWithRange1.synchronize(network);
        assertEquals(2, pstWithRange1.ranges.size());
    }
}
