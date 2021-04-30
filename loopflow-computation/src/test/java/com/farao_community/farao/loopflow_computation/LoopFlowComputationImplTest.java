/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.loopflow_computation;

import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_loopflow_extension.LoopFlowThresholdImpl;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static junit.framework.TestCase.assertEquals;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class LoopFlowComputationImplTest {

    private static final double DOUBLE_TOLERANCE = 0.1;
    private Crac crac;

    @Before
    public void setUp() {
        crac = ExampleGenerator.crac();

        LoopFlowThresholdImpl loopFlowExtensionMock = Mockito.mock(LoopFlowThresholdImpl.class);
        crac.getBranchCnec("FR-BE1").addExtension(LoopFlowThresholdImpl.class, loopFlowExtensionMock);
        crac.getBranchCnec("BE1-BE2").addExtension(LoopFlowThresholdImpl.class, loopFlowExtensionMock);
        crac.getBranchCnec("BE2-NL").addExtension(LoopFlowThresholdImpl.class, loopFlowExtensionMock);
        crac.getBranchCnec("FR-DE").addExtension(LoopFlowThresholdImpl.class, loopFlowExtensionMock);
        crac.getBranchCnec("DE-NL").addExtension(LoopFlowThresholdImpl.class, loopFlowExtensionMock);
    }

    @Test
    public void calculateLoopFlowTest() {
        ZonalData<LinearGlsk> glsk = ExampleGenerator.glskProvider();
        ReferenceProgram referenceProgram = ExampleGenerator.referenceProgram();
        SystematicSensitivityResult ptdfsAndFlows = ExampleGenerator.systematicSensitivityResult(crac, glsk);

        LoopFlowComputation loopFlowComputation = new LoopFlowComputationImpl(glsk, referenceProgram);
        LoopFlowResult loopFlowResult = loopFlowComputation.buildLoopFlowsFromReferenceFlowAndPtdf(ptdfsAndFlows, crac.getBranchCnecs());

        assertEquals(-50., loopFlowResult.getLoopFlow(crac.getBranchCnec("FR-BE1")), DOUBLE_TOLERANCE);
        assertEquals(200., loopFlowResult.getLoopFlow(crac.getBranchCnec("BE1-BE2")), DOUBLE_TOLERANCE);
        assertEquals(-50., loopFlowResult.getLoopFlow(crac.getBranchCnec("BE2-NL")), DOUBLE_TOLERANCE);
        assertEquals(50., loopFlowResult.getLoopFlow(crac.getBranchCnec("FR-DE")), DOUBLE_TOLERANCE);
        assertEquals(50., loopFlowResult.getLoopFlow(crac.getBranchCnec("DE-NL")), DOUBLE_TOLERANCE);

        assertEquals(80, loopFlowResult.getCommercialFlow(crac.getBranchCnec("FR-BE1")), DOUBLE_TOLERANCE);
        assertEquals(80., loopFlowResult.getCommercialFlow(crac.getBranchCnec("BE1-BE2")), DOUBLE_TOLERANCE);
        assertEquals(80, loopFlowResult.getCommercialFlow(crac.getBranchCnec("BE2-NL")), DOUBLE_TOLERANCE);
        assertEquals(120., loopFlowResult.getCommercialFlow(crac.getBranchCnec("FR-DE")), DOUBLE_TOLERANCE);
        assertEquals(120., loopFlowResult.getCommercialFlow(crac.getBranchCnec("DE-NL")), DOUBLE_TOLERANCE);

        assertEquals(30., loopFlowResult.getReferenceFlow(crac.getBranchCnec("FR-BE1")), DOUBLE_TOLERANCE);
        assertEquals(280., loopFlowResult.getReferenceFlow(crac.getBranchCnec("BE1-BE2")), DOUBLE_TOLERANCE);
        assertEquals(30., loopFlowResult.getReferenceFlow(crac.getBranchCnec("BE2-NL")), DOUBLE_TOLERANCE);
        assertEquals(170., loopFlowResult.getReferenceFlow(crac.getBranchCnec("FR-DE")), DOUBLE_TOLERANCE);
        assertEquals(170., loopFlowResult.getReferenceFlow(crac.getBranchCnec("DE-NL")), DOUBLE_TOLERANCE);
    }
}
