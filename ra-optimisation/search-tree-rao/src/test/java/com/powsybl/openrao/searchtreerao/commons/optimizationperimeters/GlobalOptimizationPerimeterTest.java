/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.optimizationperimeters;

import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControl;
import com.powsybl.iidm.network.impl.extensions.HvdcAngleDroopActivePowerControlImpl;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.raoapi.parameters.LoopFlowParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

import static com.powsybl.openrao.data.crac.impl.utils.NetworkImportsUtil.addHvdcLine;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class GlobalOptimizationPerimeterTest extends AbstractOptimizationPerimeterTest {

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
    }

    @Test
    void globalOptimizationPerimeterTest() {
        raoParameters.setLoopFlowParameters(new LoopFlowParameters());
        Mockito.when(prePerimeterResult.getSetpoint(pRA)).thenReturn(1000. + 2 * 1e-6); // should be filtered out
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
        assertEquals(0, optPerimeter.getRangeActionsPerState().get(pState).size());
        assertFalse(optPerimeter.getRangeActionsPerState().get(pState).contains(pRA));
        assertTrue(optPerimeter.getRangeActionsPerState().containsKey(cState1));
        assertEquals(1, optPerimeter.getRangeActionsPerState().get(cState1).size());
        assertTrue(optPerimeter.getRangeActionsPerState().get(cState1).contains(cRA));
    }

    @Test
    void testCopyWithoutHvdcRangeActionAcEmulation() {
        // set up a network with HVDC  line in ac emulation
        addHvdcLine(network);
        // add ac emulation
        network.getHvdcLine("hvdc").addExtension(HvdcAngleDroopActivePowerControl.class, new HvdcAngleDroopActivePowerControlImpl(network.getHvdcLine("hvdc"), 10, 10, true));
        // add hvdc range action to crac
        HvdcRangeAction hvdcRangeAction = crac.newHvdcRangeAction()
            .withId("hvdc-range-action-id")
            .withName("hvdc-range-action-name")
            .withNetworkElement("hvdc")
            .withOperator("operator")
            .newOnInstantUsageRule().withInstant("preventive").add()
            .newRange().withMin(-5).withMax(10).add()
            .add();
        GlobalOptimizationPerimeter optPerimeter = GlobalOptimizationPerimeter.build(crac, network, raoParameters, prePerimeterResult);
        assertTrue(optPerimeter.getRangeActions().contains(hvdcRangeAction));
        // test copy the hvdc range action is filtered from the perimeter
        GlobalOptimizationPerimeter copyPerimeter = (GlobalOptimizationPerimeter) optPerimeter.copyWithFilteredAvailableRangeAction(network);
        assertFalse(copyPerimeter.getRangeActions().contains(hvdcRangeAction));
    }
}
