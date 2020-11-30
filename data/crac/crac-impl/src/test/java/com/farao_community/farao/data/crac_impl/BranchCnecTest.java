/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.threshold.AbsoluteFlowThreshold;
import com.farao_community.farao.data.crac_impl.threshold.AbstractFlowThreshold;
import com.farao_community.farao.data.crac_impl.threshold.RelativeFlowThreshold;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.ucte.util.UcteAliasesCreation;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class BranchCnecTest {
    private final static double DOUBLE_TOLERANCE = 1;

    private BranchCnec lineCnec;
    private BranchCnec transformerCnec;
    private Set<AbstractFlowThreshold> thresholds;
    private Network network12nodes;

    @Before
    public void setUp() {
        thresholds = new HashSet<>();
        State state = Mockito.mock(State.class);
        lineCnec = new BranchCnec("line-cnec", new NetworkElement("FFR2AA1  FFR3AA1  1"), new HashSet<>(), state);
        transformerCnec = new BranchCnec("transformer-cnec", new NetworkElement("BBE1AA1  BBE1AA2  1"), new HashSet<>(), state);

        network12nodes = Importers.loadNetwork(
            "TestCase12Nodes_with_Xnodes_different_imax.uct",
            getClass().getResourceAsStream("/TestCase12Nodes_with_Xnodes_different_imax.uct"));
        UcteAliasesCreation.createAliases(network12nodes);
    }

    private void fillThresholdsAndSynchronize(BranchCnec cnec) {
        thresholds.forEach(cnec::addThreshold);
        cnec.synchronize(network12nodes);
    }

    @Test
    public void testComputeMarginOnLineWithOneThresholdOnLeftSameUnitMW() {
        thresholds.add(new AbsoluteFlowThreshold(Unit.MEGAWATT, Side.LEFT, Direction.DIRECT, 500));
        fillThresholdsAndSynchronize(lineCnec);
        assertEquals(500, lineCnec.getMaxThreshold(Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertFalse(lineCnec.getMinThreshold(Unit.MEGAWATT).isPresent());
        assertEquals(200, lineCnec.computeMargin(300, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(759, lineCnec.getMaxThreshold(Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
        assertFalse(lineCnec.getMinThreshold(Unit.AMPERE).isPresent());
        assertEquals(459, lineCnec.computeMargin(300, Unit.AMPERE), DOUBLE_TOLERANCE);
    }

    @Test
    public void testComputeMarginOnLineWithOneThresholdOnRightSameUnitMW() {
        thresholds.add(new AbsoluteFlowThreshold(Unit.MEGAWATT, Side.RIGHT, Direction.DIRECT, 500));
        fillThresholdsAndSynchronize(lineCnec);
        assertEquals(200, lineCnec.computeMargin(300, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(459, lineCnec.computeMargin(300, Unit.AMPERE), DOUBLE_TOLERANCE);
    }

    @Test
    public void testComputeMarginOnLineWithOneThresholdOnLeftSameUnitAmps() {
        thresholds.add(new AbsoluteFlowThreshold(Unit.AMPERE, Side.LEFT, Direction.DIRECT, 500));
        fillThresholdsAndSynchronize(lineCnec);
        assertEquals(329, lineCnec.getMaxThreshold(Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertFalse(lineCnec.getMinThreshold(Unit.MEGAWATT).isPresent());
        assertEquals(29, lineCnec.computeMargin(300, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(500, lineCnec.getMaxThreshold(Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
        assertFalse(lineCnec.getMinThreshold(Unit.AMPERE).isPresent());
        assertEquals(200, lineCnec.computeMargin(300, Unit.AMPERE), DOUBLE_TOLERANCE);
    }

    @Test
    public void testComputeMarginOnLineWithOneThresholdOnRightSameUnitAmps() {
        thresholds.add(new AbsoluteFlowThreshold(Unit.AMPERE, Side.RIGHT, Direction.DIRECT, 500));
        fillThresholdsAndSynchronize(lineCnec);
        assertEquals(29, lineCnec.computeMargin(300, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(200, lineCnec.computeMargin(300, Unit.AMPERE), DOUBLE_TOLERANCE);
    }

    // Tests on transformers : LEFT is low-voltage level and RIGHT is high-voltage level

    // TEST 1 : When a limit is defined in MW, as flows are always declared on LEFT side, margins must be the same
    // whether the limit is defined on LEFT or RIGHT side.
    @Test
    public void testComputeMarginOnTransformerWithOneThresholdOnLeftSameUnitMW() {
        thresholds.add(new AbsoluteFlowThreshold(Unit.MEGAWATT, Side.LEFT, Direction.DIRECT, 500));
        fillThresholdsAndSynchronize(transformerCnec);
        assertEquals(500, transformerCnec.getMaxThreshold(Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertFalse(transformerCnec.getMinThreshold(Unit.MEGAWATT).isPresent());
        assertEquals(200, transformerCnec.computeMargin(300, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1312, transformerCnec.getMaxThreshold(Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
        assertFalse(transformerCnec.getMinThreshold(Unit.AMPERE).isPresent());
        assertEquals(1012, transformerCnec.computeMargin(300, Unit.AMPERE), DOUBLE_TOLERANCE);
    }

    @Test
    public void testComputeMarginOnTransformerWithOneThresholdOnRightSameUnitMW() {
        thresholds.add(new AbsoluteFlowThreshold(Unit.MEGAWATT, Side.RIGHT, Direction.DIRECT, 500));
        fillThresholdsAndSynchronize(transformerCnec);
        assertEquals(200, transformerCnec.computeMargin(300, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1012, transformerCnec.computeMargin(300, Unit.AMPERE), DOUBLE_TOLERANCE);
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
        thresholds.add(new AbsoluteFlowThreshold(Unit.AMPERE, Side.LEFT, Direction.DIRECT, 500));
        fillThresholdsAndSynchronize(transformerCnec);
        assertEquals(500, transformerCnec.getMaxThreshold(Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
        assertFalse(transformerCnec.getMinThreshold(Unit.AMPERE).isPresent());
        assertEquals(200, transformerCnec.computeMargin(300, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(190, transformerCnec.getMaxThreshold(Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertFalse(transformerCnec.getMinThreshold(Unit.MEGAWATT).isPresent());
        assertEquals(-109, transformerCnec.computeMargin(300, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testComputeMarginOnTransformerWithOneThresholdOnRightSameUnitAmps() {
        thresholds.add(new AbsoluteFlowThreshold(Unit.AMPERE, Side.RIGHT, Direction.DIRECT, 500));
        fillThresholdsAndSynchronize(transformerCnec);
        assertEquals(563, transformerCnec.computeMargin(300, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(329, transformerCnec.getMaxThreshold(Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertFalse(transformerCnec.getMinThreshold(Unit.MEGAWATT).isPresent());
        assertEquals(29, transformerCnec.computeMargin(300, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    // Tests on concurrency between thresholds

    @Test
    public void testComputeMarginOnLineWithSeveralThresholdsWithLimitingOnLeftOrRightSide() {
        thresholds.add(new AbsoluteFlowThreshold(Unit.MEGAWATT, Side.LEFT, Direction.DIRECT, 100));
        thresholds.add(new AbsoluteFlowThreshold(Unit.MEGAWATT, Side.LEFT, Direction.OPPOSITE, 200));
        thresholds.add(new AbsoluteFlowThreshold(Unit.MEGAWATT, Side.RIGHT, Direction.DIRECT, 500));
        thresholds.add(new AbsoluteFlowThreshold(Unit.MEGAWATT, Side.RIGHT, Direction.OPPOSITE, 300));
        fillThresholdsAndSynchronize(lineCnec);

        assertEquals(100, lineCnec.getMaxThreshold(Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-200, lineCnec.getMinThreshold(Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-200, lineCnec.computeMargin(300, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(0, lineCnec.computeMargin(-200, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testComputeMarginOnLineWithSeveralThresholdsWithBoth() {
        thresholds.add(new AbsoluteFlowThreshold(Unit.MEGAWATT, Side.LEFT, Direction.DIRECT, 100));
        thresholds.add(new AbsoluteFlowThreshold(Unit.MEGAWATT, Side.LEFT, Direction.OPPOSITE, 200));
        thresholds.add(new AbsoluteFlowThreshold(Unit.MEGAWATT, Side.RIGHT, Direction.DIRECT, 500));
        thresholds.add(new AbsoluteFlowThreshold(Unit.MEGAWATT, Side.RIGHT, Direction.OPPOSITE, 300));
        thresholds.add(new AbsoluteFlowThreshold(Unit.MEGAWATT, Side.RIGHT, Direction.BOTH, 50));
        fillThresholdsAndSynchronize(lineCnec);

        assertEquals(50, lineCnec.getMaxThreshold(Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-50, lineCnec.getMinThreshold(Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-250, lineCnec.computeMargin(300, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-150, lineCnec.computeMargin(-200, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testComputeMarginOnTransformerWithSeveralThresholdsInAmps() {
        thresholds.add(new AbsoluteFlowThreshold(Unit.AMPERE, Side.LEFT, Direction.DIRECT, 100));
        thresholds.add(new AbsoluteFlowThreshold(Unit.AMPERE, Side.LEFT, Direction.OPPOSITE, 70));
        // This threshold is 86A on LEFT side, so it's limiting for DIRECT but not for OPPOSITE flow
        thresholds.add(new AbsoluteFlowThreshold(Unit.AMPERE, Side.RIGHT, Direction.BOTH, 50));
        fillThresholdsAndSynchronize(transformerCnec);

        assertEquals(86, transformerCnec.getMaxThreshold(Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-70, transformerCnec.getMinThreshold(Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-14, transformerCnec.computeMargin(100, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-30, transformerCnec.computeMargin(-100, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-70, transformerCnec.getMinThreshold(Unit.AMPERE).get(), DOUBLE_TOLERANCE);
        assertEquals(86, transformerCnec.getMaxThreshold(Unit.AMPERE).get(), DOUBLE_TOLERANCE);
    }

    @Test
    public void synchronizeTwoCnecsCreatedWithSameThresholdObject() {
        State state = Mockito.mock(State.class);
        RelativeFlowThreshold relativeFlowThreshold = new RelativeFlowThreshold(Side.LEFT, Direction.DIRECT, 50);
        Cnec cnecOnLine1 = new BranchCnec("cnec1", new NetworkElement("DDE1AA1  DDE2AA1  1"), Collections.singleton(relativeFlowThreshold), state);
        Cnec cnecOnLine2 = new BranchCnec("cnec2", new NetworkElement("BBE2AA1  X_BEFR1  1"), Collections.singleton(relativeFlowThreshold), state);

        cnecOnLine2.synchronize(network12nodes);
        cnecOnLine1.synchronize(network12nodes);

        assertEquals(2500, cnecOnLine1.getMaxThreshold(Unit.AMPERE).get(), DOUBLE_TOLERANCE);
        assertEquals(1645, cnecOnLine1.getMaxThreshold(Unit.MEGAWATT).get(), DOUBLE_TOLERANCE);
        assertEquals(750, cnecOnLine2.getMaxThreshold(Unit.AMPERE).get(), DOUBLE_TOLERANCE);
        assertEquals(494, cnecOnLine2.getMaxThreshold(Unit.MEGAWATT).get(), DOUBLE_TOLERANCE);
    }

    @Test
    public void severalThresholdTest1() {
        thresholds.add(new AbsoluteFlowThreshold(Unit.MEGAWATT, Side.LEFT, Direction.DIRECT, 500));
        thresholds.add(new AbsoluteFlowThreshold(Unit.MEGAWATT, Side.LEFT, Direction.OPPOSITE, 200));
        fillThresholdsAndSynchronize(lineCnec);

        assertEquals(500, lineCnec.getMaxThreshold(Unit.MEGAWATT).get(), DOUBLE_TOLERANCE);
        assertEquals(-200, lineCnec.getMinThreshold(Unit.MEGAWATT).get(), DOUBLE_TOLERANCE);
    }

    @Test
    public void severalThresholdTest2() {
        thresholds.add(new AbsoluteFlowThreshold(Unit.MEGAWATT, Side.LEFT, Direction.DIRECT, 500));
        thresholds.add(new AbsoluteFlowThreshold(Unit.MEGAWATT, Side.LEFT, Direction.OPPOSITE, 200));
        thresholds.add(new AbsoluteFlowThreshold(Unit.MEGAWATT, Side.RIGHT, Direction.DIRECT, 490));
        thresholds.add(new AbsoluteFlowThreshold(Unit.MEGAWATT, Side.RIGHT, Direction.OPPOSITE, 210));
        fillThresholdsAndSynchronize(lineCnec);

        assertEquals(490, lineCnec.getMaxThreshold(Unit.MEGAWATT).get(), DOUBLE_TOLERANCE);
        assertEquals(-200, lineCnec.getMinThreshold(Unit.MEGAWATT).get(), DOUBLE_TOLERANCE);
    }

    @Test
    public void unboundedCnecInOppositeDirection() {
        thresholds.add(new AbsoluteFlowThreshold(Unit.MEGAWATT, Side.LEFT, Direction.DIRECT, 500));
        thresholds.add(new AbsoluteFlowThreshold(Unit.MEGAWATT, Side.LEFT, Direction.DIRECT, 200));
        fillThresholdsAndSynchronize(lineCnec);

        assertEquals(200, lineCnec.getMaxThreshold(Unit.MEGAWATT).get(), DOUBLE_TOLERANCE);
        assertFalse(lineCnec.getMinThreshold(Unit.MEGAWATT).isPresent());
    }

    @Test
    public void unboundedCnecInDirectDirection() {
        thresholds.add(new AbsoluteFlowThreshold(Unit.MEGAWATT, Side.LEFT, Direction.OPPOSITE, 500));
        thresholds.add(new AbsoluteFlowThreshold(Unit.MEGAWATT, Side.LEFT, Direction.OPPOSITE, 200));
        fillThresholdsAndSynchronize(lineCnec);

        assertEquals(-200, lineCnec.getMinThreshold(Unit.MEGAWATT).get(), 0.1);
        assertFalse(lineCnec.getMaxThreshold(Unit.MEGAWATT).isPresent());
    }
}
