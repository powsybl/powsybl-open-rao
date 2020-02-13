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
import com.farao_community.farao.data.crac_impl.range_domain.Range;
import com.farao_community.farao.data.crac_impl.range_domain.RangeType;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
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

    private int pstLowTapPosition = -16;
    private int pstHighTapPosition = 16;

    private String networkElementId = "BBE2AA1  BBE3AA1  1";
    private PstWithRange pstWithRange;

    private Network network;
    private PstWithRange pstWithRange1;

    private NetworkElement networkElement;
    private PhaseTapChanger phaseTapChanger;

    private int initialTapPosition;

    private int absoluteStartOneRangeMin;
    private int absoluteStartOneRangeMax;
    private Range absoluteStartOneRange;

    private int absoluteCenteredZeroRangeMin;
    private int absoluteCenteredZeroRangeMax;
    private Range absoluteCenteredZeroRange;

    private int relativeDynamicRangeMin;
    private int relativeDynamicRangeMax;
    private Range relativeDynamicRange;

    private int relativeFixedRangeMin;
    private int relativeFixedRangeMax;
    private Range relativeFixedRange;

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

        networkElement = Mockito.mock(NetworkElement.class);
        Mockito.when(networkElement.getId()).thenReturn(networkElementId);

        TwoWindingsTransformer twoWindingsTransformer = Mockito.mock(TwoWindingsTransformer.class);
        phaseTapChanger = Mockito.mock(PhaseTapChanger.class);
        Mockito.when(network.getTwoWindingsTransformer(networkElement.getId())).thenReturn(twoWindingsTransformer);
        Mockito.when(twoWindingsTransformer.getPhaseTapChanger()).thenReturn(phaseTapChanger);

        initialTapPosition = 2;

        Mockito.when(phaseTapChanger.getTapPosition()).thenReturn(initialTapPosition); // then getMinValueWithRange(network, range2) will be 3
        Mockito.when(phaseTapChanger.getHighTapPosition()).thenReturn(pstHighTapPosition);
        Mockito.when(phaseTapChanger.getLowTapPosition()).thenReturn(pstLowTapPosition);

        absoluteStartOneRangeMin = 4;
        absoluteStartOneRangeMax = 22;
        absoluteStartOneRange = new Range(absoluteStartOneRangeMin, absoluteStartOneRangeMax, RangeType.ABSOLUTE_FIXED, RangeDefinition.STARTS_AT_ONE);

        absoluteCenteredZeroRangeMin = -4;
        absoluteCenteredZeroRangeMax = +6;
        absoluteCenteredZeroRange = new Range(absoluteCenteredZeroRangeMin, absoluteCenteredZeroRangeMax, RangeType.ABSOLUTE_FIXED, RangeDefinition.CENTERED_ON_ZERO);

        relativeDynamicRangeMin = -6;
        relativeDynamicRangeMax = 3;
        relativeDynamicRange = new Range(relativeDynamicRangeMin, relativeDynamicRangeMax, RangeType.RELATIVE_DYNAMIC, RangeDefinition.CENTERED_ON_ZERO);

        relativeFixedRangeMin = -3;
        relativeFixedRangeMax = 3;
        relativeFixedRange = new Range(relativeFixedRangeMin, relativeFixedRangeMax, RangeType.RELATIVE_FIXED, RangeDefinition.CENTERED_ON_ZERO);
    }

    @Test
    public void pstOtherConstructor() {
        PstWithRange pstRange1 = new PstWithRange("id", networkElement);
        assertEquals("", pstRange1.getOperator());
        PstWithRange pstRange2 = new PstWithRange("id", "name", "operator", networkElement);
        assertEquals("operator", pstRange2.getOperator());
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
    public void pstWithoutSpecificRange() {
        PstWithRange pstRangeWithoutSpecificRange = new PstWithRange("id", networkElement);
        pstRangeWithoutSpecificRange.setReferenceValue(network);
        assertEquals(pstHighTapPosition, pstRangeWithoutSpecificRange.getMaxValue(network), 0);
        assertEquals(pstLowTapPosition, pstRangeWithoutSpecificRange.getMinValue(network), 0);
    }

    @Test
    public void pstWithAbsoluteStartOneRange() {
        PstWithRange pstRangeWithAbsoluteRange = new PstWithRange("id", networkElement);
        pstRangeWithAbsoluteRange.addRange(absoluteStartOneRange);
        pstRangeWithAbsoluteRange.setReferenceValue(network);
        assertEquals(pstLowTapPosition + absoluteStartOneRangeMax - 1, pstRangeWithAbsoluteRange.getMaxValue(network), 0);
        assertEquals(pstLowTapPosition + absoluteStartOneRangeMin - 1, pstRangeWithAbsoluteRange.getMinValue(network), 0);
    }

    @Test
    public void pstWithAbsoluteCenteredZeroRange() {
        PstWithRange pstRangeWithAbsoluteRange = new PstWithRange("id", networkElement);
        pstRangeWithAbsoluteRange.addRange(absoluteCenteredZeroRange);
        pstRangeWithAbsoluteRange.setReferenceValue(network);
        assertEquals(((double) pstLowTapPosition + pstHighTapPosition) / 2 + absoluteCenteredZeroRangeMax, pstRangeWithAbsoluteRange.getMaxValue(network), 0);
        assertEquals(((double) pstLowTapPosition + pstHighTapPosition) / 2 + absoluteCenteredZeroRangeMin, pstRangeWithAbsoluteRange.getMinValue(network), 0);
    }

    @Test
    public void pstWithRelativeDynamicRange() {
        PstWithRange pstRangeWithRelativeDynamicRange = new PstWithRange("id", networkElement);
        pstRangeWithRelativeDynamicRange.addRange(relativeDynamicRange);
        pstRangeWithRelativeDynamicRange.setReferenceValue(network);
        assertEquals(initialTapPosition + relativeDynamicRangeMin, pstRangeWithRelativeDynamicRange.getMinValue(network), 0);
        assertEquals(initialTapPosition + relativeDynamicRangeMax, pstRangeWithRelativeDynamicRange.getMaxValue(network), 0);
        int updatedInitialTapPosition = 13;
        Mockito.when(phaseTapChanger.getTapPosition()).thenReturn(updatedInitialTapPosition);
        pstRangeWithRelativeDynamicRange.synchronize(network);
        assertEquals(updatedInitialTapPosition + relativeDynamicRangeMin, pstRangeWithRelativeDynamicRange.getMinValue(network), 0);
        assertEquals(updatedInitialTapPosition + relativeDynamicRangeMax, pstRangeWithRelativeDynamicRange.getMaxValue(network), 0);
    }

    @Test
    public void pstWithRelativeFixedRange() {
        PstWithRange pstRangeWithRelativeFixedRange = new PstWithRange("id", networkElement);
        pstRangeWithRelativeFixedRange.addRange(relativeFixedRange);
        pstRangeWithRelativeFixedRange.setReferenceValue(network);
        assertEquals(initialTapPosition + relativeFixedRangeMin, pstRangeWithRelativeFixedRange.getMinValue(network), 0);
        assertEquals(initialTapPosition + relativeFixedRangeMax, pstRangeWithRelativeFixedRange.getMaxValue(network), 0);
    }

    @Test
    public void pstWithSeveralRanges() {
        PstWithRange pstRangeWithSeveralRanges = new PstWithRange("id", networkElement);
        pstRangeWithSeveralRanges.addRange(absoluteCenteredZeroRange);
        pstRangeWithSeveralRanges.addRange(absoluteStartOneRange);
        pstRangeWithSeveralRanges.addRange(relativeFixedRange);
        pstRangeWithSeveralRanges.addRange(relativeDynamicRange);
        pstRangeWithSeveralRanges.setReferenceValue(network);
        pstRangeWithSeveralRanges.synchronize(network);
        assertEquals(initialTapPosition + relativeFixedRangeMin, pstRangeWithSeveralRanges.getMinValue(network), 0);
        assertEquals(initialTapPosition + relativeFixedRangeMax, pstRangeWithSeveralRanges.getMaxValue(network), 0);
        int anotherInitialTapPosition = 1;
        Mockito.when(phaseTapChanger.getTapPosition()).thenReturn(anotherInitialTapPosition);
        pstRangeWithSeveralRanges.synchronize(network);
        assertEquals(initialTapPosition + relativeFixedRangeMin, pstRangeWithSeveralRanges.getMinValue(network), 0);
        assertEquals(anotherInitialTapPosition + relativeDynamicRangeMax, pstRangeWithSeveralRanges.getMaxValue(network), 0);
    }
}
