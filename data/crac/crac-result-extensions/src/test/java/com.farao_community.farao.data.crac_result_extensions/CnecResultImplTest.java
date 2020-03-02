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
public class CnecResultImplTest {

    private static final double DOUBLE_TOLERANCE = 0.01;

    @Test
    public void testGetterAndSetter() {

        CnecResultImpl cnecResult = new CnecResultImpl(50.0, 75.0);
        assertEquals(50.0, cnecResult.getFlowInMW(), DOUBLE_TOLERANCE);
        assertEquals(75.0, cnecResult.getFlowInA(), DOUBLE_TOLERANCE);

        cnecResult.setFlowInMW(-30.0);
        cnecResult.setFlowInA(40.0);

        assertEquals(-30.0, cnecResult.getFlowInMW(), DOUBLE_TOLERANCE);
        assertEquals(40.0, cnecResult.getFlowInA(), DOUBLE_TOLERANCE);
    }
}
