/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.commons.parameters;

import com.farao_community.farao.rao_api.parameters.RaoParameters;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class MnecParametersTest {

    @Test
    public void buildFromRaoParametersTestWithLimitation() {
        RaoParameters raoParameters = new RaoParameters();

        raoParameters.setRaoWithMnecLimitation(true);
        raoParameters.setMnecAcceptableMarginDiminution(45.0);
        raoParameters.setMnecViolationCost(111);
        raoParameters.setMnecConstraintAdjustmentCoefficient(1.1);

        MnecParameters mp = MnecParameters.buildFromRaoParameters(raoParameters);

        assertNotNull(mp);
        assertEquals(45.0, mp.getMnecAcceptableMarginDiminution(), 1e-6);
        assertEquals(111, mp.getMnecViolationCost(), 1e-6);
        assertEquals(1.1, mp.getMnecConstraintAdjustmentCoefficient(), 1e-6);
    }

    @Test
    public void buildFromRaoParametersTestWithoutLimitation() {
        RaoParameters raoParameters = new RaoParameters();
        raoParameters.setRaoWithMnecLimitation(false);
        MnecParameters mp = MnecParameters.buildFromRaoParameters(raoParameters);
        assertNull(mp);
    }
}
