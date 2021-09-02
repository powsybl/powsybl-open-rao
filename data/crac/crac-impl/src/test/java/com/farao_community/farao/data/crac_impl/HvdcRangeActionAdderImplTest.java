/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class HvdcRangeActionAdderImplTest {
    private CracImpl crac;
    private String networkElementId;

    @Before
    public void setUp() {
        crac = new CracImpl("test-crac");
        networkElementId = "BBE2AA11 FFR3AA11 1";
    }

    @Test
    public void testAdd() {
        HvdcRangeAction hvdcRangeAction = crac.newHvdcRangeAction()
                .withId("id1")
                .withOperator("BE")
                .withNetworkElement(networkElementId)
                .withGroupId("groupId1")
                .newHvdcRange()
                .withMin(-10)
                .withMax(10)
                .add()
                .newFreeToUseUsageRule()
                .withInstant(Instant.PREVENTIVE)
                .withUsageMethod(UsageMethod.AVAILABLE)
                .add()
                .add();

        assertEquals(1, crac.getRangeActions().size());
        assertEquals(networkElementId, hvdcRangeAction.getNetworkElements().iterator().next().getId());
        assertEquals("BE", hvdcRangeAction.getOperator());
        assertEquals(1, hvdcRangeAction.getRanges().size());
        assertEquals(1, hvdcRangeAction.getUsageRules().size());
        assertEquals(1, crac.getNetworkElements().size());
        assertNotNull(crac.getNetworkElement(networkElementId));
    }

    @Test
    public void testAddWithoutGroupId() {
        HvdcRangeAction hvdcRangeAction = crac.newHvdcRangeAction()
                .withId("id1")
                .withOperator("BE")
                .withNetworkElement(networkElementId)
                .newHvdcRange()
                .withMin(-10)
                .withMax(10)
                .add()
                .newFreeToUseUsageRule()
                .withInstant(Instant.PREVENTIVE)
                .withUsageMethod(UsageMethod.AVAILABLE)
                .add()
                .add();

        assertEquals(1, crac.getRangeActions().size());
        assertEquals(networkElementId, hvdcRangeAction.getNetworkElements().iterator().next().getId());
        assertEquals("BE", hvdcRangeAction.getOperator());
        assertEquals(1, hvdcRangeAction.getRanges().size());
        assertEquals(1, hvdcRangeAction.getUsageRules().size());
    }

    @Test
    public void testAddWithoutRangeAndUsageRule() {
        /*
        This behaviour is considered admissible:
            - without range, the default range will be defined by the min/max value of the network
            - without usage rule, the remedial action will never be available

        This test should however return two warnings
         */
        HvdcRangeAction hvdcRangeAction = crac.newHvdcRangeAction()
                .withId("id1")
                .withOperator("BE")
                .withNetworkElement(networkElementId)
                .add();

        assertEquals(1, crac.getRangeActions().size());
        assertEquals(networkElementId, hvdcRangeAction.getNetworkElements().iterator().next().getId());
        assertEquals("BE", hvdcRangeAction.getOperator());
        assertEquals(0, hvdcRangeAction.getRanges().size());
        assertEquals(0, hvdcRangeAction.getUsageRules().size());
    }

    @Test
    public void testAddWithoutOperator() {
        HvdcRangeAction hvdcRangeAction = crac.newHvdcRangeAction()
                .withId("id1")
                .withNetworkElement(networkElementId)
                .newHvdcRange()
                .withMin(-10)
                .withMax(10)
                .add()
                .newFreeToUseUsageRule()
                .withInstant(Instant.PREVENTIVE)
                .withUsageMethod(UsageMethod.AVAILABLE)
                .add()
                .add();

        assertEquals(1, crac.getRangeActions().size());
        assertEquals(networkElementId, hvdcRangeAction.getNetworkElements().iterator().next().getId());
        assertNull(hvdcRangeAction.getOperator());
        assertEquals(1, hvdcRangeAction.getRanges().size());
        assertEquals(1, hvdcRangeAction.getUsageRules().size());
    }

    @Test(expected = FaraoException.class)
    public void testNoIdFail() {
        crac.newHvdcRangeAction()
                .withOperator("BE")
                .withNetworkElement(networkElementId)
                .add();
    }

    @Test(expected = FaraoException.class)
    public void testNoNetworkElementFail() {
        crac.newHvdcRangeAction()
                .withId("id1")
                .withOperator("BE")
                .add();
    }

    @Test
    public void testIdNotUnique() {
        crac.newNetworkAction()
                .withId("sameId")
                .withOperator("BE")
                .newTopologicalAction().withActionType(ActionType.OPEN).withNetworkElement("action-elementId").add()
                .add();

        try {
            crac.newHvdcRangeAction()
                    .withId("sameId")
                    .withOperator("BE")
                    .withNetworkElement("networkElementId")
                    .add();
            fail();
        } catch (FaraoException e) {
            // should throw
        }
    }
}
