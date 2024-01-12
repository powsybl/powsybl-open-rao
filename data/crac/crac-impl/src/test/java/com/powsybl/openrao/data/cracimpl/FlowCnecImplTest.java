/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnecAdder;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracimpl.utils.NetworkImportsUtil;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static com.powsybl.openrao.commons.Unit.*;
import static com.powsybl.openrao.data.cracapi.cnec.Side.LEFT;
import static com.powsybl.openrao.data.cracapi.cnec.Side.RIGHT;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class FlowCnecImplTest {
    private static final double DOUBLE_TOLERANCE = 1; // high tolerance for conversion AMPERE <-> MEGAWATT
    private static final String PREVENTIVE_INSTANT_ID = "preventive";

    private Crac crac;

    @BeforeEach
    public void setUp() {
        crac = new CracImplFactory().create("cracId")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE);
    }

    private FlowCnecAdder initPreventiveCnecAdder() {
        return crac.newFlowCnec().withId("line-cnec").withName("line-cnec-name").withNetworkElement("anyNetworkElement").withOperator("FR").withInstant(PREVENTIVE_INSTANT_ID).withOptimized(true);
    }

    @Test
    void testGetLocation1() {

        Network network = NetworkImportsUtil.import12NodesNetwork();

        FlowCnec cnec1 = crac.newFlowCnec().withId("cnec-1-id").withNetworkElement("BBE1AA1  BBE2AA1  1").withInstant(PREVENTIVE_INSTANT_ID).newThreshold().withUnit(MEGAWATT).withMax(1000.).withSide(Side.LEFT).add().add();

        FlowCnec cnec2 = crac.newFlowCnec().withId("cnec-2-id").withNetworkElement("DDE2AA1  NNL3AA1  1").withInstant(PREVENTIVE_INSTANT_ID).newThreshold().withUnit(MEGAWATT).withMax(1000.).withSide(Side.LEFT).add().add();

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
    void testBranchWithOneMaxThresholdOnLeftInMW() {

        FlowCnec cnec = initPreventiveCnecAdder().withNominalVoltage(380.).newThreshold().withUnit(MEGAWATT).withMax(500.).withSide(Side.LEFT).add().add();

        // bounds on LEFT side
        assertEquals(500., cnec.getUpperBound(LEFT, MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(500. / (0.38 * Math.sqrt(3)), cnec.getUpperBound(LEFT, AMPERE).orElseThrow(), DOUBLE_TOLERANCE); // = 760 A
        assertFalse(cnec.getLowerBound(LEFT, MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(LEFT, AMPERE).isPresent());

        // bounds on RIGHT side
        assertFalse(cnec.getUpperBound(RIGHT, MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(RIGHT, AMPERE).isPresent());
        assertFalse(cnec.getLowerBound(RIGHT, MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(RIGHT, AMPERE).isPresent());

        // margin
        assertEquals(200., cnec.computeMargin(300, LEFT, MEGAWATT), DOUBLE_TOLERANCE); // bound: 500 MW
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(300, RIGHT, MEGAWATT), DOUBLE_TOLERANCE); // bound: 500 MW
        assertEquals(460., cnec.computeMargin(300, LEFT, AMPERE), DOUBLE_TOLERANCE); // bound: 760 A
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(-300, RIGHT, AMPERE), DOUBLE_TOLERANCE); // bound: 760 A
    }

    @Test
    void testBranchWithOneMinThresholdOnRightInMW() {

        FlowCnec cnec = initPreventiveCnecAdder().withNominalVoltage(380.).newThreshold().withUnit(MEGAWATT).withMin(-500.).withSide(Side.RIGHT).add().add();

        // bounds on LEFT side
        assertFalse(cnec.getUpperBound(LEFT, MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(LEFT, AMPERE).isPresent());
        assertFalse(cnec.getLowerBound(LEFT, MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(LEFT, AMPERE).isPresent());

        // bounds on RIGHT side
        assertFalse(cnec.getUpperBound(RIGHT, MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(RIGHT, AMPERE).isPresent());
        assertEquals(-500., cnec.getLowerBound(RIGHT, MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-500. / (0.38 * Math.sqrt(3)), cnec.getLowerBound(RIGHT, AMPERE).orElseThrow(), DOUBLE_TOLERANCE); // = -760 A

        // margin
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(300, LEFT, MEGAWATT), DOUBLE_TOLERANCE); // bound: -500 MW
        assertEquals(800., cnec.computeMargin(300, RIGHT, MEGAWATT), DOUBLE_TOLERANCE); // bound: -500 MW
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(300, LEFT, AMPERE), DOUBLE_TOLERANCE); // bound: -760 A
        assertEquals(-240., cnec.computeMargin(-1000, RIGHT, AMPERE), DOUBLE_TOLERANCE); // bound: -760 A
    }

    @Test
    void testBranchWithOneMinThresholdOnLeftInAmpere() {

        FlowCnec cnec = initPreventiveCnecAdder().withNominalVoltage(380.).newThreshold().withUnit(AMPERE).withMin(-450.).withSide(Side.LEFT).add().add();

        // bounds on LEFT side
        assertFalse(cnec.getUpperBound(LEFT, MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(LEFT, AMPERE).isPresent());
        assertEquals(-450., cnec.getLowerBound(LEFT, AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-450. * (0.38 * Math.sqrt(3)), cnec.getLowerBound(LEFT, MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE); // -296 MW

        // bounds on RIGHT side
        assertFalse(cnec.getUpperBound(RIGHT, MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(RIGHT, AMPERE).isPresent());
        assertFalse(cnec.getLowerBound(RIGHT, MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(RIGHT, AMPERE).isPresent());

        // margin
        assertEquals(750., cnec.computeMargin(300, LEFT, AMPERE), DOUBLE_TOLERANCE); // bound: -450 A
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(300, RIGHT, AMPERE), DOUBLE_TOLERANCE); // bound: -450 A
        assertEquals(596., cnec.computeMargin(300, LEFT, MEGAWATT), DOUBLE_TOLERANCE); // bound: -296 MW
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(-300, RIGHT, MEGAWATT), DOUBLE_TOLERANCE); // bound: -296 MW
    }

    @Test
    void testBranchWithOneMaxThresholdOnRightInAmpere() {

        FlowCnec cnec = initPreventiveCnecAdder().withNominalVoltage(220.).newThreshold().withUnit(AMPERE).withMax(110.).withSide(Side.RIGHT).add().add();

        // bounds on LEFT side
        assertFalse(cnec.getUpperBound(LEFT, MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(LEFT, AMPERE).isPresent());
        assertFalse(cnec.getLowerBound(LEFT, MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(LEFT, AMPERE).isPresent());

        // bounds on RIGHT side
        assertEquals(110., cnec.getUpperBound(RIGHT, AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(110. * (0.22 * Math.sqrt(3)), cnec.getUpperBound(RIGHT, MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE); // 42 MW
        assertFalse(cnec.getLowerBound(RIGHT, MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(RIGHT, AMPERE).isPresent());

        // margin
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(300, LEFT, AMPERE), DOUBLE_TOLERANCE); // bound: 110 A
        assertEquals(-190., cnec.computeMargin(300, RIGHT, AMPERE), DOUBLE_TOLERANCE); // bound: 110 A
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(300, LEFT, MEGAWATT), DOUBLE_TOLERANCE); // bound: 42 MW
        assertEquals(342., cnec.computeMargin(-300, RIGHT, MEGAWATT), DOUBLE_TOLERANCE); // bound: 42 MW
    }

    @Test
    void testBranchWithOneMaxThresholdOnLeftInPercentImax() {

        FlowCnec cnec = initPreventiveCnecAdder().withNominalVoltage(380.).withIMax(1000.).newThreshold().withUnit(PERCENT_IMAX).withMax(1.1).withSide(Side.LEFT).add() // 1.1 = 110 %
            .add();

        // bounds on LEFT side
        assertEquals(1100., cnec.getUpperBound(LEFT, AMPERE).orElseThrow(), DOUBLE_TOLERANCE); // 1100 A
        assertEquals(1100. * (0.38 * Math.sqrt(3)), cnec.getUpperBound(LEFT, MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE); // 724 MW
        assertFalse(cnec.getLowerBound(LEFT, MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(LEFT, AMPERE).isPresent());

        // bounds on RIGHT side
        assertFalse(cnec.getUpperBound(RIGHT, MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(RIGHT, AMPERE).isPresent());
        assertFalse(cnec.getLowerBound(RIGHT, MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(RIGHT, AMPERE).isPresent());

        // margin
        assertEquals(-100, cnec.computeMargin(1200, LEFT, AMPERE), DOUBLE_TOLERANCE); // bound: 1100 A
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(1200, RIGHT, AMPERE), DOUBLE_TOLERANCE); // bound: 1100 A
        assertEquals(-26., cnec.computeMargin(750, LEFT, MEGAWATT), DOUBLE_TOLERANCE); // bound: 724 MW
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(100, RIGHT, MEGAWATT), DOUBLE_TOLERANCE); // bound: 724 MW
    }

    @Test
    void testBranchWithOneMinThresholdOnRightInPercentImax() {

        FlowCnec cnec = initPreventiveCnecAdder().withNominalVoltage(220.).withIMax(1000., RIGHT).withIMax(0., LEFT) // should not be considered as the threshold is on the right side
            .newThreshold().withUnit(PERCENT_IMAX).withMin(-0.9).withSide(Side.RIGHT).add() // 0.9 = 90 %
            .add();

        assertEquals(Set.of(RIGHT), cnec.getMonitoredSides());

        // bounds on LEFT side
        assertFalse(cnec.getUpperBound(LEFT, MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(LEFT, AMPERE).isPresent());
        assertFalse(cnec.getLowerBound(LEFT, MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(LEFT, AMPERE).isPresent());

        // bounds on RIGHT side
        assertFalse(cnec.getUpperBound(RIGHT, MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(RIGHT, AMPERE).isPresent());
        assertEquals(-900., cnec.getLowerBound(RIGHT, AMPERE).orElseThrow(), DOUBLE_TOLERANCE); // -900 A
        assertEquals(-900. * (0.22 * Math.sqrt(3)), cnec.getLowerBound(RIGHT, MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE); // -343 MW

        // margin
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(1200, LEFT, AMPERE), DOUBLE_TOLERANCE); // bound: -900 A
        assertEquals(2100., cnec.computeMargin(1200, RIGHT, AMPERE), DOUBLE_TOLERANCE); // bound: -900 A
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(-500, LEFT, MEGAWATT), DOUBLE_TOLERANCE); // bound: -343 MW
        assertEquals(443., cnec.computeMargin(100, RIGHT, MEGAWATT), DOUBLE_TOLERANCE); // bound: -343 MW
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
    void testTransformerWithOneMaxThresholdOnLeftInMW() {

        FlowCnec cnec = initPreventiveCnecAdder().withNominalVoltage(220., LEFT).withNominalVoltage(380., RIGHT).newThreshold().withUnit(MEGAWATT).withMax(500.).withSide(Side.LEFT).add().add();

        // bounds on LEFT side
        assertEquals(500., cnec.getUpperBound(LEFT, MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(500. / (0.22 * Math.sqrt(3)), cnec.getUpperBound(LEFT, AMPERE).orElseThrow(), DOUBLE_TOLERANCE); // 1312 A
        assertFalse(cnec.getLowerBound(LEFT, MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(LEFT, AMPERE).isPresent());

        // bounds on RIGHT side
        assertFalse(cnec.getUpperBound(RIGHT, MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(RIGHT, AMPERE).isPresent());
        assertFalse(cnec.getLowerBound(RIGHT, MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(RIGHT, AMPERE).isPresent());

        // margins
        assertEquals(400., cnec.computeMargin(100, LEFT, MEGAWATT), DOUBLE_TOLERANCE); // bound: 500 MW
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(1000, RIGHT, MEGAWATT), DOUBLE_TOLERANCE); // bound: 500 MW
        assertEquals(1512., cnec.computeMargin(-200, LEFT, AMPERE), DOUBLE_TOLERANCE); // bound: 1312 A
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(2000, RIGHT, AMPERE), DOUBLE_TOLERANCE); // bound: 759 A
    }

    @Test
    void testTransformerWithOneMinThresholdOnRightInMW() {

        FlowCnec cnec = initPreventiveCnecAdder().withNominalVoltage(220., LEFT).withNominalVoltage(380., RIGHT).newThreshold().withUnit(MEGAWATT).withMin(-600.).withSide(Side.RIGHT).add().add();

        // bounds on LEFT side
        assertFalse(cnec.getUpperBound(LEFT, MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(LEFT, AMPERE).isPresent());
        assertFalse(cnec.getLowerBound(LEFT, MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(LEFT, AMPERE).isPresent());

        // bounds on RIGHT side
        assertFalse(cnec.getUpperBound(RIGHT, MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(RIGHT, AMPERE).isPresent());
        assertEquals(-600., cnec.getLowerBound(RIGHT, MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-600. / (0.38 * Math.sqrt(3)), cnec.getLowerBound(RIGHT, AMPERE).orElseThrow(), DOUBLE_TOLERANCE); // - 912 A

        // margin
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(500, LEFT, MEGAWATT), DOUBLE_TOLERANCE); // bound: -600 MW
        assertEquals(-400., cnec.computeMargin(-1000, RIGHT, MEGAWATT), DOUBLE_TOLERANCE); // bound: -600 MW
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(-500, LEFT, AMPERE), DOUBLE_TOLERANCE); // bound: -1575 A
        assertEquals(1012., cnec.computeMargin(100, RIGHT, AMPERE), DOUBLE_TOLERANCE); // bound: -912 A
    }

    @Test
    void testTransformerWithOneMinThresholdOnLeftInA() {

        FlowCnec cnec = initPreventiveCnecAdder().withNominalVoltage(220., LEFT).withNominalVoltage(380., RIGHT).newThreshold().withUnit(AMPERE).withMin(-1000.).withSide(Side.LEFT).add().add();

        // bounds on LEFT side
        assertFalse(cnec.getUpperBound(LEFT, MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(LEFT, AMPERE).isPresent());
        assertEquals(-1000., cnec.getLowerBound(LEFT, AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-1000. * (0.22 * Math.sqrt(3)), cnec.getLowerBound(LEFT, MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE); // - 381 MW

        // bounds on RIGHT side
        assertFalse(cnec.getUpperBound(RIGHT, MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(RIGHT, AMPERE).isPresent());
        assertFalse(cnec.getLowerBound(RIGHT, MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(RIGHT, AMPERE).isPresent());

        // margin
        assertEquals(1300., cnec.computeMargin(300, LEFT, AMPERE), DOUBLE_TOLERANCE); // bound: -1000 A
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(-800, RIGHT, AMPERE), DOUBLE_TOLERANCE); // bound: -579 A
        assertEquals(-319., cnec.computeMargin(-700, LEFT, MEGAWATT), DOUBLE_TOLERANCE); // bound: -381 MW
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(1000, RIGHT, MEGAWATT), DOUBLE_TOLERANCE); // bound: -381 MW
    }

    @Test
    void testTransformerWithOneMaxThresholdOnRightInA() {

        FlowCnec cnec = initPreventiveCnecAdder().withNominalVoltage(220., LEFT).withNominalVoltage(380., RIGHT).newThreshold().withUnit(AMPERE).withMax(500.).withSide(Side.RIGHT).add().add();

        // bounds on LEFT side
        assertFalse(cnec.getUpperBound(LEFT, MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(LEFT, AMPERE).isPresent());
        assertFalse(cnec.getLowerBound(LEFT, MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(LEFT, AMPERE).isPresent());

        // bounds on RIGHT side
        assertEquals(500., cnec.getUpperBound(RIGHT, AMPERE).orElseThrow(), DOUBLE_TOLERANCE); // 500 A
        assertEquals(500. * (0.38 * Math.sqrt(3)), cnec.getUpperBound(RIGHT, MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE); // 329 MW
        assertFalse(cnec.getLowerBound(RIGHT, MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(RIGHT, AMPERE).isPresent());

        // margin
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(800, LEFT, AMPERE), DOUBLE_TOLERANCE); // bound: 864 A
        assertEquals(-100., cnec.computeMargin(600, RIGHT, AMPERE), DOUBLE_TOLERANCE); // bound: 500 A
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(300, LEFT, MEGAWATT), DOUBLE_TOLERANCE); // bound: 329 MW
        assertEquals(-71., cnec.computeMargin(400, RIGHT, MEGAWATT), DOUBLE_TOLERANCE); // bound: 329 MW
    }

    @Test
    void testTransformerWithOneMinThresholdOnLeftInPercentImax() {

        FlowCnec cnec = initPreventiveCnecAdder().withNominalVoltage(220., LEFT).withNominalVoltage(380., RIGHT).withIMax(2000.).newThreshold().withUnit(PERCENT_IMAX).withMin(-1.).withSide(Side.LEFT).add().add();

        // bounds on LEFT side
        assertFalse(cnec.getUpperBound(LEFT, MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(LEFT, AMPERE).isPresent());
        assertEquals(-2000., cnec.getLowerBound(LEFT, AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-2000. * (0.22 * Math.sqrt(3)), cnec.getLowerBound(LEFT, MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE); // - 762 MW

        // bounds on RIGHT side
        assertFalse(cnec.getUpperBound(RIGHT, MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(RIGHT, AMPERE).isPresent());
        assertFalse(cnec.getLowerBound(RIGHT, MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(RIGHT, AMPERE).isPresent());

        // margin
        assertEquals(2300., cnec.computeMargin(300, LEFT, AMPERE), DOUBLE_TOLERANCE); // bound: -2000 A
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(-800, RIGHT, AMPERE), DOUBLE_TOLERANCE); // bound: -1158 A
        assertEquals(62., cnec.computeMargin(-700, LEFT, MEGAWATT), DOUBLE_TOLERANCE); // bound: -762 MW
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(-1000, RIGHT, MEGAWATT), DOUBLE_TOLERANCE); // bound: -762 MW
    }

    @Test
    void testTransformerWithOneMaxThresholdOnRightInPercentImax() {

        FlowCnec cnec = initPreventiveCnecAdder().withNominalVoltage(220., LEFT).withNominalVoltage(380., RIGHT).withIMax(0., LEFT) // shouldn't be used as threshold is defined on right side
            .withIMax(2000., RIGHT).newThreshold().withUnit(PERCENT_IMAX).withMax(0.25).withSide(Side.RIGHT).add().add();

        // bounds on LEFT side
        assertFalse(cnec.getUpperBound(LEFT, MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(LEFT, AMPERE).isPresent());
        assertFalse(cnec.getLowerBound(LEFT, MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(LEFT, AMPERE).isPresent());

        // bounds on RIGHT side
        assertEquals(500., cnec.getUpperBound(RIGHT, AMPERE).orElseThrow(), DOUBLE_TOLERANCE); // 500 A
        assertEquals(500. * (0.38 * Math.sqrt(3)), cnec.getUpperBound(RIGHT, MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE); // 329 MW
        assertFalse(cnec.getLowerBound(RIGHT, MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(RIGHT, AMPERE).isPresent());

        // margin
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(800, LEFT, AMPERE), DOUBLE_TOLERANCE); // bound: 864 A
        assertEquals(-100., cnec.computeMargin(600, RIGHT, AMPERE), DOUBLE_TOLERANCE); // bound: 500 A
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(300, LEFT, MEGAWATT), DOUBLE_TOLERANCE); // bound: 329 MW
        assertEquals(-71., cnec.computeMargin(400, RIGHT, MEGAWATT), DOUBLE_TOLERANCE); // bound: 329 MW
    }

    // Tests on concurrency between thresholds

    @Test
    void testBranchWithSeveralThresholdsWithLimitingOnLeftOrRightSide() {

        FlowCnec cnec = initPreventiveCnecAdder().newThreshold().withUnit(MEGAWATT).withMax(100.).withSide(Side.LEFT).add().newThreshold().withUnit(MEGAWATT).withMin(-200.).withSide(Side.LEFT).add().newThreshold().withUnit(MEGAWATT).withMax(500.).withSide(Side.RIGHT).add().newThreshold().withUnit(MEGAWATT).withMin(-300.).withSide(Side.RIGHT).add().add();

        assertEquals(100., cnec.getUpperBound(LEFT, MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-200., cnec.getLowerBound(LEFT, MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-200., cnec.computeMargin(300, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(0., cnec.computeMargin(-200, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    void testBranchWithSeveralThresholdsWithBoth() {

        FlowCnec cnec = initPreventiveCnecAdder().newThreshold().withUnit(MEGAWATT).withMax(100.).withSide(Side.LEFT).add().newThreshold().withUnit(MEGAWATT).withMin(-200.).withSide(Side.LEFT).add().newThreshold().withUnit(MEGAWATT).withMax(500.).withSide(Side.RIGHT).add().newThreshold().withUnit(MEGAWATT).withMin(-300.).withSide(Side.RIGHT).add().newThreshold().withUnit(MEGAWATT).withMin(-50.).withMax(150.).withSide(Side.RIGHT).add().add();

        assertEquals(100., cnec.getUpperBound(LEFT, MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-200., cnec.getLowerBound(LEFT, MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-200., cnec.computeMargin(300, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(0., cnec.computeMargin(-200, LEFT, MEGAWATT), DOUBLE_TOLERANCE);

        assertEquals(150., cnec.getUpperBound(RIGHT, MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-50., cnec.getLowerBound(RIGHT, MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-150, cnec.computeMargin(300, RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-150., cnec.computeMargin(-200, RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    void testComputeMarginOnTransformerWithSeveralThresholdsInAmps() {

        FlowCnec cnec = initPreventiveCnecAdder().withNominalVoltage(220., LEFT).withNominalVoltage(380., RIGHT).newThreshold().withUnit(AMPERE).withMax(100.).withSide(Side.LEFT).add().newThreshold().withUnit(AMPERE).withMin(-70.).withSide(Side.LEFT).add().newThreshold().withUnit(AMPERE).withMin(-50.).withMax(50.).withSide(Side.RIGHT).add().add();

        assertEquals(100, cnec.getUpperBound(LEFT, AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-70, cnec.getLowerBound(LEFT, AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(0., cnec.computeMargin(100, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-30, cnec.computeMargin(-100, LEFT, AMPERE), DOUBLE_TOLERANCE);
    }

    @Test
    void unboundedCnecInOppositeDirection() {

        FlowCnec cnec = initPreventiveCnecAdder().newThreshold().withUnit(MEGAWATT).withMax(500.).withSide(Side.LEFT).add().newThreshold().withUnit(MEGAWATT).withMax(200.).withSide(Side.LEFT).add().add();

        assertEquals(200, cnec.getUpperBound(LEFT, MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(200, cnec.computeMargin(0., LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertFalse(cnec.getLowerBound(LEFT, MEGAWATT).isPresent());
    }

    @Test
    void unboundedCnecInDirectDirection() {

        FlowCnec cnec = initPreventiveCnecAdder().newThreshold().withUnit(MEGAWATT).withMin(-500.).withSide(Side.LEFT).add().newThreshold().withUnit(MEGAWATT).withMin(-200.).withSide(Side.LEFT).add().add();

        assertEquals(-200, cnec.getLowerBound(LEFT, MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(200, cnec.computeMargin(0., LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertFalse(cnec.getUpperBound(LEFT, MEGAWATT).isPresent());
    }

    @Test
    void marginsWithNegativeAndPositiveLimits() {

        FlowCnec cnec = initPreventiveCnecAdder().newThreshold().withUnit(MEGAWATT).withMin(-200.).withMax(500.).withSide(Side.LEFT).add().add();

        assertEquals(-100, cnec.computeMargin(-300, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(200, cnec.computeMargin(0, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(100, cnec.computeMargin(400, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-300, cnec.computeMargin(800, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
    }

    // other

    @Test
    void testEqualsAndHashCode() {
        FlowCnec cnec1 = initPreventiveCnecAdder().newThreshold().withUnit(MEGAWATT).withMax(1000.).withSide(Side.LEFT).add().add();
        FlowCnec cnec2 = initPreventiveCnecAdder().withId("anotherId").newThreshold().withUnit(AMPERE).withMin(-1000.).withSide(Side.LEFT).add().withNominalVoltage(220.).add();

        assertEquals(cnec1, cnec1);
        assertNotEquals(cnec1, cnec2);
        assertNotNull(cnec1);
        assertNotEquals(1, cnec1);

        assertEquals(cnec1.hashCode(), cnec1.hashCode());
        assertNotEquals(cnec1.hashCode(), cnec2.hashCode());
    }

    @Test
    void testIsConnected() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        NetworkImportsUtil.addDanglingLine(network);

        // Branch
        FlowCnec cnec1 = crac.newFlowCnec().withId("cnec-1-id").withNetworkElement("BBE1AA1  BBE2AA1  1").withInstant(PREVENTIVE_INSTANT_ID).newThreshold().withUnit(MEGAWATT).withMax(1000.).withSide(LEFT).add().add();
        assertTrue(cnec1.isConnected(network));

        network.getBranch("BBE1AA1  BBE2AA1  1").getTerminal1().disconnect();
        assertFalse(cnec1.isConnected(network));

        network.getBranch("BBE1AA1  BBE2AA1  1").getTerminal1().connect();
        network.getBranch("BBE1AA1  BBE2AA1  1").getTerminal2().disconnect();
        assertFalse(cnec1.isConnected(network));

        // DanglingLine
        FlowCnec cnec2 = crac.newFlowCnec().withId("cnec-2-id").withNetworkElement("DL1").withInstant(PREVENTIVE_INSTANT_ID).newThreshold().withUnit(MEGAWATT).withMax(1000.).withSide(LEFT).add().add();
        assertTrue(cnec2.isConnected(network));

        network.getDanglingLine("DL1").getTerminal().disconnect();
        assertFalse(cnec2.isConnected(network));

        // Generator
        FlowCnec cnec3 = crac.newFlowCnec().withId("cnec-3-id").withNetworkElement("BBE2AA1 _generator").withInstant(PREVENTIVE_INSTANT_ID).newThreshold().withUnit(MEGAWATT).withMax(1000.).withSide(LEFT).add().add();
        assertTrue(cnec3.isConnected(network));

        network.getGenerator("BBE2AA1 _generator").getTerminal().disconnect();
        assertFalse(cnec3.isConnected(network));
    }
}
