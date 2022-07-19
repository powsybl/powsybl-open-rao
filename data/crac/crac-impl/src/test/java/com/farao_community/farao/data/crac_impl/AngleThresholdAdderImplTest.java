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
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class AngleThresholdAdderImplTest {
    private static final double DOUBLE_TOLERANCE = 1e-6;
    private Crac crac;
    private Contingency contingency;

    @Before
    public void setUp() {
        crac = new CracImplFactory().create("test-crac");
        contingency = crac.newContingency().withId("conId").add();
    }

    @Test
    public void testAddThresholdInDegree() {
        AngleCnec cnec = crac.newAngleCnec()
            .withId("test-cnec").withInstant(Instant.OUTAGE).withContingency(contingency.getId())
            .withExportingNetworkElement("eneID")
            .withImportingNetworkElement("ineID")
            .newThreshold().withUnit(Unit.DEGREE).withMin(-250.0).withMax(1000.0).add()
            .add();
        assertEquals(1000.0, cnec.getUpperBound(Unit.DEGREE).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-250.0, cnec.getLowerBound(Unit.DEGREE).orElseThrow(), DOUBLE_TOLERANCE);
    }

    @Test(expected = NullPointerException.class)
    public void testNullParentFail() {
        new AngleThresholdAdderImpl(null);
    }

    @Test(expected = FaraoException.class)
    public void testUnsupportedUnitFail() {
        crac.newAngleCnec().newThreshold().withUnit(Unit.MEGAWATT);
    }

    @Test(expected = FaraoException.class)
    public void testNoUnitFail() {
        crac.newAngleCnec().newThreshold()
            .withMax(1000.0)
            .add();
    }

    @Test(expected = FaraoException.class)
    public void testNoValueFail() {
        crac.newAngleCnec().newThreshold()
            .withUnit(Unit.AMPERE)
            .add();
    }
}
