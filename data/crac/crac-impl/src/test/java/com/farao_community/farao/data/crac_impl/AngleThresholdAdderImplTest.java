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
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import com.farao_community.farao.data.crac_api.threshold.AngleThresholdAdder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
class AngleThresholdAdderImplTest {
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
        AngleCnec cnec = crac.newAngleCnec()
            .withId("test-cnec").withInstant("outage").withContingency(contingency.getId())
            .withExportingNetworkElement("eneID")
            .withImportingNetworkElement("ineID")
            .newThreshold().withUnit(Unit.DEGREE).withMin(-250.0).withMax(1000.0).add()
            .add();
        assertEquals(1000.0, cnec.getUpperBound(Unit.DEGREE).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-250.0, cnec.getLowerBound(Unit.DEGREE).orElseThrow(), DOUBLE_TOLERANCE);
    }

    @Test
    void testNullParentFail() {
        assertThrows(NullPointerException.class, () -> new AngleThresholdAdderImpl(null));
    }

    @Test
    void testUnsupportedUnitFail() {
        AngleThresholdAdder angleThresholdAdder = crac.newAngleCnec().newThreshold();
        FaraoException exception = assertThrows(FaraoException.class, () -> angleThresholdAdder.withUnit(Unit.MEGAWATT));
        assertEquals("MW Unit is not suited to measure a ANGLE value.", exception.getMessage());
    }

    @Test
    void testNoUnitFail() {
        AngleThresholdAdder angleThresholdAdder = crac.newAngleCnec().newThreshold()
            .withMax(1000.0);
        FaraoException exception = assertThrows(FaraoException.class, angleThresholdAdder::add);
        assertEquals("Cannot add Threshold without a Unit. Please use withUnit() with a non null value", exception.getMessage());
    }

    @Test
    void testNoValueFail() {
        AngleThresholdAdder angleThresholdAdder = crac.newAngleCnec().newThreshold();
        FaraoException exception = assertThrows(FaraoException.class, () -> angleThresholdAdder.withUnit(Unit.AMPERE));
        assertEquals("A Unit is not suited to measure a ANGLE value.", exception.getMessage());
    }
}
