/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.data.crac_api.threshold.VoltageThresholdAdder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
class VoltageThresholdAdderImplTest {
    private static final double DOUBLE_TOLERANCE = 1e-6;
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private Crac crac;
    private Contingency contingency;
    private Instant outageInstant;

    @BeforeEach
    public void setUp() {
        crac = new CracImplFactory().create("test-crac")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE);
        outageInstant = crac.getInstant(OUTAGE_INSTANT_ID);
        contingency = crac.newContingency().withId("conId").add();
    }

    @Test
    void testAddThresholdInDegree() {
        VoltageCnec cnec = crac.newVoltageCnec()
            .withId("test-cnec").withInstant(outageInstant).withContingency(contingency.getId())
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
        FaraoException exception = assertThrows(FaraoException.class, () -> voltageThresholdAdder.withUnit(Unit.MEGAWATT));
        assertEquals("MW Unit is not suited to measure a VOLTAGE value.", exception.getMessage());
    }

    @Test
    void testNoUnitFail() {
        VoltageThresholdAdder voltageThresholdAdder =
            crac.newVoltageCnec().newThreshold()
                .withMax(1000.0);
        FaraoException exception = assertThrows(FaraoException.class, voltageThresholdAdder::add);
        assertEquals("Cannot add Threshold without a Unit. Please use withUnit() with a non null value", exception.getMessage());
    }

    @Test
    void testNoValueFail() {
        VoltageThresholdAdder voltageThresholdAdder = crac.newVoltageCnec().newThreshold()
            .withUnit(Unit.KILOVOLT);
        FaraoException exception = assertThrows(FaraoException.class, voltageThresholdAdder::add);
        assertEquals("Cannot add a threshold without min nor max values. Please use withMin() or withMax().", exception.getMessage());
    }
}
