/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.HvdcRangeActionAdder;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
class HvdcRangeActionAdderImplTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";

    private CracImpl crac;
    private String networkElementId;

    @BeforeEach
    public void setUp() {
        crac = new CracImpl("test-crac")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT_ID, InstantKind.AUTO);
        networkElementId = "BBE2AA11 FFR3AA11 1";
    }

    @Test
    void testAdd() {
        HvdcRangeAction hvdcRangeAction = crac.newHvdcRangeAction()
                .withId("id1")
                .withOperator("BE")
                .withNetworkElement(networkElementId)
                .withGroupId("groupId1")
                .newRange().withMin(-5).withMax(10).add()
                .newOnInstantUsageRule()
                .withInstant(PREVENTIVE_INSTANT_ID)
                .withUsageMethod(UsageMethod.AVAILABLE)
                .add()
                .add();

        assertEquals(1, crac.getRangeActions().size());
        assertEquals(networkElementId, hvdcRangeAction.getNetworkElement().getId());
        assertEquals("BE", hvdcRangeAction.getOperator());
        assertEquals(1, hvdcRangeAction.getRanges().size());
        assertEquals(1, hvdcRangeAction.getUsageRules().size());
        assertEquals(1, crac.getNetworkElements().size());
        assertNotNull(crac.getNetworkElement(networkElementId));
    }

    @Test
    void testAddAuto() {
        HvdcRangeAction hvdcRangeAction = crac.newHvdcRangeAction()
                .withId("id1")
                .withOperator("BE")
                .withNetworkElement(networkElementId)
                .withGroupId("groupId1")
                .withSpeed(1)
                .withInitialSetpoint(1)
                .newRange().withMin(-5).withMax(10).add()
                .newOnInstantUsageRule()
                .withInstant(AUTO_INSTANT_ID)
                .withUsageMethod(UsageMethod.FORCED)
                .add()
                .add();

        assertEquals(1, crac.getRangeActions().size());
        assertEquals(networkElementId, hvdcRangeAction.getNetworkElement().getId());
        assertEquals("BE", hvdcRangeAction.getOperator());
        assertEquals(1, hvdcRangeAction.getRanges().size());
        assertEquals(1, hvdcRangeAction.getUsageRules().size());
        assertEquals(1, crac.getNetworkElements().size());
        assertNotNull(crac.getNetworkElement(networkElementId));
        assertEquals(1, hvdcRangeAction.getSpeed().get().intValue());
        assertEquals(1.0, hvdcRangeAction.getInitialSetpoint());
    }

    @Test
    void testAddAutoWithoutSpeed() {
        HvdcRangeActionAdder hvdcRangeActionAdder = crac.newHvdcRangeAction()
            .withId("id1")
            .withOperator("BE")
            .withNetworkElement(networkElementId)
            .withGroupId("groupId1")
            .withInitialSetpoint(1)
            .newRange().withMin(-5).withMax(10).add()
            .newOnInstantUsageRule()
            .withInstant(AUTO_INSTANT_ID)
            .withUsageMethod(UsageMethod.FORCED)
            .add();
        OpenRaoException exception = assertThrows(OpenRaoException.class, hvdcRangeActionAdder::add);
        assertEquals("Cannot create an AUTO standard range action without speed defined", exception.getMessage());
    }

    @Test
    void testAddWithoutGroupId() {
        HvdcRangeAction hvdcRangeAction = crac.newHvdcRangeAction()
                .withId("id1")
                .withOperator("BE")
                .withNetworkElement(networkElementId)
                .newRange().withMin(-5).withMax(10).add()
                .newOnInstantUsageRule()
                .withInstant(PREVENTIVE_INSTANT_ID)
                .withUsageMethod(UsageMethod.AVAILABLE)
                .add()
                .add();

        assertEquals(1, crac.getRangeActions().size());
        assertEquals(networkElementId, hvdcRangeAction.getNetworkElement().getId());
        assertEquals("BE", hvdcRangeAction.getOperator());
        assertEquals(1, hvdcRangeAction.getRanges().size());
        assertEquals(1, hvdcRangeAction.getUsageRules().size());
    }

    @Test
    void testAddWithoutUsageRule() {
        /*
        This behaviour is considered admissible:
            - without usage rule, the remedial action will never be available

        This test should however return two warnings
         */
        HvdcRangeAction hvdcRangeAction = crac.newHvdcRangeAction()
                .withId("id1")
                .withOperator("BE")
                .withNetworkElement(networkElementId)
                .newRange().withMin(-5).withMax(10).add()
                .add();

        assertEquals(1, crac.getRangeActions().size());
        assertEquals(networkElementId, hvdcRangeAction.getNetworkElement().getId());
        assertEquals("BE", hvdcRangeAction.getOperator());
        assertEquals(1, hvdcRangeAction.getRanges().size());
        assertEquals(0, hvdcRangeAction.getUsageRules().size());
    }

    @Test
    void testAddWithoutOperator() {
        HvdcRangeAction hvdcRangeAction = crac.newHvdcRangeAction()
                .withId("id1")
                .withNetworkElement(networkElementId)
                .newRange().withMin(-5).withMax(10).add()
                .newOnInstantUsageRule()
                .withInstant(PREVENTIVE_INSTANT_ID)
                .withUsageMethod(UsageMethod.AVAILABLE)
                .add()
                .add();

        assertEquals(1, crac.getRangeActions().size());
        assertEquals(networkElementId, hvdcRangeAction.getNetworkElement().getId());
        assertNull(hvdcRangeAction.getOperator());
        assertEquals(1, hvdcRangeAction.getRanges().size());
        assertEquals(1, hvdcRangeAction.getUsageRules().size());
    }

    @Test
    void testNoIdFail() {
        HvdcRangeActionAdder hvdcRangeActionAdder = crac.newHvdcRangeAction()
            .withOperator("BE")
            .withNetworkElement(networkElementId);
        OpenRaoException exception = assertThrows(OpenRaoException.class, hvdcRangeActionAdder::add);
        assertEquals("Cannot add a HvdcRangeAction object with no specified id. Please use withId()", exception.getMessage());
    }

    @Test
    void testNoNetworkElementFail() {
        HvdcRangeActionAdder hvdcRangeActionAdder = crac.newHvdcRangeAction()
            .withId("id1")
            .withOperator("BE");
        OpenRaoException exception = assertThrows(OpenRaoException.class, hvdcRangeActionAdder::add);
        assertEquals("Cannot add HvdcRangeAction without a network element. Please use withNetworkElement() with a non null value", exception.getMessage());
    }

    @Test
    void testIdNotUnique() {
        crac.newNetworkAction()
                .withId("sameId")
                .withOperator("BE")
                .newTerminalsConnectionAction().withActionType(ActionType.OPEN).withNetworkElement("action-elementId").add()
                .add();
        HvdcRangeActionAdder hvdcRangeActionAdder = crac.newHvdcRangeAction()
            .withId("sameId")
            .withOperator("BE")
            .withNetworkElement("networkElementId");
        OpenRaoException exception = assertThrows(OpenRaoException.class, hvdcRangeActionAdder::add);
        assertEquals("Cannot add HvdcRangeAction without a range. Please use newRange()", exception.getMessage());
    }
}
