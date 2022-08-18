/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.castor.algorithm;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.commons.SensitivityComputer;
import com.farao_community.farao.search_tree_rao.search_tree.algorithms.SearchTree;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SensitivityComputer.class, SearchTree.class})
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
public class AutomatonSimulatorTest {
    private AutomatonSimulator automatonSimulator;

    private Network network;
    private State state1;
    private RangeAction<?> ra1;
    private RangeAction<?> ra2;
    private RangeAction<?> ra3;
    private RangeAction<?> ra4;
    private RangeAction<?> ra5;
    private static final double DOUBLE_TOLERANCE = 0.01;

    @Before
    public void setup() {
        network = Importers.loadNetwork("network_with_alegro_hub.xiidm", getClass().getResourceAsStream("/network/network_with_alegro_hub.xiidm"));
        Crac crac = CracFactory.findDefault().create("test-crac");
        Contingency contingency1 = crac.newContingency()
                .withId("contingency1")
                .withNetworkElement("contingency1-ne")
                .add();
        crac.newFlowCnec()
                .withId("cnec")
                .withNetworkElement("cnec-ne")
                .withContingency("contingency1")
                .withInstant(Instant.AUTO)
                .withNominalVoltage(220.)
                .newThreshold().withRule(BranchThresholdRule.ON_RIGHT_SIDE).withMax(1000.).withUnit(Unit.AMPERE).add()
                .add();
        state1 = crac.getState(contingency1, Instant.AUTO);
        ra1 = crac.newHvdcRangeAction()
                .withId("ra1")
                .withNetworkElement("ra1-ne")
                .withSpeed(1)
                .newRange().withMax(1).withMin(-1).add()
                .add();

        ra2 = crac.newPstRangeAction()
                .withId("ra2")
                .withNetworkElement("ra2-ne")
                .withSpeed(2)
                .newFreeToUseUsageRule().withInstant(Instant.AUTO).withUsageMethod(UsageMethod.FORCED).add()
                .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
                .add();
        ra3 = crac.newPstRangeAction()
                .withId("ra3")
                .withNetworkElement("ra3-ne")
                .withSpeed(3)
                .newOnFlowConstraintUsageRule().withInstant(Instant.AUTO).withFlowCnec("cnec").add()
                .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
                .add();
        ra4 = crac.newPstRangeAction()
                .withId("ra4")
                .withNetworkElement("ra4-ne")
                .withSpeed(3)
                .newOnFlowConstraintUsageRule().withInstant(Instant.PREVENTIVE).withFlowCnec("cnec").add()
                .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
                .add();
        ra5 = crac.newInjectionRangeAction()
                .withId("ra5")
                .withNetworkElementAndKey(1.0, "key")
                .newRange().withMin(-1).withMax(1).add()
                .add();

        state1 = crac.getState(contingency1, Instant.AUTO);
        automatonSimulator = new AutomatonSimulator(crac,  new RaoParameters(), null, null, null, null, 0);
    }

    @Test
    public void testGatherCnecs() {
        assertEquals(1, automatonSimulator.gatherFlowCnecsForAutoRangeAction(ra2, state1, network).size());
        assertEquals(1, automatonSimulator.gatherFlowCnecsForAutoRangeAction(ra3, state1, network).size());
    }

    @Test
    public void testRoundUpAngleToTapWrtInitialSetpoint() {
        double setpoint1 = 0;
        double setpoint2 = 10;
        assertEquals(-100.0, AutomatonSimulator.roundUpAngleToTapWrtInitialSetpoint((PstRangeAction) ra2, setpoint1, setpoint2), DOUBLE_TOLERANCE);
        assertEquals(100.0, AutomatonSimulator.roundUpAngleToTapWrtInitialSetpoint((PstRangeAction) ra2, setpoint2, setpoint1), DOUBLE_TOLERANCE);
    }

    @Test (expected = FaraoException.class)
    public void testCheckAlignedRangeActionsType() {
        AutomatonSimulator.checkAlignedRangeActionsType(List.of(ra1, ra5));
    }

    @Test (expected = FaraoException.class)
    public void testCheckAlignedRangeActionsType2() {
        AutomatonSimulator.checkAlignedRangeActionsType(List.of(ra5, ra2));
    }

    @Test
    public void testCheckAlignedRangeActions1() {
        assertFalse(AutomatonSimulator.checkAlignedRangeActions("id", state1, List.of(ra2, ra3), List.of(ra2, ra3)));
        assertFalse(AutomatonSimulator.checkAlignedRangeActions("id", state1, List.of(ra2, ra4), List.of(ra2, ra3, ra4)));
        assertTrue(AutomatonSimulator.checkAlignedRangeActions("id", state1, List.of(ra2), List.of(ra2, ra3)));
    }
}
