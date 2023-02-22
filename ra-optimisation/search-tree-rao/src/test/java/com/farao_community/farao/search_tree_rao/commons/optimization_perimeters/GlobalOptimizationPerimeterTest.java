/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.commons.optimization_perimeters;

import com.farao_community.farao.rao_api.parameters.extensions.LoopFlowParametersExtension;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class GlobalOptimizationPerimeterTest extends AbstractOptimizationPerimeterTest {

    @Before
    public void setUp() {
        super.setUp();
    }

    @Test
    public void globalOptimizationPerimeterTest() {
        raoParameters.addExtension(LoopFlowParametersExtension.class, LoopFlowParametersExtension.loadDefault());
        Mockito.when(prePerimeterResult.getSetpoint(pRA)).thenReturn(-500.);
        Mockito.when(prePerimeterResult.getSetpoint(cRA)).thenReturn(-500.);
        GlobalOptimizationPerimeter optPerimeter = GlobalOptimizationPerimeter.build(crac, network, raoParameters, prePerimeterResult);

        assertEquals(pState, optPerimeter.getMainOptimizationState());
        assertEquals(Set.of(pState, cState1), optPerimeter.getRangeActionOptimizationStates());
        assertEquals(Set.of(pState, oState1, oState2, cState1, cState2), optPerimeter.getMonitoredStates());

        assertEquals(Set.of(pCnec, oCnec1, oCnec2, cCnec1, cCnec2), optPerimeter.getFlowCnecs());
        assertEquals(Set.of(pCnec, oCnec2, cCnec2), optPerimeter.getOptimizedFlowCnecs());
        assertEquals(Set.of(oCnec1, oCnec2, cCnec1), optPerimeter.getMonitoredFlowCnecs());
        assertEquals(Set.of(oCnec1, cCnec2), optPerimeter.getLoopFlowCnecs());

        assertEquals(Set.of(pNA), optPerimeter.getNetworkActions());

        assertEquals(2, optPerimeter.getRangeActionsPerState().size());
        assertTrue(optPerimeter.getRangeActionsPerState().containsKey(pState));
        assertEquals(1, optPerimeter.getRangeActionsPerState().get(pState).size());
        assertTrue(optPerimeter.getRangeActionsPerState().get(pState).contains(pRA));
        assertTrue(optPerimeter.getRangeActionsPerState().containsKey(cState1));
        assertEquals(1, optPerimeter.getRangeActionsPerState().get(cState1).size());
        assertTrue(optPerimeter.getRangeActionsPerState().get(cState1).contains(cRA));
    }
}
