/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeActionAdder;
import com.powsybl.openrao.data.cracapi.range.RangeType;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
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
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";

    private CracImpl crac;
    private String networkElementId;
    private Map<Integer, Double> validTapToAngleConversionMap;

    @BeforeEach
    public void setUp() {
        crac = new CracImpl("test-crac")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT_ID, InstantKind.AUTO);
        networkElementId = "BBE2AA1  BBE3AA1  1";
        validTapToAngleConversionMap = Map.of(-2, -20., -1, -10., 0, 0., 1, 10., 2, 20.);
    }

    @Test
    void testAdd() {
        PstRangeAction pstRangeAction = crac.newPstRangeAction()
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
                .withInstant(PREVENTIVE_INSTANT_ID)
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
            .withInstant(AUTO_INSTANT_ID)
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .withInitialTap(1)
            .withTapToAngleConversionMap(validTapToAngleConversionMap);
        OpenRaoException exception = assertThrows(OpenRaoException.class, pstRangeActionAdder::add);
        assertEquals("Cannot create an AUTO Pst range action without speed defined", exception.getMessage());
    }

    @Test
    void testAddAutoWithSpeed() {
        PstRangeAction pstRangeAction = crac.newPstRangeAction()
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
                .withInstant(AUTO_INSTANT_ID)
                .withUsageMethod(UsageMethod.AVAILABLE)
                .add()
                .withInitialTap(1)
                .withTapToAngleConversionMap(validTapToAngleConversionMap)
                .add();

        assertEquals(123, pstRangeAction.getSpeed().get().intValue());
    }

    @Test
    void testAddWithoutGroupId() {
        PstRangeAction pstRangeAction = crac.newPstRangeAction()
            .withId("id1")
            .withOperator("BE")
            .withNetworkElement(networkElementId)
            .newTapRange()
                .withMinTap(-10)
                .withMaxTap(10)
                .withRangeType(RangeType.ABSOLUTE)
                .add()
            .newOnInstantUsageRule()
                .withInstant(PREVENTIVE_INSTANT_ID)
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
        PstRangeAction pstRangeAction = crac.newPstRangeAction()
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
        PstRangeAction pstRangeAction = crac.newPstRangeAction()
            .withId("id1")
            .withNetworkElement(networkElementId)
            .newTapRange()
                .withMinTap(-10)
                .withMaxTap(10)
                .withRangeType(RangeType.ABSOLUTE)
                .add()
            .newOnInstantUsageRule()
                .withInstant(PREVENTIVE_INSTANT_ID)
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
        OpenRaoException exception = assertThrows(OpenRaoException.class, pstRangeActionAdder::add);
        assertEquals("Cannot add a PstRangeAction object with no specified id. Please use withId()", exception.getMessage());
    }

    @Test
    void testNoNetworkElementFail() {
        PstRangeActionAdder pstRangeActionAdder = crac.newPstRangeAction()
            .withId("id1")
            .withOperator("BE")
            .withInitialTap(1)
            .withTapToAngleConversionMap(validTapToAngleConversionMap);
        OpenRaoException exception = assertThrows(OpenRaoException.class, pstRangeActionAdder::add);
        assertEquals("Cannot add PstRangeAction without a network element. Please use withNetworkElement() with a non null value", exception.getMessage());
    }

    @Test
    void testIdNotUnique() {
        crac.newNetworkAction()
            .withId("sameId")
            .withOperator("BE")
            .newTerminalsConnectionAction().withActionType(ActionType.OPEN).withNetworkElement("action-elementId").add()
            .add();

        PstRangeActionAdder adder = crac.newPstRangeAction()
            .withId("sameId")
            .withOperator("BE")
            .withNetworkElement("networkElementId")
            .withInitialTap(1)
            .withTapToAngleConversionMap(validTapToAngleConversionMap);
        OpenRaoException exception = assertThrows(OpenRaoException.class, adder::add);
        assertEquals("A remedial action with id sameId already exists", exception.getMessage());
    }

    @Test
    void testNoInitialTap() {
        PstRangeActionAdder pstRangeActionAdder = crac.newPstRangeAction()
            .withId("id1")
            .withNetworkElement(networkElementId)
            .withOperator("BE")
            .withTapToAngleConversionMap(validTapToAngleConversionMap);
        OpenRaoException exception = assertThrows(OpenRaoException.class, pstRangeActionAdder::add);
        assertEquals("Cannot add PstRangeAction without a initial tap. Please use withInitialTap() with a non null value", exception.getMessage());
    }

    @Test
    void testNoTapToAngleConversionMap() {
        PstRangeActionAdder pstRangeActionAdder = crac.newPstRangeAction()
            .withId("id1")
            .withNetworkElement(networkElementId)
            .withOperator("BE")
            .withInitialTap(0);
        OpenRaoException exception = assertThrows(OpenRaoException.class, pstRangeActionAdder::add);
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
        OpenRaoException exception = assertThrows(OpenRaoException.class, pstRangeActionAdder::add);
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
        OpenRaoException exception = assertThrows(OpenRaoException.class, pstRangeActionAdder::add);
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
        OpenRaoException exception = assertThrows(OpenRaoException.class, pstRangeActionAdder::add);
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
        OpenRaoException exception = assertThrows(OpenRaoException.class, pstRangeActionAdder::add);
        assertEquals("initialTap of PST id1 must be included into its tapToAngleConversionMap", exception.getMessage());
    }

    @Test
    void testPraRelativeToPreviousInstantRange() {
        PstRangeAction pstRangeAction = crac.newPstRangeAction()
            .withId("id1")
            .withNetworkElement(networkElementId)
            .newTapRange()
            .withMinTap(-10).withMaxTap(10).withRangeType(RangeType.RELATIVE_TO_PREVIOUS_INSTANT).add()
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .withInitialTap(-2)
            .withTapToAngleConversionMap(validTapToAngleConversionMap)
            .add();
        assertTrue(pstRangeAction.getRanges().isEmpty());
    }
}
