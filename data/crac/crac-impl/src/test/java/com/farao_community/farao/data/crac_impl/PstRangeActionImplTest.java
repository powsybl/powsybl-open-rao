/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeActionAdder;
import com.farao_community.farao.data.crac_api.range.RangeType;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class PstRangeActionImplTest {
    private Crac crac;
    private PstRangeActionAdder pstRangeActionAdder;
    private String networkElementId;
    private Network network;
    private PhaseTapChanger phaseTapChanger;
    private Map<Integer, Double> tapToAngleConversionMap;

    @BeforeEach
    public void setUp() {
        crac = new CracImplFactory().create("cracId");
        network = NetworkImportsUtil.import12NodesNetwork();
        networkElementId = "BBE2AA1  BBE3AA1  1";
        phaseTapChanger = network.getTwoWindingsTransformer(networkElementId).getPhaseTapChanger();
        tapToAngleConversionMap = new HashMap<>();
        phaseTapChanger.getAllSteps().forEach((stepInt, step) -> tapToAngleConversionMap.put(stepInt, step.getAlpha()));

        pstRangeActionAdder = crac.newPstRangeAction()
            .withId("pst-range-action-id")
            .withName("pst-range-action-name")
            .withNetworkElement("BBE2AA1  BBE3AA1  1")
            .withOperator("operator")
            .newOnInstantUsageRule().withInstant(crac.getInstant(Instant.Kind.PREVENTIVE)).withUsageMethod(UsageMethod.AVAILABLE).add()
            .withTapToAngleConversionMap(tapToAngleConversionMap)
            .withInitialTap(0);
    }

    @Test
    void apply() {
        PstRangeAction pstRa = pstRangeActionAdder.add();
        assertEquals(0, network.getTwoWindingsTransformer(networkElementId).getPhaseTapChanger().getTapPosition());
        assertEquals(0, pstRa.getCurrentTapPosition(network));

        pstRa.apply(network, network.getTwoWindingsTransformer(networkElementId).getPhaseTapChanger().getStep(12).getAlpha());

        assertEquals(12, network.getTwoWindingsTransformer(networkElementId).getPhaseTapChanger().getTapPosition());
        assertEquals(12, pstRa.getCurrentTapPosition(network));
    }

    @Test
    void applyOutOfBound() {
        PstRangeAction pstRa = pstRangeActionAdder.add();
        assertThrows(FaraoException.class, () -> pstRa.apply(network, 50));
    }

    @Test
    void applyOnUnknownPst() {
        PstRangeAction pstRa = pstRangeActionAdder.withNetworkElement("unknownNetworkElement").add();
        assertThrows(FaraoException.class, () -> pstRa.apply(network, 50));
    }

    @Test
    void applyOnTransformerWithNoPhaseShifter() {
        Network network = Network.read("TestCase12Nodes_no_pst.uct", getClass().getResourceAsStream("/TestCase12Nodes_no_pst.uct"));
        PstRangeAction pstRa = pstRangeActionAdder.add();
        assertThrows(FaraoException.class, () -> pstRa.apply(network, 50));
    }

    @Test
    void pstWithoutSpecificRange() {
        PstRangeAction pstRa = pstRangeActionAdder.add();

        double minAngleInNetwork = phaseTapChanger.getStep(phaseTapChanger.getLowTapPosition()).getAlpha();
        double maxAngleInNetwork = phaseTapChanger.getStep(phaseTapChanger.getHighTapPosition()).getAlpha();
        assertEquals(0.3885, pstRa.getSmallestAngleStep(), 1e-3);
        assertEquals(minAngleInNetwork, pstRa.getMinAdmissibleSetpoint(0), 1e-3);
        assertEquals(maxAngleInNetwork, pstRa.getMaxAdmissibleSetpoint(0), 1e-3);
    }

    @Test
    void pstWithAbsoluteCenteredZeroRange() {
        PstRangeAction pstRa = pstRangeActionAdder
            .newTapRange().withMinTap(-3).withMaxTap(3).withRangeType(RangeType.ABSOLUTE).add()
            .add();

        int neutralTap = (phaseTapChanger.getHighTapPosition() + phaseTapChanger.getLowTapPosition()) / 2;

        assertEquals(phaseTapChanger.getStep(neutralTap - 3).getAlpha(), pstRa.getMinAdmissibleSetpoint(0), 0);
        assertEquals(phaseTapChanger.getStep(neutralTap + 3).getAlpha(), pstRa.getMaxAdmissibleSetpoint(0), 0);
        assertEquals(phaseTapChanger.getStep(neutralTap - 3).getAlpha(), pstRa.getMinAdmissibleSetpoint(5), 0);
        assertEquals(phaseTapChanger.getStep(neutralTap + 3).getAlpha(), pstRa.getMaxAdmissibleSetpoint(5), 0);
    }

    @Test
    void pstWithRelativeToPreviousInstantRange() {

        PstRangeAction pstRa = pstRangeActionAdder
            .newOnInstantUsageRule().withInstant(crac.getInstant(Instant.Kind.CURATIVE)).withUsageMethod(UsageMethod.AVAILABLE).add()
            .newTapRange().withMinTap(-3).withMaxTap(3).withRangeType(RangeType.RELATIVE_TO_PREVIOUS_INSTANT).add()
            .add();

        int initialTapPosition = phaseTapChanger.getTapPosition();
        double initialAngle = phaseTapChanger.getCurrentStep().getAlpha();

        assertEquals(phaseTapChanger.getStep(initialTapPosition - 3).getAlpha(), pstRa.getMinAdmissibleSetpoint(initialAngle), 0);
        assertEquals(phaseTapChanger.getStep(initialTapPosition + 3).getAlpha(), pstRa.getMaxAdmissibleSetpoint(initialAngle), 0);

        int newTapPosition = initialTapPosition + 5;
        phaseTapChanger.setTapPosition(5);
        double newAngle = phaseTapChanger.getCurrentStep().getAlpha();

        assertEquals(phaseTapChanger.getStep(newTapPosition - 3).getAlpha(), pstRa.getMinAdmissibleSetpoint(newAngle), 0);
        assertEquals(phaseTapChanger.getStep(newTapPosition + 3).getAlpha(), pstRa.getMaxAdmissibleSetpoint(newAngle), 0);
    }

    @Test
    void pstWithRelativeToInitialNetworkRange() {

        PstRangeAction pstRa = pstRangeActionAdder
            .newTapRange().withMinTap(-3).withMaxTap(3).withRangeType(RangeType.RELATIVE_TO_INITIAL_NETWORK).add()
            .add();

        int initialTapPosition = phaseTapChanger.getTapPosition();

        assertEquals(phaseTapChanger.getStep(initialTapPosition - 3).getAlpha(), pstRa.getMinAdmissibleSetpoint(0), 0);
        assertEquals(phaseTapChanger.getStep(initialTapPosition + 3).getAlpha(), pstRa.getMaxAdmissibleSetpoint(0), 0);
        assertEquals(phaseTapChanger.getStep(initialTapPosition - 3).getAlpha(), pstRa.getMinAdmissibleSetpoint(5), 0);
        assertEquals(phaseTapChanger.getStep(initialTapPosition + 3).getAlpha(), pstRa.getMaxAdmissibleSetpoint(5), 0);
    }

    @Test
    void computeCurrentValue() {

        PstRangeAction pstRa = pstRangeActionAdder.add();

        pstRa.apply(network, 0.0); // tap 0 (CENTERED_ON_ZERO)
        assertEquals(0, pstRa.getCurrentTapPosition(network), 0);
        pstRa.apply(network, 3.8946); // tap 10 (CENTERED_ON_ZERO)
        assertEquals(10, pstRa.getCurrentTapPosition(network), 0);
    }

    @Test
    void getCurrentSetpointTest() {

        PstRangeAction pstRa = pstRangeActionAdder.add();

        assertEquals(0, pstRa.getCurrentSetpoint(network), 1e-3);
        pstRa.apply(network, 3.8946); // tap 10 (CENTERED_ON_ZERO)
        assertEquals(3.8946, pstRa.getCurrentSetpoint(network), 1e-3);

    }

    @Test
    void handleDecreasingAnglesMinMax() {
        // First test case where deltaU is negative
        PstRangeAction pstRa1 = pstRangeActionAdder
            .newTapRange().withMinTap(-10).withMaxTap(10).withRangeType(RangeType.ABSOLUTE).add()
            .add();

        assertTrue(pstRa1.getMinAdmissibleSetpoint(0) <= pstRa1.getMaxAdmissibleSetpoint(0));

        // Then load a new case with a positive delta U and test min and max values
        Network network2 = Network.read("utils/TestCase12NodesWithPositiveDeltaUPST.uct", NetworkImportsUtil.class.getResourceAsStream("/utils/TestCase12NodesWithPositiveDeltaUPST.uct"));
        Map<Integer, Double> tapToAngleConversionMap2 = new HashMap<>();
        network2.getTwoWindingsTransformer(networkElementId).getPhaseTapChanger().getAllSteps().forEach((stepInt, step) -> tapToAngleConversionMap2.put(stepInt, step.getAlpha()));

        PstRangeAction pstRa2 = crac.newPstRangeAction()
            .withId("pst-range-action-id-2")
            .withName("pst-range-action-name-2")
            .withNetworkElement("BBE2AA1  BBE3AA1  1")
            .withOperator("operator")
            .newOnInstantUsageRule().withInstant(crac.getInstant(Instant.Kind.PREVENTIVE)).withUsageMethod(UsageMethod.AVAILABLE).add()
            .newTapRange().withMinTap(-10).withMaxTap(10).withRangeType(RangeType.ABSOLUTE).add()
            .withTapToAngleConversionMap(tapToAngleConversionMap2)
            .withInitialTap(0)
            .add();

        assertTrue(pstRa2.getMinAdmissibleSetpoint(0) <= pstRa2.getMaxAdmissibleSetpoint(0));
    }

    @Test
    void testGetLocation() {
        PstRangeAction pstRa = pstRangeActionAdder.add();
        Set<Optional<Country>> countries = pstRa.getLocation(network);
        assertEquals(1, countries.size());
        assertTrue(countries.contains(Optional.of(Country.BE)));
    }

    @Test
    void pstEquals() {

        PstRangeAction pstRa1 = pstRangeActionAdder.withGroupId("g1").add();
        PstRangeAction pstRa2 = pstRangeActionAdder.withId("anotherId").withGroupId("g1").add();

        assertEquals(pstRa1.hashCode(), pstRa1.hashCode());
        assertEquals(pstRa1, pstRa1);
        assertNotEquals(pstRa1.hashCode(), pstRa2.hashCode());
        assertNotEquals(pstRa1, pstRa2);
    }

}
