/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.loopflow_computation;

import com.farao_community.farao.commons.EICode;
import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Set;

import static junit.framework.TestCase.assertEquals;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({LoopFlowComputationImpl.class})
public class LoopFlowComputationWithXnodeGlskHandlerTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    @Test
    public void testCommercialFlowsWithCnecAfterDanglingLineContingency() {
        PowerMockito.mockStatic(LoopFlowComputationImpl.class);
        Mockito.when(LoopFlowComputationImpl.isInMainComponent(Mockito.any(), Mockito.any()))
                .thenAnswer(invocationOnMock -> true);

        ZonalData<LinearGlsk> glsk = Mockito.mock(ZonalData.class);
        ReferenceProgram referenceProgram = Mockito.mock(ReferenceProgram.class);
        XnodeGlskHandler xnodeGlskHandler = Mockito.mock(XnodeGlskHandler.class);

        LoopFlowComputation loopFlowComputation = new LoopFlowComputationWithXnodeGlskHandler(
                glsk,
                referenceProgram,
                xnodeGlskHandler
        );

        FlowCnec preventiveCnec = Mockito.mock(FlowCnec.class);
        FlowCnec cnecAfterClassicContingency = Mockito.mock(FlowCnec.class);
        FlowCnec cnecAfterDanglingContingency = Mockito.mock(FlowCnec.class);
        LinearGlsk classicLinearGlsk = Mockito.mock(LinearGlsk.class);
        LinearGlsk virtualHubLinearGlsk = Mockito.mock(LinearGlsk.class);
        Mockito.when(xnodeGlskHandler.isLinearGlskValidForCnec(preventiveCnec, classicLinearGlsk)).thenReturn(true);
        Mockito.when(xnodeGlskHandler.isLinearGlskValidForCnec(preventiveCnec, virtualHubLinearGlsk)).thenReturn(true);
        Mockito.when(xnodeGlskHandler.isLinearGlskValidForCnec(cnecAfterClassicContingency, classicLinearGlsk)).thenReturn(true);
        Mockito.when(xnodeGlskHandler.isLinearGlskValidForCnec(cnecAfterClassicContingency, virtualHubLinearGlsk)).thenReturn(true);
        Mockito.when(xnodeGlskHandler.isLinearGlskValidForCnec(cnecAfterDanglingContingency, classicLinearGlsk)).thenReturn(true);
        Mockito.when(xnodeGlskHandler.isLinearGlskValidForCnec(cnecAfterDanglingContingency, virtualHubLinearGlsk)).thenReturn(false);

        SystematicSensitivityResult systematicSensitivityResult = Mockito.mock(SystematicSensitivityResult.class);
        Mockito.when(systematicSensitivityResult.getSensitivityOnFlow(classicLinearGlsk, preventiveCnec)).thenReturn(0.5);
        Mockito.when(systematicSensitivityResult.getSensitivityOnFlow(virtualHubLinearGlsk, preventiveCnec)).thenReturn(-1.2);
        Mockito.when(systematicSensitivityResult.getSensitivityOnFlow(classicLinearGlsk, cnecAfterClassicContingency)).thenReturn(-1.8);
        Mockito.when(systematicSensitivityResult.getSensitivityOnFlow(virtualHubLinearGlsk, cnecAfterClassicContingency)).thenReturn(2.3);
        Mockito.when(systematicSensitivityResult.getSensitivityOnFlow(classicLinearGlsk, cnecAfterDanglingContingency)).thenReturn(1.5);
        Mockito.when(systematicSensitivityResult.getSensitivityOnFlow(virtualHubLinearGlsk, cnecAfterDanglingContingency)).thenReturn(4.2);

        EICode frCode = new EICode("FR--------------");
        EICode alegroCode = new EICode("Alegro----------");
        Mockito.when(referenceProgram.getGlobalNetPosition(frCode)).thenReturn(2000.);
        Mockito.when(referenceProgram.getGlobalNetPosition(alegroCode)).thenReturn(600.);
        Mockito.when(referenceProgram.getListOfAreas()).thenReturn(Set.of(frCode, alegroCode));
        Mockito.when(glsk.getData("FR--------------")).thenReturn(classicLinearGlsk);
        Mockito.when(glsk.getData("Alegro----------")).thenReturn(virtualHubLinearGlsk);

        LoopFlowResult loopFlowResult = loopFlowComputation.buildLoopFlowsFromReferenceFlowAndPtdf(
                systematicSensitivityResult,
                Set.of(preventiveCnec, cnecAfterClassicContingency, cnecAfterDanglingContingency)
        );

        assertEquals(2000. * 0.5 + 600. * (-1.2), loopFlowResult.getCommercialFlow(preventiveCnec), DOUBLE_TOLERANCE);
        assertEquals(2000. * (-1.8) + 600. * 2.3, loopFlowResult.getCommercialFlow(cnecAfterClassicContingency), DOUBLE_TOLERANCE);
        assertEquals(2000. * 1.5, loopFlowResult.getCommercialFlow(cnecAfterDanglingContingency), DOUBLE_TOLERANCE);
    }
}
