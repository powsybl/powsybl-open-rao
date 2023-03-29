/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.loopflow_computation;

import com.farao_community.farao.commons.EICode;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.powsybl.glsk.commons.ZonalData;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.*;
import com.powsybl.sensitivity.SensitivityVariableSet;
import com.powsybl.sensitivity.WeightedSensitivityVariable;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class LoopFlowComputationWithXnodeGlskHandlerTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    private Network mockNetwork() {
        Network network = Mockito.mock(Network.class);
        Generator gen = Mockito.mock(Generator.class);
        Load load = Mockito.mock(Load.class);
        Mockito.when(network.getGenerator("gen")).thenReturn(gen);
        Mockito.when(network.getLoad("load")).thenReturn(load);
        Mockito.doReturn(mockInjection(true)).when(gen).getTerminal();
        Mockito.doReturn(mockInjection(true)).when(load).getTerminal();
        return network;
    }

    private Terminal mockInjection(boolean inMainComponent) {
        Bus bus = Mockito.mock(Bus.class);
        Mockito.doReturn(inMainComponent).when(bus).isInMainConnectedComponent();
        Terminal.BusView busView = Mockito.mock(Terminal.BusView.class);
        Mockito.doReturn(bus).when(busView).getBus();
        Terminal terminal = Mockito.mock(Terminal.class);
        Mockito.doReturn(busView).when(terminal).getBusView();
        return terminal;
    }

    @Test
    void testCommercialFlowsWithCnecAfterDanglingLineContingency() {
        ZonalData<SensitivityVariableSet> glsk = Mockito.mock(ZonalData.class);
        ReferenceProgram referenceProgram = Mockito.mock(ReferenceProgram.class);
        XnodeGlskHandler xnodeGlskHandler = Mockito.mock(XnodeGlskHandler.class);
        Network network = mockNetwork();
        Mockito.when(xnodeGlskHandler.getNetwork()).thenReturn(network);

        FlowCnec preventiveCnec = Mockito.mock(FlowCnec.class);
        FlowCnec cnecAfterClassicContingency = Mockito.mock(FlowCnec.class);
        FlowCnec cnecAfterDanglingContingency = Mockito.mock(FlowCnec.class);
        Mockito.when(preventiveCnec.getMonitoredSides()).thenReturn(Collections.singleton(Side.LEFT));
        Mockito.when(cnecAfterClassicContingency.getMonitoredSides()).thenReturn(Collections.singleton(Side.RIGHT));
        Mockito.when(cnecAfterDanglingContingency.getMonitoredSides()).thenReturn(Collections.singleton(Side.LEFT));
        SensitivityVariableSet classicLinearGlsk = Mockito.mock(SensitivityVariableSet.class);
        SensitivityVariableSet virtualHubLinearGlsk = Mockito.mock(SensitivityVariableSet.class);
        Mockito.when(xnodeGlskHandler.isLinearGlskValidForCnec(preventiveCnec, classicLinearGlsk)).thenReturn(true);
        Mockito.when(xnodeGlskHandler.isLinearGlskValidForCnec(preventiveCnec, virtualHubLinearGlsk)).thenReturn(true);
        Mockito.when(xnodeGlskHandler.isLinearGlskValidForCnec(cnecAfterClassicContingency, classicLinearGlsk)).thenReturn(true);
        Mockito.when(xnodeGlskHandler.isLinearGlskValidForCnec(cnecAfterClassicContingency, virtualHubLinearGlsk)).thenReturn(true);
        Mockito.when(xnodeGlskHandler.isLinearGlskValidForCnec(cnecAfterDanglingContingency, classicLinearGlsk)).thenReturn(true);
        Mockito.when(xnodeGlskHandler.isLinearGlskValidForCnec(cnecAfterDanglingContingency, virtualHubLinearGlsk)).thenReturn(false);

        SystematicSensitivityResult systematicSensitivityResult = Mockito.mock(SystematicSensitivityResult.class);
        Mockito.when(systematicSensitivityResult.getSensitivityOnFlow(classicLinearGlsk, preventiveCnec, Side.LEFT)).thenReturn(0.5);
        Mockito.when(systematicSensitivityResult.getSensitivityOnFlow(virtualHubLinearGlsk, preventiveCnec, Side.LEFT)).thenReturn(-1.2);
        Mockito.when(systematicSensitivityResult.getSensitivityOnFlow(classicLinearGlsk, cnecAfterClassicContingency, Side.RIGHT)).thenReturn(-1.8);
        Mockito.when(systematicSensitivityResult.getSensitivityOnFlow(virtualHubLinearGlsk, cnecAfterClassicContingency, Side.RIGHT)).thenReturn(2.3);
        Mockito.when(systematicSensitivityResult.getSensitivityOnFlow(classicLinearGlsk, cnecAfterDanglingContingency, Side.LEFT)).thenReturn(1.5);
        Mockito.when(systematicSensitivityResult.getSensitivityOnFlow(virtualHubLinearGlsk, cnecAfterDanglingContingency, Side.LEFT)).thenReturn(4.2);

        Mockito.when(classicLinearGlsk.getVariablesById()).thenReturn(Map.of("gen", new WeightedSensitivityVariable("gen", 10f)));
        Mockito.when(virtualHubLinearGlsk.getVariablesById()).thenReturn(Map.of("load", new WeightedSensitivityVariable("load", 10f)));

        EICode frCode = new EICode("FR--------------");
        EICode alegroCode = new EICode("Alegro----------");
        Mockito.when(referenceProgram.getGlobalNetPosition(frCode)).thenReturn(2000.);
        Mockito.when(referenceProgram.getGlobalNetPosition(alegroCode)).thenReturn(600.);
        Mockito.when(referenceProgram.getListOfAreas()).thenReturn(Set.of(frCode, alegroCode));
        Mockito.when(glsk.getData("FR--------------")).thenReturn(classicLinearGlsk);
        Mockito.when(glsk.getData("Alegro----------")).thenReturn(virtualHubLinearGlsk);

        LoopFlowComputation loopFlowComputation = new LoopFlowComputationWithXnodeGlskHandler(
                glsk,
                referenceProgram,
                xnodeGlskHandler
        );

        LoopFlowResult loopFlowResult = loopFlowComputation.buildLoopFlowsFromReferenceFlowAndPtdf(
                systematicSensitivityResult,
                Set.of(preventiveCnec, cnecAfterClassicContingency, cnecAfterDanglingContingency),
                network
        );

        assertEquals(2000. * 0.5 + 600. * (-1.2), loopFlowResult.getCommercialFlow(preventiveCnec, Side.LEFT), DOUBLE_TOLERANCE);
        assertEquals(2000. * (-1.8) + 600. * 2.3, loopFlowResult.getCommercialFlow(cnecAfterClassicContingency, Side.RIGHT), DOUBLE_TOLERANCE);
        assertEquals(2000. * 1.5, loopFlowResult.getCommercialFlow(cnecAfterDanglingContingency, Side.LEFT), DOUBLE_TOLERANCE);
    }
}
