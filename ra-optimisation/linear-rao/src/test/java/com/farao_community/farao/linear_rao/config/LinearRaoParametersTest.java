/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao.config;

import com.powsybl.sensitivity.SensitivityComputationParameters;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class LinearRaoParametersTest {

    private LinearRaoParameters raoParameters;

    @Before
    public void setUp() throws Exception {
        raoParameters = new LinearRaoParameters();
    }

    @Test
    public void getName() {
        assertEquals("LinearRaoParameters", raoParameters.getName());
    }

    @Test
    public void setSensitivityComputationParameters() {
        SensitivityComputationParameters sensitivityComputationParameters = Mockito.mock(SensitivityComputationParameters.class);
        raoParameters.setSensitivityComputationParameters(sensitivityComputationParameters);

        assertSame(sensitivityComputationParameters, raoParameters.getSensitivityComputationParameters());
    }

    @Test
    public void setMaxIterations() {
        raoParameters.setMaxIterations(99);
        assertEquals(99, raoParameters.getMaxIterations());
    }

    @Test
    public void setSecurityAnalysisWithoutRao() {
        // check default value
        assertEquals(false, raoParameters.getSecurityAnalysisWithoutRao());

        raoParameters.setSecurityAnalysisWithoutRao(true);
        assertEquals(true, raoParameters.getSecurityAnalysisWithoutRao());
    }
}
