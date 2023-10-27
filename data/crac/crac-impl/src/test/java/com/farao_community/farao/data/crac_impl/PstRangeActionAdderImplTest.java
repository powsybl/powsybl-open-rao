/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.range.RangeType;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeActionAdder;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class PstRangeActionAdderImplTest {
    private CracImpl crac;
    private String networkElementId;
    private Map<Integer, Double> validTapToAngleConversionMap;

    @BeforeEach
    public void setUp() {
        crac = new CracImpl("test-crac");
        crac.newInstant("preventive", InstantKind.PREVENTIVE, null);
        crac.newInstant("outage", InstantKind.OUTAGE, "preventive");
        crac.newInstant("auto", InstantKind.AUTO, "outage");
        networkElementId = "BBE2AA1  BBE3AA1  1";
        validTapToAngleConversionMap = Map.of(-2, -20., -1, -10., 0, 0., 1, 10., 2, 20.);
    }

    @Test
    void testAdd() {
        PstRangeAction pstRangeAction = (PstRangeAction) crac.newPstRangeAction()
            .withId("id1")
            .withOperator("BE")
            .withNetworkElement(networkElementId)
            .withGroupId("groupId1")
            .newTapRange()
            .withMinTap(-10)
            .withMaxTap(10)
            .withRangeType(RangeType.ABSOLUTE)
            .add()
            .newOnInstantUsageRule()
            .withInstantId("preventive")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .withInitialTap(1)
            .withTapToAngleConversionMap(validTapToAngleConversionMap)
            .add();

        assertEquals(1, crac.getRangeActions().size());
        assertEquals(networkElementId, pstRangeAction.getNetworkElement().getId());
        assertEquals("BE", pstRangeAction.getOperator());
        assertEquals(1, pstRangeAction.getRanges().size());
        assertEquals(1, pstRangeAction.getUsageRules().size());
        assertEquals(1, crac.getNetworkElements().size());
        assertNotNull(crac.getNetworkElement(networkElementId));
    }

    @Test
    void testAddAutoWithoutSpeed() {
        PstRangeActionAdder pstRangeActionAdder = crac.newPstRangeAction()
            .withId("id1")
            .withOperator("BE")
            .withNetworkElement(networkElementId)
            .withGroupId("groupId1")
            .newTapRange()
            .withMinTap(-10)
            .withMaxTap(10)
            .withRangeType(RangeType.ABSOLUTE)
            .add()
            .newOnInstantUsageRule()
            .withInstantId("auto")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .withInitialTap(1)
            .withTapToAngleConversionMap(validTapToAngleConversionMap);
        FaraoException exception = assertThrows(FaraoException.class, pstRangeActionAdder::add);
        assertEquals("Cannot create an AUTO Pst range action without speed defined", exception.getMessage());
    }

    @Test
    void testAddAutoWithSpeed() {
        PstRangeAction pstRangeAction = (PstRangeAction) crac.newPstRangeAction()
            .withId("id1")
            .withOperator("BE")
            .withNetworkElement(networkElementId)
            .withGroupId("groupId1")
            .withSpeed(123)
            .newTapRange()
            .withMinTap(-10)
            .withMaxTap(10)
            .withRangeType(RangeType.ABSOLUTE)
            .add()
            .newOnInstantUsageRule()
            .withInstantId("auto")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .withInitialTap(1)
            .withTapToAngleConversionMap(validTapToAngleConversionMap)
            .add();

        assertEquals(123, pstRangeAction.getSpeed().orElseThrow().intValue());
    }

    @Test
    void testAddWithoutGroupId() {
        PstRangeAction pstRangeAction = (PstRangeAction) crac.newPstRangeAction()
            .withId("id1")
            .withOperator("BE")
            .withNetworkElement(networkElementId)
            .newTapRange()
            .withMinTap(-10)
            .withMaxTap(10)
            .withRangeType(RangeType.ABSOLUTE)
            .add()
            .newOnInstantUsageRule()
            .withInstantId("preventive")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .withInitialTap(0)
            .withTapToAngleConversionMap(validTapToAngleConversionMap)
            .add();

        assertEquals(1, crac.getRangeActions().size());
        assertEquals(networkElementId, pstRangeAction.getNetworkElement().getId());
        assertEquals("BE", pstRangeAction.getOperator());
        assertEquals(1, pstRangeAction.getRanges().size());
        assertEquals(1, pstRangeAction.getUsageRules().size());
    }

    @Test
    void testAddWithoutRangeAndUsageRule() {
        /*
        This behaviour is considered admissible:
            - without range, the default range will be defined by the min/max value of the network
            - without usage rule, the remedial action will never be available

        This test should however return two warnings
         */
        PstRangeAction pstRangeAction = (PstRangeAction) crac.newPstRangeAction()
            .withId("id1")
            .withOperator("BE")
            .withNetworkElement(networkElementId)
            .withInitialTap(2)
            .withTapToAngleConversionMap(validTapToAngleConversionMap)
            .add();

        assertEquals(1, crac.getRangeActions().size());
        assertEquals(networkElementId, pstRangeAction.getNetworkElement().getId());
        assertEquals("BE", pstRangeAction.getOperator());
        assertEquals(0, pstRangeAction.getRanges().size());
        assertEquals(0, pstRangeAction.getUsageRules().size());
    }

    @Test
    void testAddWithoutOperator() {
        PstRangeAction pstRangeAction = (PstRangeAction) crac.newPstRangeAction()
            .withId("id1")
            .withNetworkElement(networkElementId)
            .newTapRange()
            .withMinTap(-10)
            .withMaxTap(10)
            .withRangeType(RangeType.ABSOLUTE)
            .add()
            .newOnInstantUsageRule()
            .withInstantId("preventive")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .withInitialTap(-2)
            .withTapToAngleConversionMap(validTapToAngleConversionMap)
            .add();

        assertEquals(1, crac.getRangeActions().size());
        assertEquals(networkElementId, pstRangeAction.getNetworkElement().getId());
        assertNull(pstRangeAction.getOperator());
        assertEquals(1, pstRangeAction.getRanges().size());
        assertEquals(1, pstRangeAction.getUsageRules().size());
    }

    @Test
    void testNoIdFail() {
        PstRangeActionAdder pstRangeActionAdder = crac.newPstRangeAction()
            .withOperator("BE")
            .withNetworkElement(networkElementId)
            .withInitialTap(1)
            .withTapToAngleConversionMap(validTapToAngleConversionMap);
        FaraoException exception = assertThrows(FaraoException.class, pstRangeActionAdder::add);
        assertEquals("Cannot add a PstRangeAction object with no specified id. Please use withId()", exception.getMessage());
    }

    @Test
    void testNoNetworkElementFail() {
        PstRangeActionAdder pstRangeActionAdder = crac.newPstRangeAction()
            .withId("id1")
            .withOperator("BE")
            .withInitialTap(1)
            .withTapToAngleConversionMap(validTapToAngleConversionMap);
        FaraoException exception = assertThrows(FaraoException.class, pstRangeActionAdder::add);
        assertEquals("Cannot add PstRangeAction without a network element. Please use withNetworkElement() with a non null value", exception.getMessage());
    }

    @Test
    void testIdNotUnique() {
        crac.newNetworkAction()
            .withId("sameId")
            .withOperator("BE")
            .newTopologicalAction().withActionType(ActionType.OPEN).withNetworkElement("action-elementId").add()
            .add();

        PstRangeActionAdder adder = crac.newPstRangeAction()
            .withId("sameId")
            .withOperator("BE")
            .withNetworkElement("networkElementId")
            .withInitialTap(1)
            .withTapToAngleConversionMap(validTapToAngleConversionMap);
        FaraoException exception = assertThrows(FaraoException.class, adder::add);
        assertEquals("A remedial action with id sameId already exists", exception.getMessage());
    }

    @Test
    void testNoInitialTap() {
        PstRangeActionAdder pstRangeActionAdder = crac.newPstRangeAction()
            .withId("id1")
            .withNetworkElement(networkElementId)
            .withOperator("BE")
            .withTapToAngleConversionMap(validTapToAngleConversionMap);
        FaraoException exception = assertThrows(FaraoException.class, pstRangeActionAdder::add);
        assertEquals("Cannot add PstRangeAction without a initial tap. Please use withInitialTap() with a non null value", exception.getMessage());
    }

    @Test
    void testNoTapToAngleConversionMap() {
        PstRangeActionAdder pstRangeActionAdder = crac.newPstRangeAction()
            .withId("id1")
            .withNetworkElement(networkElementId)
            .withOperator("BE")
            .withInitialTap(0);
        FaraoException exception = assertThrows(FaraoException.class, pstRangeActionAdder::add);
        assertEquals("Cannot add PstRangeAction without a tap to angle conversion map. Please use withTapToAngleConversionMap() with a non null value", exception.getMessage());
    }

    @Test
    void testEmptyTapToAngleConversionMap() {
        PstRangeActionAdder pstRangeActionAdder = crac.newPstRangeAction()
            .withId("id1")
            .withNetworkElement(networkElementId)
            .withOperator("BE")
            .withInitialTap(0)
            .withTapToAngleConversionMap(new HashMap<>());
        FaraoException exception = assertThrows(FaraoException.class, pstRangeActionAdder::add);
        assertEquals("TapToAngleConversionMap of PST id1 should at least contain 2 entries.", exception.getMessage());
    }

    @Test
    void testIncompleteTapToAngleConversionMap() {
        PstRangeActionAdder pstRangeActionAdder = crac.newPstRangeAction()
            .withId("id1")
            .withNetworkElement(networkElementId)
            .withOperator("BE")
            .withInitialTap(0)
            .withTapToAngleConversionMap(Map.of(-2, -20., 2, 20.));
        FaraoException exception = assertThrows(FaraoException.class, pstRangeActionAdder::add);
        assertEquals("TapToAngleConversionMap of PST id1 should contain all the consecutive taps between -2 and 2", exception.getMessage());
    }

    @Test
    void testNotMonotonousTapToAngleConversionMap() {
        PstRangeActionAdder pstRangeActionAdder = crac.newPstRangeAction()
            .withId("id1")
            .withNetworkElement(networkElementId)
            .withOperator("BE")
            .withInitialTap(0)
            .withTapToAngleConversionMap(Map.of(-2, -20., -1, -15., 0, 0., 1, -10., 2, 20.));
        FaraoException exception = assertThrows(FaraoException.class, pstRangeActionAdder::add);
        assertEquals("TapToAngleConversionMap of PST id1 should be increasing or decreasing", exception.getMessage());
    }

    @Test
    void testInitialTapNotInMap() {
        PstRangeActionAdder pstRangeActionAdder = crac.newPstRangeAction()
            .withId("id1")
            .withNetworkElement(networkElementId)
            .withOperator("BE")
            .withInitialTap(10)
            .withTapToAngleConversionMap(validTapToAngleConversionMap);
        FaraoException exception = assertThrows(FaraoException.class, pstRangeActionAdder::add);
        assertEquals("initialTap of PST id1 must be included into its tapToAngleConversionMap", exception.getMessage());
    }

    @Test
    void testPraRelativeToPreviousInstantRange() {
        PstRangeAction pstRangeAction = (PstRangeAction) crac.newPstRangeAction()
            .withId("id1")
            .withNetworkElement(networkElementId)
            .newTapRange()
            .withMinTap(-10).withMaxTap(10).withRangeType(RangeType.RELATIVE_TO_PREVIOUS_INSTANT).add()
            .newOnInstantUsageRule().withInstantId("preventive").withUsageMethod(UsageMethod.AVAILABLE).add()
            .withInitialTap(-2)
            .withTapToAngleConversionMap(validTapToAngleConversionMap)
            .add();
        assertTrue(pstRangeAction.getRanges().isEmpty());
    }
}
