/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeType;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class PstRangeActionAdderImplTest {
    private SimpleCrac crac;
    private String networkElementId;

    @Before
    public void setUp() {
        crac = new SimpleCrac("test-crac");
        networkElementId = "BBE2AA1  BBE3AA1  1";
    }

    @Test
    public void testAdd() {
        PstRangeAction pstRangeAction = crac.newPstRangeAction()
                .withId("id1")
                .withOperator("BE")
                .withNetworkElement(networkElementId)
                .newPstRange()
                    .withMin(-10)
                    .withMax(10)
                    .withRangeDefinition(TapConvention.CENTERED_ON_ZERO)
                    .withRangeType(RangeType.ABSOLUTE)
                    .withUnit(Unit.TAP)
                    .add()
                .newFreeToUseUsageRule()
                    .withInstant(Instant.PREVENTIVE)
                    .withUsageMethod(UsageMethod.AVAILABLE)
                    .add()
                .add();

        assertEquals(1, crac.getRangeActions().size());
        assertEquals(networkElementId, pstRangeAction.getNetworkElements().iterator().next().getId());
        assertEquals("BE", pstRangeAction.getOperator());
        assertEquals(1, pstRangeAction.getRanges().size());
        assertEquals(1, pstRangeAction.getUsageRules().size());
    }

    @Test
    public void testAddWithoutRangeAndUsageRule() {
        /*
        This behaviour is considered admissible:
            - without range, the default range will be defined by the min/max value of the network
            - without usage rule, the remedial action will never be available

        This test should however returns two warnings
         */
        PstRangeAction pstRangeAction = crac.newPstRangeAction()
            .withId("id1")
            .withOperator("BE")
            .withNetworkElement(networkElementId)
            .add();

        assertEquals(1, crac.getRangeActions().size());
        assertEquals(networkElementId, pstRangeAction.getNetworkElements().iterator().next().getId());
        assertEquals("BE", pstRangeAction.getOperator());
        assertEquals(0, pstRangeAction.getRanges().size());
        assertEquals(0, pstRangeAction.getUsageRules().size());
    }

    @Test
    public void testAddWithoutOperator() {
        PstRangeAction pstRangeAction = crac.newPstRangeAction()
            .withId("id1")
            .withNetworkElement(networkElementId)
            .newPstRange()
                .withMin(-10)
                .withMax(10)
                .withRangeDefinition(TapConvention.CENTERED_ON_ZERO)
                .withRangeType(RangeType.ABSOLUTE)
                .withUnit(Unit.TAP)
                .add()
            .newFreeToUseUsageRule()
                .withInstant(Instant.PREVENTIVE)
                .withUsageMethod(UsageMethod.AVAILABLE)
                .add()
            .add();

        assertEquals(1, crac.getRangeActions().size());
        assertEquals(networkElementId, pstRangeAction.getNetworkElements().iterator().next().getId());
        assertNull(pstRangeAction.getOperator());
        assertEquals(1, pstRangeAction.getRanges().size());
        assertEquals(1, pstRangeAction.getUsageRules().size());
    }

    @Test(expected = FaraoException.class)
    public void testNoIdFail() {
        crac.newPstRangeAction()
            .withOperator("BE")
                .withNetworkElement(networkElementId)
                .add();
    }

    @Test(expected = FaraoException.class)
    public void testNoNetworkElementFail() {
        crac.newPstRangeAction()
                .withId("id1")
                .withOperator("BE")
                .add();
    }

}
