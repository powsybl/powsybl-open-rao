/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.threshold;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_impl.SimpleCracFactory;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static com.farao_community.farao.data.crac_api.Side.LEFT;
import static org.junit.Assert.assertEquals;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class FlowThresholdAdderTest {
    private static final double DOUBLE_TOLERANCE = 1e-6;
    private Crac crac;
    private Contingency contingency;

    @Before
    public void setUp() {
        crac = new SimpleCracFactory().create("test-crac", Set.of(Instant.OUTAGE, Instant.CURATIVE));
        contingency = crac.newContingency().setId("conId").add();
    }

    @Test
    public void testAddThresholdInMW() {
        BranchCnec cnec = crac.newBranchCnec()
            .setId("test-cnec").setInstant(Instant.OUTAGE).setContingency(contingency)
            .newNetworkElement().setId("neID").add()
            .newThreshold().setUnit(Unit.MEGAWATT).setMin(-250.0).setMax(1000.0).setRule(BranchThresholdRule.ON_LEFT_SIDE).add()
            .add();
        assertEquals(1000.0, cnec.getUpperBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-250.0, cnec.getLowerBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testAddThresholdInA() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        BranchCnec cnec = crac.newBranchCnec()
            .setId("test-cnec").setInstant(Instant.OUTAGE).setContingency(contingency)
            .newNetworkElement().setId("BBE1AA1  BBE2AA1  1").add()
            .newThreshold().setUnit(Unit.AMPERE).setMin(-1000.).setMax(1000.).setRule(BranchThresholdRule.ON_LEFT_SIDE).add()
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
        BranchCnec cnec = crac.newBranchCnec()
            .setId("test-cnec").setInstant(Instant.CURATIVE).setContingency(contingency)
            .newNetworkElement().setId(lineId).add()
            .newThreshold().setUnit(Unit.PERCENT_IMAX).setMin(-0.8).setMax(0.5).setRule(BranchThresholdRule.ON_LEFT_SIDE).add()
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
        crac.newBranchCnec().newThreshold().setUnit(Unit.KILOVOLT);
    }

    @Test(expected = FaraoException.class)
    public void testNoUnitFail() {
        crac.newBranchCnec().newThreshold()
            .setMax(1000.0)
            .setRule(BranchThresholdRule.ON_LEFT_SIDE)
            .add();
    }

    @Test(expected = FaraoException.class)
    public void testNoValueFail() {
        crac.newBranchCnec().newThreshold()
            .setUnit(Unit.AMPERE)
            .setRule(BranchThresholdRule.ON_LEFT_SIDE)
            .add();
    }

    @Test(expected = FaraoException.class)
    public void testNoSideFail() {
        crac.newBranchCnec().newThreshold()
            .setUnit(Unit.AMPERE)
            .add();
    }
}
