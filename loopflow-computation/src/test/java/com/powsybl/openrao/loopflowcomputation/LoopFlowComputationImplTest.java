/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.loopflowcomputation;

import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.loopflowextension.LoopFlowThresholdImpl;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceProgram;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityResult;
import com.powsybl.sensitivity.SensitivityAnalysisParameters;
import com.powsybl.sensitivity.SensitivityVariableSet;
import com.powsybl.sensitivity.WeightedSensitivityVariable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class LoopFlowComputationImplTest {

    private static final double DOUBLE_TOLERANCE = 0.1;
    private Crac crac;

    @BeforeEach
    public void setUp() {
        crac = ExampleGenerator.crac();

        LoopFlowThresholdImpl loopFlowExtensionMock = Mockito.mock(LoopFlowThresholdImpl.class);
        crac.getFlowCnec("FR-BE1").addExtension(LoopFlowThresholdImpl.class, loopFlowExtensionMock);
        crac.getFlowCnec("BE1-BE2").addExtension(LoopFlowThresholdImpl.class, loopFlowExtensionMock);
        crac.getFlowCnec("BE2-NL").addExtension(LoopFlowThresholdImpl.class, loopFlowExtensionMock);
        crac.getFlowCnec("FR-DE").addExtension(LoopFlowThresholdImpl.class, loopFlowExtensionMock);
        crac.getFlowCnec("DE-NL").addExtension(LoopFlowThresholdImpl.class, loopFlowExtensionMock);
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
    void calculateLoopFlowTest() {
        ZonalData<SensitivityVariableSet> glsk = ExampleGenerator.glskProvider();
        ReferenceProgram referenceProgram = ExampleGenerator.referenceProgram();
        SystematicSensitivityResult ptdfsAndFlows = ExampleGenerator.systematicSensitivityResult(crac, glsk);

        Network network = Mockito.mock(Network.class);
        Generator gen = Mockito.mock(Generator.class);
        Load load = Mockito.mock(Load.class);
        Mockito.when(network.getGenerator(any())).thenReturn(gen);
        Mockito.when(network.getLoad(any())).thenReturn(load);
        Mockito.doReturn(mockInjection(true)).when(gen).getTerminal();
        Mockito.doReturn(mockInjection(true)).when(load).getTerminal();

        LoopFlowComputation loopFlowComputation = new LoopFlowComputationImpl(glsk, referenceProgram, Unit.MEGAWATT);
        LoopFlowResult loopFlowResult = loopFlowComputation.buildLoopFlowsFromReferenceFlowAndPtdf(ptdfsAndFlows, crac.getFlowCnecs(), network);

        assertEquals(-50., loopFlowResult.getLoopFlow(crac.getFlowCnec("FR-BE1"), TwoSides.ONE, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(200., loopFlowResult.getLoopFlow(crac.getFlowCnec("BE1-BE2"), TwoSides.ONE, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-50., loopFlowResult.getLoopFlow(crac.getFlowCnec("BE2-NL"), TwoSides.ONE, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(50., loopFlowResult.getLoopFlow(crac.getFlowCnec("FR-DE"), TwoSides.TWO, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(50., loopFlowResult.getLoopFlow(crac.getFlowCnec("DE-NL"), TwoSides.TWO, Unit.MEGAWATT), DOUBLE_TOLERANCE);

        assertEquals(80, loopFlowResult.getCommercialFlow(crac.getFlowCnec("FR-BE1"), TwoSides.ONE, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(80., loopFlowResult.getCommercialFlow(crac.getFlowCnec("BE1-BE2"), TwoSides.ONE, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(80, loopFlowResult.getCommercialFlow(crac.getFlowCnec("BE2-NL"), TwoSides.ONE, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(120., loopFlowResult.getCommercialFlow(crac.getFlowCnec("FR-DE"), TwoSides.TWO, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(120., loopFlowResult.getCommercialFlow(crac.getFlowCnec("DE-NL"), TwoSides.TWO, Unit.MEGAWATT), DOUBLE_TOLERANCE);

        assertEquals(30., loopFlowResult.getReferenceFlow(crac.getFlowCnec("FR-BE1"), TwoSides.ONE, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(280., loopFlowResult.getReferenceFlow(crac.getFlowCnec("BE1-BE2"), TwoSides.ONE, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(30., loopFlowResult.getReferenceFlow(crac.getFlowCnec("BE2-NL"), TwoSides.ONE, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(170., loopFlowResult.getReferenceFlow(crac.getFlowCnec("FR-DE"), TwoSides.TWO, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(170., loopFlowResult.getReferenceFlow(crac.getFlowCnec("DE-NL"), TwoSides.TWO, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    void testIsInMainComponent() {
        SensitivityVariableSet linearGlsk = Mockito.mock(SensitivityVariableSet.class);
        Network network = Mockito.mock(Network.class);

        Mockito.doReturn(Map.of("gen1", new WeightedSensitivityVariable("gen1", 5f))).when(linearGlsk).getVariablesById();
        Mockito.doReturn(null).when(network).getGenerator("gen1");
        Mockito.doReturn(null).when(network).getLoad("gen1");
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> LoopFlowComputationImpl.isInMainComponent(linearGlsk, network));
        assertEquals("gen1 is neither a generator nor a load nor a dangling line in the network. It is not a valid GLSK.", exception.getMessage());

        Mockito.doReturn(Map.of(
            "gen1", new WeightedSensitivityVariable("gen1", 5f),
            "load1", new WeightedSensitivityVariable("load1", 6f),
            "dl1", new WeightedSensitivityVariable("dl1", 6f)))
            .when(linearGlsk).getVariablesById();
        Generator gen1 = Mockito.mock(Generator.class);
        Load load1 = Mockito.mock(Load.class);
        DanglingLine dl1 = Mockito.mock(DanglingLine.class);
        Mockito.doReturn(gen1).when(network).getGenerator("gen1");
        Mockito.doReturn(load1).when(network).getLoad("load1");
        Mockito.doReturn(dl1).when(network).getDanglingLine("dl1");

        Mockito.doReturn(mockInjection(false)).when(gen1).getTerminal();
        Mockito.doReturn(mockInjection(false)).when(load1).getTerminal();
        Mockito.doReturn(mockInjection(false)).when(dl1).getTerminal();
        assertFalse(LoopFlowComputationImpl.isInMainComponent(linearGlsk, network));

        Mockito.doReturn(mockInjection(true)).when(gen1).getTerminal();
        Mockito.doReturn(mockInjection(false)).when(load1).getTerminal();
        Mockito.doReturn(mockInjection(false)).when(dl1).getTerminal();
        assertTrue(LoopFlowComputationImpl.isInMainComponent(linearGlsk, network));

        Mockito.doReturn(mockInjection(false)).when(gen1).getTerminal();
        Mockito.doReturn(mockInjection(true)).when(load1).getTerminal();
        Mockito.doReturn(mockInjection(false)).when(dl1).getTerminal();
        assertTrue(LoopFlowComputationImpl.isInMainComponent(linearGlsk, network));

        Mockito.doReturn(mockInjection(false)).when(gen1).getTerminal();
        Mockito.doReturn(mockInjection(false)).when(load1).getTerminal();
        Mockito.doReturn(mockInjection(true)).when(dl1).getTerminal();
        assertTrue(LoopFlowComputationImpl.isInMainComponent(linearGlsk, network));

        Mockito.doReturn(mockInjection(false)).when(gen1).getTerminal();
        Mockito.doReturn(mockInjection(true)).when(load1).getTerminal();
        Mockito.doReturn(mockInjection(true)).when(dl1).getTerminal();
        assertTrue(LoopFlowComputationImpl.isInMainComponent(linearGlsk, network));

        Mockito.doReturn(null).when(network).getGenerator("gen1");
        Mockito.doReturn(null).when(network).getLoad("gen1");
        Mockito.doReturn(null).when(network).getDanglingLine("gen1");
        Mockito.doReturn(null).when(network).getGenerator("load1");
        Mockito.doReturn(null).when(network).getLoad("load1");
        Mockito.doReturn(null).when(network).getDanglingLine("load1");
        Mockito.doReturn(null).when(network).getGenerator("dl1");
        Mockito.doReturn(null).when(network).getLoad("dl1");
        Mockito.doReturn(dl1).when(network).getDanglingLine("dl1");
        exception = assertThrows(OpenRaoException.class, () -> LoopFlowComputationImpl.isInMainComponent(linearGlsk, network));
        String message = exception.getMessage();
        assertTrue(message.contains(" is neither a generator nor a load nor a dangling line in the network. It is not a valid GLSK."));
        assertTrue(message.contains("gen1") || message.contains("load1"));
    }

    @Test
    void testIsInMainComponentNullBus() {
        SensitivityVariableSet linearGlsk = Mockito.mock(SensitivityVariableSet.class);
        Network network = Mockito.mock(Network.class);

        Terminal.BusView busView = Mockito.mock(Terminal.BusView.class);
        Mockito.doReturn(null).when(busView).getBus();
        Terminal terminal = Mockito.mock(Terminal.class);
        Mockito.doReturn(busView).when(terminal).getBusView();

        Mockito.doReturn(Set.of(
            new WeightedSensitivityVariable("gen1", 5f),
            new WeightedSensitivityVariable("load1", 6f)))
            .when(linearGlsk).getVariables();
        Generator gen1 = Mockito.mock(Generator.class);
        Load load1 = Mockito.mock(Load.class);
        Mockito.doReturn(gen1).when(network).getGenerator("gen1");
        Mockito.doReturn(load1).when(network).getLoad("load1");

        Mockito.doReturn(terminal).when(gen1).getTerminal();
        Mockito.doReturn(mockInjection(false)).when(load1).getTerminal();
        assertFalse(LoopFlowComputationImpl.isInMainComponent(linearGlsk, network));

        Mockito.doReturn(mockInjection(false)).when(gen1).getTerminal();
        Mockito.doReturn(terminal).when(load1).getTerminal();
        assertFalse(LoopFlowComputationImpl.isInMainComponent(linearGlsk, network));
    }

    @Test
    void testComputeLoopFlowsWithIsolatedGlsk() {
        ZonalData<SensitivityVariableSet> glsk = ExampleGenerator.glskProvider();
        ReferenceProgram referenceProgram = ExampleGenerator.referenceProgram();
        SystematicSensitivityResult ptdfsAndFlows = ExampleGenerator.systematicSensitivityResult(crac, glsk);

        Network network = Mockito.mock(Network.class);

        Generator genDe = Mockito.mock(Generator.class);
        Generator genNl = Mockito.mock(Generator.class);
        Generator genBe = Mockito.mock(Generator.class);
        Load loadFr = Mockito.mock(Load.class);
        Load loadBe = Mockito.mock(Load.class);
        DanglingLine danglingLine = Mockito.mock(DanglingLine.class);

        Mockito.when(network.getGenerator("Generator DE")).thenReturn(genDe);
        Mockito.when(network.getGenerator("Generator NL")).thenReturn(genNl);
        Mockito.when(network.getGenerator("Generator FR")).thenReturn(null);
        Mockito.when(network.getGenerator("Generator BE 1")).thenReturn(null);
        Mockito.when(network.getGenerator("Generator BE 2")).thenReturn(genBe);
        Mockito.when(network.getLoad("Generator DE")).thenReturn(null);
        Mockito.when(network.getLoad("Generator NL")).thenReturn(null);
        Mockito.when(network.getLoad("Generator FR")).thenReturn(loadFr);
        Mockito.when(network.getLoad("Generator BE 1")).thenReturn(loadBe);
        Mockito.when(network.getLoad("Generator BE 2")).thenReturn(null);
        Mockito.when(network.getDanglingLine("BE1-XBE")).thenReturn(danglingLine);

        Mockito.doReturn(mockInjection(true)).when(genDe).getTerminal();
        Mockito.doReturn(mockInjection(false)).when(genNl).getTerminal();
        Mockito.doReturn(mockInjection(false)).when(genBe).getTerminal();
        Mockito.doReturn(mockInjection(true)).when(loadFr).getTerminal();
        Mockito.doReturn(mockInjection(false)).when(loadBe).getTerminal();
        Mockito.doReturn(mockInjection(false)).when(danglingLine).getTerminal();

        LoopFlowComputation loopFlowComputation = new LoopFlowComputationImpl(glsk, referenceProgram, Unit.MEGAWATT);
        LoopFlowResult loopFlowResult = loopFlowComputation.buildLoopFlowsFromReferenceFlowAndPtdf(ptdfsAndFlows, crac.getFlowCnecs(), network);

        assertEquals(30., loopFlowResult.getLoopFlow(crac.getFlowCnec("FR-BE1"), TwoSides.ONE, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(280., loopFlowResult.getLoopFlow(crac.getFlowCnec("BE1-BE2"), TwoSides.ONE, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(30., loopFlowResult.getLoopFlow(crac.getFlowCnec("BE2-NL"), TwoSides.ONE, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(170., loopFlowResult.getLoopFlow(crac.getFlowCnec("FR-DE"), TwoSides.TWO, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(170., loopFlowResult.getLoopFlow(crac.getFlowCnec("DE-NL"), TwoSides.TWO, Unit.MEGAWATT), DOUBLE_TOLERANCE);

        assertEquals(0, loopFlowResult.getCommercialFlow(crac.getFlowCnec("FR-BE1"), TwoSides.ONE, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(0., loopFlowResult.getCommercialFlow(crac.getFlowCnec("BE1-BE2"), TwoSides.ONE, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(0., loopFlowResult.getCommercialFlow(crac.getFlowCnec("BE2-NL"), TwoSides.ONE, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(0., loopFlowResult.getCommercialFlow(crac.getFlowCnec("FR-DE"), TwoSides.TWO, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(0., loopFlowResult.getCommercialFlow(crac.getFlowCnec("DE-NL"), TwoSides.TWO, Unit.MEGAWATT), DOUBLE_TOLERANCE);

        assertEquals(30., loopFlowResult.getReferenceFlow(crac.getFlowCnec("FR-BE1"), TwoSides.ONE, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(280., loopFlowResult.getReferenceFlow(crac.getFlowCnec("BE1-BE2"), TwoSides.ONE, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(30., loopFlowResult.getReferenceFlow(crac.getFlowCnec("BE2-NL"), TwoSides.ONE, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(170., loopFlowResult.getReferenceFlow(crac.getFlowCnec("FR-DE"), TwoSides.TWO, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(170., loopFlowResult.getReferenceFlow(crac.getFlowCnec("DE-NL"), TwoSides.TWO, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    void testCalculateLoopFlows() {
        ZonalData<SensitivityVariableSet> glsk = ExampleGenerator.glskProvider();
        ReferenceProgram referenceProgram = ExampleGenerator.referenceProgram();
        Network network = ExampleGenerator.network();
        SensitivityAnalysisParameters sensitivityAnalysisParameters = new SensitivityAnalysisParameters();
        sensitivityAnalysisParameters.getLoadFlowParameters().setDc(true);
        LoopFlowResult loopFlowResult = new LoopFlowComputationImpl(glsk, referenceProgram, Unit.MEGAWATT)
            .calculateLoopFlows(network, "OpenLoadFlow", sensitivityAnalysisParameters, crac.getFlowCnecs(), crac.getOutageInstant());

        assertEquals(-20., loopFlowResult.getLoopFlow(crac.getFlowCnec("FR-BE1"), TwoSides.ONE, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(80., loopFlowResult.getLoopFlow(crac.getFlowCnec("BE1-BE2"), TwoSides.ONE, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-20., loopFlowResult.getLoopFlow(crac.getFlowCnec("BE2-NL"), TwoSides.ONE, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(20., loopFlowResult.getLoopFlow(crac.getFlowCnec("FR-DE"), TwoSides.TWO, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(20., loopFlowResult.getLoopFlow(crac.getFlowCnec("DE-NL"), TwoSides.TWO, Unit.MEGAWATT), DOUBLE_TOLERANCE);

        assertEquals(40., loopFlowResult.getCommercialFlow(crac.getFlowCnec("FR-BE1"), TwoSides.ONE, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(40., loopFlowResult.getCommercialFlow(crac.getFlowCnec("BE1-BE2"), TwoSides.ONE, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(40., loopFlowResult.getCommercialFlow(crac.getFlowCnec("BE2-NL"), TwoSides.ONE, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(60., loopFlowResult.getCommercialFlow(crac.getFlowCnec("FR-DE"), TwoSides.TWO, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(60., loopFlowResult.getCommercialFlow(crac.getFlowCnec("DE-NL"), TwoSides.TWO, Unit.MEGAWATT), DOUBLE_TOLERANCE);

        assertEquals(20., loopFlowResult.getReferenceFlow(crac.getFlowCnec("FR-BE1"), TwoSides.ONE, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(120., loopFlowResult.getReferenceFlow(crac.getFlowCnec("BE1-BE2"), TwoSides.ONE, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(20., loopFlowResult.getReferenceFlow(crac.getFlowCnec("BE2-NL"), TwoSides.ONE, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(80., loopFlowResult.getReferenceFlow(crac.getFlowCnec("FR-DE"), TwoSides.TWO, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(80., loopFlowResult.getReferenceFlow(crac.getFlowCnec("DE-NL"), TwoSides.TWO, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    void testCalculateLoopFlowsInAmpere() {
        // Same test as testCalculateLoopFlows pu all the computation is done in ampere
        // We expect to find the same result as before divided by (Unom * sqrt(3))/1000
        ZonalData<SensitivityVariableSet> glsk = ExampleGenerator.glskProvider();
        ReferenceProgram referenceProgram = ExampleGenerator.referenceProgram();
        Network network = ExampleGenerator.network();
        SensitivityAnalysisParameters sensitivityAnalysisParameters = new SensitivityAnalysisParameters();
        sensitivityAnalysisParameters.getLoadFlowParameters().setDc(false);
        LoopFlowResult loopFlowResult = new LoopFlowComputationImpl(glsk, referenceProgram, Unit.AMPERE)
            .calculateLoopFlows(network, "OpenLoadFlow", sensitivityAnalysisParameters, crac.getFlowCnecs(), crac.getOutageInstant());
        double factorMwToA = 1000. / (Math.sqrt(3.) * 400.); //all cnecs' nominal voltage is 400MW
        assertEquals(-20. * factorMwToA, loopFlowResult.getLoopFlow(crac.getFlowCnec("FR-BE1"), TwoSides.ONE, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(80. * factorMwToA, loopFlowResult.getLoopFlow(crac.getFlowCnec("BE1-BE2"), TwoSides.ONE, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(-20. * factorMwToA, loopFlowResult.getLoopFlow(crac.getFlowCnec("BE2-NL"), TwoSides.ONE, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(20. * factorMwToA, loopFlowResult.getLoopFlow(crac.getFlowCnec("FR-DE"), TwoSides.TWO, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(20. * factorMwToA, loopFlowResult.getLoopFlow(crac.getFlowCnec("DE-NL"), TwoSides.TWO, Unit.AMPERE), DOUBLE_TOLERANCE);

        assertEquals(40. * factorMwToA, loopFlowResult.getCommercialFlow(crac.getFlowCnec("FR-BE1"), TwoSides.ONE, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(40. * factorMwToA, loopFlowResult.getCommercialFlow(crac.getFlowCnec("BE1-BE2"), TwoSides.ONE, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(40. * factorMwToA, loopFlowResult.getCommercialFlow(crac.getFlowCnec("BE2-NL"), TwoSides.ONE, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(60. * factorMwToA, loopFlowResult.getCommercialFlow(crac.getFlowCnec("FR-DE"), TwoSides.TWO, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(60. * factorMwToA, loopFlowResult.getCommercialFlow(crac.getFlowCnec("DE-NL"), TwoSides.TWO, Unit.AMPERE), DOUBLE_TOLERANCE);

        assertEquals(20. * factorMwToA, loopFlowResult.getReferenceFlow(crac.getFlowCnec("FR-BE1"), TwoSides.ONE, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(120. * factorMwToA, loopFlowResult.getReferenceFlow(crac.getFlowCnec("BE1-BE2"), TwoSides.ONE, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(20. * factorMwToA, loopFlowResult.getReferenceFlow(crac.getFlowCnec("BE2-NL"), TwoSides.ONE, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(80. * factorMwToA, loopFlowResult.getReferenceFlow(crac.getFlowCnec("FR-DE"), TwoSides.TWO, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(80. * factorMwToA, loopFlowResult.getReferenceFlow(crac.getFlowCnec("DE-NL"), TwoSides.TWO, Unit.AMPERE), DOUBLE_TOLERANCE);

        // if we try to get result in unit other than flow unit => return null
        Exception e1 = assertThrows(OpenRaoException.class, () -> loopFlowResult.getLoopFlow(crac.getFlowCnec("DE-NL"), TwoSides.TWO, Unit.MEGAWATT));
        assertEquals("No loop-flow value found for cnec DE-NL on side TWO in MW", e1.getMessage());
        Exception e2 = assertThrows(OpenRaoException.class, () -> loopFlowResult.getCommercialFlow(crac.getFlowCnec("DE-NL"), TwoSides.TWO, Unit.MEGAWATT));
        assertEquals("No commercial flow value found for cnec DE-NL on side TWO in MW", e2.getMessage());
        Exception e3 = assertThrows(OpenRaoException.class, () -> loopFlowResult.getReferenceFlow(crac.getFlowCnec("DE-NL"), TwoSides.TWO, Unit.MEGAWATT));
        assertEquals("No reference flow value found for cnec DE-NL on side TWO in MW", e3.getMessage());
    }
}
