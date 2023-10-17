/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeActionAdder;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
class HvdcRangeActionAdderImplTest {
    private static final Instant instantPrev = new InstantImpl("preventive", InstantKind.PREVENTIVE, null);
    private static final Instant instantOutage = new InstantImpl("outage", InstantKind.OUTAGE, instantPrev);
    private static final Instant instantAuto = new InstantImpl("auto", InstantKind.AUTO, instantOutage);
    private CracImpl crac;
    private String networkElementId;

    @BeforeEach
    public void setUp() {
        crac = new CracImpl("test-crac");
        networkElementId = "BBE2AA11 FFR3AA11 1";
    }

    @Test
    void testAdd() {
        HvdcRangeAction hvdcRangeAction = (HvdcRangeAction) crac.newHvdcRangeAction()
            .withId("id1")
            .withOperator("BE")
            .withNetworkElement(networkElementId)
            .withGroupId("groupId1")
            .newRange().withMin(-5).withMax(10).add()
            .newOnInstantUsageRule()
            .withInstant(instantPrev)
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
        HvdcRangeAction hvdcRangeAction = (HvdcRangeAction) crac.newHvdcRangeAction()
            .withId("id1")
            .withOperator("BE")
            .withNetworkElement(networkElementId)
            .withGroupId("groupId1")
            .withSpeed(1)
            .withInitialSetpoint(1)
            .newRange().withMin(-5).withMax(10).add()
            .newOnInstantUsageRule()
            .withInstant(instantAuto)
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
            .withInstant(instantAuto)
            .withUsageMethod(UsageMethod.FORCED)
            .add();
        assertThrows(FaraoException.class, hvdcRangeActionAdder::add);
    }

    @Test
    void testAddWithoutGroupId() {
        HvdcRangeAction hvdcRangeAction = (HvdcRangeAction) crac.newHvdcRangeAction()
            .withId("id1")
            .withOperator("BE")
            .withNetworkElement(networkElementId)
            .newRange().withMin(-5).withMax(10).add()
            .newOnInstantUsageRule()
            .withInstant(instantPrev)
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
        HvdcRangeAction hvdcRangeAction = (HvdcRangeAction) crac.newHvdcRangeAction()
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
        HvdcRangeAction hvdcRangeAction = (HvdcRangeAction) crac.newHvdcRangeAction()
            .withId("id1")
            .withNetworkElement(networkElementId)
            .newRange().withMin(-5).withMax(10).add()
            .newOnInstantUsageRule()
            .withInstant(instantPrev)
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
        assertThrows(FaraoException.class, hvdcRangeActionAdder::add);
    }

    @Test
    void testNoNetworkElementFail() {
        HvdcRangeActionAdder hvdcRangeActionAdder = crac.newHvdcRangeAction()
            .withId("id1")
            .withOperator("BE");
        assertThrows(FaraoException.class, hvdcRangeActionAdder::add);
    }

    @Test
    void testIdNotUnique() {
        crac.newNetworkAction()
            .withId("sameId")
            .withOperator("BE")
            .newTopologicalAction().withActionType(ActionType.OPEN).withNetworkElement("action-elementId").add()
            .add();
        HvdcRangeActionAdder hvdcRangeActionAdder = crac.newHvdcRangeAction()
            .withId("sameId")
            .withOperator("BE")
            .withNetworkElement("networkElementId");
        assertThrows(FaraoException.class, hvdcRangeActionAdder::add);
    }
}
