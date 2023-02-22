/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.commons.parameters;

import com.farao_community.farao.rao_api.parameters.RangeActionsOptimizationParameters;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class SolverParametersTest {

    @Test
    public void buildFromRaoParametersTest() {
        RaoParameters raoParameters = new RaoParameters();

        raoParameters.setLinearOptimizationSolver(RangeActionsOptimizationParameters.Solver.CBC);
        raoParameters.setRelativeMipGap(0.5);
        raoParameters.setSolverSpecificParameters("coucouSpecificParameters");

        SolverParameters sp = SolverParameters.buildFromRaoParameters(raoParameters);

        assertEquals(RangeActionsOptimizationParameters.Solver.CBC, sp.getSolver());
        assertEquals(0.5, sp.getRelativeMipGap(), 1e-6);
        assertEquals("coucouSpecificParameters", sp.getSolverSpecificParameters());
    }
}
