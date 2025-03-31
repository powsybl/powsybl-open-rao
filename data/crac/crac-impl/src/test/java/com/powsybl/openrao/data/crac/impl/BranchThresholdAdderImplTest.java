/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.api.threshold.BranchThresholdAdder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.powsybl.iidm.network.TwoSides.ONE;
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
        crac = new CracImplFactory().create("test-crac")
            .newInstant("preventive", InstantKind.PREVENTIVE)
            .newInstant("outage", InstantKind.OUTAGE)
            .newInstant("auto", InstantKind.AUTO)
            .newInstant("curative", InstantKind.CURATIVE);
        contingency = crac.newContingency().withId("conId").add();
    }

    @Test
    void testAddThresholdInMW() {
        FlowCnec cnec = crac.newFlowCnec()
            .withId("test-cnec").withInstant("outage").withContingency(contingency.getId())
            .withNetworkElement("neID")
            .newThreshold().withUnit(Unit.MEGAWATT).withMin(-250.0).withMax(1000.0).withSide(TwoSides.ONE).add()
            .add();
        assertEquals(1000.0, cnec.getUpperBound(ONE, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-250.0, cnec.getLowerBound(ONE, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
    }

    @Test
    void testAddThresholdInA() {
        FlowCnec cnec = crac.newFlowCnec()
            .withId("test-cnec").withInstant("outage").withContingency(contingency.getId())
            .withNetworkElement("BBE1AA1  BBE2AA1  1")
            .newThreshold().withUnit(Unit.AMPERE).withMin(-1000.).withMax(1000.).withSide(TwoSides.ONE).add()
            .withNominalVoltage(220.)
            .add();
        assertEquals(1000.0, cnec.getUpperBound(ONE, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-1000.0, cnec.getLowerBound(ONE, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
    }

    @Test
    void testAddThresholdInPercent() {
        FlowCnec cnec = crac.newFlowCnec()
            .withId("test-cnec").withInstant("curative").withContingency(contingency.getId())
            .withNetworkElement("BBE1AA1  BBE2AA1  1")
            .newThreshold().withUnit(Unit.PERCENT_IMAX).withMin(-0.8).withMax(0.5).withSide(TwoSides.ONE).add()
            .withNominalVoltage(220.)
            .withIMax(5000.)
            .add();

        assertEquals(0.5 * 5000., cnec.getUpperBound(ONE, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-0.8 * 5000., cnec.getLowerBound(ONE, Unit.AMPERE).orElseThrow(), DOUBLE_TOLERANCE);
    }

    @Test
    void testNullParentFail() {
        assertThrows(NullPointerException.class, () -> new BranchThresholdAdderImpl(null));
    }

    @Test
    void testUnsupportedUnitFail() {
        BranchThresholdAdder branchThresholdAdder = crac.newFlowCnec().newThreshold();
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> branchThresholdAdder.withUnit(Unit.KILOVOLT));
        assertEquals("kV Unit is not suited to measure a FLOW value.", exception.getMessage());
    }

    @Test
    void testNoUnitFail() {
        BranchThresholdAdder branchThresholdAdder = crac.newFlowCnec().newThreshold()
            .withMax(1000.0)
            .withSide(TwoSides.ONE);
        OpenRaoException exception = assertThrows(OpenRaoException.class, branchThresholdAdder::add);
        assertEquals("Cannot put Threshold without a Unit. Please use withUnit() with a non null value", exception.getMessage());
    }

    @Test
    void testNoValueFail() {
        BranchThresholdAdder branchThresholdAdder = crac.newFlowCnec().newThreshold()
            .withUnit(Unit.AMPERE)
            .withSide(TwoSides.ONE);
        OpenRaoException exception = assertThrows(OpenRaoException.class, branchThresholdAdder::add);
        assertEquals("Cannot put a threshold without min nor max values. Please use withMin() or withMax().", exception.getMessage());
    }

    @Test
    void testNoSideFail() {
        BranchThresholdAdder branchThresholdAdder = crac.newFlowCnec().newThreshold()
            .withUnit(Unit.AMPERE)
            .withMax(1000.0);
        OpenRaoException exception = assertThrows(OpenRaoException.class, branchThresholdAdder::add);
        assertEquals("Cannot put BranchThreshold without a Side. Please use withSide() with a non null value", exception.getMessage());
    }
}
