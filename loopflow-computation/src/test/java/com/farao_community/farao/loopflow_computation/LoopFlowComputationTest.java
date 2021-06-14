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
import com.powsybl.iidm.network.*;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Map;

import static junit.framework.Assert.assertFalse;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

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
        crac.getBranchCnec("FR-BE1").addExtension(CnecLoopFlowExtension.class, loopFlowExtensionMock);
        crac.getBranchCnec("BE1-BE2").addExtension(CnecLoopFlowExtension.class, loopFlowExtensionMock);
        crac.getBranchCnec("BE2-NL").addExtension(CnecLoopFlowExtension.class, loopFlowExtensionMock);
        crac.getBranchCnec("FR-DE").addExtension(CnecLoopFlowExtension.class, loopFlowExtensionMock);
        crac.getBranchCnec("DE-NL").addExtension(CnecLoopFlowExtension.class, loopFlowExtensionMock);
    }

    @Test
    public void calculateLoopFlowTest() {
        Network network = ExampleGenerator.network();
        ZonalData<LinearGlsk> glsk = ExampleGenerator.glskProvider();
        ReferenceProgram referenceProgram = ExampleGenerator.referenceProgram();
        SystematicSensitivityResult ptdfsAndFlows = ExampleGenerator.systematicSensitivityResult(crac, glsk);

        LoopFlowComputation loopFlowComputation = new LoopFlowComputation(glsk, referenceProgram);
        LoopFlowResult loopFlowResult = loopFlowComputation.buildLoopFlowsFromReferenceFlowAndPtdf(network, ptdfsAndFlows, crac.getBranchCnecs());

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

    @Test
    public void testIsInMainComponent() {
        LinearGlsk linearGlsk = Mockito.mock(LinearGlsk.class);
        Map<String, Float> map = Map.of("gen1", 5f,"load1", 6f, "load2", 6f);
        Mockito.doReturn(map).when(linearGlsk).getGLSKs();

        Network network = Mockito.mock(Network.class);

        Mockito.doReturn(null).when(network).getGenerator("gen1");
        Mockito.doReturn(null).when(network).getLoad("load1");
        Mockito.doReturn(null).when(network).getLoad("load2");

        assertFalse(LoopFlowComputation.isInMainComponent(linearGlsk, network));

        Generator gen1 = Mockito.mock(Generator.class);
        Load load1 = Mockito.mock(Load.class);
        Load load2 = Mockito.mock(Load.class);
        Mockito.doReturn(gen1).when(network).getGenerator("gen1");
        Mockito.doReturn(load1).when(network).getLoad("load1");
        Mockito.doReturn(load2).when(network).getLoad("load2");

        Mockito.doReturn(mockInjection(false)).when(gen1).getTerminal();
        Mockito.doReturn(mockInjection(false)).when(load1).getTerminal();
        Mockito.doReturn(mockInjection(false)).when(load2).getTerminal();
        assertFalse(LoopFlowComputation.isInMainComponent(linearGlsk, network));

        Mockito.doReturn(mockInjection(true)).when(gen1).getTerminal();
        Mockito.doReturn(mockInjection(false)).when(load1).getTerminal();
        Mockito.doReturn(mockInjection(false)).when(load2).getTerminal();
        assertTrue(LoopFlowComputation.isInMainComponent(linearGlsk, network));

        Mockito.doReturn(mockInjection(false)).when(gen1).getTerminal();
        Mockito.doReturn(mockInjection(true)).when(load1).getTerminal();
        Mockito.doReturn(mockInjection(false)).when(load2).getTerminal();
        assertTrue(LoopFlowComputation.isInMainComponent(linearGlsk, network));

        Mockito.doReturn(mockInjection(false)).when(gen1).getTerminal();
        Mockito.doReturn(mockInjection(false)).when(load1).getTerminal();
        Mockito.doReturn(mockInjection(true)).when(load2).getTerminal();
        assertTrue(LoopFlowComputation.isInMainComponent(linearGlsk, network));

        Mockito.doReturn(mockInjection(false)).when(gen1).getTerminal();
        Mockito.doReturn(mockInjection(true)).when(load1).getTerminal();
        Mockito.doReturn(mockInjection(true)).when(load2).getTerminal();
        assertTrue(LoopFlowComputation.isInMainComponent(linearGlsk, network));

        Mockito.doReturn(null).when(network).getGenerator("gen1");
        Mockito.doReturn(null).when(network).getLoad("load1");
        assertTrue(LoopFlowComputation.isInMainComponent(linearGlsk, network));
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
}
