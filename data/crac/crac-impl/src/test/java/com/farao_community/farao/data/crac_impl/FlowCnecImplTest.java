/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.threshold.BranchThreshold;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_impl.cnec.AbstractBranchCnec;
import com.farao_community.farao.data.crac_impl.cnec.FlowCnecImpl;
import com.farao_community.farao.data.crac_impl.threshold.BranchThresholdImpl;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.ucte.util.UcteAliasesCreation;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static com.farao_community.farao.data.crac_api.Side.LEFT;
import static org.junit.Assert.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class FlowCnecImplTest {
    private final static double DOUBLE_TOLERANCE = 1;

    private AbstractBranchCnec lineCnec;
    private AbstractBranchCnec transformerCnec;
    private Set<BranchThresholdImpl> thresholds;
    private Network network12nodes;

    @Before
    public void setUp() {
        thresholds = new HashSet<>();
        State state = Mockito.mock(State.class);
        lineCnec = new FlowCnecImpl("line-cnec", new NetworkElement("FFR2AA1  FFR3AA1  1"), "FR", state, true, false, new HashSet<>(), 0.0);
        transformerCnec = new FlowCnecImpl("transformer-cnec", new NetworkElement("BBE1AA1  BBE1AA2  1"), "BE", state, true, false, new HashSet<>(), 0.0);

        network12nodes = Importers.loadNetwork(
            "TestCase12Nodes_with_Xnodes_different_imax.uct",
            getClass().getResourceAsStream("/TestCase12Nodes_with_Xnodes_different_imax.uct"));
        UcteAliasesCreation.createAliases(network12nodes);
    }

    private void fillThresholdsAndSynchronize(AbstractBranchCnec cnec) {
        thresholds.forEach(cnec::addThreshold);
        cnec.synchronize(network12nodes);
    }

    @Test
    public void testComputeMarginOnLineWithOneThresholdOnLeftSameUnitMW() {
        thresholds.add(new BranchThresholdImpl(Unit.MEGAWATT, null, 500., BranchThresholdRule.ON_LEFT_SIDE));
        fillThresholdsAndSynchronize(lineCnec);
        assertEquals(500, lineCnec.getUpperBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertFalse(lineCnec.getLowerBound(LEFT, Unit.MEGAWATT).isPresent());
        assertEquals(200, lineCnec.computeMargin(300, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(759, lineCnec.getUpperBound(LEFT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
        assertFalse(lineCnec.getLowerBound(LEFT, Unit.AMPERE).isPresent());
        assertEquals(459, lineCnec.computeMargin(300, LEFT, Unit.AMPERE), DOUBLE_TOLERANCE);
    }

    @Test
    public void testComputeMarginOnLineWithOneThresholdOnRightSameUnitMW() {
        thresholds.add(new BranchThresholdImpl(Unit.MEGAWATT, null, 500., BranchThresholdRule.ON_RIGHT_SIDE));
        fillThresholdsAndSynchronize(lineCnec);
        assertEquals(200, lineCnec.computeMargin(300, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(459, lineCnec.computeMargin(300, LEFT, Unit.AMPERE), DOUBLE_TOLERANCE);
    }

    @Test
    public void testComputeMarginOnLineWithOneThresholdOnLeftSameUnitAmps() {
        thresholds.add(new BranchThresholdImpl(Unit.AMPERE, null, 500., BranchThresholdRule.ON_LEFT_SIDE));
        fillThresholdsAndSynchronize(lineCnec);
        assertEquals(329, lineCnec.getUpperBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertFalse(lineCnec.getLowerBound(LEFT, Unit.MEGAWATT).isPresent());
        assertEquals(29, lineCnec.computeMargin(300, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(500, lineCnec.getUpperBound(LEFT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
        assertFalse(lineCnec.getLowerBound(LEFT, Unit.AMPERE).isPresent());
        assertEquals(200, lineCnec.computeMargin(300, LEFT, Unit.AMPERE), DOUBLE_TOLERANCE);
    }

    @Test
    public void testComputeMarginOnLineWithOneThresholdOnRightSameUnitAmps() {
        thresholds.add(new BranchThresholdImpl(Unit.AMPERE, null, 500., BranchThresholdRule.ON_RIGHT_SIDE));
        fillThresholdsAndSynchronize(lineCnec);
        assertEquals(29, lineCnec.computeMargin(300, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(200, lineCnec.computeMargin(300, LEFT, Unit.AMPERE), DOUBLE_TOLERANCE);
    }

    // Tests on transformers : LEFT is low-voltage level and RIGHT is high-voltage level

    // TEST 1 : When a limit is defined in MW, as flows are always declared on LEFT side, margins must be the same
    // whether the limit is defined on LEFT or RIGHT side.
    @Test
    public void testComputeMarginOnTransformerWithOneThresholdOnLeftSameUnitMW() {
        thresholds.add(new BranchThresholdImpl(Unit.MEGAWATT, null, 500., BranchThresholdRule.ON_LEFT_SIDE));
        fillThresholdsAndSynchronize(transformerCnec);
        assertEquals(500, transformerCnec.getUpperBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertFalse(transformerCnec.getLowerBound(LEFT, Unit.MEGAWATT).isPresent());
        assertEquals(200, transformerCnec.computeMargin(300, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1312, transformerCnec.getUpperBound(LEFT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
        assertFalse(transformerCnec.getLowerBound(LEFT, Unit.AMPERE).isPresent());
        assertEquals(1012, transformerCnec.computeMargin(300, LEFT, Unit.AMPERE), DOUBLE_TOLERANCE);
    }

    @Test
    public void testComputeMarginOnTransformerWithOneThresholdOnRightSameUnitMW() {
        thresholds.add(new BranchThresholdImpl(Unit.MEGAWATT, null, 500., BranchThresholdRule.ON_RIGHT_SIDE));
        fillThresholdsAndSynchronize(transformerCnec);
        assertEquals(200, transformerCnec.computeMargin(300, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1012, transformerCnec.computeMargin(300, LEFT, Unit.AMPERE), DOUBLE_TOLERANCE);
    }

    // TEST 2 : When a limit is defined in A:
    // - If it is on left:
    //   - For a flow in Amps we have directly 500 - 300 = 200 A
    //   - For a flow in MW: it's still on the good side so we can call threshold compute margin method directly
    //     Threshold will be converted 500A -> 190,5MW, so 190,5 - 300 = -109MW
    // - If it is on the right:
    //   - For a flow in Amps: flow has to be computed on the right side 300A -> 300 * 220 / 380 = 173,68A, then we can
    //     compute the margin 500 - 174 = 326,31A, then we have to convert it for the left side again 326A -> 563,63A
    //   - For a flow in MW: it's the same for side one and two so no need to convert, we can call threshold compute
    //     margin method, that will convert threshold to MW 500A -> 328,7MW, then make the difference 328,7 - 300 = 28,7MW
    @Test
    public void testComputeMarginOnTransformerWithOneThresholdOnLeftSameUnitAmps() {
        thresholds.add(new BranchThresholdImpl(Unit.AMPERE, null, 500., BranchThresholdRule.ON_LEFT_SIDE));
        fillThresholdsAndSynchronize(transformerCnec);
        assertEquals(500, transformerCnec.getUpperBound(LEFT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
        assertFalse(transformerCnec.getLowerBound(LEFT, Unit.AMPERE).isPresent());
        assertEquals(200, transformerCnec.computeMargin(300, LEFT, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(190, transformerCnec.getUpperBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertFalse(transformerCnec.getLowerBound(LEFT, Unit.MEGAWATT).isPresent());
        assertEquals(-109, transformerCnec.computeMargin(300, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testComputeMarginOnTransformerWithOneThresholdOnRightSameUnitAmps() {
        thresholds.add(new BranchThresholdImpl(Unit.AMPERE, null, 500., BranchThresholdRule.ON_RIGHT_SIDE));
        fillThresholdsAndSynchronize(transformerCnec);
        assertEquals(563, transformerCnec.computeMargin(300, LEFT, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(329, transformerCnec.getUpperBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertFalse(transformerCnec.getLowerBound(LEFT, Unit.MEGAWATT).isPresent());
        assertEquals(29, transformerCnec.computeMargin(300, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    // Tests on concurrency between thresholds

    @Test
    public void testComputeMarginOnLineWithSeveralThresholdsWithLimitingOnLeftOrRightSide() {
        thresholds.add(new BranchThresholdImpl(Unit.MEGAWATT, null, 100., BranchThresholdRule.ON_LEFT_SIDE));
        thresholds.add(new BranchThresholdImpl(Unit.MEGAWATT, -200., null, BranchThresholdRule.ON_LEFT_SIDE));
        thresholds.add(new BranchThresholdImpl(Unit.MEGAWATT, null, 500., BranchThresholdRule.ON_RIGHT_SIDE));
        thresholds.add(new BranchThresholdImpl(Unit.MEGAWATT, -300., null, BranchThresholdRule.ON_RIGHT_SIDE));
        fillThresholdsAndSynchronize(lineCnec);

        assertEquals(100, lineCnec.getUpperBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-200, lineCnec.getLowerBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-200, lineCnec.computeMargin(300, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(0, lineCnec.computeMargin(-200, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testComputeMarginOnLineWithSeveralThresholdsWithBoth() {
        thresholds.add(new BranchThresholdImpl(Unit.MEGAWATT, null, 100., BranchThresholdRule.ON_LEFT_SIDE));
        thresholds.add(new BranchThresholdImpl(Unit.MEGAWATT, -200., null, BranchThresholdRule.ON_LEFT_SIDE));
        thresholds.add(new BranchThresholdImpl(Unit.MEGAWATT, null, 500., BranchThresholdRule.ON_RIGHT_SIDE));
        thresholds.add(new BranchThresholdImpl(Unit.MEGAWATT, -300., null, BranchThresholdRule.ON_RIGHT_SIDE));
        thresholds.add(new BranchThresholdImpl(Unit.MEGAWATT, -50., 50., BranchThresholdRule.ON_RIGHT_SIDE));
        fillThresholdsAndSynchronize(lineCnec);

        assertEquals(50, lineCnec.getUpperBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-50, lineCnec.getLowerBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-250, lineCnec.computeMargin(300, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-150, lineCnec.computeMargin(-200, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testComputeMarginOnTransformerWithSeveralThresholdsInAmps() {
        thresholds.add(new BranchThresholdImpl(Unit.AMPERE, null, 100., BranchThresholdRule.ON_LEFT_SIDE));
        thresholds.add(new BranchThresholdImpl(Unit.AMPERE, -70., null, BranchThresholdRule.ON_LEFT_SIDE));
        // This threshold is 86A on LEFT side, so it's limiting for DIRECT but not for OPPOSITE flow
        thresholds.add(new BranchThresholdImpl(Unit.AMPERE, -50., 50., BranchThresholdRule.ON_RIGHT_SIDE));
        fillThresholdsAndSynchronize(transformerCnec);

        assertEquals(86, transformerCnec.getUpperBound(LEFT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-70, transformerCnec.getLowerBound(LEFT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-14, transformerCnec.computeMargin(100, LEFT, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-30, transformerCnec.computeMargin(-100, LEFT, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-70, transformerCnec.getLowerBound(LEFT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(86, transformerCnec.getUpperBound(LEFT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testSetOperator() {
        BranchCnec cnec = new FlowCnecImpl("line-cnec", new NetworkElement("FFR2AA1  FFR3AA1  1"), "FR", null, true, false, new HashSet<>(), 0.0);
        assertEquals("FR", cnec.getOperator());
        cnec = new FlowCnecImpl("line-cnec", "line-cnec", new NetworkElement("FFR2AA1  FFR3AA1  1"), "D7", null, true, false, new HashSet<>(), 0.0);
        assertEquals("D7", cnec.getOperator());
    }

    @Test
    public void synchronizeTwoCnecsCreatedWithSameThresholdObject() {
        State state = Mockito.mock(State.class);
        BranchThreshold relativeFlowThreshold = new BranchThresholdImpl(Unit.PERCENT_IMAX, null, 0.5, BranchThresholdRule.ON_LEFT_SIDE);
        BranchCnec cnecOnLine1 = new FlowCnecImpl("cnec1", new NetworkElement("DDE1AA1  DDE2AA1  1"), "D7", state, true, false, Collections.singleton(relativeFlowThreshold), 0.);
        BranchCnec cnecOnLine2 = new FlowCnecImpl("cnec2", new NetworkElement("BBE2AA1  X_BEFR1  1"), "BE", state, true, false, Collections.singleton(relativeFlowThreshold), 0.);

        cnecOnLine2.synchronize(network12nodes);
        cnecOnLine1.synchronize(network12nodes);

        assertEquals(2500, cnecOnLine1.getUpperBound(LEFT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(1645, cnecOnLine1.getUpperBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(750, cnecOnLine2.getUpperBound(LEFT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(494, cnecOnLine2.getUpperBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
    }

    @Test
    public void severalThresholdTest1() {
        thresholds.add(new BranchThresholdImpl(Unit.MEGAWATT, null, 500., BranchThresholdRule.ON_LEFT_SIDE));
        thresholds.add(new BranchThresholdImpl(Unit.MEGAWATT, -200., null, BranchThresholdRule.ON_LEFT_SIDE));
        fillThresholdsAndSynchronize(lineCnec);

        assertEquals(500, lineCnec.getUpperBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-200, lineCnec.getLowerBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
    }

    @Test
    public void severalThresholdTest2() {
        thresholds.add(new BranchThresholdImpl(Unit.MEGAWATT, null, 500., BranchThresholdRule.ON_LEFT_SIDE));
        thresholds.add(new BranchThresholdImpl(Unit.MEGAWATT, -200., null, BranchThresholdRule.ON_LEFT_SIDE));
        thresholds.add(new BranchThresholdImpl(Unit.MEGAWATT, null, 490., BranchThresholdRule.ON_RIGHT_SIDE));
        thresholds.add(new BranchThresholdImpl(Unit.MEGAWATT, -210., null, BranchThresholdRule.ON_RIGHT_SIDE));
        fillThresholdsAndSynchronize(lineCnec);

        assertEquals(490, lineCnec.getUpperBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-200, lineCnec.getLowerBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
    }

    @Test
    public void unboundedCnecInOppositeDirection() {
        thresholds.add(new BranchThresholdImpl(Unit.MEGAWATT, null, 500., BranchThresholdRule.ON_LEFT_SIDE));
        thresholds.add(new BranchThresholdImpl(Unit.MEGAWATT, null, 200., BranchThresholdRule.ON_LEFT_SIDE));
        fillThresholdsAndSynchronize(lineCnec);

        assertEquals(200, lineCnec.getUpperBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(200, lineCnec.computeMargin(0., LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertFalse(lineCnec.getLowerBound(LEFT, Unit.MEGAWATT).isPresent());
    }

    @Test
    public void unboundedCnecInDirectDirection() {
        thresholds.add(new BranchThresholdImpl(Unit.MEGAWATT, -500., null, BranchThresholdRule.ON_LEFT_SIDE));
        thresholds.add(new BranchThresholdImpl(Unit.MEGAWATT, -200., null, BranchThresholdRule.ON_LEFT_SIDE));
        fillThresholdsAndSynchronize(lineCnec);

        assertEquals(-200, lineCnec.getLowerBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(200, lineCnec.computeMargin(0., LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertFalse(lineCnec.getUpperBound(LEFT, Unit.MEGAWATT).isPresent());
    }

    @Test
    public void marginsWithNegativeAndPositiveLimits() {
        thresholds.add(new BranchThresholdImpl(Unit.MEGAWATT, -200., 500., BranchThresholdRule.ON_LEFT_SIDE));
        fillThresholdsAndSynchronize(lineCnec);

        assertEquals(-100, lineCnec.computeMargin(-300, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(200, lineCnec.computeMargin(0, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(100, lineCnec.computeMargin(400, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-300, lineCnec.computeMargin(800, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    public void marginsWithPositiveLimits() {
        thresholds.add(new BranchThresholdImpl(Unit.MEGAWATT, 300., 500., BranchThresholdRule.ON_LEFT_SIDE));
        fillThresholdsAndSynchronize(lineCnec);

        assertEquals(-600, lineCnec.computeMargin(-300, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(50, lineCnec.computeMargin(350, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(70, lineCnec.computeMargin(430, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-300, lineCnec.computeMargin(800, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    public void marginsWithNegativeLimits() {
        thresholds.add(new BranchThresholdImpl(Unit.MEGAWATT, -500., -300., BranchThresholdRule.ON_LEFT_SIDE));
        fillThresholdsAndSynchronize(lineCnec);

        assertEquals(-300, lineCnec.computeMargin(-800, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(70, lineCnec.computeMargin(-430, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(50, lineCnec.computeMargin(-350, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-300, lineCnec.computeMargin(0, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    public void testOnSynchronize(String networkElementId, BranchThresholdRule rule, double expectedValue) {
        BranchThreshold threshold = new BranchThresholdImpl(Unit.PERCENT_IMAX, null, 1., rule);
        BranchCnec cnec = new FlowCnecImpl("cnec", new NetworkElement(networkElementId), "FR", Mockito.mock(State.class), true, false, Set.of(threshold), 0.);
        cnec.synchronize(network12nodes);
        assertEquals(expectedValue, cnec.getUpperBound(threshold.getSide(), Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testSynchronizeOnHalfLine1WithOrderCode() {
        testOnSynchronize("FFR3AA1  X_BEFR1  1", BranchThresholdRule.ON_LEFT_SIDE, 500);
    }

    @Test
    public void testSynchronizeOnHalfLine2WithOrderCode() {
        testOnSynchronize("BBE2AA1  X_BEFR1  1", BranchThresholdRule.ON_LEFT_SIDE, 1500);
    }

    @Test
    public void testSynchronizeOnHalfLine1WithElementName() {
        testOnSynchronize("DDE2AA1  X_NLDE1  E_NAME_H1", BranchThresholdRule.ON_LEFT_SIDE, 2000);
    }

    @Test
    public void testSynchronizeOnHalfLine2WithElementName() {
        testOnSynchronize("NNL3AA1  X_NLDE1  E_NAME_H2", BranchThresholdRule.ON_LEFT_SIDE, 3000);
    }

    @Test
    public void testSynchronizeOnTieLine() {
        // It takes most limiting threshold
        testOnSynchronize("BBE2AA1  X_BEFR1  1 + FFR3AA1  X_BEFR1  1", BranchThresholdRule.ON_LEFT_SIDE, 500);
    }

    @Test
    public void testSynchronizeOnTransformerGoodSide() {
        testOnSynchronize("BBE1AA1  BBE1AA2  1", BranchThresholdRule.ON_RIGHT_SIDE, 1000);
        testOnSynchronize("BBE2AA2  BBE2AA1  2", BranchThresholdRule.ON_RIGHT_SIDE, 1200);
    }

    @Test
    public void testSynchronizeOnTransformerWrongSide() {
        testOnSynchronize("BBE1AA1  BBE1AA2  1", BranchThresholdRule.ON_LEFT_SIDE, 1727); // 1000 * 380 / 220
        testOnSynchronize("BBE2AA2  BBE2AA1  2", BranchThresholdRule.ON_LEFT_SIDE, 695); // 1200 / 380 * 220
    }

    @Test
    public void testCopy() {
        thresholds.add(new BranchThresholdImpl(Unit.MEGAWATT, -500., null, BranchThresholdRule.ON_LEFT_SIDE));
        thresholds.add(new BranchThresholdImpl(Unit.MEGAWATT, -200., 200., BranchThresholdRule.ON_LEFT_SIDE));
        fillThresholdsAndSynchronize(lineCnec);

        assertTrue(lineCnec.copy() instanceof FlowCnecImpl);
        FlowCnecImpl copy = (FlowCnecImpl) lineCnec.copy();
        assertEquals(lineCnec.getId(), copy.getId());
        assertEquals(lineCnec.getName(), copy.getName());
        assertEquals(lineCnec.getNetworkElement(), copy.getNetworkElement());
        assertEquals(lineCnec.getOperator(), copy.getOperator());
        assertEquals(lineCnec.getState(), copy.getState());
        assertEquals(lineCnec.isOptimized(), copy.isOptimized());
        assertEquals(lineCnec.isMonitored(), copy.isMonitored());
        assertEquals(lineCnec.getThresholds(), copy.getThresholds());
        assertEquals(lineCnec.getReliabilityMargin(), copy.getReliabilityMargin(), 0.001);
    }

    @Test
    public void testCopyWithArguments() {
        thresholds.add(new BranchThresholdImpl(Unit.MEGAWATT, -500., null, BranchThresholdRule.ON_LEFT_SIDE));
        thresholds.add(new BranchThresholdImpl(Unit.MEGAWATT, -200., 200., BranchThresholdRule.ON_LEFT_SIDE));
        fillThresholdsAndSynchronize(lineCnec);

        NetworkElement ne = new NetworkElement("ne");
        State state = new PreventiveState();

        assertTrue(lineCnec.copy(ne, state) instanceof FlowCnecImpl);
        FlowCnecImpl copy = (FlowCnecImpl) lineCnec.copy(ne, state);
        assertEquals(lineCnec.getId(), copy.getId());
        assertEquals(lineCnec.getName(), copy.getName());
        assertNotEquals(lineCnec.getNetworkElement(), copy.getNetworkElement());
        assertEquals(ne, copy.getNetworkElement());
        assertEquals(lineCnec.getOperator(), copy.getOperator());
        assertNotEquals(lineCnec.getState(), copy.getState());
        assertEquals(state, copy.getState());
        assertEquals(lineCnec.isOptimized(), copy.isOptimized());
        assertEquals(lineCnec.isMonitored(), copy.isMonitored());
        assertEquals(lineCnec.getThresholds(), copy.getThresholds());
        assertEquals(lineCnec.getReliabilityMargin(), copy.getReliabilityMargin(), 0.001);
    }

    @Test
    public void testGetLocation() {
        Set<Optional<Country>> countries = lineCnec.getLocation(network12nodes);
        assertEquals(1, countries.size());
        assertTrue(countries.contains(Optional.of(Country.FR)));

        countries = transformerCnec.getLocation(network12nodes);
        assertEquals(1, countries.size());
        assertTrue(countries.contains(Optional.of(Country.BE)));
    }

    @Test
    public void testGetLocation2() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Cnec cnec = new FlowCnecImpl("line-cnec", new NetworkElement("DDE2AA1  NNL3AA1  1"), "DE",
                Mockito.mock(State.class), true, false, new HashSet<>(), 0.0);
        Set<Optional<Country>> countries = cnec.getLocation(network);
        assertEquals(2, countries.size());
        assertTrue(countries.contains(Optional.of(Country.DE)));
        assertTrue(countries.contains(Optional.of(Country.NL)));
    }
}
