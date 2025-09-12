/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.crac.impl;

import com.powsybl.iidm.network.*;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.impl.utils.NetworkImportsUtil;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnecAdder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

import static com.powsybl.openrao.commons.Unit.*;
import static com.powsybl.iidm.network.TwoSides.ONE;
import static com.powsybl.iidm.network.TwoSides.TWO;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class FlowCnecImplTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final double DOUBLE_TOLERANCE = 1; // high tolerance for conversion AMPERE <-> MEGAWATT

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

        FlowCnec cnec1 = crac.newFlowCnec().withId("cnec-1-id").withNetworkElement("BBE1AA1  BBE2AA1  1").withInstant(PREVENTIVE_INSTANT_ID).newThreshold().withUnit(MEGAWATT).withMax(1000.).withSide(TwoSides.ONE).add().add();
        FlowCnec cnec2 = crac.newFlowCnec().withId("cnec-2-id").withNetworkElement("DDE2AA1  NNL3AA1  1").withInstant(PREVENTIVE_INSTANT_ID).newThreshold().withUnit(MEGAWATT).withMax(1000.).withSide(TwoSides.ONE).add().add();

        Set<Country> countries = cnec1.getLocation(network);
        assertEquals(Set.of(Country.BE), countries);

        countries = cnec2.getLocation(network);
        assertEquals(Set.of(Country.DE, Country.NL), countries);
    }

    @Test
    void testComputeValue() {
        Network network = Mockito.mock(Network.class);
        Branch branch1 = Mockito.mock(Branch.class);
        Terminal terminal11 = Mockito.mock(Terminal.class);
        Terminal terminal12 = Mockito.mock(Terminal.class);
        Terminal terminal21 = Mockito.mock(Terminal.class);

        Mockito.when(network.getBranch("BBE1AA1  BBE2AA1  1")).thenReturn(branch1);
        Mockito.when(terminal11.getP()).thenReturn(300.);
        Mockito.when(terminal12.getP()).thenReturn(1100.);

        Mockito.when(branch1.getTerminal(ONE)).thenReturn(terminal11);
        Mockito.when(branch1.getTerminal(TWO)).thenReturn(terminal12);

        Branch branch2 = Mockito.mock(Branch.class);
        Mockito.when(network.getBranch("DDE2AA1  NNL3AA1  1")).thenReturn(branch2);
        Mockito.when(terminal21.getP()).thenReturn(100.);
        Mockito.when(branch2.getTerminal(ONE)).thenReturn(terminal21);

        FlowCnec cnecWithTwoSides = crac.newFlowCnec().withId("cnec-1-id").withNetworkElement("BBE1AA1  BBE2AA1  1").withInstant(PREVENTIVE_INSTANT_ID)
            .newThreshold().withUnit(MEGAWATT).withMin(500.).withMax(1000.).withSide(TwoSides.ONE).add()
            .newThreshold().withUnit(MEGAWATT).withMin(2000.).withMax(3000.).withSide(TwoSides.TWO).add()
            .add();
        assertThrows(OpenRaoException.class, () -> cnecWithTwoSides.computeValue(network, KILOVOLT));

        assertEquals(300., ((FlowCnecValue) cnecWithTwoSides.computeValue(network, MEGAWATT)).side1Value());
        assertEquals(1100., ((FlowCnecValue) cnecWithTwoSides.computeValue(network, MEGAWATT)).side2Value());

        FlowCnec cnecWithOneSide = crac.newFlowCnec().withId("cnec-2-id").withNetworkElement("DDE2AA1  NNL3AA1  1").withInstant(PREVENTIVE_INSTANT_ID).newThreshold().withUnit(MEGAWATT).withMax(1000.).withSide(TwoSides.ONE).add().add();

        assertEquals(100., ((FlowCnecValue) cnecWithOneSide.computeValue(network, MEGAWATT)).side1Value());
        assertEquals(Double.NaN, ((FlowCnecValue) cnecWithOneSide.computeValue(network, MEGAWATT)).side2Value());
    }

    @Test
    void testComputeValueAmpere() {
        Network network = Mockito.mock(Network.class);
        Branch branch3 = Mockito.mock(Branch.class);
        Terminal terminal31 = Mockito.mock(Terminal.class);
        Terminal terminal32 = Mockito.mock(Terminal.class);

        Mockito.when(network.getBranch("AAE2AA1  AAE3AA1  1")).thenReturn(branch3);
        Mockito.when(terminal31.getP()).thenReturn(-66.);
        Mockito.when(terminal31.getI()).thenReturn(55.);
        Mockito.when(terminal32.getP()).thenReturn(22.);
        Mockito.when(terminal32.getI()).thenReturn(Double.NaN);
        Mockito.when(branch3.getTerminal(ONE)).thenReturn(terminal31);
        Mockito.when(branch3.getTerminal(TWO)).thenReturn(terminal32);

        FlowCnec cnecA = crac.newFlowCnec().withId("cnec-A-id").withNetworkElement("AAE2AA1  AAE3AA1  1").withInstant(PREVENTIVE_INSTANT_ID)
            .withNominalVoltage(222.)
            .newThreshold().withUnit(AMPERE).withMin(5.).withMax(10.).withSide(TwoSides.ONE).add()
            .newThreshold().withUnit(AMPERE).withMin(20.).withMax(300.).withSide(TwoSides.TWO).add()
            .add();

        assertEquals(-55., ((FlowCnecValue) cnecA.computeValue(network, AMPERE)).side1Value());
        assertEquals(57.2, ((FlowCnecValue) cnecA.computeValue(network, AMPERE)).side2Value(), 0.1);
    }

    @Test
    void testComputeWorstMargin() {
        Network network = Mockito.mock(Network.class, Mockito.RETURNS_DEEP_STUBS);
        Branch branch1 = Mockito.mock(Branch.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(network.getBranch("BBE1AA1  BBE2AA1  1")).thenReturn(branch1);
        Mockito.when(branch1.getTerminal(ONE).getP()).thenReturn(300.);
        Mockito.when(branch1.getTerminal(TWO).getP()).thenReturn(1100.);

        Branch branch2 = Mockito.mock(Branch.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(network.getBranch("DDE2AA1  NNL3AA1  1")).thenReturn(branch2);
        Mockito.when(branch2.getTerminal(ONE).getP()).thenReturn(100.);

        FlowCnec cnecWithTwoSides = crac.newFlowCnec().withId("cnec-1-id").withNetworkElement("BBE1AA1  BBE2AA1  1").withInstant(PREVENTIVE_INSTANT_ID)
            .newThreshold().withUnit(MEGAWATT).withMin(500.).withMax(1000.).withSide(TwoSides.ONE).add()
            .newThreshold().withUnit(MEGAWATT).withMin(2000.).withMax(3000.).withSide(TwoSides.TWO).add()
            .add();
        assertThrows(OpenRaoException.class, () -> cnecWithTwoSides.computeMargin(network, KILOVOLT));
        assertEquals(-900., cnecWithTwoSides.computeMargin(network, MEGAWATT));

        FlowCnec cnecWithOneSide = crac.newFlowCnec().withId("cnec-2-id").withNetworkElement("DDE2AA1  NNL3AA1  1").withInstant(PREVENTIVE_INSTANT_ID).newThreshold().withUnit(MEGAWATT).withMax(1000.).withSide(TwoSides.ONE).add().add();
        assertEquals(900., cnecWithOneSide.computeMargin(network, MEGAWATT));
    }

    @Test
    void testComputeSecurityStatus() {
        Network network = Mockito.mock(Network.class, Mockito.RETURNS_DEEP_STUBS);
        Branch branch1 = Mockito.mock(Branch.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(network.getBranch("BBE1AA1  BBE2AA1  1")).thenReturn(branch1);
        Mockito.when(branch1.getTerminal(ONE).getP()).thenReturn(300.);
        Mockito.when(branch1.getTerminal(TWO).getP()).thenReturn(3100.);

        Branch branch2 = Mockito.mock(Branch.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(network.getBranch("DDE2AA1  NNL3AA1  1")).thenReturn(branch2);
        Mockito.when(branch2.getTerminal(ONE).getP()).thenReturn(100.);

        FlowCnec cnecWithTwoSides = crac.newFlowCnec().withId("cnec-1-id").withNetworkElement("BBE1AA1  BBE2AA1  1").withInstant(PREVENTIVE_INSTANT_ID)
            .newThreshold().withUnit(MEGAWATT).withMin(500.).withMax(1000.).withSide(TwoSides.ONE).add()
            .newThreshold().withUnit(MEGAWATT).withMin(2000.).withMax(3000.).withSide(TwoSides.TWO).add()
            .add();
        assertThrows(OpenRaoException.class, () -> cnecWithTwoSides.computeMargin(network, KILOVOLT));
        assertEquals(Cnec.SecurityStatus.HIGH_AND_LOW_CONSTRAINTS, cnecWithTwoSides.computeSecurityStatus(network, MEGAWATT));

        FlowCnec cnecWithOneSide = crac.newFlowCnec().withId("cnec-2-id").withNetworkElement("DDE2AA1  NNL3AA1  1").withInstant(PREVENTIVE_INSTANT_ID).newThreshold().withUnit(MEGAWATT).withMax(1000.).withSide(TwoSides.ONE).add().add();
        assertEquals(Cnec.SecurityStatus.SECURE, cnecWithOneSide.computeSecurityStatus(network, MEGAWATT));

        FlowCnec cnec3 = crac.newFlowCnec().withId("cnec-3-id").withNetworkElement("DDE2AA1  NNL3AA1  1").withInstant(PREVENTIVE_INSTANT_ID).newThreshold().withUnit(MEGAWATT).withMax(10.).withSide(TwoSides.ONE).add().add();
        assertEquals(Cnec.SecurityStatus.HIGH_CONSTRAINT, cnec3.computeSecurityStatus(network, MEGAWATT));

    }

    // test threshold on branches whose nominal voltage is the same on both side

    @Test
    void testBranchWithOneMaxThresholdOnLeftInMW() {

        FlowCnec cnec = initPreventiveCnecAdder().withNominalVoltage(380.).newThreshold().withUnit(MEGAWATT).withMax(500.).withSide(TwoSides.ONE).add().add();

        // bounds on ONE side
        assertEquals(500., cnec.getUpperBound(ONE, MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(500. / (0.38 * Math.sqrt(3)), cnec.getUpperBound(ONE, AMPERE).orElseThrow(), DOUBLE_TOLERANCE); // = 760 A
        assertFalse(cnec.getLowerBound(ONE, MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(ONE, AMPERE).isPresent());

        // bounds on TWO side
        assertFalse(cnec.getUpperBound(TWO, MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(TWO, AMPERE).isPresent());
        assertFalse(cnec.getLowerBound(TWO, MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(TWO, AMPERE).isPresent());

        // margin
        assertEquals(200., cnec.computeMargin(300, ONE, MEGAWATT), DOUBLE_TOLERANCE); // bound: 500 MW
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(300, TWO, MEGAWATT), DOUBLE_TOLERANCE); // bound: 500 MW
        assertEquals(460., cnec.computeMargin(300, ONE, AMPERE), DOUBLE_TOLERANCE); // bound: 760 A
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(-300, TWO, AMPERE), DOUBLE_TOLERANCE); // bound: 760 A
    }

    @Test
    void testBranchWithOneMinThresholdOnRightInMW() {

        FlowCnec cnec = initPreventiveCnecAdder().withNominalVoltage(380.).newThreshold().withUnit(MEGAWATT).withMin(-500.).withSide(TwoSides.TWO).add().add();

        // bounds on ONE side
        assertFalse(cnec.getUpperBound(ONE, MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(ONE, AMPERE).isPresent());
        assertFalse(cnec.getLowerBound(ONE, MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(ONE, AMPERE).isPresent());

        // bounds on TWO side
        assertFalse(cnec.getUpperBound(TWO, MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(TWO, AMPERE).isPresent());
        assertEquals(-500., cnec.getLowerBound(TWO, MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-500. / (0.38 * Math.sqrt(3)), cnec.getLowerBound(TWO, AMPERE).orElseThrow(), DOUBLE_TOLERANCE); // = -760 A

        // margin
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(300, ONE, MEGAWATT), DOUBLE_TOLERANCE); // bound: -500 MW
        assertEquals(800., cnec.computeMargin(300, TWO, MEGAWATT), DOUBLE_TOLERANCE); // bound: -500 MW
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(300, ONE, AMPERE), DOUBLE_TOLERANCE); // bound: -760 A
        assertEquals(-240., cnec.computeMargin(-1000, TWO, AMPERE), DOUBLE_TOLERANCE); // bound: -760 A
    }

    @Test
    void testBranchWithOneMinThresholdOnLeftInAmpere() {

        FlowCnec cnec = initPreventiveCnecAdder().withNominalVoltage(380.).newThreshold().withUnit(AMPERE).withMin(-450.).withSide(TwoSides.ONE).add().add();

        // bounds on ONE side
        assertFalse(cnec.getUpperBound(ONE, MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(ONE, AMPERE).isPresent());
        assertEquals(-450., cnec.getLowerBound(ONE, AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-450. * (0.38 * Math.sqrt(3)), cnec.getLowerBound(ONE, MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE); // -296 MW

        // bounds on TWO side
        assertFalse(cnec.getUpperBound(TWO, MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(TWO, AMPERE).isPresent());
        assertFalse(cnec.getLowerBound(TWO, MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(TWO, AMPERE).isPresent());

        // margin
        assertEquals(750., cnec.computeMargin(300, ONE, AMPERE), DOUBLE_TOLERANCE); // bound: -450 A
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(300, TWO, AMPERE), DOUBLE_TOLERANCE); // bound: -450 A
        assertEquals(596., cnec.computeMargin(300, ONE, MEGAWATT), DOUBLE_TOLERANCE); // bound: -296 MW
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(-300, TWO, MEGAWATT), DOUBLE_TOLERANCE); // bound: -296 MW
    }

    @Test
    void testBranchWithOneMaxThresholdOnRightInAmpere() {

        FlowCnec cnec = initPreventiveCnecAdder().withNominalVoltage(220.).newThreshold().withUnit(AMPERE).withMax(110.).withSide(TwoSides.TWO).add().add();

        // bounds on ONE side
        assertFalse(cnec.getUpperBound(ONE, MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(ONE, AMPERE).isPresent());
        assertFalse(cnec.getLowerBound(ONE, MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(ONE, AMPERE).isPresent());

        // bounds on TWO side
        assertEquals(110., cnec.getUpperBound(TWO, AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(110. * (0.22 * Math.sqrt(3)), cnec.getUpperBound(TWO, MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE); // 42 MW
        assertFalse(cnec.getLowerBound(TWO, MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(TWO, AMPERE).isPresent());

        // margin
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(300, ONE, AMPERE), DOUBLE_TOLERANCE); // bound: 110 A
        assertEquals(-190., cnec.computeMargin(300, TWO, AMPERE), DOUBLE_TOLERANCE); // bound: 110 A
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(300, ONE, MEGAWATT), DOUBLE_TOLERANCE); // bound: 42 MW
        assertEquals(342., cnec.computeMargin(-300, TWO, MEGAWATT), DOUBLE_TOLERANCE); // bound: 42 MW
    }

    @Test
    void testBranchWithOneMaxThresholdOnLeftInPercentImax() {

        FlowCnec cnec = initPreventiveCnecAdder().withNominalVoltage(380.).withIMax(1000.).newThreshold().withUnit(PERCENT_IMAX).withMax(1.1).withSide(TwoSides.ONE).add() // 1.1 = 110 %
            .add();

        // bounds on ONE side
        assertEquals(1100., cnec.getUpperBound(ONE, AMPERE).orElseThrow(), DOUBLE_TOLERANCE); // 1100 A
        assertEquals(1100. * (0.38 * Math.sqrt(3)), cnec.getUpperBound(ONE, MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE); // 724 MW
        assertFalse(cnec.getLowerBound(ONE, MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(ONE, AMPERE).isPresent());

        // bounds on TWO side
        assertFalse(cnec.getUpperBound(TWO, MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(TWO, AMPERE).isPresent());
        assertFalse(cnec.getLowerBound(TWO, MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(TWO, AMPERE).isPresent());

        // margin
        assertEquals(-100, cnec.computeMargin(1200, ONE, AMPERE), DOUBLE_TOLERANCE); // bound: 1100 A
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(1200, TWO, AMPERE), DOUBLE_TOLERANCE); // bound: 1100 A
        assertEquals(-26., cnec.computeMargin(750, ONE, MEGAWATT), DOUBLE_TOLERANCE); // bound: 724 MW
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(100, TWO, MEGAWATT), DOUBLE_TOLERANCE); // bound: 724 MW
    }

    @Test
    void testBranchWithOneMinThresholdOnRightInPercentImax() {

        FlowCnec cnec = initPreventiveCnecAdder().withNominalVoltage(220.).withIMax(1000., TWO).withIMax(0., ONE) // should not be considered as the threshold is on the right side
            .newThreshold().withUnit(PERCENT_IMAX).withMin(-0.9).withSide(TwoSides.TWO).add() // 0.9 = 90 %
            .add();

        assertEquals(Set.of(TWO), cnec.getMonitoredSides());

        // bounds on ONE side
        assertFalse(cnec.getUpperBound(ONE, MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(ONE, AMPERE).isPresent());
        assertFalse(cnec.getLowerBound(ONE, MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(ONE, AMPERE).isPresent());

        // bounds on TWO side
        assertFalse(cnec.getUpperBound(TWO, MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(TWO, AMPERE).isPresent());
        assertEquals(-900., cnec.getLowerBound(TWO, AMPERE).orElseThrow(), DOUBLE_TOLERANCE); // -900 A
        assertEquals(-900. * (0.22 * Math.sqrt(3)), cnec.getLowerBound(TWO, MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE); // -343 MW

        // margin
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(1200, ONE, AMPERE), DOUBLE_TOLERANCE); // bound: -900 A
        assertEquals(2100., cnec.computeMargin(1200, TWO, AMPERE), DOUBLE_TOLERANCE); // bound: -900 A
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(-500, ONE, MEGAWATT), DOUBLE_TOLERANCE); // bound: -343 MW
        assertEquals(443., cnec.computeMargin(100, TWO, MEGAWATT), DOUBLE_TOLERANCE); // bound: -343 MW
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

        FlowCnec cnec = initPreventiveCnecAdder().withNominalVoltage(220., ONE).withNominalVoltage(380., TWO).newThreshold().withUnit(MEGAWATT).withMax(500.).withSide(TwoSides.ONE).add().add();

        // bounds on ONE side
        assertEquals(500., cnec.getUpperBound(ONE, MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(500. / (0.22 * Math.sqrt(3)), cnec.getUpperBound(ONE, AMPERE).orElseThrow(), DOUBLE_TOLERANCE); // 1312 A
        assertFalse(cnec.getLowerBound(ONE, MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(ONE, AMPERE).isPresent());

        // bounds on TWO side
        assertFalse(cnec.getUpperBound(TWO, MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(TWO, AMPERE).isPresent());
        assertFalse(cnec.getLowerBound(TWO, MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(TWO, AMPERE).isPresent());

        // margins
        assertEquals(400., cnec.computeMargin(100, ONE, MEGAWATT), DOUBLE_TOLERANCE); // bound: 500 MW
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(1000, TWO, MEGAWATT), DOUBLE_TOLERANCE); // bound: 500 MW
        assertEquals(1512., cnec.computeMargin(-200, ONE, AMPERE), DOUBLE_TOLERANCE); // bound: 1312 A
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(2000, TWO, AMPERE), DOUBLE_TOLERANCE); // bound: 759 A
    }

    @Test
    void testTransformerWithOneMinThresholdOnRightInMW() {

        FlowCnec cnec = initPreventiveCnecAdder().withNominalVoltage(220., ONE).withNominalVoltage(380., TWO).newThreshold().withUnit(MEGAWATT).withMin(-600.).withSide(TwoSides.TWO).add().add();

        // bounds on ONE side
        assertFalse(cnec.getUpperBound(ONE, MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(ONE, AMPERE).isPresent());
        assertFalse(cnec.getLowerBound(ONE, MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(ONE, AMPERE).isPresent());

        // bounds on TWO side
        assertFalse(cnec.getUpperBound(TWO, MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(TWO, AMPERE).isPresent());
        assertEquals(-600., cnec.getLowerBound(TWO, MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-600. / (0.38 * Math.sqrt(3)), cnec.getLowerBound(TWO, AMPERE).orElseThrow(), DOUBLE_TOLERANCE); // - 912 A

        // margin
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(500, ONE, MEGAWATT), DOUBLE_TOLERANCE); // bound: -600 MW
        assertEquals(-400., cnec.computeMargin(-1000, TWO, MEGAWATT), DOUBLE_TOLERANCE); // bound: -600 MW
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(-500, ONE, AMPERE), DOUBLE_TOLERANCE); // bound: -1575 A
        assertEquals(1012., cnec.computeMargin(100, TWO, AMPERE), DOUBLE_TOLERANCE); // bound: -912 A
    }

    @Test
    void testTransformerWithOneMinThresholdOnLeftInA() {

        FlowCnec cnec = initPreventiveCnecAdder().withNominalVoltage(220., ONE).withNominalVoltage(380., TWO).newThreshold().withUnit(AMPERE).withMin(-1000.).withSide(TwoSides.ONE).add().add();

        // bounds on ONE side
        assertFalse(cnec.getUpperBound(ONE, MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(ONE, AMPERE).isPresent());
        assertEquals(-1000., cnec.getLowerBound(ONE, AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-1000. * (0.22 * Math.sqrt(3)), cnec.getLowerBound(ONE, MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE); // - 381 MW

        // bounds on TWO side
        assertFalse(cnec.getUpperBound(TWO, MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(TWO, AMPERE).isPresent());
        assertFalse(cnec.getLowerBound(TWO, MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(TWO, AMPERE).isPresent());

        // margin
        assertEquals(1300., cnec.computeMargin(300, ONE, AMPERE), DOUBLE_TOLERANCE); // bound: -1000 A
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(-800, TWO, AMPERE), DOUBLE_TOLERANCE); // bound: -579 A
        assertEquals(-319., cnec.computeMargin(-700, ONE, MEGAWATT), DOUBLE_TOLERANCE); // bound: -381 MW
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(1000, TWO, MEGAWATT), DOUBLE_TOLERANCE); // bound: -381 MW
    }

    @Test
    void testTransformerWithOneMaxThresholdOnRightInA() {

        FlowCnec cnec = initPreventiveCnecAdder().withNominalVoltage(220., ONE).withNominalVoltage(380., TWO).newThreshold().withUnit(AMPERE).withMax(500.).withSide(TwoSides.TWO).add().add();

        // bounds on ONE side
        assertFalse(cnec.getUpperBound(ONE, MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(ONE, AMPERE).isPresent());
        assertFalse(cnec.getLowerBound(ONE, MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(ONE, AMPERE).isPresent());

        // bounds on TWO side
        assertEquals(500., cnec.getUpperBound(TWO, AMPERE).orElseThrow(), DOUBLE_TOLERANCE); // 500 A
        assertEquals(500. * (0.38 * Math.sqrt(3)), cnec.getUpperBound(TWO, MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE); // 329 MW
        assertFalse(cnec.getLowerBound(TWO, MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(TWO, AMPERE).isPresent());

        // margin
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(800, ONE, AMPERE), DOUBLE_TOLERANCE); // bound: 864 A
        assertEquals(-100., cnec.computeMargin(600, TWO, AMPERE), DOUBLE_TOLERANCE); // bound: 500 A
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(300, ONE, MEGAWATT), DOUBLE_TOLERANCE); // bound: 329 MW
        assertEquals(-71., cnec.computeMargin(400, TWO, MEGAWATT), DOUBLE_TOLERANCE); // bound: 329 MW
    }

    @Test
    void testTransformerWithOneMinThresholdOnLeftInPercentImax() {

        FlowCnec cnec = initPreventiveCnecAdder().withNominalVoltage(220., ONE).withNominalVoltage(380., TWO).withIMax(2000.).newThreshold().withUnit(PERCENT_IMAX).withMin(-1.).withSide(TwoSides.ONE).add().add();

        // bounds on ONE side
        assertFalse(cnec.getUpperBound(ONE, MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(ONE, AMPERE).isPresent());
        assertEquals(-2000., cnec.getLowerBound(ONE, AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-2000. * (0.22 * Math.sqrt(3)), cnec.getLowerBound(ONE, MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE); // - 762 MW

        // bounds on TWO side
        assertFalse(cnec.getUpperBound(TWO, MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(TWO, AMPERE).isPresent());
        assertFalse(cnec.getLowerBound(TWO, MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(TWO, AMPERE).isPresent());

        // margin
        assertEquals(2300., cnec.computeMargin(300, ONE, AMPERE), DOUBLE_TOLERANCE); // bound: -2000 A
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(-800, TWO, AMPERE), DOUBLE_TOLERANCE); // bound: -1158 A
        assertEquals(62., cnec.computeMargin(-700, ONE, MEGAWATT), DOUBLE_TOLERANCE); // bound: -762 MW
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(-1000, TWO, MEGAWATT), DOUBLE_TOLERANCE); // bound: -762 MW
    }

    @Test
    void testTransformerWithOneMaxThresholdOnRightInPercentImax() {

        FlowCnec cnec = initPreventiveCnecAdder().withNominalVoltage(220., ONE).withNominalVoltage(380., TWO).withIMax(0., ONE) // shouldn't be used as threshold is defined on right side
            .withIMax(2000., TWO).newThreshold().withUnit(PERCENT_IMAX).withMax(0.25).withSide(TwoSides.TWO).add().add();

        // bounds on ONE side
        assertFalse(cnec.getUpperBound(ONE, MEGAWATT).isPresent());
        assertFalse(cnec.getUpperBound(ONE, AMPERE).isPresent());
        assertFalse(cnec.getLowerBound(ONE, MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(ONE, AMPERE).isPresent());

        // bounds on TWO side
        assertEquals(500., cnec.getUpperBound(TWO, AMPERE).orElseThrow(), DOUBLE_TOLERANCE); // 500 A
        assertEquals(500. * (0.38 * Math.sqrt(3)), cnec.getUpperBound(TWO, MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE); // 329 MW
        assertFalse(cnec.getLowerBound(TWO, MEGAWATT).isPresent());
        assertFalse(cnec.getLowerBound(TWO, AMPERE).isPresent());

        // margin
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(800, ONE, AMPERE), DOUBLE_TOLERANCE); // bound: 864 A
        assertEquals(-100., cnec.computeMargin(600, TWO, AMPERE), DOUBLE_TOLERANCE); // bound: 500 A
        assertEquals(Double.POSITIVE_INFINITY, cnec.computeMargin(300, ONE, MEGAWATT), DOUBLE_TOLERANCE); // bound: 329 MW
        assertEquals(-71., cnec.computeMargin(400, TWO, MEGAWATT), DOUBLE_TOLERANCE); // bound: 329 MW
    }

    // Tests on concurrency between thresholds

    @Test
    void testBranchWithSeveralThresholdsWithLimitingOnLeftOrRightSide() {

        FlowCnec cnec = initPreventiveCnecAdder().newThreshold().withUnit(MEGAWATT).withMax(100.).withSide(TwoSides.ONE).add().newThreshold().withUnit(MEGAWATT).withMin(-200.).withSide(TwoSides.ONE).add().newThreshold().withUnit(MEGAWATT).withMax(500.).withSide(TwoSides.TWO).add().newThreshold().withUnit(MEGAWATT).withMin(-300.).withSide(TwoSides.TWO).add().add();

        assertEquals(100., cnec.getUpperBound(ONE, MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-200., cnec.getLowerBound(ONE, MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-200., cnec.computeMargin(300, ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(0., cnec.computeMargin(-200, ONE, MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    void testBranchWithSeveralThresholdsWithBoth() {

        FlowCnec cnec = initPreventiveCnecAdder().newThreshold().withUnit(MEGAWATT).withMax(100.).withSide(TwoSides.ONE).add().newThreshold().withUnit(MEGAWATT).withMin(-200.).withSide(TwoSides.ONE).add().newThreshold().withUnit(MEGAWATT).withMax(500.).withSide(TwoSides.TWO).add().newThreshold().withUnit(MEGAWATT).withMin(-300.).withSide(TwoSides.TWO).add().newThreshold().withUnit(MEGAWATT).withMin(-50.).withMax(150.).withSide(TwoSides.TWO).add().add();

        assertEquals(100., cnec.getUpperBound(ONE, MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-200., cnec.getLowerBound(ONE, MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-200., cnec.computeMargin(300, ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(0., cnec.computeMargin(-200, ONE, MEGAWATT), DOUBLE_TOLERANCE);

        assertEquals(150., cnec.getUpperBound(TWO, MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-50., cnec.getLowerBound(TWO, MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-150, cnec.computeMargin(300, TWO, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-150., cnec.computeMargin(-200, TWO, MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    void testComputeMarginOnTransformerWithSeveralThresholdsInAmps() {

        FlowCnec cnec = initPreventiveCnecAdder().withNominalVoltage(220., ONE).withNominalVoltage(380., TWO).newThreshold().withUnit(AMPERE).withMax(100.).withSide(TwoSides.ONE).add().newThreshold().withUnit(AMPERE).withMin(-70.).withSide(TwoSides.ONE).add().newThreshold().withUnit(AMPERE).withMin(-50.).withMax(50.).withSide(TwoSides.TWO).add().add();

        assertEquals(100, cnec.getUpperBound(ONE, AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-70, cnec.getLowerBound(ONE, AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(0., cnec.computeMargin(100, ONE, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-30, cnec.computeMargin(-100, ONE, AMPERE), DOUBLE_TOLERANCE);
    }

    @Test
    void unboundedCnecInOppositeDirection() {

        FlowCnec cnec = initPreventiveCnecAdder().newThreshold().withUnit(MEGAWATT).withMax(500.).withSide(TwoSides.ONE).add().newThreshold().withUnit(MEGAWATT).withMax(200.).withSide(TwoSides.ONE).add().add();

        assertEquals(200, cnec.getUpperBound(ONE, MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(200, cnec.computeMargin(0., ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertFalse(cnec.getLowerBound(ONE, MEGAWATT).isPresent());
    }

    @Test
    void unboundedCnecInDirectDirection() {

        FlowCnec cnec = initPreventiveCnecAdder().newThreshold().withUnit(MEGAWATT).withMin(-500.).withSide(TwoSides.ONE).add().newThreshold().withUnit(MEGAWATT).withMin(-200.).withSide(TwoSides.ONE).add().add();

        assertEquals(-200, cnec.getLowerBound(ONE, MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(200, cnec.computeMargin(0., ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertFalse(cnec.getUpperBound(ONE, MEGAWATT).isPresent());
    }

    @Test
    void marginsWithNegativeAndPositiveLimits() {

        FlowCnec cnec = initPreventiveCnecAdder().newThreshold().withUnit(MEGAWATT).withMin(-200.).withMax(500.).withSide(TwoSides.ONE).add().add();

        assertEquals(-100, cnec.computeMargin(-300, ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(200, cnec.computeMargin(0, ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(100, cnec.computeMargin(400, ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-300, cnec.computeMargin(800, ONE, MEGAWATT), DOUBLE_TOLERANCE);
    }

    // other

    @Test
    void testEqualsAndHashCode() {
        FlowCnec cnec1 = initPreventiveCnecAdder().newThreshold().withUnit(MEGAWATT).withMax(1000.).withSide(TwoSides.ONE).add().add();
        FlowCnec cnec2 = initPreventiveCnecAdder().withId("anotherId").newThreshold().withUnit(AMPERE).withMin(-1000.).withSide(TwoSides.ONE).add().withNominalVoltage(220.).add();

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
        FlowCnec cnec1 = crac.newFlowCnec().withId("cnec-1-id").withNetworkElement("BBE1AA1  BBE2AA1  1").withInstant(PREVENTIVE_INSTANT_ID).newThreshold().withUnit(MEGAWATT).withMax(1000.).withSide(ONE).add().add();
        assertTrue(cnec1.isConnected(network));

        network.getBranch("BBE1AA1  BBE2AA1  1").getTerminal1().disconnect();
        assertFalse(cnec1.isConnected(network));

        network.getBranch("BBE1AA1  BBE2AA1  1").getTerminal1().connect();
        network.getBranch("BBE1AA1  BBE2AA1  1").getTerminal2().disconnect();
        assertFalse(cnec1.isConnected(network));

        // DanglingLine
        FlowCnec cnec2 = crac.newFlowCnec().withId("cnec-2-id").withNetworkElement("DL1").withInstant(PREVENTIVE_INSTANT_ID).newThreshold().withUnit(MEGAWATT).withMax(1000.).withSide(ONE).add().add();
        assertTrue(cnec2.isConnected(network));

        network.getDanglingLine("DL1").getTerminal().disconnect();
        assertFalse(cnec2.isConnected(network));

        // Generator
        FlowCnec cnec3 = crac.newFlowCnec().withId("cnec-3-id").withNetworkElement("BBE2AA1 _generator").withInstant(PREVENTIVE_INSTANT_ID).newThreshold().withUnit(MEGAWATT).withMax(1000.).withSide(ONE).add().add();
        assertTrue(cnec3.isConnected(network));

        network.getGenerator("BBE2AA1 _generator").getTerminal().disconnect();
        assertFalse(cnec3.isConnected(network));
    }
}
