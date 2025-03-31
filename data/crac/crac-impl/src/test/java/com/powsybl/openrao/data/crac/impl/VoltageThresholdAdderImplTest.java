/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
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
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.crac.api.threshold.VoltageThresholdAdder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
class VoltageThresholdAdderImplTest {
    private static final double DOUBLE_TOLERANCE = 1e-6;
    private Crac crac;
    private Contingency contingency;

    @BeforeEach
    public void setUp() {
        crac = new CracImplFactory().create("test-crac")
            .newInstant("preventive", InstantKind.PREVENTIVE)
            .newInstant("outage", InstantKind.OUTAGE);
        contingency = crac.newContingency().withId("conId").add();
    }

    @Test
    void testAddThresholdInDegree() {
        VoltageCnec cnec = crac.newVoltageCnec()
            .withId("test-cnec").withInstant("outage").withContingency(contingency.getId())
            .withNetworkElement("neID")
            .newThreshold().withUnit(Unit.KILOVOLT).withMin(-250.0).withMax(1000.0).add()
            .add();
        assertEquals(1000.0, cnec.getUpperBound(Unit.KILOVOLT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-250.0, cnec.getLowerBound(Unit.KILOVOLT).orElseThrow(), DOUBLE_TOLERANCE);
    }

    @Test
    void testNullParentFail() {
        assertThrows(NullPointerException.class, () -> new VoltageThresholdAdderImpl(null));
    }

    @Test
    void testUnsupportedUnitFail() {
        VoltageThresholdAdder voltageThresholdAdder = crac.newVoltageCnec().newThreshold();
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> voltageThresholdAdder.withUnit(Unit.MEGAWATT));
        assertEquals("MW Unit is not suited to measure a VOLTAGE value.", exception.getMessage());
    }

    @Test
    void testNoUnitFail() {
        VoltageThresholdAdder voltageThresholdAdder =
            crac.newVoltageCnec().newThreshold()
                .withMax(1000.0);
        OpenRaoException exception = assertThrows(OpenRaoException.class, voltageThresholdAdder::add);
        assertEquals("Cannot add Threshold without a Unit. Please use withUnit() with a non null value", exception.getMessage());
    }

    @Test
    void testNoValueFail() {
        VoltageThresholdAdder voltageThresholdAdder = crac.newVoltageCnec().newThreshold()
            .withUnit(Unit.KILOVOLT);
        OpenRaoException exception = assertThrows(OpenRaoException.class, voltageThresholdAdder::add);
        assertEquals("Cannot add a threshold without min nor max values. Please use withMin() or withMax().", exception.getMessage());
    }
}
