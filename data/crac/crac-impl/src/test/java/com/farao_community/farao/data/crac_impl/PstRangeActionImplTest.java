/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.range_action.RangeType;
import com.farao_community.farao.data.crac_api.range_action.TapRange;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import org.junit.Before;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.spy;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class PstRangeActionImplTest extends AbstractRangeActionTest {
    private String networkElementId;
    private Network network;
    private PhaseTapChanger phaseTapChanger;
    private NetworkElement networkElement;
    private UsageRule freeToUse;

    @Before
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        networkElementId = "BBE2AA1  BBE3AA1  1";
        networkElement = new NetworkElement(networkElementId);
        phaseTapChanger = network.getTwoWindingsTransformer(networkElementId).getPhaseTapChanger();
        freeToUse = new FreeToUseImpl(UsageMethod.AVAILABLE, Instant.PREVENTIVE);
    }

    @Test
    public void pstConstructor() {
        PstRangeActionImpl pstRangeAction = new PstRangeActionImpl("id", "name", "operator", List.of(freeToUse), new ArrayList<>(), networkElement, "groupId");
        assertEquals("id", pstRangeAction.getId());
        assertEquals("name", pstRangeAction.getName());
        assertEquals("operator", pstRangeAction.getOperator());
        assertEquals(1, pstRangeAction.getUsageRules().size());
        assertTrue(pstRangeAction.getRanges().isEmpty());
        assertEquals("BBE2AA1  BBE3AA1  1", pstRangeAction.getNetworkElement().getId());
        assertTrue(pstRangeAction.getGroupId().isPresent());
        assertEquals("groupId", pstRangeAction.getGroupId().get());
    }

    @Test
    public void pstEquals() {
        PstRangeActionImpl pstRangeAction = new PstRangeActionImpl("id", "name", "operator", List.of(freeToUse), new ArrayList<>(), networkElement, "groupId");
        PstRangeActionImpl samePstRangeAction = new PstRangeActionImpl("id", "name", "operator", List.of(freeToUse), new ArrayList<>(), networkElement, "groupId");

        assertEquals(pstRangeAction.hashCode(), samePstRangeAction.hashCode());
        assertEquals(pstRangeAction, samePstRangeAction);
        PstRangeActionImpl differentPstRangeAction = new PstRangeActionImpl("id", "name", "operator", List.of(freeToUse), new ArrayList<>(), networkElement, "anotherGroupId");
        assertNotEquals(pstRangeAction.hashCode(), differentPstRangeAction.hashCode());
        assertNotEquals(pstRangeAction, differentPstRangeAction);
    }

    @Test
    public void apply() {
        PstRangeActionImpl pst = new PstRangeActionImpl("id", "name", "operator", List.of(freeToUse), new ArrayList<>(), networkElement, "groupId");
        assertEquals(0, network.getTwoWindingsTransformer(networkElementId).getPhaseTapChanger().getTapPosition());
        pst.apply(network, network.getTwoWindingsTransformer(networkElementId).getPhaseTapChanger().getStep(12).getAlpha());
        assertEquals(12, network.getTwoWindingsTransformer(networkElementId).getPhaseTapChanger().getTapPosition());
    }

    @Test
    public void applyOutOfBound() {
        PstRangeActionImpl pst = new PstRangeActionImpl("id", "name", "operator", List.of(freeToUse), new ArrayList<>(), networkElement, "groupId");
        try {
            pst.apply(network, 50);
            fail();
        } catch (FaraoException e) {
            assertEquals("Angle value", e.getMessage().substring(0, 11)); // In order to avoid numeric values in the expected String
        }
    }

    @Test
    public void applyOnUnknownPst() {
        PstRangeActionImpl pstRangeAction = new PstRangeActionImpl("id", "name", "operator", List.of(freeToUse), new ArrayList<>(), new NetworkElement("unknown PST"), "groupId");
        try {
            pstRangeAction.apply(network, 50);
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

        PstRangeActionImpl notAPstRangeAction = new PstRangeActionImpl("id", "name", "operator", List.of(freeToUse), new ArrayList<>(), networkElement, "groupId");
        try {
            notAPstRangeAction.apply(network, 50);
            fail();
        } catch (FaraoException e) {
            assertEquals(String.format("Transformer %s is not a PST but is defined as a TapRange", notAPstRangeAction.getId()), e.getMessage());
        }
    }

    @Test
    public void pstWithoutSpecificRange() {
        PstRangeActionImpl pstRangeActionWithoutSpecificRange = new PstRangeActionImpl("id", "name", "operator", List.of(freeToUse), new ArrayList<>(), new NetworkElement("unknown PST"), "groupId");
        pstRangeActionWithoutSpecificRange.synchronize(network);
        assertEquals(phaseTapChanger.getStep(phaseTapChanger.getLowTapPosition()).getAlpha(), pstRangeActionWithoutSpecificRange.getMinValue(pstRangeActionWithoutSpecificRange.getCurrentValue(network)), 0);
        assertEquals(phaseTapChanger.getStep(phaseTapChanger.getHighTapPosition()).getAlpha(), pstRangeActionWithoutSpecificRange.getMaxValue(pstRangeActionWithoutSpecificRange.getCurrentValue(network)), 0);
    }

    @Test
    public void pstWithAbsoluteStartOneRange() {
        TapRange range = new TapRangeImpl(3, 13, RangeType.ABSOLUTE, TapConvention.STARTS_AT_ONE);
        PstRangeActionImpl pst = new PstRangeActionImpl("id", "name", "operator", List.of(freeToUse), List.of(range), networkElement);
        pst.synchronize(network);

        assertEquals(phaseTapChanger.getStep(phaseTapChanger.getLowTapPosition() + 2).getAlpha(), pst.getMinValue(pst.getCurrentValue(network)), 0);
        assertEquals(phaseTapChanger.getStep(phaseTapChanger.getLowTapPosition() + 12).getAlpha(), pst.getMaxValue(pst.getCurrentValue(network)), 0);
    }

    @Test
    public void pstWithAbsoluteCenteredZeroRange() {
        TapRange range = new TapRangeImpl(-3, 3, RangeType.ABSOLUTE, TapConvention.CENTERED_ON_ZERO);
        PstRangeActionImpl pst = new PstRangeActionImpl("id", "name", "operator", List.of(freeToUse), List.of(range), networkElement);
        pst.synchronize(network);

        int neutralTap = (phaseTapChanger.getHighTapPosition() + phaseTapChanger.getLowTapPosition()) / 2;
        assertEquals(phaseTapChanger.getStep(neutralTap - 3).getAlpha(), pst.getMinValue(pst.getCurrentValue(network)), 0);
        assertEquals(phaseTapChanger.getStep(neutralTap + 3).getAlpha(), pst.getMaxValue(pst.getCurrentValue(network)), 0);
    }

    @Test
    public void pstWithRelativeToPreviousInstantRange() {
        TapRange range = new TapRangeImpl(-3, 3, RangeType.RELATIVE_TO_PREVIOUS_INSTANT, TapConvention.CENTERED_ON_ZERO);
        PstRangeActionImpl pst = new PstRangeActionImpl("id", "name", "operator", List.of(freeToUse), List.of(range), networkElement);
        pst.synchronize(network);

        int initialTapPosition = phaseTapChanger.getTapPosition();
        assertEquals(phaseTapChanger.getStep(initialTapPosition - 3).getAlpha(), pst.getMinValue(pst.getCurrentValue(network)), 0);
        assertEquals(phaseTapChanger.getStep(initialTapPosition + 3).getAlpha(), pst.getMaxValue(pst.getCurrentValue(network)), 0);

        int newTapPosition = initialTapPosition + 5;
        phaseTapChanger.setTapPosition(newTapPosition);
        assertEquals(phaseTapChanger.getStep(newTapPosition - 3).getAlpha(), pst.getMinValue(pst.getCurrentValue(network)), 0);
        assertEquals(phaseTapChanger.getStep(newTapPosition + 3).getAlpha(), pst.getMaxValue(pst.getCurrentValue(network)), 0);
    }

    @Test
    public void pstWithRelativeToInitialNetworkRange() {
        TapRange range = new TapRangeImpl(-3, 3, RangeType.RELATIVE_TO_INITIAL_NETWORK, TapConvention.CENTERED_ON_ZERO);
        PstRangeActionImpl pst = new PstRangeActionImpl("id", "name", "operator", List.of(freeToUse), List.of(range), networkElement);
        pst.synchronize(network);

        int initialTapPosition = phaseTapChanger.getTapPosition();
        assertEquals(phaseTapChanger.getStep(initialTapPosition - 3).getAlpha(), pst.getMinValue(pst.getCurrentValue(network)), 0);
        assertEquals(phaseTapChanger.getStep(initialTapPosition + 3).getAlpha(), pst.getMaxValue(pst.getCurrentValue(network)), 0);

        int newTapPosition = initialTapPosition + 5;
        phaseTapChanger.setTapPosition(newTapPosition);
        assertEquals(phaseTapChanger.getStep(initialTapPosition - 3).getAlpha(), pst.getMinValue(pst.getCurrentValue(network)), 0);
        assertEquals(phaseTapChanger.getStep(initialTapPosition + 3).getAlpha(), pst.getMaxValue(pst.getCurrentValue(network)), 0);
    }

    @Test
    public void desynchronize() {
        TapRange range = new TapRangeImpl(-3, 3, RangeType.RELATIVE_TO_INITIAL_NETWORK, TapConvention.CENTERED_ON_ZERO);
        PstRangeActionImpl pst = new PstRangeActionImpl("id", "name", "operator", List.of(freeToUse), List.of(range), networkElement);
        pst.synchronize(network);

        int initialTapPosition = phaseTapChanger.getTapPosition();
        assertEquals(phaseTapChanger.getStep(initialTapPosition + 3).getAlpha(), pst.getMaxValue(pst.getCurrentValue(network)), 0);
        pst.desynchronize();

        try {
            pst.getCurrentValue(network);
            fail();
        } catch (FaraoException e) {
            assertEquals("PST pst_range_id have not been synchronized so tap cannot be converted to angle", e.getMessage());
        }
    }

    @Test
    public void computeCurrentValueFromCenteredOnZero() throws NoSuchFieldException {
        PstRangeActionImpl pst = new PstRangeActionImpl("id", "name", "operator", List.of(freeToUse), new ArrayList<>(), networkElement);
        PstRangeActionImpl pstRangeAction = spy(pst);

        Whitebox.setInternalState(pstRangeAction, "lowTapPosition", -16);
        Whitebox.setInternalState(pstRangeAction, "highTapPosition", 16);

        pstRangeAction.apply(network, 0.0); // tap 0 (CENTERED_ON_ZERO)
        assertEquals(0, pstRangeAction.getCurrentTapPosition(network, TapConvention.CENTERED_ON_ZERO), 0);
        assertEquals(17, pstRangeAction.getCurrentTapPosition(network, TapConvention.STARTS_AT_ONE), 0);

        pstRangeAction.apply(network, 3.8946); // tap 10 (CENTERED_ON_ZERO)
        assertEquals(10, pstRangeAction.getCurrentTapPosition(network, TapConvention.CENTERED_ON_ZERO), 0);
        assertEquals(27, pstRangeAction.getCurrentTapPosition(network, TapConvention.STARTS_AT_ONE), 0);
    }

    @Test
    public void computeCurrentValueFromStartsAtOne() throws NoSuchFieldException {
        PstRangeActionImpl pst = new PstRangeActionImpl("id", "name", "operator", List.of(freeToUse), new ArrayList<>(), networkElement);
        PstRangeActionImpl pstRangeAction = spy(pst);

        // As the network contains taps CENTERED_ON_ZERO, but we want to test the case where the taps STARTS_AT_ONE,
        // we artifically modify the lowTapPosition and highTapPosition, and we need also to shift the taps in the assertEquals,
        // because the conversion from angle to tap is based on the network (so it gives a tap CENTERED_ON_ZERO)
        int tapShift = 17;
        Whitebox.setInternalState(pstRangeAction, "lowTapPosition", 1);
        Whitebox.setInternalState(pstRangeAction, "highTapPosition", 33);

        pstRangeAction.apply(network, 0.0); // tap 17 (STARTS_AT_ONE)
        assertEquals(0, pstRangeAction.getCurrentTapPosition(network, TapConvention.CENTERED_ON_ZERO) + tapShift, 0);
        assertEquals(17, pstRangeAction.getCurrentTapPosition(network, TapConvention.STARTS_AT_ONE) + tapShift, 0);

        pstRangeAction.apply(network, -3.8946); // tap 7 (STARTS_AT_ONE)
        assertEquals(-10, pstRangeAction.getCurrentTapPosition(network, TapConvention.CENTERED_ON_ZERO) + tapShift, 0);
        assertEquals(7, pstRangeAction.getCurrentTapPosition(network, TapConvention.STARTS_AT_ONE) + tapShift, 0);
    }

    @Test
    public void getCurrentValueTest() {
        PstRangeActionImpl pst = new PstRangeActionImpl("id", "name", "operator", List.of(freeToUse), new ArrayList<>(), networkElement);
        pst.synchronize(network);
        assertEquals(0, pst.getCurrentValue(network), 0);
    }

    @Test
    public void convertToStartsAtOneFails() throws NoSuchFieldException {
        PstRangeActionImpl pst = new PstRangeActionImpl("id", "name", "operator", List.of(freeToUse), new ArrayList<>(), networkElement);
        PstRangeActionImpl pstRangeAction = spy(pst);
        Whitebox.setInternalState(pstRangeAction, "lowTapPosition", -12);
        Whitebox.setInternalState(pstRangeAction, "highTapPosition", 35);
        try {
            pstRangeAction.getCurrentTapPosition(network, TapConvention.STARTS_AT_ONE);
        } catch (FaraoException e) {
            assertEquals("Unhandled range definition, between -12 and 35.", e.getMessage());
        }
    }

    @Test
    public void convertToCenteredOnZero() throws NoSuchFieldException {
        PstRangeActionImpl pst = new PstRangeActionImpl("id", "name", "operator", List.of(freeToUse), new ArrayList<>(), networkElement);
        PstRangeActionImpl pstRangeAction = spy(pst);
        Whitebox.setInternalState(pstRangeAction, "lowTapPosition", -12);
        Whitebox.setInternalState(pstRangeAction, "highTapPosition", 35);
        try {
            pstRangeAction.getCurrentTapPosition(network, TapConvention.CENTERED_ON_ZERO);
        } catch (FaraoException e) {
            assertEquals("Unhandled range definition, between -12 and 35.", e.getMessage());
        }
    }

    @Test
    public void getMinValueWithNoSynchronizationFails() {
        PstRangeActionImpl pst = new PstRangeActionImpl("id", "name", "operator", List.of(freeToUse), new ArrayList<>(), networkElement);
        try {
            pst.getCurrentValue(network);
            fail();
        } catch (FaraoException e) {
            assertEquals("PST pst_range_id have not been synchronized so tap cannot be converted to angle", e.getMessage());
        }
    }

    @Test
    public void synchronizeTwiceFails() {
        PstRangeActionImpl pst = new PstRangeActionImpl("id", "name", "operator", List.of(freeToUse), new ArrayList<>(), networkElement);
        pst.synchronize(network);
        try {
            pst.synchronize(network);
        } catch (AlreadySynchronizedException e) {
            assertEquals("PST pst_range_id has already been synchronized", e.getMessage());
        }
    }

    @Test
    public void handleDecreasingAnglesMinMax() {
        // First test case where deltaU is negative
        TapRange range = new TapRangeImpl(-10, 10, RangeType.ABSOLUTE, TapConvention.CENTERED_ON_ZERO);
        PstRangeActionImpl pst = new PstRangeActionImpl("id", "name", "operator", List.of(freeToUse), List.of(range), networkElement);
        pst.synchronize(network);
        assertTrue("Failed to compute min and max tap values for PST with negative deltaU",
                pst.getMinValue(pst.getCurrentValue(network)) <= pst.getMaxValue(pst.getCurrentValue(network)));

        // Then load a new case with a positive delta U and test min and max values
        Network network2 = Importers.loadNetwork("utils/TestCase12NodesWithPositiveDeltaUPST.uct", NetworkImportsUtil.class.getResourceAsStream("/utils/TestCase12NodesWithPositiveDeltaUPST.uct"));
        PstRangeActionImpl pst2 = new PstRangeActionImpl("id2", "name2", "operator", List.of(freeToUse), List.of(range), networkElement);
        pst2.synchronize(network2);
        assertTrue("Failed to compute min and max tap values for PST with positive deltaU",
                pst2.getMinValue(pst2.getCurrentValue(network2)) <= pst2.getMaxValue(pst2.getCurrentValue(network2)));
    }

    @Test
    public void testGetLocation() {
        PstRangeActionImpl pst = new PstRangeActionImpl("id", "name", "operator", List.of(freeToUse), new ArrayList<>(), networkElement);
        Set<Optional<Country>> countries = pst.getLocation(network);
        assertEquals(1, countries.size());
        assertTrue(countries.contains(Optional.of(Country.BE)));
    }
}
