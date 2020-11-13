/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.loopflow_computation;

import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
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
public class LoopFlowComputationTest {

    private static final double DOUBLE_TOLERANCE = 0.1;
    private Crac crac;

    @Before
    public void setUp() {
        crac = ExampleGenerator.crac();

        CnecLoopFlowExtension loopFlowExtensionMock = Mockito.mock(CnecLoopFlowExtension.class);
        crac.getCnec("FR-BE1").addExtension(CnecLoopFlowExtension.class, loopFlowExtensionMock);
        crac.getCnec("BE1-BE2").addExtension(CnecLoopFlowExtension.class, loopFlowExtensionMock);
        crac.getCnec("BE2-NL").addExtension(CnecLoopFlowExtension.class, loopFlowExtensionMock);
        crac.getCnec("FR-DE").addExtension(CnecLoopFlowExtension.class, loopFlowExtensionMock);
        crac.getCnec("DE-NL").addExtension(CnecLoopFlowExtension.class, loopFlowExtensionMock);
    }

    @Test
    public void calculateLoopFlowTest() {
        ZonalData<LinearGlsk> glsk = ExampleGenerator.glskProvider();
        ReferenceProgram referenceProgram = ExampleGenerator.referenceProgram();
        SystematicSensitivityResult ptdfsAndFlows = ExampleGenerator.systematicSensitivityResult(crac, glsk);

        LoopFlowComputation loopFlowComputation = new LoopFlowComputation(glsk, referenceProgram);
        LoopFlowResult loopFlowResult = loopFlowComputation.buildLoopFlowsFromReferenceFlowAndPtdf(ptdfsAndFlows, crac.getCnecs());

        assertEquals(-50., loopFlowResult.getLoopFlow(crac.getCnec("FR-BE1")), DOUBLE_TOLERANCE);
        assertEquals(200., loopFlowResult.getLoopFlow(crac.getCnec("BE1-BE2")), DOUBLE_TOLERANCE);
        assertEquals(-50., loopFlowResult.getLoopFlow(crac.getCnec("BE2-NL")), DOUBLE_TOLERANCE);
        assertEquals(50., loopFlowResult.getLoopFlow(crac.getCnec("FR-DE")), DOUBLE_TOLERANCE);
        assertEquals(50., loopFlowResult.getLoopFlow(crac.getCnec("DE-NL")), DOUBLE_TOLERANCE);

        assertEquals(80, loopFlowResult.getCommercialFlow(crac.getCnec("FR-BE1")), DOUBLE_TOLERANCE);
        assertEquals(80., loopFlowResult.getCommercialFlow(crac.getCnec("BE1-BE2")), DOUBLE_TOLERANCE);
        assertEquals(80, loopFlowResult.getCommercialFlow(crac.getCnec("BE2-NL")), DOUBLE_TOLERANCE);
        assertEquals(120., loopFlowResult.getCommercialFlow(crac.getCnec("FR-DE")), DOUBLE_TOLERANCE);
        assertEquals(120., loopFlowResult.getCommercialFlow(crac.getCnec("DE-NL")), DOUBLE_TOLERANCE);

        assertEquals(30., loopFlowResult.getReferenceFlow(crac.getCnec("FR-BE1")), DOUBLE_TOLERANCE);
        assertEquals(280., loopFlowResult.getReferenceFlow(crac.getCnec("BE1-BE2")), DOUBLE_TOLERANCE);
        assertEquals(30., loopFlowResult.getReferenceFlow(crac.getCnec("BE2-NL")), DOUBLE_TOLERANCE);
        assertEquals(170., loopFlowResult.getReferenceFlow(crac.getCnec("FR-DE")), DOUBLE_TOLERANCE);
        assertEquals(170., loopFlowResult.getReferenceFlow(crac.getCnec("DE-NL")), DOUBLE_TOLERANCE);
    }
}
