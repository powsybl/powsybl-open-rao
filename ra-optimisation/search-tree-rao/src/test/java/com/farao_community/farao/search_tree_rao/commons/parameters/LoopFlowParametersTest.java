/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.commons.parameters;

import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_api.parameters.extensions.LoopFlowParametersExtension;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class LoopFlowParametersTest {

    @Test
    public void buildFromRaoParametersTestWithLimitation() {
        RaoParameters raoParameters = new RaoParameters();
        raoParameters.addExtension(LoopFlowParametersExtension.class, LoopFlowParametersExtension.loadDefault());
        LoopFlowParametersExtension loopFlowParameters = raoParameters.getExtension(LoopFlowParametersExtension.class);

        loopFlowParameters.setApproximation(LoopFlowParametersExtension.Approximation.UPDATE_PTDF_WITH_TOPO);
        loopFlowParameters.setAcceptableIncrease(0.0);
        loopFlowParameters.setViolationCost(100);
        loopFlowParameters.setConstraintAdjustmentCoefficient(2.4);

        LoopFlowParameters lfp = LoopFlowParameters.buildFromRaoParameters(raoParameters);

        assertNotNull(lfp);
        assertEquals(LoopFlowParametersExtension.Approximation.UPDATE_PTDF_WITH_TOPO, lfp.getLoopFlowApproximationLevel());
        assertEquals(0.0, lfp.getLoopFlowAcceptableAugmentation(), 1e-6);
        assertEquals(100, lfp.getLoopFlowViolationCost(), 1e-6);
        assertEquals(2.4, lfp.getLoopFlowConstraintAdjustmentCoefficient(), 1e-6);
    }

    @Test
    public void buildFromRaoParametersTestWithoutLimitation() {
        RaoParameters raoParameters = new RaoParameters();
        LoopFlowParameters lfp = LoopFlowParameters.buildFromRaoParameters(raoParameters);
        assertNull(lfp);
    }
}
