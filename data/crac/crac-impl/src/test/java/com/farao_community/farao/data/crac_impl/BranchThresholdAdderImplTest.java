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
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdAdder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.farao_community.farao.data.crac_api.cnec.Side.LEFT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class BranchThresholdAdderImplTest {
    private static final double DOUBLE_TOLERANCE = 1e-6;
    private Crac crac;
    private Contingency contingency;

    @BeforeEach
    public void setUp() {
        crac = new CracImplFactory().create("test-crac");
        crac.addInstant("preventive", InstantKind.PREVENTIVE, null);
        crac.addInstant("outage", InstantKind.OUTAGE, "preventive");
        crac.addInstant("auto", InstantKind.AUTO, "outage");
        crac.addInstant("curative", InstantKind.CURATIVE, "auto");
        contingency = crac.newContingency().withId("conId").add();
    }

    @Test
    void testAddThresholdInMW() {
        FlowCnec cnec = crac.newFlowCnec()
            .withId("test-cnec").withInstantId("outage").withContingency(contingency.getId())
            .withNetworkElement("neID")
            .newThreshold().withUnit(Unit.MEGAWATT).withMin(-250.0).withMax(1000.0).withSide(Side.LEFT).add()
            .add();
        assertEquals(1000.0, cnec.getUpperBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-250.0, cnec.getLowerBound(LEFT, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
    }

    @Test
    void testAddThresholdInA() {
        FlowCnec cnec = crac.newFlowCnec()
            .withId("test-cnec").withInstantId("outage").withContingency(contingency.getId())
            .withNetworkElement("BBE1AA1  BBE2AA1  1")
            .newThreshold().withUnit(Unit.AMPERE).withMin(-1000.).withMax(1000.).withSide(Side.LEFT).add()
            .withNominalVoltage(220.)
            .add();
        assertEquals(1000.0, cnec.getUpperBound(LEFT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-1000.0, cnec.getLowerBound(LEFT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
    }

    @Test
    void testAddThresholdInPercent() {
        FlowCnec cnec = crac.newFlowCnec()
            .withId("test-cnec").withInstantId("curative").withContingency(contingency.getId())
            .withNetworkElement("BBE1AA1  BBE2AA1  1")
            .newThreshold().withUnit(Unit.PERCENT_IMAX).withMin(-0.8).withMax(0.5).withSide(Side.LEFT).add()
            .withNominalVoltage(220.)
            .withIMax(5000.)
            .add();

        assertEquals(0.5 * 5000., cnec.getUpperBound(LEFT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-0.8 * 5000., cnec.getLowerBound(LEFT, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
    }

    @Test
    void testNullParentFail() {
        assertThrows(NullPointerException.class, () -> new BranchThresholdAdderImpl(null));
    }

    @Test
    void testUnsupportedUnitFail() {
        BranchThresholdAdder branchThresholdAdder = crac.newFlowCnec().newThreshold();
        FaraoException exception = assertThrows(FaraoException.class, () -> branchThresholdAdder.withUnit(Unit.KILOVOLT));
        assertEquals("kV Unit is not suited to measure a FLOW value.", exception.getMessage());
    }

    @Test
    void testNoUnitFail() {
        BranchThresholdAdder branchThresholdAdder = crac.newFlowCnec().newThreshold()
            .withMax(1000.0)
            .withSide(Side.LEFT);
        FaraoException exception = assertThrows(FaraoException.class, branchThresholdAdder::add);
        assertEquals("Cannot add Threshold without a Unit. Please use withUnit() with a non null value", exception.getMessage());
    }

    @Test
    void testNoValueFail() {
        BranchThresholdAdder branchThresholdAdder = crac.newFlowCnec().newThreshold()
            .withUnit(Unit.AMPERE)
            .withSide(Side.LEFT);
        FaraoException exception = assertThrows(FaraoException.class, branchThresholdAdder::add);
        assertEquals("Cannot add a threshold without min nor max values. Please use withMin() or withMax().", exception.getMessage());
    }

    @Test
    void testNoSideFail() {
        BranchThresholdAdder branchThresholdAdder = crac.newFlowCnec().newThreshold()
            .withUnit(Unit.AMPERE)
            .withMax(1000.0);
        FaraoException exception = assertThrows(FaraoException.class, branchThresholdAdder::add);
        assertEquals("Cannot add BranchThreshold without a Side. Please use withSide() with a non null value", exception.getMessage());
    }
}
