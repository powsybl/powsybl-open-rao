/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.*;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;
import java.util.Set;

import static com.farao_community.farao.data.crac_api.cnec.Side.LEFT;
import static com.farao_community.farao.data.crac_api.cnec.Side.RIGHT;
import static org.junit.Assert.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class FlowCnecImplTest {

    private final static double DOUBLE_TOLERANCE = 1; // high tolerance for conversion AMPERE <-> MEGAWATT

    private Crac crac;

    @Before
    public void setUp() {
        crac = new CracImplFactory().create("cracId");
    }

    private FlowCnecAdder initPreventiveCnecAdder() {
        return crac.newFlowCnec()
            .withId("line-cnec")
            .withName("line-cnec-name")
            .withNetworkElement("anyNetworkElement")
            .withOperator("FR")
            .withInstant(Instant.PREVENTIVE)
            .withOptimized(true);
    }

    @Test
    public void testGetLocation1() {

        Network network = NetworkImportsUtil.import12NodesNetwork();

        FlowCnec cnec1 = crac.newFlowCnec()
            .withId("cnec-1-id")
            .withNetworkElement("BBE1AA1  BBE2AA1  1")
            .withInstant(Instant.PREVENTIVE)
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(1000.).withRule(BranchThresholdRule.ON_LEFT_SIDE).add()
            .add();

        FlowCnec cnec2 = crac.newFlowCnec()
            .withId("cnec-2-id")
            .withNetworkElement("DDE2AA1  NNL3AA1  1")
            .withInstant(Instant.PREVENTIVE)
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(1000.).withRule(BranchThresholdRule.ON_LEFT_SIDE).add()
            .add();

        Set<Optional<Country>> countries = cnec1.getLocation(network);
        assertEquals(1, countries.size());
        assertTrue(countries.contains(Optional.of(Country.BE)));

        countries = cnec2.getLocation(network);
        assertEquals(2, countries.size());
        assertTrue(countries.contains(Optional.of(Country.DE)));
        assertTrue(countries.contains(Optional.of(Country.NL)));
    }

    // test threshold on branches whose nominal voltage is the same on both side

    @Test
    public void testBranchWithOneMaxThresholdOnLeftInMW() {

        FlowCnec cnec = initPreventiveCnecAdder()
            .withNominalVoltage(380.)
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withRule(BranchThresholdRule.ON_LEFT_SIDE).add()
            .add();

        // bounds on LEFT side
        assertEquals(500., cnec.getUpperBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(500. / (0.38 * Math.sqrt(3)), cnec.getUpperBound(LEFT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE); // = 760 A
        assertFalse(cnec.getLowerBound(LEFT, Unit.MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(LEFT, Unit.AMPERE).isPresent());

        // bounds on RIGHT side (same values are expected as nominal voltage is the same on two sides)
        assertEquals(500., cnec.getUpperBound(RIGHT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(500. / (0.38 * Math.sqrt(3)), cnec.getUpperBound(RIGHT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE); // = 760 A
        assertFalse(cnec.getLowerBound(RIGHT, Unit.MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(RIGHT, Unit.AMPERE).isPresent());

        // margin
        assertEquals(200., cnec.computeMargin(300, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE); // bound: 500 MW
        assertEquals(200., cnec.computeMargin(300, RIGHT, Unit.MEGAWATT), DOUBLE_TOLERANCE); // bound: 500 MW
        assertEquals(460., cnec.computeMargin(300, LEFT, Unit.AMPERE), DOUBLE_TOLERANCE); // bound: 760 A
        assertEquals(1060., cnec.computeMargin(-300, RIGHT, Unit.AMPERE), DOUBLE_TOLERANCE); // bound: 760 A
    }

    @Test
    public void testBranchWithOneMinThresholdOnRightInMW() {

        FlowCnec cnec = initPreventiveCnecAdder()
            .withNominalVoltage(380.)
            .newThreshold().withUnit(Unit.MEGAWATT).withMin(-500.).withRule(BranchThresholdRule.ON_RIGHT_SIDE).add()
            .add();

        // bounds on LEFT side
        assertFalse(cnec.getUpperBound(LEFT, Unit.MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(LEFT, Unit.AMPERE).isPresent());
        assertEquals(-500., cnec.getLowerBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-500. / (0.38 * Math.sqrt(3)), cnec.getLowerBound(LEFT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE); // = -760 A

        // bounds on RIGHT side (same values are expected as nominal voltage is the same on two sides)
        assertFalse(cnec.getUpperBound(LEFT, Unit.MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(LEFT, Unit.AMPERE).isPresent());
        assertEquals(-500., cnec.getLowerBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-500. / (0.38 * Math.sqrt(3)), cnec.getLowerBound(LEFT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE); // = -760 A

        // margin
        assertEquals(800., cnec.computeMargin(300, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE); // bound: -500 MW
        assertEquals(800., cnec.computeMargin(300, RIGHT, Unit.MEGAWATT), DOUBLE_TOLERANCE); // bound: -500 MW
        assertEquals(1060, cnec.computeMargin(300, LEFT, Unit.AMPERE), DOUBLE_TOLERANCE); // bound: -760 A
        assertEquals(-240., cnec.computeMargin(-1000, RIGHT, Unit.AMPERE), DOUBLE_TOLERANCE); // bound: -760 A
    }

    @Test
    public void testBranchWithOneMinThresholdOnLeftInAmpere() {

        FlowCnec cnec = initPreventiveCnecAdder()
            .withNominalVoltage(380.)
            .newThreshold().withUnit(Unit.AMPERE).withMin(-450.).withRule(BranchThresholdRule.ON_LEFT_SIDE).add()
            .add();

        // bounds on LEFT side
        assertFalse(cnec.getUpperBound(LEFT, Unit.MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(LEFT, Unit.AMPERE).isPresent());
        assertEquals(-450., cnec.getLowerBound(LEFT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-450. * (0.38 * Math.sqrt(3)), cnec.getLowerBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE); // -296 MW

        // bounds on RIGHT side (same values are expected as nominal voltage is the same on two sides)
        assertFalse(cnec.getUpperBound(RIGHT, Unit.MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(RIGHT, Unit.AMPERE).isPresent());
        assertEquals(-450., cnec.getLowerBound(RIGHT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-450. * (0.38 * Math.sqrt(3)), cnec.getLowerBound(RIGHT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE); // -296 MW

        // margin
        assertEquals(750., cnec.computeMargin(300, LEFT, Unit.AMPERE), DOUBLE_TOLERANCE); // bound: -450 A
        assertEquals(750., cnec.computeMargin(300, RIGHT, Unit.AMPERE), DOUBLE_TOLERANCE); // bound: -450 A
        assertEquals(596., cnec.computeMargin(300, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE); // bound: -296 MW
        assertEquals(-4., cnec.computeMargin(-300, RIGHT, Unit.MEGAWATT), DOUBLE_TOLERANCE); // bound: -296 MW
    }

    @Test
    public void testBranchWithOneMaxThresholdOnRightInAmpere() {

        FlowCnec cnec = initPreventiveCnecAdder()
            .withNominalVoltage(220.)
            .newThreshold().withUnit(Unit.AMPERE).withMax(110.).withRule(BranchThresholdRule.ON_RIGHT_SIDE).add()
            .add();

        // bounds on LEFT side
        assertEquals(110., cnec.getUpperBound(LEFT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(110. * (0.22 * Math.sqrt(3)), cnec.getUpperBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE); // 42 MW
        assertFalse(cnec.getLowerBound(LEFT, Unit.MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(LEFT, Unit.AMPERE).isPresent());

        // bounds on RIGHT side (same values are expected as nominal voltage is the same on two sides)
        assertEquals(110., cnec.getUpperBound(RIGHT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(110. * (0.22 * Math.sqrt(3)), cnec.getUpperBound(RIGHT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE); // 42 MW
        assertFalse(cnec.getLowerBound(RIGHT, Unit.MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(RIGHT, Unit.AMPERE).isPresent());

        // margin
        assertEquals(-190., cnec.computeMargin(300, LEFT, Unit.AMPERE), DOUBLE_TOLERANCE); // bound: 110 A
        assertEquals(-190., cnec.computeMargin(300, RIGHT, Unit.AMPERE), DOUBLE_TOLERANCE); // bound: 110 A
        assertEquals(-258., cnec.computeMargin(300, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE); // bound: 42 MW
        assertEquals(342., cnec.computeMargin(-300, RIGHT, Unit.MEGAWATT), DOUBLE_TOLERANCE); // bound: 42 MW
    }

    @Test
    public void testBranchWithOneMaxThresholdOnLeftInPercentImax() {

        FlowCnec cnec = initPreventiveCnecAdder()
            .withNominalVoltage(380.)
            .withIMax(1000.)
            .newThreshold().withUnit(Unit.PERCENT_IMAX).withMax(1.1).withRule(BranchThresholdRule.ON_LEFT_SIDE).add() // 1.1 = 110 %
            .add();

        // bounds on LEFT side
        assertEquals(1100., cnec.getUpperBound(LEFT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE); // 1100 A
        assertEquals(1100. * (0.38 * Math.sqrt(3)), cnec.getUpperBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE); // 724 MW
        assertFalse(cnec.getLowerBound(LEFT, Unit.MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(LEFT, Unit.AMPERE).isPresent());

        // bounds on RIGHT side (same values are expected as nominal voltage is the same on two sides)
        assertEquals(1100., cnec.getUpperBound(RIGHT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE); // 1100 A
        assertEquals(1100. * (0.38 * Math.sqrt(3)), cnec.getUpperBound(RIGHT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE); // 724 MW
        assertFalse(cnec.getLowerBound(RIGHT, Unit.MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(RIGHT, Unit.AMPERE).isPresent());

        // margin
        assertEquals(-100, cnec.computeMargin(1200, LEFT, Unit.AMPERE), DOUBLE_TOLERANCE); // bound: 1100 A
        assertEquals(-100, cnec.computeMargin(1200, RIGHT, Unit.AMPERE), DOUBLE_TOLERANCE); // bound: 1100 A
        assertEquals(-26., cnec.computeMargin(750, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE); // bound: 724 MW
        assertEquals(624., cnec.computeMargin(100, RIGHT, Unit.MEGAWATT), DOUBLE_TOLERANCE); // bound: 724 MW
    }

    @Test
    public void testBranchWithOneMinThresholdOnRightInPercentImax() {

        FlowCnec cnec = initPreventiveCnecAdder()
            .withNominalVoltage(220.)
            .withIMax(1000., RIGHT)
            .withIMax(0., LEFT) // should not be considered as the threshold is on the right side
            .newThreshold().withUnit(Unit.PERCENT_IMAX).withMin(-0.9).withRule(BranchThresholdRule.ON_RIGHT_SIDE).add() // 0.9 = 90 %
            .add();

        // bounds on LEFT side
        assertFalse(cnec.getUpperBound(LEFT, Unit.MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(LEFT, Unit.AMPERE).isPresent());
        assertEquals(-900., cnec.getLowerBound(LEFT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE); // -900 A
        assertEquals(-900. * (0.22 * Math.sqrt(3)), cnec.getLowerBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE); // -343 MW

        // bounds on RIGHT side (same values are expected as nominal voltage is the same on two sides)
        assertFalse(cnec.getUpperBound(RIGHT, Unit.MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(RIGHT, Unit.AMPERE).isPresent());
        assertEquals(-900., cnec.getLowerBound(RIGHT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE); // -900 A
        assertEquals(-900. * (0.22 * Math.sqrt(3)), cnec.getLowerBound(RIGHT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE); // -343 MW

        // margin
        assertEquals(2100., cnec.computeMargin(1200, LEFT, Unit.AMPERE), DOUBLE_TOLERANCE); // bound: -900 A
        assertEquals(2100., cnec.computeMargin(1200, RIGHT, Unit.AMPERE), DOUBLE_TOLERANCE); // bound: -900 A
        assertEquals(-157, cnec.computeMargin(-500, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE); // bound: -343 MW
        assertEquals(443., cnec.computeMargin(100, RIGHT, Unit.MEGAWATT), DOUBLE_TOLERANCE); // bound: -343 MW
    }

    // test threshold on transformer whose nominal voltage is NOT the same on both side

    /*
       - when measured in MEGAWATT, the bounds on both side of a transformer are always equal

       - when measured in AMPERE, the bounds on both side of a transformer are different, one is multiply by the voltage ratio

       - when computing the margin in MEGAWATT, the returned margin will be same whether the considered flow (in MW) is considered
         on the left or right side of the transformer

       - when computing the margin in AMPERE, the returned margin will be different whether the flow (in A) is considered on the
         left or on the right side of the transformer
     */

    @Test
    public void testTransformerWithOneMaxThresholdOnLeftInMW() {

        FlowCnec cnec = initPreventiveCnecAdder()
            .withNominalVoltage(220., LEFT)
            .withNominalVoltage(380., RIGHT)
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withRule(BranchThresholdRule.ON_LEFT_SIDE).add()
            .add();

        // bounds on LEFT side
        assertEquals(500., cnec.getUpperBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(500. / (0.22 * Math.sqrt(3)), cnec.getUpperBound(LEFT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE); // 1312 A
        assertFalse(cnec.getLowerBound(LEFT, Unit.MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(LEFT, Unit.AMPERE).isPresent());

        // bounds on RIGHT side (same values are expected as nominal voltage is the same on two sides)
        assertEquals(500., cnec.getUpperBound(RIGHT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(500. / (0.38 * Math.sqrt(3)), cnec.getUpperBound(RIGHT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE); // 759 A
        assertFalse(cnec.getLowerBound(RIGHT, Unit.MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(RIGHT, Unit.AMPERE).isPresent());

        // margins
        assertEquals(400., cnec.computeMargin(100, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE); // bound: 500 MW
        assertEquals(-500., cnec.computeMargin(1000, RIGHT, Unit.MEGAWATT), DOUBLE_TOLERANCE); // bound: 500 MW
        assertEquals(1512., cnec.computeMargin(-200, LEFT, Unit.AMPERE), DOUBLE_TOLERANCE); // bound: 1312 A
        assertEquals(-1241, cnec.computeMargin(2000, RIGHT, Unit.AMPERE), DOUBLE_TOLERANCE); // bound: 759 A
    }

    @Test
    public void testTransformerWithOneMinThresholdOnRightInMW() {

        FlowCnec cnec = initPreventiveCnecAdder()
            .withNominalVoltage(220., LEFT)
            .withNominalVoltage(380., RIGHT)
            .newThreshold().withUnit(Unit.MEGAWATT).withMin(-600.).withRule(BranchThresholdRule.ON_RIGHT_SIDE).add()
            .add();

        // bounds on LEFT side
        assertFalse(cnec.getUpperBound(LEFT, Unit.MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(LEFT, Unit.AMPERE).isPresent());
        assertEquals(-600., cnec.getLowerBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-600. / (0.22 * Math.sqrt(3)), cnec.getLowerBound(LEFT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE); // - 1575 A

        // bounds on RIGHT side (same values are expected as nominal voltage is the same on two sides)
        assertFalse(cnec.getUpperBound(RIGHT, Unit.MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(RIGHT, Unit.AMPERE).isPresent());
        assertEquals(-600., cnec.getLowerBound(RIGHT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-600. / (0.38 * Math.sqrt(3)), cnec.getLowerBound(RIGHT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE); // - 912 A

        // margin
        assertEquals(1100., cnec.computeMargin(500, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE); // bound: -600 MW
        assertEquals(-400., cnec.computeMargin(-1000, RIGHT, Unit.MEGAWATT), DOUBLE_TOLERANCE); // bound: -600 MW
        assertEquals(1075., cnec.computeMargin(-500, LEFT, Unit.AMPERE), DOUBLE_TOLERANCE); // bound: -1575 A
        assertEquals(1012., cnec.computeMargin(100, RIGHT, Unit.AMPERE), DOUBLE_TOLERANCE); // bound: -912 A
    }

    @Test
    public void testTransformerWithOneMinThresholdOnLeftInA() {

        FlowCnec cnec = initPreventiveCnecAdder()
            .withNominalVoltage(220., LEFT)
            .withNominalVoltage(380., RIGHT)
            .newThreshold().withUnit(Unit.AMPERE).withMin(-1000.).withRule(BranchThresholdRule.ON_LEFT_SIDE).add()
            .add();

        // bounds on LEFT side
        assertFalse(cnec.getUpperBound(LEFT, Unit.MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(LEFT, Unit.AMPERE).isPresent());
        assertEquals(-1000., cnec.getLowerBound(LEFT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-1000. * (0.22 * Math.sqrt(3)), cnec.getLowerBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE); // - 381 MW

        // bounds on RIGHT side (same values are expected as nominal voltage is the same on two sides)
        assertFalse(cnec.getUpperBound(RIGHT, Unit.MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(RIGHT, Unit.AMPERE).isPresent());
        assertEquals(-1000. * 220 / 380, cnec.getLowerBound(RIGHT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE); // - 579 A
        assertEquals(-1000. * (0.22 * Math.sqrt(3)), cnec.getLowerBound(RIGHT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE); // - 381 MW

        // margin
        assertEquals(1300., cnec.computeMargin(300, LEFT, Unit.AMPERE), DOUBLE_TOLERANCE); // bound: -1000 A
        assertEquals(-221., cnec.computeMargin(-800, RIGHT, Unit.AMPERE), DOUBLE_TOLERANCE); // bound: -579 A
        assertEquals(-319., cnec.computeMargin(-700, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE); // bound: -381 MW
        assertEquals(1381., cnec.computeMargin(1000, RIGHT, Unit.MEGAWATT), DOUBLE_TOLERANCE); // bound: -381 MW
    }

    @Test
    public void testTransformerWithOneMaxThresholdOnRightInA() {

        FlowCnec cnec = initPreventiveCnecAdder()
            .withNominalVoltage(220., LEFT)
            .withNominalVoltage(380., RIGHT)
            .newThreshold().withUnit(Unit.AMPERE).withMax(500.).withRule(BranchThresholdRule.ON_RIGHT_SIDE).add()
            .add();

        // bounds on LEFT side
        assertEquals(500. * 380 / 220, cnec.getUpperBound(LEFT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE); // 864 A
        assertEquals(500. * (0.38 * Math.sqrt(3)), cnec.getUpperBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE); // 329 MW
        assertFalse(cnec.getLowerBound(LEFT, Unit.MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(LEFT, Unit.AMPERE).isPresent());

        // bounds on RIGHT side (same values are expected as nominal voltage is the same on two sides)
        assertEquals(500., cnec.getUpperBound(RIGHT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE); // 500 A
        assertEquals(500. * (0.38 * Math.sqrt(3)), cnec.getUpperBound(RIGHT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE); // 329 MW
        assertFalse(cnec.getLowerBound(RIGHT, Unit.MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(RIGHT, Unit.AMPERE).isPresent());

        // margin
        assertEquals(64., cnec.computeMargin(800, LEFT, Unit.AMPERE), DOUBLE_TOLERANCE); // bound: 864 A
        assertEquals(-100., cnec.computeMargin(600, RIGHT, Unit.AMPERE), DOUBLE_TOLERANCE); // bound: 500 A
        assertEquals(29., cnec.computeMargin(300, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE); // bound: 329 MW
        assertEquals(-71., cnec.computeMargin(400, RIGHT, Unit.MEGAWATT), DOUBLE_TOLERANCE); // bound: 329 MW
    }

    @Test
    public void testTransformerWithOneMinThresholdOnLeftInPercentImax() {

        FlowCnec cnec = initPreventiveCnecAdder()
            .withNominalVoltage(220., LEFT)
            .withNominalVoltage(380., RIGHT)
            .withIMax(2000.)
            .newThreshold().withUnit(Unit.PERCENT_IMAX).withMin(-1.).withRule(BranchThresholdRule.ON_LEFT_SIDE).add()
            .add();

        // bounds on LEFT side
        assertFalse(cnec.getUpperBound(LEFT, Unit.MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(LEFT, Unit.AMPERE).isPresent());
        assertEquals(-2000., cnec.getLowerBound(LEFT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-2000. * (0.22 * Math.sqrt(3)), cnec.getLowerBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE); // - 762 MW

        // bounds on RIGHT side (same values are expected as nominal voltage is the same on two sides)
        assertFalse(cnec.getUpperBound(RIGHT, Unit.MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(RIGHT, Unit.AMPERE).isPresent());
        assertEquals(-2000. * 220 / 380, cnec.getLowerBound(RIGHT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE); // - 1158 A
        assertEquals(-2000. * (0.22 * Math.sqrt(3)), cnec.getLowerBound(RIGHT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE); // - 762 MW

        // margin
        assertEquals(2300., cnec.computeMargin(300, LEFT, Unit.AMPERE), DOUBLE_TOLERANCE); // bound: -2000 A
        assertEquals(358., cnec.computeMargin(-800, RIGHT, Unit.AMPERE), DOUBLE_TOLERANCE); // bound: -1158 A
        assertEquals(62., cnec.computeMargin(-700, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE); // bound: -762 MW
        assertEquals(-238., cnec.computeMargin(-1000, RIGHT, Unit.MEGAWATT), DOUBLE_TOLERANCE); // bound: -762 MW
    }

    @Test
    public void testTransformerWithOneMaxThresholdOnRightInPercentImax() {

        FlowCnec cnec = initPreventiveCnecAdder()
            .withNominalVoltage(220., LEFT)
            .withNominalVoltage(380., RIGHT)
            .withIMax(0., LEFT) // shouldn't be used as threshold is defined on right side
            .withIMax(2000., RIGHT)
            .newThreshold().withUnit(Unit.PERCENT_IMAX).withMax(0.25).withRule(BranchThresholdRule.ON_RIGHT_SIDE).add()
            .add();

        // bounds on LEFT side
        assertEquals(500. * 380 / 220, cnec.getUpperBound(LEFT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE); // 864 A
        assertEquals(500. * (0.38 * Math.sqrt(3)), cnec.getUpperBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE); // 329 MW
        assertFalse(cnec.getLowerBound(LEFT, Unit.MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(LEFT, Unit.AMPERE).isPresent());

        // bounds on RIGHT side (same values are expected as nominal voltage is the same on two sides)
        assertEquals(500., cnec.getUpperBound(RIGHT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE); // 500 A
        assertEquals(500. * (0.38 * Math.sqrt(3)), cnec.getUpperBound(RIGHT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE); // 329 MW
        assertFalse(cnec.getLowerBound(RIGHT, Unit.MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(RIGHT, Unit.AMPERE).isPresent());

        // margin
        assertEquals(64., cnec.computeMargin(800, LEFT, Unit.AMPERE), DOUBLE_TOLERANCE); // bound: 864 A
        assertEquals(-100., cnec.computeMargin(600, RIGHT, Unit.AMPERE), DOUBLE_TOLERANCE); // bound: 500 A
        assertEquals(29., cnec.computeMargin(300, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE); // bound: 329 MW
        assertEquals(-71., cnec.computeMargin(400, RIGHT, Unit.MEGAWATT), DOUBLE_TOLERANCE); // bound: 329 MW
    }

    // Tests on concurrency between thresholds

    @Test
    public void testBranchWithSeveralThresholdsWithLimitingOnLeftOrRightSide() {

        FlowCnec cnec = initPreventiveCnecAdder()
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(100.).withRule(BranchThresholdRule.ON_LEFT_SIDE).add()
            .newThreshold().withUnit(Unit.MEGAWATT).withMin(-200.).withRule(BranchThresholdRule.ON_LEFT_SIDE).add()
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withRule(BranchThresholdRule.ON_RIGHT_SIDE).add()
            .newThreshold().withUnit(Unit.MEGAWATT).withMin(-300.).withRule(BranchThresholdRule.ON_RIGHT_SIDE).add()
            .add();

        assertEquals(100., cnec.getUpperBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-200., cnec.getLowerBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-200., cnec.computeMargin(300, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(0., cnec.computeMargin(-200, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testBranchWithSeveralThresholdsWithBoth() {

        FlowCnec cnec = initPreventiveCnecAdder()
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(100.).withRule(BranchThresholdRule.ON_LEFT_SIDE).add()
            .newThreshold().withUnit(Unit.MEGAWATT).withMin(-200.).withRule(BranchThresholdRule.ON_LEFT_SIDE).add()
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withRule(BranchThresholdRule.ON_RIGHT_SIDE).add()
            .newThreshold().withUnit(Unit.MEGAWATT).withMin(-300.).withRule(BranchThresholdRule.ON_RIGHT_SIDE).add()
            .newThreshold().withUnit(Unit.MEGAWATT).withMin(-50.).withMax(150.).withRule(BranchThresholdRule.ON_RIGHT_SIDE).add()
            .add();

        assertEquals(100., cnec.getUpperBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-50., cnec.getLowerBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-200., cnec.computeMargin(300, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-150., cnec.computeMargin(-200, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testComputeMarginOnTransformerWithSeveralThresholdsInAmps() {

        FlowCnec cnec = initPreventiveCnecAdder()
            .withNominalVoltage(220., LEFT)
            .withNominalVoltage(380., RIGHT)
            .newThreshold().withUnit(Unit.AMPERE).withMax(100.).withRule(BranchThresholdRule.ON_LEFT_SIDE).add()
            .newThreshold().withUnit(Unit.AMPERE).withMin(-70.).withRule(BranchThresholdRule.ON_LEFT_SIDE).add()
            .newThreshold().withUnit(Unit.AMPERE).withMin(-50.).withMax(50.).withRule(BranchThresholdRule.ON_RIGHT_SIDE).add()
            .add();

        assertEquals(86, cnec.getUpperBound(LEFT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-70, cnec.getLowerBound(LEFT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-14, cnec.computeMargin(100, LEFT, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-30, cnec.computeMargin(-100, LEFT, Unit.AMPERE), DOUBLE_TOLERANCE);
    }

    @Test
    public void unboundedCnecInOppositeDirection() {

        FlowCnec cnec = initPreventiveCnecAdder()
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withRule(BranchThresholdRule.ON_LEFT_SIDE).add()
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(200.).withRule(BranchThresholdRule.ON_LEFT_SIDE).add()
            .add();

        assertEquals(200, cnec.getUpperBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(200, cnec.computeMargin(0., LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertFalse(cnec.getLowerBound(LEFT, Unit.MEGAWATT).isPresent());
    }

    @Test
    public void unboundedCnecInDirectDirection() {

        FlowCnec cnec = initPreventiveCnecAdder()
            .newThreshold().withUnit(Unit.MEGAWATT).withMin(-500.).withRule(BranchThresholdRule.ON_LEFT_SIDE).add()
            .newThreshold().withUnit(Unit.MEGAWATT).withMin(-200.).withRule(BranchThresholdRule.ON_LEFT_SIDE).add()
            .add();

        assertEquals(-200, cnec.getLowerBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(200, cnec.computeMargin(0., LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertFalse(cnec.getUpperBound(LEFT, Unit.MEGAWATT).isPresent());
    }

    @Test
    public void marginsWithNegativeAndPositiveLimits() {

        FlowCnec cnec = initPreventiveCnecAdder()
            .newThreshold().withUnit(Unit.MEGAWATT).withMin(-200.).withMax(500.).withRule(BranchThresholdRule.ON_LEFT_SIDE).add()
            .add();

        assertEquals(-100, cnec.computeMargin(-300, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(200, cnec.computeMargin(0, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(100, cnec.computeMargin(400, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-300, cnec.computeMargin(800, LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    // other

    @Test
    public void testEqualsAndHashCode() {
        FlowCnec cnec1 = initPreventiveCnecAdder().newThreshold().withUnit(Unit.MEGAWATT).withMax(1000.).withRule(BranchThresholdRule.ON_LEFT_SIDE).add().add();
        FlowCnec cnec2 = initPreventiveCnecAdder().withId("anotherId").newThreshold().withUnit(Unit.AMPERE).withMin(-1000.).withRule(BranchThresholdRule.ON_LEFT_SIDE).add().withNominalVoltage(220.).add();

        assertEquals(cnec1, cnec1);
        assertNotEquals(cnec1, cnec2);
        assertNotEquals(cnec1, null);
        assertNotEquals(cnec1, 1);

        assertEquals(cnec1.hashCode(), cnec1.hashCode());
        assertNotEquals(cnec1.hashCode(), cnec2.hashCode());
    }
}
