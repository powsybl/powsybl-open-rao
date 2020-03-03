/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CnecResultTest {

    private static final double DOUBLE_TOLERANCE = 0.01;

    @Test
    public void testConstructorWithTwoArguments() {
        CnecResult cnecResult = new CnecResult(50.0, 75.0);
        assertEquals(50.0, cnecResult.getFlowInMW(), DOUBLE_TOLERANCE);
        assertEquals(75.0, cnecResult.getFlowInA(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testConstructorWithOneArgument() {
        CnecResult cnecResult = new CnecResult(-45.0);
        assertEquals(-45.0, cnecResult.getFlowInMW(), DOUBLE_TOLERANCE);
        assertEquals(Double.NaN, cnecResult.getFlowInA(), DOUBLE_TOLERANCE);
    }

    @Test
    public void getNameTest() {
        CnecResult cnecResult = new CnecResult(-45.0);
        assertEquals("CnecResult", cnecResult.getName());
    }
}
