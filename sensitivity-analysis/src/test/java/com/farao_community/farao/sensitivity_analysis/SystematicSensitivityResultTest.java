/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.glsk.ucte.UcteGlskDocument;
import com.powsybl.iidm.network.BusRef;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.*;
import com.powsybl.sensitivity.factors.*;
import com.powsybl.sensitivity.factors.functions.BranchFlow;
import com.powsybl.sensitivity.factors.functions.BranchIntensity;
import com.powsybl.sensitivity.factors.functions.BusVoltage;
import com.powsybl.sensitivity.factors.variables.InjectionIncrease;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import com.powsybl.sensitivity.factors.variables.PhaseTapChangerAngle;
import com.powsybl.sensitivity.factors.variables.TargetVoltage;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class SystematicSensitivityResultTest {
    private static final double EPSILON = 1e-2;

    private Network network;
    private FlowCnec nStateCnec;
    private FlowCnec contingencyCnec;
    private RangeAction rangeAction;
    private LinearGlsk linearGlsk;

    private RangeActionSensitivityProvider rangeActionSensitivityProvider;
    private PtdfSensitivityProvider ptdfSensitivityProvider;

    @Before
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.createWithPreventivePstRange();

        ZonalData<LinearGlsk> glskProvider = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/glsk_proportional_12nodes.xml"))
            .getZonalGlsks(network, Instant.parse("2016-07-28T22:30:00Z"));

        // Ra Provider
        rangeActionSensitivityProvider = new RangeActionSensitivityProvider(crac.getRangeActions(), crac.getFlowCnecs(), Stream.of(Unit.MEGAWATT, Unit.AMPERE).collect(Collectors.toSet()));

        // Ptdf Provider
        ptdfSensitivityProvider = new PtdfSensitivityProvider(glskProvider, crac.getFlowCnecs(), Collections.singleton(Unit.MEGAWATT));

        nStateCnec = crac.getFlowCnec("cnec1basecase");
        rangeAction = crac.getRangeAction("pst");
        contingencyCnec = crac.getFlowCnec("cnec1stateCurativeContingency1");
        linearGlsk = glskProvider.getData("10YFR-RTE------C");
    }

    @Test
    public void testPostTreatIntensities() {
        // When
        SensitivityAnalysisResult sensitivityAnalysisResult = SensitivityAnalysis.run(network, network.getVariantManager().getWorkingVariantId(), rangeActionSensitivityProvider, ptdfSensitivityProvider.getContingencies(network), SensitivityAnalysisParameters.load());
        SystematicSensitivityResult result = new SystematicSensitivityResult().completeData(sensitivityAnalysisResult, network, ptdfSensitivityProvider.getContingencies(network), false);

        // Before postTreating intensities
        assertEquals(-20, result.getReferenceFlow(contingencyCnec), EPSILON);
        assertEquals(200, result.getReferenceIntensity(contingencyCnec), EPSILON);

        // After postTreating intensities
        result.postTreatIntensities();
        assertEquals(-20, result.getReferenceFlow(contingencyCnec), EPSILON);
        assertEquals(-200, result.getReferenceIntensity(contingencyCnec), EPSILON);
    }

    @Test
    public void testPstResultManipulation() {
        // When
        SensitivityAnalysisResult sensitivityAnalysisResult = SensitivityAnalysis.run(network, network.getVariantManager().getWorkingVariantId(), rangeActionSensitivityProvider, rangeActionSensitivityProvider.getContingencies(network), SensitivityAnalysisParameters.load());
        SystematicSensitivityResult result = new SystematicSensitivityResult().completeData(sensitivityAnalysisResult, network, rangeActionSensitivityProvider.getContingencies(network), false).postTreatIntensities();

        // Then
        assertTrue(result.isSuccess());

        //  in basecase
        assertEquals(10, result.getReferenceFlow(nStateCnec), EPSILON);
        assertEquals(25, result.getReferenceIntensity(nStateCnec), EPSILON);
        assertEquals(0.5, result.getSensitivityOnFlow(rangeAction, nStateCnec), EPSILON);
        assertEquals(0.25, result.getSensitivityOnIntensity(rangeAction, nStateCnec), EPSILON);

        //  after contingency
        assertEquals(-20, result.getReferenceFlow(contingencyCnec), EPSILON);
        assertEquals(-200, result.getReferenceIntensity(contingencyCnec), EPSILON);
        assertEquals(-5, result.getSensitivityOnFlow(rangeAction, contingencyCnec), EPSILON);
        assertEquals(-5, result.getSensitivityOnIntensity(rangeAction, contingencyCnec), EPSILON);
    }

    @Test
    public void testPtdfResultManipulation() {
        // When
        SensitivityAnalysisResult sensitivityAnalysisResult = SensitivityAnalysis.run(network, network.getVariantManager().getWorkingVariantId(), ptdfSensitivityProvider, ptdfSensitivityProvider.getContingencies(network), SensitivityAnalysisParameters.load());
        SystematicSensitivityResult result = new SystematicSensitivityResult().completeData(sensitivityAnalysisResult, network, ptdfSensitivityProvider.getContingencies(network), false).postTreatIntensities();

        // Then
        assertTrue(result.isSuccess());

        //  in basecase
        assertEquals(10, result.getReferenceFlow(nStateCnec), EPSILON);
        assertEquals(0.140, result.getSensitivityOnFlow(linearGlsk, nStateCnec), EPSILON);

        //  after contingency
        assertEquals(-20, result.getReferenceFlow(contingencyCnec), EPSILON);
        assertEquals(6, result.getSensitivityOnFlow(linearGlsk, contingencyCnec), EPSILON);
    }

    @Test
    public void testNokSensiResult() {
        // When
        SensitivityAnalysisResult sensitivityAnalysisResult = Mockito.mock(SensitivityAnalysisResult.class);
        Mockito.when(sensitivityAnalysisResult.isOk()).thenReturn(false);
        SystematicSensitivityResult result = new SystematicSensitivityResult().completeData(sensitivityAnalysisResult, network, new ArrayList<>(), false).postTreatIntensities();

        // Then
        assertFalse(result.isSuccess());
    }

    @Test
    public void checkIfPstToBranchIntensityIsDisconnectedTest() {
        BranchIntensity funcOnBranch = new BranchIntensity("FFR2AA1  FFR3AA1  1", "FFR2AA1  FFR3AA1  1", "FFR2AA1  FFR3AA1  1");
        PhaseTapChangerAngle varOnPst = new PhaseTapChangerAngle("BBE2AA1  BBE3AA1  1", "BBE2AA1  BBE3AA1  1", "BBE2AA1  BBE3AA1  1");

        SensitivityValue pstOnBranchFlow = new SensitivityValue(
                new BranchIntensityPerPSTAngle(funcOnBranch, varOnPst),
                10., 10., 10.
        );

        // branch and PST connected
        assertFalse(SystematicSensitivityResult.isfFunctionOrVariableDisconnected(pstOnBranchFlow, network));

        // branch disconnected
        network.getBranch("FFR2AA1  FFR3AA1  1").getTerminal1().disconnect();
        assertTrue(SystematicSensitivityResult.isfFunctionOrVariableDisconnected(pstOnBranchFlow, network));

        // pst out of main component
        network = NetworkImportsUtil.import12NodesNetwork();
        network.getBranch("BBE2AA1  FFR3AA1  1").getTerminal1().disconnect();
        network.getBranch("NNL2AA1  BBE3AA1  1").getTerminal2().disconnect();
        assertTrue(SystematicSensitivityResult.isfFunctionOrVariableDisconnected(pstOnBranchFlow, network));
    }

    @Test
    public void checkIfGlskToBranchFlowIsDisconnectedTest() {
        BranchFlow funcOnBranch = new BranchFlow("DDE2AA1  DDE3AA1  1", "DDE2AA1  DDE3AA1  1", "DDE2AA1  DDE3AA1  1");
        LinearGlsk varOnGlsk = new LinearGlsk("10YFR-RTE------C", "10YFR-RTE------C", linearGlsk.getGLSKs());

        SensitivityValue pstOnBranchFlow = new SensitivityValue(
                new BranchFlowPerLinearGlsk(funcOnBranch, varOnGlsk),
                10., 10., 10.
        );

        // branch and GLSK connected
        assertFalse(SystematicSensitivityResult.isfFunctionOrVariableDisconnected(pstOnBranchFlow, network));

        // branch disconnected
        network.getBranch("DDE2AA1  DDE3AA1  1").getTerminal1().disconnect();
        assertTrue(SystematicSensitivityResult.isfFunctionOrVariableDisconnected(pstOnBranchFlow, network));

        // GLSK out of main component
        network = NetworkImportsUtil.import12NodesNetwork();
        network.getBranch("BBE2AA1  FFR3AA1  1").getTerminal2().disconnect();
        network.getBranch("FFR2AA1  DDE3AA1  1").getTerminal1().disconnect();
        assertTrue(SystematicSensitivityResult.isfFunctionOrVariableDisconnected(pstOnBranchFlow, network));
    }

    @Test
    public void uncoveredFunctionAndVariablesTest() {
        BranchFlow funcOnBranch = new BranchFlow("DDE2AA1  DDE3AA1  1", "DDE2AA1  DDE3AA1  1", "DDE2AA1  DDE3AA1  1");
        BusVoltage busVoltage = new BusVoltage("DDE2AA1", "DDE2AA1", Mockito.mock(BusRef.class));
        TargetVoltage targetVoltage = new TargetVoltage("FFR3AA1", "FFR3AA1", "FFR3AA1");
        InjectionIncrease injectionIncrease = new InjectionIncrease("FFR3AA1", "FFR3AA1", "FFR3AA1");

        SensitivityValue sensiOnBusVoltage = new SensitivityValue(
                new BusVoltagePerTargetV(busVoltage, targetVoltage),
                10., 10., 10.
        );

        SensitivityValue sensiOfInjectionIncrease = new SensitivityValue(
                new BranchFlowPerInjectionIncrease(funcOnBranch, injectionIncrease),
                10., 10., 10.
        );

        try {
            SystematicSensitivityResult.isfFunctionOrVariableDisconnected(sensiOnBusVoltage, network);
            fail();
        } catch (NotImplementedException e) {
            // should throw
        }

        try {
            SystematicSensitivityResult.isfFunctionOrVariableDisconnected(sensiOfInjectionIncrease, network);
            fail();
        } catch (NotImplementedException e) {
            // should throw
        }
    }

}
