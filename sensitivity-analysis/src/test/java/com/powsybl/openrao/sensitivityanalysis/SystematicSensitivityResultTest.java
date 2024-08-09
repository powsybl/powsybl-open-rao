/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.sensitivityanalysis;

import com.powsybl.contingency.*;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracapi.CracFactory;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.rangeaction.HvdcRangeAction;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.cracimpl.utils.CommonCracCreation;
import com.powsybl.openrao.data.cracimpl.utils.NetworkImportsUtil;
import com.powsybl.glsk.ucte.UcteGlskDocument;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class SystematicSensitivityResultTest {
    private static final double EPSILON = 1e-2;
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";

    private Network network;
    private FlowCnec nStateCnec;
    private FlowCnec contingencyCnec;
    private RangeAction<?> rangeAction;
    private HvdcRangeAction hvdcRangeAction;
    private SensitivityVariableSet linearGlsk;
    private Crac crac;

    private RangeActionSensitivityProvider rangeActionSensitivityProvider;
    private PtdfSensitivityProvider ptdfSensitivityProvider;
    private int outageInstantOrder;

    public void setUpWith12Nodes() {
        network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.createWithPreventivePstRange(Set.of(TwoSides.ONE, TwoSides.TWO));
        outageInstantOrder = crac.getInstant(CURATIVE_INSTANT_ID).getOrder();

        ZonalData<SensitivityVariableSet> glskProvider = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/glsk_proportional_12nodes.xml"))
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
    void testPostTreatIntensities() {
        setUpWith12Nodes();
        // When
        SensitivityAnalysisResult sensitivityAnalysisResult = SensitivityAnalysis.find().run(network,
            rangeActionSensitivityProvider.getAllFactors(network),
            ptdfSensitivityProvider.getContingencies(network),
            new ArrayList<>(),
            SensitivityAnalysisParameters.load());
        SystematicSensitivityResult result = new SystematicSensitivityResult().completeData(sensitivityAnalysisResult, outageInstantOrder);

        // Before postTreating intensities
        assertEquals(-20, result.getReferenceFlow(contingencyCnec, TwoSides.ONE), EPSILON);
        assertEquals(200, result.getReferenceIntensity(contingencyCnec, TwoSides.ONE), EPSILON);
        assertEquals(25, result.getReferenceFlow(contingencyCnec, TwoSides.TWO), EPSILON);
        assertEquals(205, result.getReferenceIntensity(contingencyCnec, TwoSides.TWO), EPSILON);

        // After postTreating intensities
        result.postTreatIntensities();
        assertEquals(-20, result.getReferenceFlow(contingencyCnec, TwoSides.ONE), EPSILON);
        assertEquals(-200, result.getReferenceIntensity(contingencyCnec, TwoSides.ONE), EPSILON);
        assertEquals(25, result.getReferenceFlow(contingencyCnec, TwoSides.TWO), EPSILON);
        assertEquals(205, result.getReferenceIntensity(contingencyCnec, TwoSides.TWO), EPSILON);
    }

    @Test
    void testPstResultManipulation() {
        setUpWith12Nodes();
        // When
        SensitivityAnalysisResult sensitivityAnalysisResult = SensitivityAnalysis.find().run(network,
            rangeActionSensitivityProvider.getAllFactors(network),
            rangeActionSensitivityProvider.getContingencies(network),
            new ArrayList<>(),
            SensitivityAnalysisParameters.load());
        SystematicSensitivityResult result = new SystematicSensitivityResult().completeData(sensitivityAnalysisResult, outageInstantOrder).postTreatIntensities();

        // Then
        assertTrue(result.isSuccess());
        assertEquals(SystematicSensitivityResult.SensitivityComputationStatus.SUCCESS, result.getStatus());

        //  in basecase
        assertEquals(10, result.getReferenceFlow(nStateCnec, TwoSides.ONE), EPSILON);
        assertEquals(25, result.getReferenceIntensity(nStateCnec, TwoSides.ONE), EPSILON);
        assertEquals(0.5, result.getSensitivityOnFlow(rangeAction, nStateCnec, TwoSides.ONE), EPSILON);
        assertEquals(-15, result.getReferenceFlow(nStateCnec, TwoSides.TWO), EPSILON);
        assertEquals(-30, result.getReferenceIntensity(nStateCnec, TwoSides.TWO), EPSILON);
        assertEquals(-0.55, result.getSensitivityOnFlow(rangeAction, nStateCnec, TwoSides.TWO), EPSILON);

        //  after contingency
        assertEquals(-20, result.getReferenceFlow(contingencyCnec, TwoSides.ONE), EPSILON);
        assertEquals(-200, result.getReferenceIntensity(contingencyCnec, TwoSides.ONE), EPSILON);
        assertEquals(-5, result.getSensitivityOnFlow(rangeAction, contingencyCnec, TwoSides.ONE), EPSILON);
        assertEquals(25, result.getReferenceFlow(contingencyCnec, TwoSides.TWO), EPSILON);
        assertEquals(205, result.getReferenceIntensity(contingencyCnec, TwoSides.TWO), EPSILON);
        assertEquals(5.5, result.getSensitivityOnFlow(rangeAction, contingencyCnec, TwoSides.TWO), EPSILON);
    }

    @Test
    void testPtdfResultManipulation() {
        setUpWith12Nodes();
        // When
        SensitivityAnalysisResult sensitivityAnalysisResult = SensitivityAnalysis.find().run(network,
            ptdfSensitivityProvider.getAllFactors(network),
            ptdfSensitivityProvider.getContingencies(network),
            new ArrayList<>(),
            SensitivityAnalysisParameters.load());
        SystematicSensitivityResult result = new SystematicSensitivityResult().completeData(sensitivityAnalysisResult, outageInstantOrder).postTreatIntensities();

        // Then
        assertTrue(result.isSuccess());

        //  in basecase
        assertEquals(10, result.getReferenceFlow(nStateCnec, TwoSides.ONE), EPSILON);
        assertEquals(0.140, result.getSensitivityOnFlow(linearGlsk, nStateCnec, TwoSides.ONE), EPSILON);
        assertEquals(-15, result.getReferenceFlow(nStateCnec, TwoSides.TWO), EPSILON);
        assertEquals(-0.19, result.getSensitivityOnFlow(linearGlsk, nStateCnec, TwoSides.TWO), EPSILON);

        //  after contingency
        assertEquals(-20, result.getReferenceFlow(contingencyCnec, TwoSides.ONE), EPSILON);
        assertEquals(6, result.getSensitivityOnFlow(linearGlsk, contingencyCnec, TwoSides.ONE), EPSILON);
        assertEquals(25, result.getReferenceFlow(contingencyCnec, TwoSides.TWO), EPSILON);
        assertEquals(-6.5, result.getSensitivityOnFlow(linearGlsk, contingencyCnec, TwoSides.TWO), EPSILON);
    }

    @Test
    void testFailureSensiResult() {
        setUpWith12Nodes();
        // When
        SensitivityAnalysisResult sensitivityAnalysisResult = Mockito.mock(SensitivityAnalysisResult.class);
        SystematicSensitivityResult result = new SystematicSensitivityResult().completeData(sensitivityAnalysisResult, outageInstantOrder).postTreatIntensities();

        // Then
        assertFalse(result.isSuccess());
        assertEquals(SystematicSensitivityResult.SensitivityComputationStatus.FAILURE, result.getStatus());
    }

    private void setUpForHvdc() {
        crac = CracFactory.findDefault().create("test-crac")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT_ID, InstantKind.AUTO)
            .newInstant(CURATIVE_INSTANT_ID, InstantKind.CURATIVE);
        crac.newContingency()
            .withId("co")
            .withContingencyElement("NNL2AA11 BBE3AA11 1", ContingencyElementType.LINE)
            .add();
        nStateCnec = crac.newFlowCnec()
            .withId("cnec-prev")
            .withNetworkElement("BBE1AA11 FFR5AA11 1")
            .withInstant(PREVENTIVE_INSTANT_ID)
            .newThreshold().withMax(1000.).withUnit(Unit.MEGAWATT).withSide(TwoSides.ONE).add()
            .add();
        contingencyCnec = crac.newFlowCnec()
            .withId("cnec-cur")
            .withNetworkElement("BBE1AA11 FFR5AA11 1")
            .withContingency("co")
            .withInstant(OUTAGE_INSTANT_ID)
            .newThreshold().withMax(1000.).withUnit(Unit.MEGAWATT).withSide(TwoSides.TWO).add()
            .add();
        hvdcRangeAction = crac.newHvdcRangeAction()
            .withId("hvdc-ra")
            .withNetworkElement("BBE2AA11 FFR3AA11 1")
            .newRange().withMin(-1000.).withMax(1000.).add()
            .add();

        network = Network.read("TestCase16NodesWithHvdc.xiidm", getClass().getResourceAsStream("/TestCase16NodesWithHvdc.xiidm"));

        rangeActionSensitivityProvider = new RangeActionSensitivityProvider(crac.getRangeActions(), crac.getFlowCnecs(), Stream.of(Unit.MEGAWATT, Unit.AMPERE).collect(Collectors.toSet()));
    }

    @Test
    void testPostTreatHvdcNoEffect() {
        setUpForHvdc();
        Map<String, HvdcRangeAction> hvdcs = Map.of(hvdcRangeAction.getNetworkElement().getId(), hvdcRangeAction);
        SensitivityAnalysisResult sensitivityAnalysisResult = SensitivityAnalysis.find().run(network,
            rangeActionSensitivityProvider.getAllFactors(network),
            rangeActionSensitivityProvider.getContingencies(network),
            new ArrayList<>(),
            SensitivityAnalysisParameters.load());
        SystematicSensitivityResult result = new SystematicSensitivityResult()
            .completeData(sensitivityAnalysisResult, outageInstantOrder)
            .postTreatIntensities()
            .postTreatHvdcs(network, hvdcs);

        assertEquals(30., result.getReferenceFlow(nStateCnec, TwoSides.ONE), EPSILON);
        assertEquals(40., result.getReferenceIntensity(nStateCnec, TwoSides.ONE), EPSILON);
        assertEquals(0.34, result.getSensitivityOnFlow(hvdcRangeAction, nStateCnec, TwoSides.ONE), EPSILON);

        assertEquals(26., result.getReferenceFlow(contingencyCnec, TwoSides.TWO), EPSILON);
        assertEquals(-31., result.getReferenceIntensity(contingencyCnec, TwoSides.TWO), EPSILON);
        assertEquals(-7.5, result.getSensitivityOnFlow(hvdcRangeAction, contingencyCnec, TwoSides.TWO), EPSILON);
    }

    @Test
    void testPostTreatHvdcInvert() {
        setUpForHvdc();
        Map<String, HvdcRangeAction> hvdcs = Map.of(hvdcRangeAction.getNetworkElement().getId(), hvdcRangeAction);
        network.getHvdcLine("BBE2AA11 FFR3AA11 1").setConvertersMode(HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER);
        SensitivityAnalysisResult sensitivityAnalysisResult = SensitivityAnalysis.find().run(network,
            rangeActionSensitivityProvider.getAllFactors(network),
            rangeActionSensitivityProvider.getContingencies(network),
            new ArrayList<>(),
            SensitivityAnalysisParameters.load());
        SystematicSensitivityResult result = new SystematicSensitivityResult()
            .completeData(sensitivityAnalysisResult, outageInstantOrder)
            .postTreatIntensities()
            .postTreatHvdcs(network, hvdcs);

        assertEquals(30., result.getReferenceFlow(nStateCnec, TwoSides.ONE), EPSILON);
        assertEquals(40., result.getReferenceIntensity(nStateCnec, TwoSides.ONE), EPSILON);
        assertEquals(-0.34, result.getSensitivityOnFlow(hvdcRangeAction, nStateCnec, TwoSides.ONE), EPSILON);

        assertEquals(26., result.getReferenceFlow(contingencyCnec, TwoSides.TWO), EPSILON);
        assertEquals(-31., result.getReferenceIntensity(contingencyCnec, TwoSides.TWO), EPSILON);
        assertEquals(7.5, result.getSensitivityOnFlow(hvdcRangeAction, contingencyCnec, TwoSides.TWO), EPSILON);
    }

    @Test
    void testPartialContingencyFailures() {
        setUpWith12Nodes();

        // The contingency points to an element that does not exist in the network => n-1 status should be set to failure
        Contingency contingency = new Contingency("wrong_contingency", new BranchContingency("fake_branch"));
        State contingencyState = Mockito.mock(State.class);
        Mockito.when(contingencyState.getContingency()).thenReturn(Optional.of(contingency));
        Mockito.when(contingencyState.getInstant()).thenReturn(crac.getOutageInstant());

        SensitivityFactor sensitivityFactor1 = new SensitivityFactor(
            SensitivityFunctionType.BRANCH_ACTIVE_POWER_1,
            "BBE2AA1  FFR3AA1  1",
            SensitivityVariableType.TRANSFORMER_PHASE,
            "BBE2AA1  BBE3AA1  1",
            false,
            new ContingencyContext(contingency.getId(), ContingencyContextType.SPECIFIC)
        );
        SensitivityFactor sensitivityFactor2 = new SensitivityFactor(
            SensitivityFunctionType.BRANCH_ACTIVE_POWER_1,
            "BBE2AA1  FFR3AA1  1",
            SensitivityVariableType.TRANSFORMER_PHASE,
            "BBE2AA1  BBE3AA1  1",
            false,
            new ContingencyContext(null, ContingencyContextType.NONE)
        );

        SensitivityAnalysisResult sensitivityAnalysisResult = SensitivityAnalysis.find().run(network,
            List.of(sensitivityFactor1, sensitivityFactor2),
            List.of(contingency),
            new ArrayList<>(),
            SensitivityAnalysisParameters.load());
        SystematicSensitivityResult result = new SystematicSensitivityResult().completeData(sensitivityAnalysisResult, outageInstantOrder).postTreatIntensities();

        // N is in SUCCESS, N-1 in failure
        assertTrue(result.isSuccess());
        assertEquals(SystematicSensitivityResult.SensitivityComputationStatus.PARTIAL_FAILURE, result.getStatus());
        assertEquals(SystematicSensitivityResult.SensitivityComputationStatus.SUCCESS, result.getStatus(crac.getPreventiveState()));
        assertEquals(SystematicSensitivityResult.SensitivityComputationStatus.FAILURE, result.getStatus(contingencyState));
    }

}
