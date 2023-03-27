/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.commons.optimization_perimeters;

import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.rao_api.parameters.extensions.LoopFlowParametersExtension;
import com.farao_community.farao.search_tree_rao.castor.algorithm.BasecaseScenario;
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
public class PreventiveOptimizationPerimeterTest extends AbstractOptimizationPerimeterTest {

    @BeforeEach
    public void setUp() {
        super.setUp();
    }

    @Test
    public void fullPreventivePerimeter1Test() {
        Mockito.when(prePerimeterResult.getSetpoint(pRA)).thenReturn(500.);
        Mockito.when(prePerimeterResult.getSensitivityStatus(Mockito.any())).thenReturn(ComputationStatus.DEFAULT);
        BasecaseScenario basecaseScenario = new BasecaseScenario(pState, Set.of(oState1, oState2, cState2));
        OptimizationPerimeter optPerimeter = PreventiveOptimizationPerimeter.buildFromBasecaseScenario(basecaseScenario, crac, network, raoParameters, prePerimeterResult);

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
    public void fullPreventivePerimeter2Test() {
        raoParameters.addExtension(LoopFlowParametersExtension.class, new LoopFlowParametersExtension());
        Mockito.when(prePerimeterResult.getSetpoint(pRA)).thenReturn(10000.);
        Mockito.when(prePerimeterResult.getSensitivityStatus(Mockito.any())).thenReturn(ComputationStatus.DEFAULT);
        BasecaseScenario basecaseScenario = new BasecaseScenario(pState, Set.of(oState1, oState2, cState2));
        OptimizationPerimeter optPerimeter = PreventiveOptimizationPerimeter.buildFromBasecaseScenario(basecaseScenario, crac, network, raoParameters, prePerimeterResult);

        assertEquals(Set.of(oCnec1, cCnec2), optPerimeter.getLoopFlowCnecs());
        assertTrue(optPerimeter.getRangeActions().isEmpty());
        assertTrue(optPerimeter.getRangeActionOptimizationStates().isEmpty());
    }

    @Test
    public void fullPreventivePerimeter3Test() {
        raoParameters.addExtension(LoopFlowParametersExtension.class, new LoopFlowParametersExtension());
        raoParameters.getExtension(LoopFlowParametersExtension.class).setCountries(Set.of(Country.BE));
        Mockito.when(prePerimeterResult.getSensitivityStatus(Mockito.any())).thenReturn(ComputationStatus.DEFAULT);
        BasecaseScenario basecaseScenario = new BasecaseScenario(pState, Set.of(oState1, oState2, cState2));
        OptimizationPerimeter optPerimeter = PreventiveOptimizationPerimeter.buildFromBasecaseScenario(basecaseScenario, crac, network, raoParameters, prePerimeterResult);

        assertEquals(Set.of(cCnec2), optPerimeter.getLoopFlowCnecs()); // the other loop-flow CNEC is not considered as outside of the loopFlow countries scope
    }

    @Test
    public void fullWithPreventiveCnecOnlyTest() {
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
