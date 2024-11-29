/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.commons.optimizationperimeters;

import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.raoapi.parameters.LoopFlowParameters;
import com.powsybl.openrao.searchtreerao.castor.algorithm.Perimeter;
import com.powsybl.iidm.network.Country;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class PreventiveOptimizationPerimeterTest extends AbstractOptimizationPerimeterTest {

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
    }

    @Test
    void fullPreventivePerimeter1Test() {
        Mockito.when(prePerimeterResult.getSetpoint(pRA)).thenReturn(500.);
        Mockito.when(prePerimeterResult.getSensitivityStatus(Mockito.any())).thenReturn(ComputationStatus.DEFAULT);
        Perimeter preventivePerimeter = new Perimeter(pState, Set.of(oState1, oState2, cState2));
        OptimizationPerimeter optPerimeter = PreventiveOptimizationPerimeter.buildFromBasecaseScenario(preventivePerimeter, crac, network, raoParameters, prePerimeterResult);

        assertEquals(pState, optPerimeter.getMainOptimizationState());
        assertEquals(Set.of(pState), optPerimeter.getRangeActionOptimizationStates());
        assertEquals(Set.of(pState, oState1, oState2, cState2), optPerimeter.getMonitoredStates());

        assertEquals(Set.of(pCnec, oCnec1, oCnec2, cCnec2), optPerimeter.getFlowCnecs());
        assertEquals(Set.of(pCnec, oCnec2, cCnec2), optPerimeter.getOptimizedFlowCnecs());
        assertEquals(Set.of(oCnec1, oCnec2), optPerimeter.getMonitoredFlowCnecs());
        assertTrue(optPerimeter.getLoopFlowCnecs().isEmpty()); // loop-flow not monitored according to raoParameters

        assertEquals(Set.of(pNA), optPerimeter.getNetworkActions());

        assertEquals(1, optPerimeter.getRangeActionsPerState().size());
        assertTrue(optPerimeter.getRangeActionsPerState().containsKey(pState));
        assertEquals(1, optPerimeter.getRangeActionsPerState().get(pState).size());
        assertTrue(optPerimeter.getRangeActionsPerState().get(pState).contains(pRA));
    }

    @Test
    void fullPreventivePerimeter2Test() {
        raoParameters.setLoopFlowParameters(new LoopFlowParameters());
        Mockito.when(prePerimeterResult.getSetpoint(pRA)).thenReturn(10000.);
        Mockito.when(prePerimeterResult.getSensitivityStatus(Mockito.any())).thenReturn(ComputationStatus.DEFAULT);
        Perimeter preventivePerimeter = new Perimeter(pState, Set.of(oState1, oState2, cState2));
        OptimizationPerimeter optPerimeter = PreventiveOptimizationPerimeter.buildFromBasecaseScenario(preventivePerimeter, crac, network, raoParameters, prePerimeterResult);

        assertEquals(Set.of(oCnec1, cCnec2), optPerimeter.getLoopFlowCnecs());
        assertTrue(optPerimeter.getRangeActions().isEmpty());
        assertTrue(optPerimeter.getRangeActionOptimizationStates().isEmpty());
    }

    @Test
    void fullPreventivePerimeter3Test() {
        LoopFlowParameters loopFlowParameters = new LoopFlowParameters();
        loopFlowParameters.setCountries(Set.of(Country.BE));
        raoParameters.setLoopFlowParameters(loopFlowParameters);
        Mockito.when(prePerimeterResult.getSensitivityStatus(Mockito.any())).thenReturn(ComputationStatus.DEFAULT);
        Perimeter preventivePerimeter = new Perimeter(pState, Set.of(oState1, oState2, cState2));
        OptimizationPerimeter optPerimeter = PreventiveOptimizationPerimeter.buildFromBasecaseScenario(preventivePerimeter, crac, network, raoParameters, prePerimeterResult);

        assertEquals(Set.of(cCnec2), optPerimeter.getLoopFlowCnecs()); // the other loop-flow CNEC is not considered as outside of the loopFlow countries scope
    }

    @Test
    void fullWithPreventiveCnecOnlyTest() {
        Mockito.when(prePerimeterResult.getSetpoint(pRA)).thenReturn(500.);
        Mockito.when(prePerimeterResult.getSensitivityStatus(Mockito.any())).thenReturn(ComputationStatus.DEFAULT);
        OptimizationPerimeter optPerimeter = PreventiveOptimizationPerimeter.buildWithPreventiveCnecsOnly(crac, network, raoParameters, prePerimeterResult);

        assertEquals(pState, optPerimeter.getMainOptimizationState());
        assertEquals(Set.of(pState), optPerimeter.getRangeActionOptimizationStates());
        assertEquals(Set.of(pState), optPerimeter.getMonitoredStates());

        assertEquals(Set.of(pCnec), optPerimeter.getFlowCnecs());
        assertEquals(Set.of(pCnec), optPerimeter.getOptimizedFlowCnecs());
        assertTrue(optPerimeter.getMonitoredFlowCnecs().isEmpty());
        assertTrue(optPerimeter.getLoopFlowCnecs().isEmpty());

        assertEquals(Set.of(pNA), optPerimeter.getNetworkActions());

        assertEquals(1, optPerimeter.getRangeActionsPerState().size());
        assertTrue(optPerimeter.getRangeActionsPerState().containsKey(pState));
        assertEquals(1, optPerimeter.getRangeActionsPerState().get(pState).size());
        assertTrue(optPerimeter.getRangeActionsPerState().get(pState).contains(pRA));
    }
}
