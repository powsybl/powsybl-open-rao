/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.usage_rule.FreeToUseImpl;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class PstRangeActionAdderImplTest {
    private static final double DOUBLE_TOLERANCE = 1e-3;
    private SimpleCrac crac;
    private Network network;
    private String networkElementId;

    @Before
    public void setUp() {
        crac = new SimpleCrac("test-crac", "test-crac", Set.of(Instant.OUTAGE, Instant.CURATIVE));
        network = NetworkImportsUtil.import12NodesNetwork();
        networkElementId = "BBE2AA1  BBE3AA1  1";
    }

    @Test
    public void testAdd() {
        Crac crac1 = crac.newPstRangeAction()
                .setId("id1")
                .setOperator("RTE")
                .setUnit(Unit.TAP)
                .setMinValue(0.0)
                .setMaxValue(10.0)
                .newNetworkElement().setId(networkElementId).add()
                .add();
        assertSame(crac, crac1);
        assertEquals(1, crac.getRangeActions().size());
        assertEquals(networkElementId, crac.getRangeAction("id1").getNetworkElements().iterator().next().getId());
        assertEquals("RTE", crac.getRangeAction("id1").getOperator());
        crac.getRangeAction("id1").synchronize(network);
        assertEquals(0.0, crac.getRangeAction("id1").getMinValue(network, 5), DOUBLE_TOLERANCE);
        // TAP position 10 should be converted to 3.894 degrees
        assertEquals(3.894, crac.getRangeAction("id1").getMaxValue(network, 5), DOUBLE_TOLERANCE);
        // Verify that the PST is free to use
        // TODO : change this when usage rules are implemented
        assertEquals(1, crac.getRangeAction("id1").getUsageRules().size());
        assertEquals(FreeToUseImpl.class, crac.getRangeAction("id1").getUsageRules().get(0).getClass());
        FreeToUseImpl usageRule = (FreeToUseImpl) crac.getRangeAction("id1").getUsageRules().get(0);
        assertEquals(UsageMethod.AVAILABLE, usageRule.getUsageMethod());
    }

    @Test(expected = FaraoException.class)
    public void testNoIdFail() {
        crac.newPstRangeAction()
                .setUnit(Unit.TAP)
                .setMinValue(0.0)
                .setMaxValue(10.0)
                .newNetworkElement().setId("neId").setName("neName").add()
                .add();
    }

    @Test
    public void testNoOperatorOk() {
        crac.newPstRangeAction()
                .setId("id")
                .setUnit(Unit.TAP)
                .setMinValue(0.0)
                .setMaxValue(10.0)
                .newNetworkElement().setId("neId").setName("neName").add()
                .add();
        assertEquals(1, crac.getRangeActions().size());
    }

    @Test(expected = FaraoException.class)
    public void testNoUnitFail() {
        crac.newPstRangeAction()
                .setId("id")
                .setMinValue(0.0)
                .setMaxValue(10.0)
                .newNetworkElement().setId("neId").setName("neName").add()
                .add();
    }

    @Test(expected = FaraoException.class)
    public void testWrongUnitFail() {
        crac.newPstRangeAction()
                .setId("id")
                .setUnit(Unit.DEGREE)
                .setMinValue(0.0)
                .setMaxValue(10.0)
                .newNetworkElement().setId("neId").setName("neName").add()
                .add();
    }

    @Test(expected = FaraoException.class)
    public void testNoMinValueFail() {
        crac.newPstRangeAction()
                .setId("id")
                .setUnit(Unit.TAP)
                .setMaxValue(10.0)
                .newNetworkElement().setId("neId").setName("neName").add()
                .add();
    }

    @Test(expected = FaraoException.class)
    public void testNoMaxValueFail() {
        crac.newPstRangeAction()
                .setId("id")
                .setUnit(Unit.TAP)
                .setMinValue(0.0)
                .newNetworkElement().setId("neId").setName("neName").add()
                .add();
    }

    @Test(expected = FaraoException.class)
    public void testNoNetworkElementFail() {
        crac.newPstRangeAction()
                .setId("id")
                .setUnit(Unit.TAP)
                .setMinValue(0.0)
                .setMaxValue(10.0)
                .add();
    }

    @Test(expected = FaraoException.class)
    public void testOnlyOneNetworkElement() {
        crac.newPstRangeAction()
                .newNetworkElement().setId("neId1").setName("neName1").add()
                .newNetworkElement().setId("neId2").setName("neName2").add();
    }

    @Test(expected = NullPointerException.class)
    public void testNullParentFail() {
        PstRangeActionAdderImpl tmp = new PstRangeActionAdderImpl(null);
    }
}
