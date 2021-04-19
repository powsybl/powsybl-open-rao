/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;

import static com.farao_community.farao.data.crac_api.cnec.Side.LEFT;
import static org.junit.Assert.assertEquals;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class BranchThresholdAdderImplTest {
    private static final double DOUBLE_TOLERANCE = 1e-6;
    private Crac crac;
    private Contingency contingency;

    @Before
    public void setUp() {
        crac = new CracImplFactory().create("test-crac");
        contingency = crac.newContingency().withId("conId").add();
    }

    @Test
    public void testAddThresholdInMW() {
        BranchCnec<?> cnec = crac.newFlowCnec()
            .withId("test-cnec").withInstant(Instant.OUTAGE).withContingency(contingency.getId())
            .withNetworkElement("neID")
            .newThreshold().withUnit(Unit.MEGAWATT).withMin(-250.0).withMax(1000.0).withRule(BranchThresholdRule.ON_LEFT_SIDE).add()
            .add();
        assertEquals(1000.0, cnec.getUpperBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-250.0, cnec.getLowerBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testAddThresholdInA() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        BranchCnec<?>  cnec = crac.newFlowCnec()
            .withId("test-cnec").withInstant(Instant.OUTAGE).withContingency(contingency.getId())
            .withNetworkElement("BBE1AA1  BBE2AA1  1")
            .newThreshold().withUnit(Unit.AMPERE).withMin(-1000.).withMax(1000.).withRule(BranchThresholdRule.ON_LEFT_SIDE).add()
            .add();
        cnec.synchronize(network);
        assertEquals(1000.0, cnec.getUpperBound(LEFT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-1000.0, cnec.getLowerBound(LEFT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testAddThresholdInPercent() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        String lineId = "BBE1AA1  BBE2AA1  1";
        double lineLimit = network.getLine(lineId).getCurrentLimits1().getPermanentLimit();
        BranchCnec<?>  cnec = crac.newFlowCnec()
            .withId("test-cnec").withInstant(Instant.CURATIVE).withContingency(contingency.getId())
            .withNetworkElement(lineId)
            .newThreshold().withUnit(Unit.PERCENT_IMAX).withMin(-0.8).withMax(0.5).withRule(BranchThresholdRule.ON_LEFT_SIDE).add()
            .add();
        cnec.synchronize(network);
        assertEquals(0.5 * lineLimit, cnec.getUpperBound(LEFT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-0.8 * lineLimit, cnec.getLowerBound(LEFT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
    }

    @Test(expected = NullPointerException.class)
    public void testNullParentFail() {
        new BranchThresholdAdderImpl(null);
    }

    @Test(expected = FaraoException.class)
    public void testUnsupportedUnitFail() {
        crac.newFlowCnec().newThreshold().withUnit(Unit.KILOVOLT);
    }

    @Test(expected = FaraoException.class)
    public void testNoUnitFail() {
        crac.newFlowCnec().newThreshold()
            .withMax(1000.0)
            .withRule(BranchThresholdRule.ON_LEFT_SIDE)
            .add();
    }

    @Test(expected = FaraoException.class)
    public void testNoValueFail() {
        crac.newFlowCnec().newThreshold()
            .withUnit(Unit.AMPERE)
            .withRule(BranchThresholdRule.ON_LEFT_SIDE)
            .add();
    }

    @Test(expected = FaraoException.class)
    public void testNoSideFail() {
        crac.newFlowCnec().newThreshold()
            .withUnit(Unit.AMPERE)
            .withMax(1000.0)
            .add();
    }
}
