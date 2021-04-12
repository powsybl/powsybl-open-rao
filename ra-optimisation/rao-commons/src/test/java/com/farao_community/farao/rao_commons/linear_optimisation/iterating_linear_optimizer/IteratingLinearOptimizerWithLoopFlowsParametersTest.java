/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *//*


package com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer;

import com.farao_community.farao.rao_api.RaoParameters;
import org.junit.Test;

import static org.junit.Assert.*;

*/
/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 *//*

public class IteratingLinearOptimizerWithLoopFlowsParametersTest {

    @Test
    public void isLoopflowApproximation() {
        IteratingLinearOptimizerWithLoopFLowsParameters parameters =
            new IteratingLinearOptimizerWithLoopFLowsParameters(20, 12, RaoParameters.LoopFlowApproximationLevel.UPDATE_PTDF_WITH_TOPO, 1.0);
        assertEquals(RaoParameters.LoopFlowApproximationLevel.UPDATE_PTDF_WITH_TOPO, parameters.getLoopflowApproximationLevel());
        assertEquals(1.0, parameters.getLoopFlowViolationCost(), 0.1);
    }

    @Test
    public void setLoopFlowApproximation() {
        IteratingLinearOptimizerWithLoopFLowsParameters parameters =
            new IteratingLinearOptimizerWithLoopFLowsParameters(20, 12, RaoParameters.LoopFlowApproximationLevel.FIXED_PTDF, 1.0);
        parameters.setLoopFlowApproximationLevel(RaoParameters.LoopFlowApproximationLevel.UPDATE_PTDF_WITH_TOPO_AND_PST);
        parameters.setLoopFlowViolationCost(10.0);
        assertEquals(RaoParameters.LoopFlowApproximationLevel.UPDATE_PTDF_WITH_TOPO_AND_PST, parameters.getLoopflowApproximationLevel());
        assertEquals(10.0, parameters.getLoopFlowViolationCost(), 0.1);
    }
}
*/
