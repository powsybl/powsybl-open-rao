/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class LinearRaoParametersTest {

    private static final double DOUBLE_TOLERANCE = 0.1;

    private LinearRaoParameters raoParameters;

    @Before
    public void setUp() {
        raoParameters = new LinearRaoParameters();
    }

    @Test
    public void getName() {
        assertEquals("LinearRaoParameters", raoParameters.getName());
    }

    @Test
    public void setSecurityAnalysisWithoutRao() {
        // check default value
        assertEquals(false, raoParameters.isSecurityAnalysisWithoutRao());

        raoParameters.setSecurityAnalysisWithoutRao(true);
        assertEquals(true, raoParameters.isSecurityAnalysisWithoutRao());
    }

    @Test
    public void setObjectiveFunction() {
        raoParameters.setObjectiveFunction(LinearRaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE);
        assertEquals(LinearRaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE, raoParameters.getObjectiveFunction());
    }

    @Test
    public void setFallbackOvercost() {
        raoParameters.setFallbackOvercost(100.0);
        assertEquals(100.0, raoParameters.getFallbackOvercost(), DOUBLE_TOLERANCE);
    }
}
